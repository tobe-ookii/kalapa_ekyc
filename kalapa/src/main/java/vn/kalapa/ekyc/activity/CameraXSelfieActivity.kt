package vn.kalapa.ekyc.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.material.card.MaterialCardView
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
import vn.kalapa.ekyc.liveness.LivenessHandler
import vn.kalapa.ekyc.liveness.models.LivenessAction
import vn.kalapa.ekyc.managers.KLPFaceDetectorListener
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.KLPGifImageView
import vn.kalapa.ekyc.views.ProgressView
import vn.kalapa.ekyc.liveness.InputFace
import vn.kalapa.ekyc.liveness.LivenessSessionStatus
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.utils.Common.vibratePhone
import vn.kalapa.ekyc.utils.LIVENESS_VERSION
import vn.kalapa.ekyc.utils.toBitmap

typealias InputImageListener = (inputImage: Bitmap) -> Unit

class CameraXSelfieActivity : CameraXActivity(
    activityLayoutId = R.layout.activity_camera_x_selfie,
    lensFacing = LENS_FACING.FRONT,
    true,
    100
), KalapaSDKCallback {

    companion object {
        internal var faceData: String? = null
    }

    private var isVerifySucceed = false
    private var faceDetected = false
    private lateinit var ivError: KLPGifImageView
    private lateinit var klpLivenessHandler: LivenessHandler
    private var faceMaskColor = "#FFFFFF"

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
    private lateinit var ivFaceMask: ImageView
    private lateinit var cardviewBorder: MaterialCardView
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


    private fun getIntentData() {
        try {
            Helpers.printLog("getIntentData faceData $faceData ${Common.checkIfImageOrStorageIsGranted(this@CameraXSelfieActivity, true)}")
            if (!faceData.isNullOrEmpty()) {
                stopCamera()
                faceBitmap = BitmapUtil.base64ToBitmap(faceData!!)
                isCapturedFaceOK()
                verifyImage()
            } else
                setupLivenessProcess()
        } catch (e: Exception) {
            Helpers.printLog("getIntentData exception ${e.localizedMessage}")
            setupLivenessProcess()
        }

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
                    if (this::klpLivenessHandler.isInitialized)
                        klpLivenessHandler.processSession(croppedImage, croppedImage)
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
        if (!isCameraMode) {
            btnNext.visibility = View.VISIBLE
            btnRetry.visibility = View.VISIBLE
            holderCapture.visibility = View.INVISIBLE
            this.isCameraMode = false
            Helpers.setBackgroundColorTintList(btnRetry, KalapaSDK.config.mainColor)
            btnRetry.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        } else {
            // Camera Mode
            holderCapture.visibility = View.VISIBLE
            btnNext.visibility = View.INVISIBLE
            btnRetry.visibility = View.INVISIBLE
            tvError.visibility = View.VISIBLE
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getIntentData()
    }

    private fun setupLivenessProcess() {
        val livenessVersion =
            if (KalapaSDK.config.livenessVersion == LIVENESS_VERSION.ACTIVE.version) LIVENESS_VERSION.ACTIVE else if (KalapaSDK.config.livenessVersion == LIVENESS_VERSION.SEMI_ACTIVE.version) LIVENESS_VERSION.SEMI_ACTIVE else LIVENESS_VERSION.PASSIVE
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
                            Helpers.setColorTintList(ivFaceMask, faceMaskColor)
                            ivError.visibility = View.VISIBLE
                            if (livenessIcon != null) ivError.setGifImageResource(livenessIcon)
                            else {
                                if (KalapaSDK.config.livenessVersion == LIVENESS_VERSION.SEMI_ACTIVE.version || KalapaSDK.config.livenessVersion == LIVENESS_VERSION.PASSIVE.version)
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
                            Helpers.fadeOut(this@CameraXSelfieActivity, ivFaceMask) {
                                ivFaceMask.visibility = View.INVISIBLE
                            }
                            faceBitmap = BitmapUtil.resizeImageFromGallery(faceBitmap)
//                            ivBitmapReview.setImageBitmap(faceBitmap)
                            Helpers.printLog("opticalResolution Frame: ${faceBitmap.width} ${faceBitmap.height}")
//                            ivPreviewImage.setImageBitmap(faceBitmap)
                            isCapturedFaceOK()
                        } else
                            takePhoto()
                    }
                }

                @SuppressLint("ResourceType")
                override fun onExpired() {
                    computingDetection = false
                    cameraAnalyzer?.clearAnalyzer()
                    runOnUiThread {
                        Helpers.transitionFromColorToColor(cardviewBorder, "#CECECE", resources.getString(R.color.ekyc_red))
                        previewViewLayerMode(false)
                        stopCamera()
                        btnNext.visibility = View.INVISIBLE
                        tvError.setTextColor(resources.getColor(R.color.ekyc_red))
                        tvError.visibility = View.VISIBLE
                        tvError.text =
                            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_error_timeout))
                        ivError.setGifImageResource(R.drawable.gif_error_small)
                    }
                }

            }, getCameraRotationDegree()
        )
    }

    override fun onCaptureSuccess(cameraDegree: Int) {
        // No Capture in this
        vibratePhone(this)
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
                    errorMessage = KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_center))
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
                        errorMessage = KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_many_faces))
                        btnNext.visibility = View.INVISIBLE
                    } else {
                        if (LivenessAction.isFaceMarginRight(it[0], faceBitmap.width, faceBitmap.height)) {
                            Helpers.printLog("CameraXSelfieActivity onCaptureSuccess: OK!")
                            ivError.setGifImageResource(R.drawable.gif_success_small)
                            tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                            errorMessage = KLPLanguageManager.get(resources.getString(R.string.klp_done_title))
                            btnNext.visibility = View.VISIBLE
                        } else {
                            btnNext.visibility = View.INVISIBLE
                            Helpers.printLog("CameraXSelfieActivity onCaptureSuccess: Not margin right!")
                            errorMessage = KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_center))
                        }
                    }
                }
                // Only one face. Margin right or not
                ivPreviewImage.visibility = View.VISIBLE
                ivPreviewImage.setImageBitmap(faceBitmap)
                tvError.text = errorMessage
                Helpers.transitionFromColorToColor(cardviewBorder, "#CECECE", faceMaskColor)
            }
    }


    override fun setupCustomUI() {
//        ivBitmapReview = findViewById(R.id.iv_bitmap_preview)
        this.ivError = findViewById(R.id.iv_error)
        ivFaceMask = findViewById(R.id.iv_face_mask)
        cardviewBorder = findViewById(R.id.cardview_border)

        ivError.setGifImageResource(if (KalapaSDK.config.livenessVersion == LIVENESS_VERSION.ACTIVE.version) R.drawable.gif_hold_steady else R.drawable.gif_success_small)
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
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_default))
        }
        this.tvGuide.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        Helpers.setBackgroundColorTintList(this.btnCapture, KalapaSDK.config.mainColor)
        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
        tvTitle.text = KalapaSDK.config.customTitle.ifEmpty {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_title))
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
        if (!this::klpLivenessHandler.isInitialized)
            setupLivenessProcess()
        clearSession()
        startCamera()
