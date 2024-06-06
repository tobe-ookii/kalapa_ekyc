//package vn.kalapa.ekyc.capturesdk
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Matrix
//import android.media.Image
//import android.os.Handler
//import android.os.Looper
//import android.os.SystemClock
//import android.os.Trace
//import android.util.Size
//import android.view.View
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.camera.core.AspectRatio
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.core.graphics.scaleMatrix
//import vn.kalapa.R
//import vn.kalapa.ekyc.*
//import vn.kalapa.ekyc.capturesdk.tflite.CardClassifier
//import vn.kalapa.ekyc.capturesdk.tflite.Classifier
//import vn.kalapa.ekyc.capturesdk.tflite.DetectorFactory
//import vn.kalapa.ekyc.capturesdk.utils.YuvToRgbConverter
//import vn.kalapa.ekyc.utils.Helpers
//import vn.kalapa.ekyc.nfcsdk.activities.NFCActivity
//import vn.kalapa.ekyc.utils.BitmapUtil
//import vn.kalapa.ekyc.utils.ImageUtils
//import vn.kalapa.ekyc.views.ProgressView
//
//
//typealias InputImageProxyListener = (imageProxy: ImageProxy) -> Unit
//
//class CameraXAutoCaptureActivity(private val modelString: String = "klp_model.tflite") :
//    CameraXActivity(activityLayoutId = R.layout.activity_camera_x_id_card, hideAutoCapture = false),
//    KalapaSDKCallback {
//    private lateinit var ivPreviewImage: ImageView
//    private lateinit var tvTitle: TextView
//    private lateinit var tvGuide1: TextView
//    private lateinit var ivGuide: ImageView
//
//    private var isProcessingFrame = false
//
//    // For Autocapture
//    private var detector: CardClassifier? = null
//    private lateinit var ivBitmapReview: ImageView
//    private var computingDetection = false
//
//
//    override fun setupCustomUI() {
////        cardMaskView = findViewById(R.id.cardMaskView)
//        ivGuide = findViewById(R.id.iv_action)
//        ivPreviewImage = findViewById(R.id.iv_preview_image)
//        ivPreviewImage.isDrawingCacheEnabled = false
//        tvTitle = findViewById(R.id.tv_title)
//        tvGuide1 = findViewById(R.id.tv_guide)
//        ivGuide.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
//
//        ivBitmapReview = findViewById(R.id.iv_bitmap_preview)
//        tvTitle.text =
//            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_front))
//        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
////        tvGuide0.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_scan_back_document))
//        tvGuide1.text =
//            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_scan_back_document))
////        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
//        tvGuide1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
//
//    }
//
//
//    override fun previewViewLayerMode(isCameraMode: Boolean) {
//        super.previewViewLayerMode(isCameraMode)
//        if (isCameraMode) {
//            holderAutoCapture.visibility = View.VISIBLE
//            ivGuide.visibility = View.VISIBLE
//            tvGuide.visibility = View.VISIBLE
//        } else {
//            if (KalapaSDK.isFoldOpen(this@CameraXAutoCaptureActivity)) {
//                holderAutoCapture.visibility = View.INVISIBLE
//                ivGuide.visibility = View.INVISIBLE
//                tvGuide.visibility = View.INVISIBLE
//            }
//        }
//    }
//
//    private fun processFrame(imageProxy: ImageProxy): String {
//        isProcessingFrame = false
//        while (true) {
//            if (!isProcessingFrame)
//                return ""
//            Thread.sleep(100)
//        }
//    }
//
//    private fun onCardOutOfMaskHandleUI() {
//        sendError(KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.please_place_card_into_mask)))
//    }
//
//    private fun cardInMaskHandleUI() {
//        sendError("")
//    }
//
//    private var currError = ""
//    override fun sendError(message: String?) {
//        if (currError.isNotEmpty() && currError == message) return
//        currError = message ?: KalapaSDK.config.languageUtils.getLanguageString(
//            resources.getString(
//                R.string.klp_liveness_processing_failed
//            )
//        )
//        Helpers.printLog("Failed! ")
//        ProgressView.hideProgress()
//        this.runOnUiThread {
//            if (KalapaSDK.isFoldOpen(this@CameraXAutoCaptureActivity)) {
//                tvGuide1.visibility = View.INVISIBLE
//            }
//            tvError.visibility = View.VISIBLE
//            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
//            btnNext.visibility = View.INVISIBLE
//            tvError.text = currError
//        }
//        Helpers.printLog("onError message: $message")
//    }
//
//    override fun sendDone(nextAction: () -> Unit) {
//        TODO("Not yet implemented")
//    }
//
//    override fun setupAnalyzer(): ImageAnalysis? {
//        return ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetRotation(viewFinder.display.rotation)
//            .build()
//            .also {
//                it.setAnalyzer(cameraExecutor, IDCardAnalyze {
//                    // updating the list of recognised objects
//                    processFrame(it)
//                })
//            }
//    }
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
//        // We dont use it
//    }
//
//    private fun renewSession() {
//        detector = DetectorFactory.getDetector(assets, modelString)
//        detector?.setNumThreads(1)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        renewSession()
//    }
//
//    override fun verifyImage() {
//
//    }
//
//    override fun onBackBtnClicked() {
//        showEndEkyc()
//    }
//
//    override fun onInfoBtnClicked() {
//
//    }
//
//    private fun processImage() {
//        if (computingDetection) {
//            return
//        }
//        computingDetection = true
//        Handler(Looper.getMainLooper()).run {
//            val results: List<Classifier.Recognition> = detector!!.recognizeImage(tmpBitmap)
//            Helpers.printLog("Result: $results")
//            if (results.isEmpty() || results.size < 3) {
//                onCardOutOfMaskHandleUI()
//            } else {
//                cardInMaskHandleUI()
//            }
//            computingDetection = false
//        }
//    }
//
//    private fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
//        // Because of the variable row stride it's not possible to know in
//        // advance the actual necessary dimensions of the yuv planes.
//        for (i in planes.indices) {
//            val buffer = planes[i].buffer
//            if (yuvBytes[i] == null) {
//                Helpers.printLog("Initializing buffer %d at size %d", i, buffer.capacity())
//                yuvBytes[i] = ByteArray(buffer.capacity())
//            }
//            buffer[yuvBytes[i]]
//        }
//    }
//
//
//}
//
//class IDCardAnalyze(private val listener: InputImageProxyListener) : ImageAnalysis.Analyzer {
//
//    override fun analyze(image: ImageProxy) {
//        listener(image)
//        image.close()
//    }
//}
//
//private enum class DetectorMode {
//    TF_OD_API
//}