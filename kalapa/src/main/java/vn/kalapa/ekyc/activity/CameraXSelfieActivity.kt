package vn.kalapa.ekyc.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.KalapaCaptureHandler
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.KalapaSDKCallback
import vn.kalapa.ekyc.KalapaSDKMediaType
import vn.kalapa.ekyc.KalapaSDKResultCode
import vn.kalapa.ekyc.capturesdk.CameraXActivity
import vn.kalapa.ekyc.liveness.LivenessHandler
import vn.kalapa.ekyc.liveness.models.LivenessAction
import vn.kalapa.ekyc.managers.KLPFaceDetectorListener
import vn.kalapa.ekyc.toBitmap
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.KLPGifImageView
import vn.kalapa.ekyc.views.ProgressView
import vn.kalapa.ekyc.liveness.InputFace
import vn.kalapa.ekyc.liveness.LivenessSessionStatus

typealias InputImageListener = (inputImage: Bitmap) -> Unit

class CameraXSelfieActivity : CameraXActivity(
    activityLayoutId = R.layout.activity_camera_x_selfie,
    lensFacing = LENS_FACING.FRONT,
    true,
    100
), KalapaSDKCallback {
    private var isVerifySucceed = false
    private var faceDetected = false
    private lateinit var ivError: KLPGifImageView
    private lateinit var klpLivenessHandler: LivenessHandler

    //    private lateinit var ivBitmapReview: ImageView
    private var computingDetection = false
    private lateinit var tvTitle: TextView

    //    private lateinit var klpLivenessHandler: LivenessHandler
    private lateinit var ivPreviewImage: ImageView
    protected lateinit var faceBitmap: Bitmap
    private var errorTimestamp = System.currentTimeMillis()
    private var currentStatus: LivenessSessionStatus? = null
    private var currentMessage: String? = ""
    private val TAG = "CameraXSelfieActivity"
    private var NO_FACE_COUNT = 0
    override fun showEndEkyc() {
        Helpers.showEndKYC(this, object : DialogListener {
            override fun onYes() {
                KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
                finish()
            }

            override fun onNo() {

            }
        })
    }

    private fun setupFaceAnalyzer(): ImageAnalysis {
        var targetResolution = getOpticalResolution(CameraSelector.DEFAULT_FRONT_CAMERA, true)
        Helpers.printLog("setupFaceAnalyzer targetResolution $targetResolution")
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(targetResolution)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, FaceDetectionAnalyzer { inputImage ->
                    var croppedImage = BitmapUtil.crop(inputImage, inputImage.width, inputImage.width, 0.5f, 0.5f)
//                    runOnUiThread {
//                        ivBitmapReview.setImageBitmap(croppedImage)
//                    }
                    klpLivenessHandler.processSession(croppedImage, croppedImage, 1f, 0f)
                    computingDetection = true
                    while (true) {
//                        Helpers.printLog("Still Processing")
                        if (!computingDetection) {
//                            Helpers.printLog("Next Frame to process")
                            return@FaceDetectionAnalyzer
                        }
                        Thread.sleep(100)
                    }
                })
            }
    }

    override fun previewViewLayerMode(isCameraMode: Boolean) {
        super.previewViewLayerMode(isCameraMode)
        if (!isCameraMode) {
            Helpers.setBackgroundColorTintList(btnRetry, KalapaSDK.config.mainColor)
            btnRetry.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        } else {
            // Camera Mode
            tvError.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLivenessProcess()
    }

    private fun setupLivenessProcess() {
        val livenessVersion =
            if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.ACTIVE.version) Common.LIVENESS_VERSION.ACTIVE else if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.SEMI_ACTIVE.version) Common.LIVENESS_VERSION.SEMI_ACTIVE else Common.LIVENESS_VERSION.PASSIVE
        klpLivenessHandler = LivenessHandler(
            this,
            livenessVersion,
            object : KLPFaceDetectorListener {
                override fun onSuccessListener(faces: List<Face>) {
                    computingDetection = false
                }

                override fun onMessage(status: LivenessSessionStatus?, message: String?) {
                    computingDetection = false
                    if ((status == LivenessSessionStatus.VERIFIED || status == LivenessSessionStatus.EXPIRED) ||
                        (System.currentTimeMillis() - errorTimestamp > 1000) // 1s cập nhật 1 lần.
                    ) {
                        Helpers.printLog("$TAG onMessage: ${status?.name} - message: $message")
                        errorTimestamp = System.currentTimeMillis()
//                        tvError.text = getLivenessMessage(message)
                        val livenessIcon = getLivenessIcon(status, message)

                        runOnUiThread {
                            tvError.visibility = View.VISIBLE
                            tvError.text = getLivenessMessage(status, message, null)
                            ivError.visibility = View.VISIBLE
                            if (livenessIcon != null) ivError.setGifImageResource(livenessIcon)
                            else {
                                if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.SEMI_ACTIVE.version || KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.PASSIVE.version)
                                    ivError.visibility = View.INVISIBLE
                            }
                        }
                    }
                }

                override fun onFaceDetected(typicalFrame: Bitmap, typicalFace: Bitmap?) {
//                    Helpers.printLog("$TAG onFaceDetected")
                    computingDetection = false
                    faceDetected = true
                    Helpers.printLog("opticalResolution Frame: ${typicalFrame.width} ${typicalFrame.height}")
//                    takePhoto()
                    Handler(Looper.getMainLooper()).post {
                        stopCamera()
                    }
                    runOnUiThread {
                        if (typicalFace != null) {
//                            val matrix = Matrix()
//                            matrix.setScale(1f, -1f)
//                            matrix.postRotate(getCameraRotationDegree().toFloat())
//                            faceBitmap = typicalFrame
                            faceBitmap = BitmapUtil.rotateBitmapToStraight(typicalFrame, getCameraRotationDegree(), true)
//                            faceBitmap = BitmapUtil.crop(faceBitmap, faceBitmap.width, faceBitmap.width, 0.5f, 0.5f)
//                            faceBitmap = Bitmap.createBitmap(typicalFrame, 0, 0, typicalFrame.width, typicalFrame.height, matrix, false)
                            faceBitmap = BitmapUtil.resizeImageFromGallery(faceBitmap)
//                            ivBitmapReview.setImageBitmap(faceBitmap)
                            Helpers.printLog("opticalResolution Frame: ${faceBitmap.width} ${faceBitmap.height}")
//                            ivPreviewImage.setImageBitmap(faceBitmap)
                            isCapturedFaceOK()
                        } else
                            takePhoto()
                    }
                }

                override fun onExpired() {
                    computingDetection = false
                    cameraAnalyzer?.clearAnalyzer()
                    runOnUiThread {
                        previewViewLayerMode(false)
                        stopCamera()
                        btnNext.visibility = View.INVISIBLE
                        tvError.setTextColor(resources.getColor(R.color.ekyc_red))
                        tvError.visibility = View.VISIBLE
                        tvError.text =
                            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_error_timeout))
                        ivError.setGifImageResource(R.drawable.gif_error_small)
                    }
                }

            }, getCameraRotationDegree()
        )
    }

    override fun onCaptureSuccess(cameraDegree: Int) {
        // No Capture in this
        vibratePhone()
        val degreeDifferent = cameraDegree != getCameraRotationDegree()
        val rotation = if (degreeDifferent) {
            ((getCameraRotationDegree() - cameraDegree + 270) % 360)
        } else cameraDegree
//        Helpers.printLog("onCaptureSuccess $cameraDegree ${getCameraRotationDegree()} $rotation")
        faceBitmap = BitmapUtil.rotateBitmapToStraight(tmpBitmap!!, rotation, true) // tmpBitmap!! //
        Helpers.printLog("opticalResolution S: ${faceBitmap.width} ${faceBitmap.height} ${ivPreviewImage.width} ${ivPreviewImage.height} $cameraDegree ${getCameraRotationDegree()}  $rotation")
        if (faceBitmap.width > 1200 && faceBitmap.height > 1200)
            faceBitmap = BitmapUtil.resizeImageFromGallery(faceBitmap)
        Helpers.printLog("opticalResolution S: ${faceBitmap.width} ${faceBitmap.height} ${ivPreviewImage.width} ${ivPreviewImage.height} $cameraDegree ${getCameraRotationDegree()}  $rotation")
        isCapturedFaceOK()
        stopCamera()
    }

    private fun isCapturedFaceOK() {
        val faceDetectorOptions: FaceDetectorOptions = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.9f)
            .build()
        var faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)
        faceDetector.process(InputImage.fromBitmap(faceBitmap, 0))
            .addOnSuccessListener {
                var errorMessage: String
                ivError.setGifImageResource(R.drawable.gif_error_small)
                if (it.size == 0) {
                    // No face
                    Helpers.printLog("CameraXSelfieActivity onCaptureSuccess: No faces!")
                    errorMessage = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_not_center))
                    tvError.setTextColor(resources.getColor(R.color.ekyc_red))
                    btnNext.visibility = View.INVISIBLE
                } else {
                    var isFaceSizeJustBiggerThanTooSmall = 0
                    for (face in it) {
                        val inputFace = InputFace(System.currentTimeMillis(), face, faceBitmap.width, faceBitmap.height)
                        if (LivenessAction.isFaceSizeJustBiggerThanTooSmall(inputFace)) isFaceSizeJustBiggerThanTooSmall++
                    }

                    if (it.size > 1 && isFaceSizeJustBiggerThanTooSmall > 1) {
                        Helpers.printLog("CameraXSelfieActivity onCaptureSuccess: Too many faces!")
                        // More than one face
                        errorMessage = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_too_many_faces))
                        btnNext.visibility = View.INVISIBLE
                    } else {
                        if (LivenessAction.isFaceMarginRight(it[0], faceBitmap.width, faceBitmap.height, 0f, 0f)) {
                            Helpers.printLog("CameraXSelfieActivity onCaptureSuccess: OK!")
                            ivError.setGifImageResource(R.drawable.gif_success_small)
                            tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                            errorMessage = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_done_title))
                            btnNext.visibility = View.VISIBLE
                        } else {
                            btnNext.visibility = View.INVISIBLE
                            Helpers.printLog("CameraXSelfieActivity onCaptureSuccess: Not margin right!")
                            errorMessage = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_not_center))
                        }
                    }
                }
                // Only one face. Margin right or not
                ivPreviewImage.visibility = View.VISIBLE
                ivPreviewImage.setImageBitmap(faceBitmap)
                tvError.text = errorMessage
            }
    }


    override fun setupCustomUI() {
//        ivBitmapReview = findViewById(R.id.iv_bitmap_preview)
        this.ivError = findViewById(R.id.iv_error)
        ivError.setGifImageResource(if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.ACTIVE.version) R.drawable.gif_hold_steady else R.drawable.gif_success_small)
        this.tvTitle = findViewById(R.id.tv_title)
        this.ivPreviewImage = findViewById(R.id.iv_preview_image)
        this.ivError.visibility = View.INVISIBLE
        this.tvError.text = ""
        tvError.visibility = View.VISIBLE