//        Helpers.printLog("on retry clicked computingDetection $computingDetection")
    }

    private fun clearSession() {
        Helpers.fadeIn(this@CameraXSelfieActivity, ivFaceMask) {
            runOnUiThread {
                ivFaceMask.visibility = View.VISIBLE
                Helpers.transitionFromColorToColor(cardviewBorder, "#FFFFFF", "#CECECE")
            }
        }
        if (this::klpLivenessHandler.isInitialized) {
            faceDetected = false
            klpLivenessHandler.renewSession()
            ivError.visibility = View.INVISIBLE
            ivPreviewImage.visibility = View.INVISIBLE
            cameraAnalyzer = setupFaceAnalyzer()
            computingDetection = false
        }
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
            btnRetry.visibility = View.VISIBLE
            ivError.setGifImageResource(R.drawable.gif_error_small)
            tvError.text = message
                ?: KLPLanguageManager.get(resources.getString(R.string.klp_liveness_result_fail))
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
            vibratePhone(this)

        }
        if (KalapaSDK.config.livenessVersion == LIVENESS_VERSION.SEMI_ACTIVE.version || KalapaSDK.config.livenessVersion == LIVENESS_VERSION.PASSIVE.version) {
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
                    vibratePhone(this)
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

    private enum class AnimStatus {
        ANIM_LOADING, ANIM_FAILED, ANIM_PROCESSING
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


    @SuppressLint("ResourceType")
    private fun getLivenessMessage(
        status: LivenessSessionStatus?,
        input: String?,
        errCode: Int?
    ): String? {
        tvError.setTextColor(resources.getColor(R.color.ekyc_red))
        faceMaskColor = resources.getString(R.color.ekyc_red)

        if (status == LivenessSessionStatus.NO_FACE)
            NO_FACE_COUNT++
        else
            NO_FACE_COUNT = 0
        if (status != null && status == LivenessSessionStatus.EXPIRED) {
            return KLPLanguageManager.get(resources.getString(R.string.klp_liveness_error_timeout))
//            "Phiên xác thực hết hạn, vui lòng thử lại"
        } else return if (status == LivenessSessionStatus.FAILED && input != "Processing") {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_result_fail))
        } else return if (status == LivenessSessionStatus.TOO_LARGE) {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_too_close))
        } else return if (status == LivenessSessionStatus.TOO_SMALL) {
            if (input == "ComeClose") {
                tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                faceMaskColor = resources.getString(R.color.white)
                KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_too_far))
            } else
                return KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_too_far))
        } else return if (status == LivenessSessionStatus.TOO_MANY_FACES) {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_many_faces))
        } else return if (status == LivenessSessionStatus.NO_FACE && NO_FACE_COUNT > 3) {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_noface))
        } else return if (status == LivenessSessionStatus.OFF_CENTER) {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_center))
        } else return if (status == LivenessSessionStatus.ANGLE_NOT_CORRECT) {
            KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_angle))
        } else { // PROCESSING
            setCircleViewAnimation(AnimStatus.ANIM_LOADING)
            tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            Helpers.printLog("PROCESSING input $input")
            faceMaskColor = if (input.isNullOrEmpty()) resources.getString(R.color.white) else KalapaSDK.config.mainColor

            val s = when (input) { // PROCESSING.
                "" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_look_straight)) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "HoldSteady3Seconds" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_look_straight
                    )
                ) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "HoldSteady2Seconds" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_look_straight
                    )
                ) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "TurnLeft" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_turn_left)) // "Quay trái")
                "TurnRight" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_turn_right
                    )
                )// "Quay phải")
                "TiltLeft" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_tilt_left))// "Nghiêng trái")
                "TiltRight" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_tilt_right
                    )
                ) // "Nghiêng phải")
                "LookUp", "TurnUp" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_turn_up
                    )
                ) //"Quay lên")
                "TurnDown", "LookDown" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_turn_down
                    )
                )//"Quay xuống")
                "GoFar" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_message_too_close)) //"Tiến mặt lại gần camera hơn một chút")
                "ComeClose" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_move_closer
                    )
                ) //"Lùi mặt ra xa khỏi camera một chút")
                "HoldSteady2Second" -> KLPLanguageManager.get(
                    resources.getString(
                        R.string.klp_liveness_message_look_straight
                    )
                ) //"Giữ đầu ngay ngo       ắn, nhìn thẳng trong 2 giây")
