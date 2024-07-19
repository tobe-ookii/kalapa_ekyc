package vn.kalapa.ekyc.capturesdk

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.activity.CameraXActivity
import vn.kalapa.ekyc.capturesdk.tflite.Classifier
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView


class CameraXAutoCaptureActivity(private val modelString: String = "klp_model_2_metadata.tflite") :
    CameraXActivity(activityLayoutId = R.layout.activity_camera_x_id_card, hideAutoCapture = false),
    KalapaSDKCallback {
    private lateinit var ivPreviewImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvGuide1: TextView
    private lateinit var ivGuide: ImageView

    private var isProcessingFrame = false

    // For Autocapture
    private var detector: CardClassifier? = null
    private lateinit var ivBitmapReview: ImageView
    private var computingDetection = false


    private fun getIntentData() {
        val layoutId = intent.getStringExtra("layout")
        val captureType = intent.getStringExtra("capture_type")
        Helpers.printLog("Layout: $layoutId, CaptureType: $captureType")
    }

    override fun setupCustomUI() {
        getIntentData()
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
            if (KalapaSDK.isFoldOpen(this@CameraXAutoCaptureActivity)) {
                holderAutoCapture.visibility = View.INVISIBLE
                ivGuide.visibility = View.INVISIBLE
                tvGuide.visibility = View.INVISIBLE
            }
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
            if (KalapaSDK.isFoldOpen(this@CameraXAutoCaptureActivity)) {
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
        TODO("Not yet implemented")
    }

    @SuppressLint("RestrictedApi")
    override fun setupAnalyzer(): ImageAnalysis? {
        return ImageAnalysis.Builder()
            .setTargetResolution(Size(224, 224))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetRotation(getCameraRotationDegree())
//            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, IDCardAnalyze { inputImage, planes, degree ->
                    processFrame(inputImage, planes, degree)
                })
            }
    }

    private fun processFrame(bitmap: Bitmap, planes: Array<ImageProxy.PlaneProxy>, degree: Int): String {
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

    override fun onCaptureSuccess(rotationDegree: Int) {
        // We dont use it
    }

    private fun renewSession() {
        detector = DetectorFactory.getDetector(assets, modelString)
        detector?.setNumThreads(1)
    }

    override fun onResume() {
        super.onResume()
        renewSession()
    }

    override fun verifyImage() {

    }

    override fun onBackBtnClicked() {
        showEndEkyc()
    }

    override fun onInfoBtnClicked() {

    }

    private fun fillBytes(planes: Array<ImageProxy.PlaneProxy>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                Helpers.printLog("Initializing buffer %d at size %d", i, buffer.capacity())
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }


}
typealias InputImageListener = (inputImage: Bitmap, planes: Array<ImageProxy.PlaneProxy>, degree: Int) -> Unit

class IDCardAnalyze(private val listener: InputImageListener) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {

        val items = mutableListOf<Classifier.Recognition>()

        // TODO 2: Convert Image to Bitmap then to TensorImage
        val tfImage = TensorImage.fromBitmap(toBitmap(imageProxy))
        // TODO 3: Process the image using the trained model, sort and pick out the top results
        val outputs = klpModel2.process(tfImage)
            .outputAsCategoryList
//                .probabilityAsCategoryList.apply {    sortByDescending { it.score } // Sort with highest confidence first
//                }.take(MAX_RESULT_DISPLAY) // take the top results
        // TODO 4: Converting the top probability items into a list of recognitions
        for (output in outputs) {
            print("Something: $output")
            items.add(Classifier.Recognition(output.label, output.score))
        }
//            klpModel2.close()
        // START - Placeholder code at the start of the codelab. Comment this block of code out.
//            for (i in 0 until MAX_RESULT_DISPLAY) {
//                items.add(Recognition("Fake label $i", Random.nextFloat()))
//            }
        // END - Placeholder code at the start of the codelab. Comment this block of code out.

        // Return the result
        listener(items.toList())

        // Close the image,this tells CameraX to feed the next image to the analyzer
        imageProxy.close()
    }
}
