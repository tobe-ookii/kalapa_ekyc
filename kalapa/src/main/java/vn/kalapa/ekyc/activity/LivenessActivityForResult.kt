package vn.kalapa.ekyc.activity

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.*
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.google.mlkit.vision.face.Face
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.liveness.LivenessHandler
import vn.kalapa.ekyc.managers.KLPFaceDetectorListener
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.Res
import vn.kalapa.ekyc.views.KLPGifImageView
import vn.kalapa.ekyc.views.ProgressView
import vn.kalapa.ekyc.liveness.LivenessSessionStatus

class LivenessActivityForResult : CameraActivity(R.layout.activity_camera_liveness_no_step, LENS_FACING.FRONT, hideAutoCapture = true, refocusFrequency = 10000), KalapaSDKCallback {
    private var showingGuide = false
    private val TEXT_SIZE_DIP = 10f
    private var isVerifySucceed = false
    private var rgbFrameBitmap: Bitmap? = null
    private var faceDetected = false
    private val TAG = "LivenessActivity"
    private lateinit var klpLivenessHandler: LivenessHandler
    private var currentStatus: LivenessSessionStatus? = null
    private var currentMessage: String? = ""
    private val URL_PATH = "/api/kyc/app/check-selfie"
    private var computingDetection: Boolean = false
    var errorTimestamp = System.currentTimeMillis()
    private lateinit var res: Res
    private var session: String? = null
    private var mrz: String? = null
    private var flowType: String? = null
    var isFinishedConfirm = false
    var transactionId: String? = null
    private lateinit var ivError: KLPGifImageView

    //    lateinit var ivTestImage: ImageView
    override fun getResources(): Resources {
        res = Res(
            super.getResources(),
            KalapaSDK.config.mainColor,
            KalapaSDK.config.mainTextColor,
            KalapaSDK.config.backgroundColor
        )
        return res
    }

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

