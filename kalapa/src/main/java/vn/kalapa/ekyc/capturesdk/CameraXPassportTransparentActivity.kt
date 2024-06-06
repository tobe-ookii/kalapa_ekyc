package vn.kalapa.ekyc.capturesdk//package vn.kalapa.faceotp.activity
//
//import android.graphics.Color
//import android.os.Handler
//import android.os.Looper
//import android.view.View
//import android.widget.ImageView
//import android.widget.TextView
//import vn.kalapa.faceotp.DialogListener
//import vn.kalapa.faceotp.KalapaSDKResultCode
//import vn.kalapa.faceotp.KalapaSDK
//import vn.kalapa.faceotp.KalapaSDKCallback
//import vn.kalapa.faceotp.KalapaSDKMediaType
//import vn.kalapa.faceotp.R
//import vn.kalapa.faceotp.fragment.BottomGuideFragment
//import vn.kalapa.faceotp.fragment.GuideType
//import vn.kalapa.faceotp.utils.BitmapUtil
//import vn.kalapa.faceotp.utils.Helpers
//import vn.kalapa.faceotp.views.CardMaskView
//import vn.kalapa.faceotp.views.ProgressView
//
//class CameraXPassportTransparentActivity : CameraXActivity(activityLayoutId = R.layout.activity_camera_x_passport), KalapaSDKCallback {
//    private lateinit var cardMaskView: CardMaskView
//    private lateinit var ivPreviewImage: ImageView
//    private lateinit var tvTitle: TextView
//    private lateinit var tvGuide0: TextView
//    private lateinit var tvGuide1: TextView
//    private lateinit var ivGuide: ImageView
//    override fun setupCustomUI() {
//        cardMaskView = findViewById(R.id.cardMaskView)
//        ivPreviewImage = findViewById(R.id.iv_preview_image)
//        ivPreviewImage.isDrawingCacheEnabled = false
//        tvTitle = findViewById(R.id.tv_title)
//        tvGuide0 = findViewById(R.id.tv_guide_0)
//        tvGuide1 = findViewById(R.id.tv_guide)
//        ivGuide = findViewById(R.id.iv_action)
//
//        tvTitle.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_passport_title))
//        tvGuide0.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_passport_guide_1))
//        tvGuide1.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_passport_guide_2))
//        tvTitle.setTextColor(resources.getColor(R.color.white))
//        tvGuide0.setTextColor(resources.getColor(R.color.white))
//        tvGuide1.setTextColor(resources.getColor(R.color.white))
//        ivCloseEkyc.setColorFilter(resources.getColor(R.color.white))
//        ivGuide.setColorFilter(resources.getColor(R.color.white))
//    }
//
//    override fun previewViewLayerMode(isCameraMode: Boolean) {
//        super.previewViewLayerMode(isCameraMode)
//        if (isCameraMode) {
//            ivPreviewImage.visibility = View.INVISIBLE
//            cardMaskView.setBackgroundColor(resources.getColor(R.color.black40))
//            cardMaskView.visibility = View.VISIBLE
////            tvTitle.setTextColor(resources.getColor(R.color.white))
////            tvGuide0.setTextColor(resources.getColor(R.color.white))
////            tvGuide1.setTextColor(resources.getColor(R.color.white))
////            ivCloseEkyc.setColorFilter(resources.getColor(R.color.white))
////            ivGuide.setColorFilter(resources.getColor(R.color.white))
//        } else {
//            val mainColor = Color.parseColor(KalapaSDK.config.mainColor)
//            val mainTextColor = Color.parseColor(KalapaSDK.config.mainTextColor)
////            tvTitle.setTextColor(mainColor)
////            tvGuide0.setTextColor(mainTextColor)
////            tvGuide1.setTextColor(mainTextColor)
////            ivCloseEkyc.setColorFilter(mainColor)
////            ivGuide.setColorFilter(mainColor)
////            cardMaskView.setBackgroundColor(resources.getColor(R.color.black40))
////            if (tmpBitmap != null) {
////                ivPreviewImage.visibility = View.VISIBLE
////                ivPreviewImage.setImageBitmap(tmpBitmap)
////            }
////            cardMaskView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
//        }
//    }
//
//
//    override fun showEndEkyc() {
//        Helpers.showEndKYC(this, object : DialogListener {
//            override fun onYes() {
//                KalapaSDK.captureHandler.onError(KalapaCaptureResultCode.USER_LEAVE)
//                finish()
//            }
//
//            override fun onNo() {
//
//            }
//        })
//    }
//
//    override fun onCaptureSuccess(rotationDegree: Int) {
//        tmpBitmap = cardMaskView.crop(tmpBitmap!!, rotationDegree.toFloat())
//    }
//
//    override fun verifyImage() {
//        runOnUiThread {
//            ProgressView.showProgress(this@CameraXPassportTransparentActivity)
//        }
//        Handler(Looper.getMainLooper()).postDelayed({
//            val resizedBitmap = BitmapUtil.resizeBitmapToBitmap(tmpBitmap!!)
//            Helpers.printLog("ResizedBitmap from ${tmpBitmap!!.byteCount / 1024} to ${resizedBitmap.byteCount / 1024}")
//            KalapaSDK.captureHandler.process(BitmapUtil.convertBitmapToBase64(resizedBitmap), KalapaSDKMediaType.PASSPORT, this)
//        }, 100)
//    }
//
//    override fun onBackBtnClicked() {
//        showEndEkyc()
//    }
//
//    override fun onInfoBtnClicked() {
//    }
//
//    override fun sendError(message: String?) {
//        Helpers.printLog("Failed! ")
//        ProgressView.hideProgress()
//        this.runOnUiThread {
//            tvError.visibility = View.VISIBLE
//            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
//            cardMaskView.dashColor = resources.getColor(R.color.ekyc_red)
//            btnNext.visibility = View.INVISIBLE
////            ivError.setImageDrawable(res.getDrawable(R.drawable.ic_failed_solid))
////            ivError.visibility = View.VISIBLE
//            tvError.text = message ?: KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_processing_failed))
//        }
//        Helpers.printLog("onError message: $message")
//    }
//
//    override fun sendDone(nextAction: () -> Unit) {
//        nextAction()
//        runOnUiThread {
//            ProgressView.hideProgress()
//        }
//        KalapaSDK.captureHandler.onError(KalapaCaptureResultCode.SUCCESS)
//        finish()
//    }
//
//}