//        this.ivError.visibility = View.INVISIBLE
        this.btnRetry.visibility = View.INVISIBLE
        this.btnNext.visibility = View.INVISIBLE
        Helpers.setBackgroundColorTintList(this.btnNext, KalapaSDK.config.mainColor)
        this.btnNext.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
        this.tvGuide.text = KalapaSDK.config.customSubTitle.ifEmpty {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_guide_title))
        }
        this.tvGuide.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        Helpers.setBackgroundColorTintList(this.btnCapture, KalapaSDK.config.mainColor)
        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
        tvTitle.text = KalapaSDK.config.customTitle.ifEmpty {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_liveness_detection))
        }
    }

    override fun setupAnalyzer(): ImageAnalysis? {
//        return null
        return setupFaceAnalyzer()
    }


    override fun verifyImage() {
//        stopCamera()
        runOnUiThread {
            ProgressView.showProgress(this@CameraXSelfieActivity)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            (KalapaSDK.handler as KalapaCaptureHandler).process(
                BitmapUtil.convertBitmapToBase64(faceBitmap),
                KalapaSDKMediaType.PORTRAIT,
                this
            )
        }, 100)
    }

    override fun onBackBtnClicked() {
        showEndEkyc()
    }

    private fun renewSession() {
        clearSession()
        startCamera()
//        Helpers.printLog("on retry clicked computingDetection $computingDetection")
    }

    private fun clearSession() {
        faceDetected = false
        klpLivenessHandler.renewSession()
        ivError.visibility = View.INVISIBLE
        ivPreviewImage.visibility = View.INVISIBLE
        cameraAnalyzer = setupFaceAnalyzer()
        computingDetection = false
    }

    override fun onRetryClicked() {
        previewViewLayerMode(true)
        renewSession()
        Helpers.printLog("On Back Clicked & Preview Mode... Finish Activity...")
    }

    override fun onResume() {
        super.onResume()
        clearSession()
    }

    override fun onInfoBtnClicked() {
    }

    class FaceDetectionAnalyzer(private val listener: InputImageListener) : ImageAnalysis.Analyzer {

        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(image: ImageProxy) {
            image.toBitmap()?.let { listener(it) }
            image.close()
        }
    }

    override fun sendError(message: String?) {
        Helpers.printLog("Failed! ")
        ProgressView.hideProgress()
        isVerifySucceed = false
        this.runOnUiThread {
            tvError.visibility = View.VISIBLE
            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
//            cardMaskView.dashColor = resources.getColor(R.color.ekyc_red)
            btnNext.visibility = View.INVISIBLE
            ivError.setGifImageResource(R.drawable.gif_error_small)
            tvError.text = message
                ?: KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_processing_failed))
        }
        Helpers.printLog("onError message: $message")
    }

    override fun sendDone(nextAction: () -> Unit) {
        nextAction()
        runOnUiThread {
            ProgressView.hideProgress()
        }
        Helpers.printLog("CameraXSelfieActivity onFinish")
        finish()
    }

    private var currAction = ""
    private fun getLivenessIcon(status: LivenessSessionStatus?, input: String?): Int? {
        Helpers.printLog("Status: $status Message $input")
        if (status == LivenessSessionStatus.PROCESSING && input != null && currAction != input) {
            currAction = input
            vibratePhone()
        }
        if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.SEMI_ACTIVE.version || KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.PASSIVE.version) {
            return when (status) {
                // REMOVE VERIFIED ICON
//                LivenessSessionStatus.VERIFIED -> R.drawable.gif_success_small
                else -> null
            }
        }
        return when (status) {
            // REMOVE VERIFIED ICON
//            LivenessSessionStatus.VERIFIED -> {
//                return R.drawable.gif_success_small
//            }

            LivenessSessionStatus.UNVERIFIED -> {
//                setCircleViewAnimation(AnimStatus.ANIM_LOADING)
                return R.drawable.gif_hold_steady
            }

            LivenessSessionStatus.PROCESSING -> {

                return when (input) {
                    "" -> {
//                        setCircleViewAnimation(AnimStatus.ANIM_PROCESSING)
                        return null
                    }

                    "TurnLeft" -> R.drawable.gif_turn_left
                    "TurnRight" -> R.drawable.gif_turn_right
                    "TurnUp", "LookUp" -> R.drawable.gif_turn_up
                    "TurnDown", "LookDown" -> R.drawable.gif_turn_down
                    "TiltLeft" -> R.drawable.gif_tilt_left
                    "TiltRight" -> R.drawable.gif_tilt_right
                    "NodeHead" -> R.drawable.gif_turn_down
                    "EyeBlink" -> R.drawable.gif_hold_steady
                    "ShakeHead" -> R.drawable.gif_hold_steady
                    else -> R.drawable.gif_hold_steady
                }
            }

            LivenessSessionStatus.TOO_SMALL -> {
                if (input == "ComeClose") {
                    vibratePhone()
                    setCircleViewAnimation(AnimStatus.ANIM_LOADING)
                    return R.drawable.gif_hold_steady
                } else {
                    setCircleViewAnimation(AnimStatus.ANIM_LOADING)
                    return null
                }
            }

            LivenessSessionStatus.TOO_LARGE -> {
                setCircleViewAnimation(AnimStatus.ANIM_LOADING)
                null
            }

            LivenessSessionStatus.EXPIRED -> {
                setCircleViewAnimation(AnimStatus.ANIM_FAILED)
                R.drawable.gif_error_small
            }

            LivenessSessionStatus.FAILED -> {
                setCircleViewAnimation(AnimStatus.ANIM_FAILED)
                R.drawable.gif_error_small
            }

            else -> {
                setCircleViewAnimation(AnimStatus.ANIM_LOADING)
                R.drawable.gif_hold_steady
            }
        }
    }

    private fun vibratePhone() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    private fun setCircleViewAnimation(status: AnimStatus) {
//        Helpers.printLog("Set Anim: ${status.name}")
//        runOnUiThread {
//            when (status) {
//                AnimStatus.ANIM_SUCCESS -> cardMaskView.dashColor = resources.getColor(R.color.ekyc_green)
//                AnimStatus.ANIM_WARNING -> cardMaskView.dashColor = resources.getColor(R.color.ekyc_red)
//                AnimStatus.ANIM_FAILED -> cardMaskView.dashColor = resources.getColor(R.color.ekyc_red)
//                AnimStatus.ANIM_LOADING -> cardMaskView.dashColor = resources.getColor(R.color.white)
//                else -> cardMaskView.dashColor = Color.parseColor(KalapaSDK.config.mainColor)
//            }
//        }
    }


    private fun getLivenessMessage(
        status: LivenessSessionStatus?,
        input: String?,
        errCode: Int?
    ): String? {
        tvError.setTextColor(resources.getColor(R.color.ekyc_red))
        if (status == LivenessSessionStatus.NO_FACE)
            NO_FACE_COUNT++
        else
            NO_FACE_COUNT = 0
        if (status != null && status == LivenessSessionStatus.EXPIRED) {
            return KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_error_timeout))
