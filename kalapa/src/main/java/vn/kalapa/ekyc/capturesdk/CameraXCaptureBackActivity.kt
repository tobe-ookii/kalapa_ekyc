package vn.kalapa.ekyc.capturesdk

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView



class CameraXCaptureBackActivity() : CameraXActivity(activityLayoutId = R.layout.activity_camera_x_back_card, hideAutoCapture = true), KalapaSDKCallback {
    private lateinit var ivPreviewImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvGuide1: TextView
    private lateinit var ivGuide: ImageView

    private var isProcessingFrame = false

    // For Autocapture
    private lateinit var ivBitmapReview: ImageView

    override fun setupCustomUI() {
//        cardMaskView = findViewById(R.id.cardMaskView)
        ivGuide = findViewById(R.id.iv_action)
        ivPreviewImage = findViewById(R.id.iv_preview_image)
        ivPreviewImage.isDrawingCacheEnabled = false
        tvTitle = findViewById(R.id.tv_title)
        tvGuide1 = findViewById(R.id.tv_guide)
        ivGuide.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))

        ivBitmapReview = findViewById(R.id.iv_bitmap_preview)
        tvTitle.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_front))
        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
//        tvGuide0.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_scan_back_document))
        tvGuide1.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_scan_back_document))
//        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvGuide1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))

    }


    override fun previewViewLayerMode(isCameraMode: Boolean) {
        super.previewViewLayerMode(isCameraMode)
        if (isCameraMode) {
            holderAutoCapture.visibility = View.VISIBLE
            ivGuide.visibility = View.VISIBLE
            tvGuide.visibility = View.VISIBLE
        } else {
            if (KalapaSDK.isFoldOpen(this@CameraXCaptureBackActivity)) {
                holderAutoCapture.visibility = View.INVISIBLE
                ivGuide.visibility = View.INVISIBLE
                tvGuide.visibility = View.INVISIBLE
            }
        }
    }

    private fun processFrame(imageProxy: ImageProxy): String {
        isProcessingFrame = false
        while (true) {
            if (!isProcessingFrame)
                return ""
            Thread.sleep(100)
        }
    }

    private fun onCardOutOfMaskHandleUI() {
        sendError(KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.please_place_card_into_mask)))
    }

    private fun cardInMaskHandleUI() {
        sendError("")
    }

    private var currError = ""
    override fun sendError(message: String?) {
        if (currError.isNotEmpty() && currError == message) return
        currError = message ?: KalapaSDK.config.languageUtils.getLanguageString(
            resources.getString(
                R.string.klp_liveness_processing_failed
            )
        )
        Helpers.printLog("Failed! ")
        ProgressView.hideProgress()
        this.runOnUiThread {
            if (KalapaSDK.isFoldOpen(this@CameraXCaptureBackActivity)) {
                tvGuide1.visibility = View.INVISIBLE
            }
            tvError.visibility = View.VISIBLE
            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
            btnNext.visibility = View.INVISIBLE
            tvError.text = currError
        }
        Helpers.printLog("onError message: $message")
    }

    override fun sendDone(nextAction: () -> Unit) {
        nextAction()
        runOnUiThread {
            ProgressView.hideProgress()
        }
//        KalapaSDK.captureHandler.onError(KalapaCaptureResultCode.SUCCESS)
        finish()
    }

    override fun setupAnalyzer(): ImageAnalysis? {
        return null
    }

    override fun showEndEkyc() {
        Helpers.showEndKYC(this, object : DialogListener {
            override fun onYes() {
                KalapaSDK.captureHandler.onError(KalapaCaptureResultCode.USER_LEAVE)
                finish()
            }

            override fun onNo() {

            }
        })
    }

    override fun onCaptureSuccess(rotationDegree: Int) {
        // We dont use it
    }

    private fun renewSession() {
    }

    override fun onResume() {
        super.onResume()
        renewSession()
    }

    override fun verifyImage() {
        KalapaSDK.captureHandler.process(
            BitmapUtil.convertBitmapToBase64(tmpBitmap!!),
            KalapaSDKMediaType.FRONT,
            this@CameraXCaptureBackActivity
        )
    }

    override fun onBackBtnClicked() {
        showEndEkyc()
    }

    override fun onInfoBtnClicked() {

    }

}