    override fun onBackBtnClicked() {
        if (showingGuide) {
            supportFragmentManager.popBackStack()
            showingGuide = false
            return
        }
        if (isCameraMode) {
            Helpers.printLog("On Back Clicked... Finish Activity...")

            Helpers.showEndKYC(this, object : DialogListener {
                override fun onYes() {
                    KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
                    finish()
                }

                override fun onNo() {

                }
            })
        } else {
            previewViewLayerMode(true)
            renewSession()
            Helpers.printLog("On Back Clicked & Preview Mode... Finish Activity...")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mrz = intent.getStringExtra("mrz")
        transactionId = intent.getStringExtra("transaction_id")
        val livenessVersion = if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.ACTIVE.version) Common.LIVENESS_VERSION.ACTIVE else if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.SEMI_ACTIVE.version) Common.LIVENESS_VERSION.SEMI_ACTIVE else Common.LIVENESS_VERSION.PASSIVE
        klpLivenessHandler = LivenessHandler(this,
            livenessVersion,
            object : KLPFaceDetectorListener {
                override fun onSuccessListener(faces: List<Face>) {
                    computingDetection = false
                }

                override fun onMessage(status: LivenessSessionStatus?, message: String?) {
                    computingDetection = false

                    if ((status == LivenessSessionStatus.VERIFIED || status == LivenessSessionStatus.EXPIRED) ||
                        ((status != currentStatus || currentMessage != message) && System.currentTimeMillis() - errorTimestamp > 1000 && status != LivenessSessionStatus.NO_FACE)
                    ) {
                        Helpers.printLog("$TAG onMessage: ${status?.name} - message: $message")
                        errorTimestamp = System.currentTimeMillis()
//                        tvError.text = getLivenessMessage(message)
                        val livenessIcon = getLivenessIcon(status, message)

                        runOnUiThread {
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
                    Helpers.printLog("$TAG onFaceDetected")
                    computingDetection = false
                    faceDetected = true
                    runOnUiThread {
                        setCircleViewAnimation(AnimStatus.ANIM_SUCCESS)
                        previewViewLayerMode(false)
                        ivPreviewImage.visibility = View.VISIBLE
                        val matrix = Matrix()
                        // Mirror is basically a rotation
                        matrix.setScale(1f, -1f)
                        matrix.postRotate(getCameraRotationDegree().toFloat())
                        // so you got to move your bitmap back to it's place. otherwise you will not see it
//                        ivError.setGifImageResource(R.drawable.gif_success)
                        tmpBitmap =
                            Bitmap.createBitmap(
                                typicalFrame,
                                0,
                                0,
                                typicalFrame.width,
                                typicalFrame.height,
                                matrix,
                                false
                            )
                        if (typicalFace != null) {
                            faceBitmap = typicalFace
//                            ivTestImage.setImageBitmap(
//                                faceBitmap
//                            )
                        }
                        ivPreviewImage.setImageBitmap(
                            tmpBitmap
                        )


//                        removeErrorMessage()
//                        tvError.setTextColor(res.getColor(R.color.ekyc_green))
//                        tvError.visibility = View.VISIBLE
//                        tvError.text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_message_face_autocaptured_succeed))
                    }
                    // New way
                    // Old way
//                    captureImage()

                }

                override fun onExpired() {
                    runOnUiThread {
                        previewViewLayerMode(false)
                        btnNext.visibility = View.GONE
                        tvError.setTextColor(res.getColor(R.color.ekyc_red))
                        tvError.text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_error_timeout))
                        ivError.setGifImageResource(R.drawable.gif_error_small)
                    }
                }

            })
    }

    private val DESIRED_PREVIEW_SIZE = Size(320, 320)

    override fun onBackPressed() {
        onBackBtnClicked()
    }


    override fun onResume() {
        super.onResume()
        Helpers.printLog("LivenessActivityForResult onResume")
        renewSession()
    }

    override fun onPause() {
        super.onPause()
        computingDetection = false
    }

    override fun onEmulatorDetected() {
        Helpers.showDialog(
            this@LivenessActivityForResult,
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_warning_title)),
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_emulator_warning_body)), R.drawable.frowning_face
        ) {
            KalapaSDK.handler.onError(KalapaSDKResultCode.EMULATOR_DETECTED)
            finish()
        }
    }

    private fun renewSession() {
        cardMaskView.dashColor = res.getColor(R.color.white)
        faceDetected = false
        computingDetection = false
        klpLivenessHandler.renewSession()
        ivError.visibility = View.INVISIBLE
        Helpers.printLog("on retry clicked computingDetection $computingDetection")
    }

    override fun onRetryClicked() {
        renewSession()
    }

    override fun updateActiveModel() {
        TODO("Not yet implemented")
    }

    override fun setupCustomUI() {
//        ivTestImage = findViewById(R.id.iv_test_image)
        this.ivError = findViewById(R.id.iv_error)
        this.ivError.visibility = View.INVISIBLE
        this.tvError.text = ""
//        this.ivError.visibility = View.INVISIBLE
        this.btnRetry.visibility = View.INVISIBLE
        this.btnNext.visibility = View.INVISIBLE
        Helpers.setBackgroundColorTintList(this.btnNext, KalapaSDK.config.mainColor)
        this.btnNext.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))

        this.tvGuide.text = KalapaSDK.config.customSubTitle.ifEmpty { KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_guide_title)) }
        this.tvGuide.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        Helpers.setBackgroundColorTintList(this.btnCapture, KalapaSDK.config.mainColor)
        findViewById<TextView>(R.id.tv_title).text = KalapaSDK.config.customTitle.ifEmpty { KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_title_liveness_detection)) }
    }

    private fun setCircleViewAnimation(status: AnimStatus) {
//        Helpers.printLog("Set Anim: ${status.name}")
        runOnUiThread {
            when (status) {
                AnimStatus.ANIM_SUCCESS -> cardMaskView.dashColor = res.getColor(R.color.ekyc_green)
                AnimStatus.ANIM_WARNING -> cardMaskView.dashColor = res.getColor(R.color.ekyc_red)
                AnimStatus.ANIM_FAILED -> cardMaskView.dashColor = res.getColor(R.color.ekyc_red)
                AnimStatus.ANIM_LOADING -> cardMaskView.dashColor = res.getColor(R.color.white)
                else -> cardMaskView.dashColor = Color.parseColor(KalapaSDK.config.mainColor)
            }
        }
    }


    override fun processImage() {
        readyForNextImage()
        if (computingDetection) {
            return
        }
        runInBackground {
            computingDetection = true
            rgbFrameBitmap =
                Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
            rgbFrameBitmap!!.setPixels(
                getRgbBytes(),
                0,
                previewWidth,
                0,
                0,
                previewWidth,
                previewHeight
            )
            Handler().postDelayed({
                tmpBitmap = cardMaskView.crop(rgbFrameBitmap!!, getCameraRotationDegree())
                tmpBitmap = BitmapUtil.rotateBitmapToStraight(rgbFrameBitmap!!, getCameraRotationDegree())
//                runOnUiThread {
//                    ivTestImage.setImageBitmap(tmpBitmap)
//                }
                klpLivenessHandler.processSession(rgbFrameBitmap!!, tmpBitmap, cardMaskView.transOff, cardMaskView.translationY)

            }, 50)
        }
    }

    override fun verifyImage() {
        runOnUiThread {
            ProgressView.showProgress(this@LivenessActivityForResult)
        }
        Handler().postDelayed({
            (KalapaSDK.handler as KalapaCaptureHandler).process(BitmapUtil.convertBitmapToBase64(BitmapUtil.resizeBitmapToBitmap(faceBitmap)), KalapaSDKMediaType.PORTRAIT, this)
        }, 100)

    }

    fun getLivenessIcon(status: LivenessSessionStatus?, input: String?): Int? {
        Helpers.printLog("Status: $status Message $input")
        if (status == LivenessSessionStatus.PROCESSING)
            vibratePhone()
        if (KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.SEMI_ACTIVE.version || KalapaSDK.config.livenessVersion == Common.LIVENESS_VERSION.PASSIVE.version) {
            return when (status) {
                LivenessSessionStatus.VERIFIED -> R.drawable.gif_success_small
                else -> null
            }
        }
        return when (status) {
            LivenessSessionStatus.VERIFIED -> {
                return R.drawable.gif_success_small
            }

            LivenessSessionStatus.UNVERIFIED -> {
                setCircleViewAnimation(AnimStatus.ANIM_LOADING)
                return R.drawable.gif_hold_steady
            }

            LivenessSessionStatus.PROCESSING -> {

                return when (input) {
                    "" -> {
                        setCircleViewAnimation(AnimStatus.ANIM_PROCESSING)
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

    var NO_FACE_COUNT = 0
    fun getLivenessMessage(status: LivenessSessionStatus?, input: String?, errCode: Int?): String? {
        this.currentStatus = status
        this.currentMessage = input
        tvError.setTextColor(res.getColor(R.color.ekyc_red))
        if (status == LivenessSessionStatus.NO_FACE)
            NO_FACE_COUNT++
        else
            NO_FACE_COUNT = 0
        if (status != null && status == LivenessSessionStatus.EXPIRED) {
            return KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_error_timeout))
//            "Phiên xác thực hết hạn, vui lòng thử lại"
        } else return if (status == LivenessSessionStatus.FAILED && input != "Processing") {
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_processing_failed))
        } else return if (status == LivenessSessionStatus.TOO_LARGE) {
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_too_close))
        } else return if (status == LivenessSessionStatus.TOO_LARGE) {
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_eye_closes))
        } else return if (status == LivenessSessionStatus.TOO_SMALL) {
            if (input == "ComeClose") {
                tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_guide_liveness_closer_face))
            } else
                return KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_too_far))
        } else return if (status == LivenessSessionStatus.TOO_MANY_FACES) {
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_too_many_faces))
        } else return if (status == LivenessSessionStatus.NO_FACE && NO_FACE_COUNT > 3) {
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_guide_liveness_no_face))
        } else return if (status == LivenessSessionStatus.OFF_CENTER) {
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_guide_liveness_no_face))
        } else { // PROCESSING
            setCircleViewAnimation(AnimStatus.ANIM_LOADING)
            tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            Helpers.printLog("PROCESSING input $input")
            val s = when (input) { // PROCESSING.
                "" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_look_straight)) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "HoldSteady3Seconds" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_look_straight)) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "HoldSteady2Seconds" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_look_straight)) // "Giữ đầu ngay ngắn, nhìn thẳng trong 3 giây")
                "TurnLeft" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_turnleft)) // "Quay trái")
                "TurnRight" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_turnright))// "Quay phải")
                "TiltLeft" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_tiltleft))// "Nghiêng trái")
                "TiltRight" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_tiltright)) // "Nghiêng phải")
                "LookUp", "TurnUp" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_turnup)) //"Quay lên")
                "TurnDown", "LookDown" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_turndown))//"Quay xuống")
                "GoFar" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_too_close)) //"Tiến mặt lại gần camera hơn một chút")
                "ComeClose" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_guide_liveness_closer_face)) //"Lùi mặt ra xa khỏi camera một chút")
                "HoldSteady2Second" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_look_straight)) //"Giữ đầu ngay ngo       ắn, nhìn thẳng trong 2 giây")