//            "Phiên xác thực hết hạn, vui lòng thử lại"
        } else return if (status == LivenessSessionStatus.FAILED && input != "Processing") {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_processing_failed))
        } else return if (status == LivenessSessionStatus.TOO_LARGE) {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_too_close))
        } else return if (status == LivenessSessionStatus.TOO_SMALL) {
            if (input == "ComeClose") {
                tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_move_closer))
            } else
                return KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_too_far))
        } else return if (status == LivenessSessionStatus.TOO_MANY_FACES) {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_too_many_faces))
        } else return if (status == LivenessSessionStatus.NO_FACE && NO_FACE_COUNT > 3) {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_no_face))
        } else return if (status == LivenessSessionStatus.OFF_CENTER) {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_not_center))
        } else return if (status == LivenessSessionStatus.ANGLE_NOT_CORRECT) {
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_angle_not_correct))
        } else { // PROCESSING
            setCircleViewAnimation(AnimStatus.ANIM_LOADING)
            tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            Helpers.printLog("PROCESSING input $input")
            val s = when (input) { // PROCESSING.
                "" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_look_straight)) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "HoldSteady3Seconds" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_look_straight
                    )
                ) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "HoldSteady2Seconds" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_look_straight
                    )
                ) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "TurnLeft" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_turnleft)) // "Quay trái")
                "TurnRight" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_turnright
                    )
                )// "Quay phải")
                "TiltLeft" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_tiltleft))// "Nghiêng trái")
                "TiltRight" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_tiltright
                    )
                ) // "Nghiêng phải")
                "LookUp", "TurnUp" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_turnup
                    )
                ) //"Quay lên")
                "TurnDown", "LookDown" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_turndown
                    )
                )//"Quay xuống")
                "GoFar" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_too_close)) //"Tiến mặt lại gần camera hơn một chút")
                "ComeClose" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_move_closer
                    )
                ) //"Lùi mặt ra xa khỏi camera một chút")
                "HoldSteady2Second" -> KalapaSDK.config.languageUtils.getLanguageString(
                    resources.getString(
                        R.string.klp_liveness_look_straight
                    )
                ) //"Giữ đầu ngay ngo       ắn, nhìn thẳng trong 2 giây")