//                "EyeBlink" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_eyeblink) //"Nháy mắt")
//                "ShakeHead" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_shakehead) //"Lắc đầu")
//                "NodeHead" -> KLPLanguageManager.get(resources.getString(R.string.klp_liveness_nodhead) //"Gật đầu")
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
                    faceMaskColor = KalapaSDK.config.mainColor//resources.getString(R.color.ekyc_green)

                    setCircleViewAnimation(AnimStatus.ANIM_PROCESSING)
                    return KLPLanguageManager.get(resources.getString(R.string.klp_done_title))
                }
                //"Thành công!"
                "Connecting" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    faceMaskColor = resources.getString(R.color.white)
                    KLPLanguageManager.get(resources.getString(R.string.klp_please_wait)) //"Đang kết nối...")
                }

                "Initializing" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    faceMaskColor = resources.getString(R.color.white)
                    KLPLanguageManager.get(resources.getString(R.string.klp_please_wait))
                } //"Đang khởi tạo..."
                "Processing" -> {
                    if (status != null && status == LivenessSessionStatus.VERIFIED) {
                        // REMOVE VERIFIED ICON
                        ivError.setGifImageResource(R.drawable.gif_success_small)
                        tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                        faceMaskColor = KalapaSDK.config.mainColor // resources.getString(R.color.ekyc_green)

                        return if (errCode != null) input else KLPLanguageManager.get(
                            resources.getString(R.string.klp_liveness_message_look_straight)
                        )
                    } // "Xác thực thành công"
                    if (status != null && status == LivenessSessionStatus.FAILED) {
                        return KLPLanguageManager.get(
                            resources.getString(
                                R.string.klp_liveness_result_fail
                            )
                        )
                    } // "Xác thực thất bại"
                    else KLPLanguageManager.get(resources.getString(R.string.klp_please_wait)) // "Đang xác thực...")
                }

                "ConnectionFailed" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    faceMaskColor = resources.getString(R.color.white)
                    ivError.setGifImageResource(R.drawable.gif_error_small)
                    return KLPLanguageManager.get(resources.getString(R.string.klp_error_network)) //"Kết nối thất bại")
                }

                "Finished" -> {
                    tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                    ivError.setGifImageResource(R.drawable.gif_success_small)
                    faceMaskColor = KalapaSDK.config.mainColor //resources.getString(R.color.ekyc_green)
                    return KLPLanguageManager.get(resources.getString(R.string.klp_liveness_result_fail)) //"Hoàn thành!")
                }

                else -> input
            }
            s
        }
    }
}