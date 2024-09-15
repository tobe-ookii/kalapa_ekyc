package vn.kalapa.ekyc.capturesdk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.KalapaSDKResultCode
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.activity.CameraXActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.nfcsdk.activities.NFCActivity
import vn.kalapa.ekyc.utils.toBitmap

typealias OCROnlyImageInput = (inputImage: Bitmap, degree: Int) -> Unit

class CameraXMRZActivity : CameraXActivity(activityLayoutId = R.layout.activity_camera_x_mrz) {
    //    private lateinit var cardMaskView: CardMaskView
    private lateinit var ivPreviewImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvGuide0: TextView
    private lateinit var tvGuide1: TextView
    private lateinit var ivGuide: ImageView
    private var idCardNumbersMap = HashMap<String, Int>()
    private var enteredNFCActivity = false
    private var isProcessingFrame = false

//    private lateinit var ivBitmapReview: ImageView
    override fun setupCustomUI() {
//        cardMaskView = findViewById(R.id.cardMaskView)
        ivGuide = findViewById(R.id.iv_action)
        ivPreviewImage = findViewById(R.id.iv_preview_image)
        ivPreviewImage.isDrawingCacheEnabled = false
        tvTitle = findViewById(R.id.tv_title)
        tvGuide0 = findViewById(R.id.tv_guide_0)
        tvGuide1 = findViewById(R.id.tv_guide)
        ivGuide.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))

//        ivBitmapReview = findViewById(R.id.iv_bitmap_preview)
        tvTitle.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_mrz_scan_title))
        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
//        tvGuide0.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_scan_back_document))
        tvGuide1.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_mrz_scan_note))
//        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvGuide1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvGuide0.visibility = View.GONE

    }

    private fun returnToNFCActivity(ocrMRZ: String) {
        if (!enteredNFCActivity) {
            enteredNFCActivity = true
            val intent = Intent(this@CameraXMRZActivity, NFCActivity::class.java)
            intent.putExtra("mrz", ocrMRZ)
            startActivity(intent)
            finish()
        }
    }

    private fun processFrame(bitmap: Bitmap, degree: Int): String {
        // When using Latin script library
//        Helpers.printLog("Process frame: ${System.currentTimeMillis()}")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var rotatedBitmap = BitmapUtil.rotateBitmapToStraight(bitmap, degree)
        rotatedBitmap = BitmapUtil.crop(
            rotatedBitmap,
            rotatedBitmap.width,
            rotatedBitmap.width * 5 / 8,
            0.5f,
            0.5f
        )
//        runOnUiThread {
//            ivBitmapReview.setImageBitmap(rotatedBitmap)
//        }
        isProcessingFrame = true
        recognizer.process(InputImage.fromBitmap(rotatedBitmap, 0))
            .addOnSuccessListener {
                if (it.textBlocks.size > 0) {
                    for (line in it.textBlocks) {
                        if (line.text.startsWith("IDVNM")) {
                            Helpers.printLog("CameraXMRZActivity text blocks OK ${line.text}")
                            val idCardNumber = Common.getIdCardNumberFromMRZ(line.text)
                            if (!idCardNumber.isNullOrEmpty()) {
                                returnToNFCActivity(line.text)
                            }
                        } else {
//                            Helpers.printLog("CameraXMRZActivity text blocks not OK ${line.text}")
                        }
                    }
                }
                isProcessingFrame = false
            }.addOnFailureListener {
                isProcessingFrame = false
                Helpers.printLog("addOnFailureListener $it")
            }
        while (true) {
            if (!isProcessingFrame)
                return ""
            Thread.sleep(100)
        }
    }

    override fun setupAnalyzer(): ImageAnalysis? {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, MRZAnalyzer { inputImage, degree ->
                    processFrame(inputImage, degree)
//                    Helpers.printLog("InputImage is processing: ${inputImage.width} ${inputImage.height}")
                })
            }
    }

    override fun previewViewLayerMode(isCameraMode: Boolean) {
        super.previewViewLayerMode(isCameraMode)
//        if (isCameraMode) {
//            ivPreviewImage.visibility = View.INVISIBLE
////            cardMaskView.setBackgroundColor(resources.getColor(R.color.black40))
////            cardMaskView.visibility = View.VISIBLE
//            tvTitle.setTextColor(resources.getColor(R.color.white))
//            tvGuide0.setTextColor(resources.getColor(R.color.white))
//            tvGuide1.setTextColor(resources.getColor(R.color.white))
//            ivCloseEkyc.setColorFilter(resources.getColor(R.color.white))
//            ivGuide.setColorFilter(resources.getColor(R.color.white))
//        } else {
//            val mainColor = Color.parseColor(KalapaSDK.config.mainColor)
//            val mainTextColor = Color.parseColor(KalapaSDK.config.mainTextColor)
//            tvTitle.setTextColor(mainColor)
//            tvGuide0.setTextColor(mainTextColor)
//            tvGuide1.setTextColor(mainTextColor)
//            ivCloseEkyc.setColorFilter(mainColor)
//            ivGuide.setColorFilter(mainColor)
//            if (tmpBitmap != null) {
//                ivPreviewImage.visibility = View.VISIBLE
//                ivPreviewImage.setImageBitmap(tmpBitmap)
//            }
////            cardMaskView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
//        }
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
        idCardNumbersMap.clear()
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

}

class MRZAnalyzer(private val listener: OCROnlyImageInput) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        image.toBitmap()?.let { listener(it, image.imageInfo.rotationDegrees) }
        image.close()
    }
}