//                "EyeBlink" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_eyeblink) //"Nháy mắt")
//                "ShakeHead" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_shakehead) //"Lắc đầu")
//                "NodeHead" -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_nodhead) //"Gật đầu")
                "Success" -> {
//                if (Kalapa.livenessVersion == 3) {
//                    setCircleViewAnimation(LivenessActivity.AnimStatus.ANIM_VERIFIED)
//                    return resources.getString(R.string.klp_liveness_capture_success)
//                } else {
//                    currentStep++
//                    setCircleViewAnimation(LivenessActivity.AnimStatus.ANIM_PROCESSING, currentStep % 3, 3)
//                    return resources.getString(R.string.klp_liveness_success)
//                }
                    tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                    setCircleViewAnimation(AnimStatus.ANIM_PROCESSING)
                    return KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_done_title))
                }
                //"Thành công!"
                "Connecting" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_please_wait)) //"Đang kết nối...")
                }

                "Initializing" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_please_wait))
                } //"Đang khởi tạo..."
                "Processing" -> {
                    if (status != null && status == LivenessSessionStatus.VERIFIED) {
                        // REMOVE VERIFIED ICON
                        ivError.setGifImageResource(R.drawable.gif_success_small)
                        tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                        return if (errCode != null) input else KalapaSDK.config.languageUtils.getLanguageString(
                            resources.getString(R.string.klp_liveness_look_straight)
                        )
                    } // "Xác thực thành công"
                    if (status != null && status == LivenessSessionStatus.FAILED) {
                        return KalapaSDK.config.languageUtils.getLanguageString(
                            resources.getString(
                                R.string.klp_liveness_processing_failed
                            )
                        )
                    } // "Xác thực thất bại"
                    else KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_please_wait)) // "Đang xác thực...")
                }

                "ConnectionFailed" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))

                    ivError.setGifImageResource(R.drawable.gif_error_small)
                    return KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_error_network)) //"Kết nối thất bại")
                }

                "Finished" -> {
                    tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                    ivError.setGifImageResource(R.drawable.gif_success_small)
                    return KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_processing_failed)) //"Hoàn thành!")
                }

                else -> input
            }
            s
        }
    }
}