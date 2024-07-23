package vn.kalapa.ekyc.capturesdk

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.activity.CameraXActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView


class CameraXCaptureBackActivity : CameraXActivity(
    activityLayoutId = R.layout.activity_camera_x_id_card,
    hideAutoCapture = true
), KalapaSDKCallback {
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
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_back))
        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
//        tvGuide0.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_scan_back_document))
        tvGuide1.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_subtitle_back))
//        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvGuide1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        Helpers.setBackgroundColorTintList(btnCapture, KalapaSDK.config.mainColor)
        Helpers.setBackgroundColorTintList(btnRetry, KalapaSDK.config.mainColor)
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
        ProgressView.hideProgress()
        if (currError.isNotEmpty() && currError == message && tvError.visibility == View.VISIBLE) return
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
                KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
                finish()
            }

            override fun onNo() {

            }
        })
    }

    override fun onCaptureSuccess(cameraDegree: Int) {
        // We dont use it
        val rotation =
            if (cameraDegree != getCameraRotationDegree()) ((getCameraRotationDegree() - cameraDegree + 270) % 360) else cameraDegree
        Helpers.printLog("onCaptureSuccess $cameraDegree ${getCameraRotationDegree()} $rotation")
        tmpBitmap =
            BitmapUtil.rotateBitmapToStraight(tmpBitmap!!, rotation, false) // tmpBitmap!! //
        tmpBitmap =
            BitmapUtil.crop(tmpBitmap!!, tmpBitmap!!.width, tmpBitmap!!.width * 5 / 8, 0.5f, 0.5f)
        ivPreviewImage.visibility = View.VISIBLE
        ivPreviewImage.setImageBitmap(tmpBitmap)
    }

    private fun renewSession() {
        ivPreviewImage.visibility = View.INVISIBLE
        tmpBitmap = null
    }

    override fun onResume() {
        super.onResume()
        renewSession()
    }

    override fun verifyImage() {
        ProgressView.showProgress(this@CameraXCaptureBackActivity)
        (KalapaSDK.handler as KalapaCaptureHandler).process(
            BitmapUtil.convertBitmapToBase64(tmpBitmap!!),
            KalapaSDKMediaType.BACK,
            this@CameraXCaptureBackActivity
        )
    }

    override fun onRetryClicked() {
        super.onRetryClicked()
        renewSession()
    }

    override fun onBackBtnClicked() {
        if (!isCameraMode)
            onRetryClicked()
        else
            showEndEkyc()
    }

    override fun onInfoBtnClicked() {

    }

}

