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
//    private var enteredNFCActivity = false
//    private var isProcessingFrame = false
//
//    // For Autocapture
//    private var detector: CardClassifier? = null
//    private val NUMTHREADS = 1
//    private val MAINTAIN_ASPECT = true
//    private val MINIMUM_CONFIDENCE_TF_OD_API = 0.3f
//    private val MODE = DetectorMode.TF_OD_API
//
//    private val yuvBytes = arrayOfNulls<ByteArray>(3)
//    private var rgbBytes: IntArray? = null
//    private var yRowStride = 0
//    private var postInferenceCallback: Runnable? = null
//    private var imageConverter: Runnable? = null
//
//    private lateinit var ivBitmapReview: ImageView
//    private var computingDetection = false
//
//    private var previewWidth = 0
//    private var previewHeight = 0
//
//    private var rgbFrameBitmap: Bitmap? = null
//    private var croppedBitmap: Bitmap? = null
//
//    private lateinit var cropCopyBitmap: Bitmap
//
//    lateinit var frameToCropTransform: Matrix
//    lateinit var cropToFrameTransform: Matrix
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
//    private fun returnToNFCActivity(ocrMRZ: String) {
//        if (!enteredNFCActivity) {
//            enteredNFCActivity = true
//            val intent = Intent(this@CameraXAutoCaptureActivity, NFCActivity::class.java)
//            intent.putExtra("mrz", ocrMRZ)
//            startActivity(intent)
//            finish()
//        }
//    }
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
//    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
//    private fun processFrame(imageProxy: ImageProxy): String {
//        // Process here
//        if (viewFinder.width == 0 || viewFinder.height == 0) {
//            Helpers.printLog("previewWidth & previewHeight null")
//            return ""
//        }
//        if (previewWidth == 0 || previewHeight == 0) {
//            previewWidth = viewFinder.width
//            previewHeight = viewFinder.height
//            val cropSize = detector!!.inputSize
//            rgbFrameBitmap =
//                Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
//            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
//            frameToCropTransform = ImageUtils.getTransformationMatrix(
//                previewWidth,
//                previewHeight,
//                cropSize,
//                cropSize,
//                imageProxy.imageInfo.rotationDegrees,
//                MAINTAIN_ASPECT
//            )
//            cropToFrameTransform = Matrix()
//            frameToCropTransform.invert(cropToFrameTransform)
//        }
//
//        if (rgbBytes == null) {
//            rgbBytes = IntArray(previewWidth * previewHeight)
//        }
//        try {
//            Helpers.printLog("Start Processing Frame")
//            isProcessingFrame = true
//            Trace.beginSection("imageAvailable")
//            val planes = imageProxy.image!!.planes
//            fillBytes(planes, yuvBytes)
//            yRowStride = planes[0].rowStride
//            val uvRowStride = planes[1].rowStride
//            val uvPixelStride = planes[1].pixelStride
//            imageConverter = Runnable {
//                ImageUtils.convertYUV420ToARGB8888(
//                    yuvBytes[0],
//                    yuvBytes[1],
//                    yuvBytes[2],
//                    previewWidth,
//                    previewHeight,
//                    yRowStride,
//                    uvRowStride,
//                    uvPixelStride,
//                    rgbBytes
//                )
//            }
//            postInferenceCallback = Runnable {
//                imageProxy.close()
//                isProcessingFrame = false
//            }
//            processImage()
//            Helpers.printLog("Start Processing Image")
//        } catch (e: Exception) {
//            Helpers.printLog(e, "Exception!")
//            Trace.endSection()
//            return ""
//        }
//        Trace.endSection()
//        // End of process here
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
//            .setTargetResolution(Size(320, 320))
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
//        detector?.setNumThreads(NUMTHREADS)
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
//            readyForNextImage()
//            return
//        }
//        computingDetection = true
//        rgbFrameBitmap!!.setPixels(
//            getRgbBytes(),
//            0,
//            previewWidth,
//            0,
//            0,
//            previewWidth,
//            previewHeight
//        )
//        readyForNextImage()
//
//        val canvas = Canvas(croppedBitmap!!)
//        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform, null)
//        // For examining the actual TF input.
////        runOnUiThread {
////            ivBitmapReview.setImageBitmap(croppedBitmap)
////        }
//        Handler(Looper.getMainLooper()).run {
//            val startTime = SystemClock.uptimeMillis()
//
//            val results: List<Classifier.Recognition> = detector!!.recognizeImage(croppedBitmap)
////                if (results.isNotEmpty())
////                    Log.e("CHECK", "run: " + results.size)
//            Helpers.printLog("Result: $results")
//            if (results.isEmpty() || results.size < 3) {
//                onCardOutOfMaskHandleUI()
//            }else{
//                cardInMaskHandleUI()
//            }
//            computingDetection = false
//        }
//    }
//
//    private fun getRgbBytes(): IntArray? {
//        imageConverter!!.run()
//        return rgbBytes
//    }
//
//
//    private fun readyForNextImage() {
//        if (postInferenceCallback != null) {
//            postInferenceCallback!!.run()
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