//                "EyeBlink" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_eyeblink) //"Nháy mắt")
//                "ShakeHead" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_shakehead) //"Lắc đầu")
//                "NodeHead" -> KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_nodhead) //"Gật đầu")
                "Success" -> {
//                if (Kalapa.livenessVersion == 3) {
//                    setCircleViewAnimation(LivenessActivity.AnimStatus.ANIM_VERIFIED)
//                    return resources.getString(R.string.klp_liveness_capture_success)
//                } else {
//                    currentStep++
//                    setCircleViewAnimation(LivenessActivity.AnimStatus.ANIM_PROCESSING, currentStep % 3, 3)
//                    return resources.getString(R.string.klp_liveness_success)
//                }
                    tvError.setTextColor(res.getColor(R.color.ekyc_green))
                    setCircleViewAnimation(AnimStatus.ANIM_PROCESSING)
                    return KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_done_title))
                }
                //"Thành công!"
                "Connecting" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_please_wait)) //"Đang kết nối...")
                }

                "Initializing" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
                    KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_please_wait))
                } //"Đang khởi tạo..."
                "Processing" -> {
                    if (status != null && status == LivenessSessionStatus.VERIFIED) {
                        ivError.setGifImageResource(R.drawable.gif_success_small)
                        tvError.setTextColor(res.getColor(R.color.ekyc_green))
                        return if (errCode != null) input else KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_done_title))
                    } // "Xác thực thành công"
                    if (status != null && status == LivenessSessionStatus.FAILED) {
                        return KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_processing_failed))
                    } // "Xác thực thất bại"
                    else KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_please_wait)) // "Đang xác thực...")
                }

                "ConnectionFailed" -> {
                    tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))

                    ivError.setGifImageResource(R.drawable.gif_error_small)
                    return KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_error_network)) //"Kết nối thất bại")
                }

                "Finished" -> {
                    tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                    ivError.setGifImageResource(R.drawable.gif_success_small)
                    return KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_processing_failed)) //"Hoàn thành!")
                }

                else -> input
            }
            s
        }
    }

    private fun removeErrorMessage() {
        tvError.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
//        ivError.visibility = View.INVISIBLE
        tvError.text = ""
    }


    override fun onPreviewSizeChosen(size: Size, rotation: Int) {
        // 1.1. Setup View for bounding box
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            TEXT_SIZE_DIP,
            resources!!.displayMetrics
        )
        val desity = res.displayMetrics.density
        // 1.2. Setup Tracker
        val offsetY = res.getDimension(R.dimen.camera_offset_y) * 2
        Helpers.printLog(
            "Density: ",
            res.displayMetrics.density,
            " Ratio: ",
            (cardMaskView.centerY / res.displayMetrics.heightPixels),
            " Offset: ",
            res.getDimension(R.dimen.camera_offset_y),
            " Normalize Offest: ",
            offsetY
        )
    }

    override fun getLayoutId(): Int {
        return R.layout.tfe_od_camera_connection_fragment_tracking
    }

    override fun getDesiredPreviewFrameSize(): Size {
        return DESIRED_PREVIEW_SIZE
    }

    override fun setNumThreads(numThreads: Int) {

    }

    override fun setUseNNAPI(isChecked: Boolean) {
    }

    private fun vibratePhone() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    override fun onInfoBtnClicked() {
//        showingGuide = true
//        Helpers.printLog("On Info Btn Clicked")
//        val bottomFragment = BottomGuideFragment(GuideType.SELFIE)
//        supportFragmentManager.beginTransaction()
//            .setCustomAnimations(
//                R.anim.slide_in_bottom,
//                R.anim.slide_in_bottom,
//                R.anim.slide_in_bottom,
//                R.anim.slide_out_bottom
//            )
//            .replace(R.id.fragment_container, bottomFragment)
//            .addToBackStack(null)
//            .commit()
    }

    override fun sendError(message: String?) {
        Helpers.printLog("Failed! ")
        ProgressView.hideProgress()
        isVerifySucceed = false
        this.runOnUiThread {
            tvError.visibility = View.VISIBLE
            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
            cardMaskView.dashColor = res.getColor(R.color.ekyc_red)
            btnNext.visibility = View.INVISIBLE
            ivError.setGifImageResource(R.drawable.gif_error_small)
            tvError.text = message ?: KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_processing_failed))
        }
        Helpers.printLog("onError message: $message")
    }

    override fun sendDone(nextAction: () -> Unit) {
        nextAction()
        runOnUiThread {
            ProgressView.hideProgress()
        }
        KalapaSDK.handler.onError(KalapaSDKResultCode.SUCCESS)
        finish()
    }

}

enum class AnimStatus {
    ANIM_LOADING,
    ANIM_PROCESSING,
    ANIM_FAILED,
    ANIM_SUCCESS,
    ANIM_WARNING
}
