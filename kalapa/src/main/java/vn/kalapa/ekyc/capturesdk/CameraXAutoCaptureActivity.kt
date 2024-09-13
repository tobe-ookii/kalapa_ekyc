package vn.kalapa.ekyc.capturesdk

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
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
import vn.kalapa.ekyc.capturesdk.tflite.KLPDetector
import vn.kalapa.ekyc.capturesdk.tflite.OnImageDetectedListener
import vn.kalapa.ekyc.capturesdk.tflite.OverlayView
import vn.kalapa.ekyc.fragment.BottomGuideFragment
import vn.kalapa.ekyc.fragment.GuideType
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common.Companion.vibratePhone
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView


class CameraXAutoCaptureActivity(private val modelString: String = "klp_model_16.tflite") :
    CameraXActivity(activityLayoutId = R.layout.activity_camera_x_id_card, hideAutoCapture = false),
    KalapaSDKCallback, OnImageDetectedListener {
    private lateinit var ivPreviewImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvGuide1: TextView
    private lateinit var ivGuide: ImageView
    private var detector: KLPDetector? = null
    private lateinit var overlay: OverlayView
    private lateinit var ivCardInMask: ImageView

//    private lateinit var ivBitmapReview: ImageView
    private lateinit var documentType: KalapaSDKMediaType
    private fun getIntentData() {
        documentType = KalapaSDKMediaType.fromName(intent.getStringExtra("document_type") ?: KalapaSDKMediaType.BACK.name)
        Helpers.printLog("DocumentType: $documentType")
    }

    override fun onAutoCaptureToggle(isAutoCapturing: Boolean) {
        detector?.shouldCapture = isAutoCapturing
    }

    override fun setupCustomUI() {
        getIntentData()
//        cardMaskView = findViewById(R.id.cardMaskView)
        ivGuide = findViewById(R.id.iv_action)
        ivGuide.setImageResource(
            when (documentType) {
                KalapaSDKMediaType.FRONT -> R.drawable.klp_ic_footer_front
                KalapaSDKMediaType.BACK -> R.drawable.klp_ic_footer_back
                else -> R.drawable.ic_passport_black
            }
        )
        tvTitle = findViewById(R.id.tv_title)
        tvTitle.text =
            when (documentType) {
                KalapaSDKMediaType.FRONT -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_front))
                KalapaSDKMediaType.BACK -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_title_back))
                else -> KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_ekyc))
            }
        ivPreviewImage = findViewById(R.id.iv_preview_image)
        ivPreviewImage.isDrawingCacheEnabled = false
        tvGuide1 = findViewById(R.id.tv_guide)
        ivGuide.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
        overlay = findViewById(R.id.overlay)
//        ivBitmapReview = findViewById(R.id.iv_bitmap_preview)
        ivCardInMask = findViewById(R.id.iv_card_in_mask)

        tvTitle.setTextColor((Color.parseColor(KalapaSDK.config.mainTextColor)))
        tvGuide1.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_capture_note))
        tvGuide1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))

    }


    override fun postSetupCamera() {
        cameraExecutor.execute {
            detector = KLPDetector(this@CameraXAutoCaptureActivity, modelString, "klp_label.txt", isAutocapturing, this)
        }
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

    private var currError = ""

    override fun sendError(message: String?) {
//        Helpers.printLog("Failed! $message")
        if (message == null || currError == message) return
        ProgressView.hideProgress()
        this.runOnUiThread {
            currError = message
            tvError.visibility = View.VISIBLE
            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
            tvError.text = currError
            btnNext.visibility = View.INVISIBLE
        }
        Helpers.printLog("onError message: $message")
    }

    override fun sendDone(nextAction: () -> Unit) {
        nextAction()
        runOnUiThread {
            ProgressView.hideProgress()
        }
        finish()
    }


    override fun onRetryClicked() {
        super.onRetryClicked()
        renewSession()
    }

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun setupAnalyzer(): ImageAnalysis? {
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )
//            runOnUiThread { ivBitmapReview.setImageBitmap(rotatedBitmap) }
//                    Helpers.printLog("KLPDetector rotatedBitmap width: ${imageProxy.width} height: ${imageProxy.height} height ${rotatedBitmap.height}  ${rotatedBitmap.width} viewFinder Height ${viewFinder.height} Height ${viewFinder.width}")
            detector?.detect(rotatedBitmap) {
                imageProxy.close()
            }
        }
        return imageAnalysis
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
        val rotation =
            if (cameraDegree != getCameraRotationDegree()) ((getCameraRotationDegree() - cameraDegree + 270) % 360) else cameraDegree
        Helpers.printLog("onCaptureSuccess $cameraDegree ${getCameraRotationDegree()} $rotation")
        tmpBitmap = BitmapUtil.rotateBitmapToStraight(tmpBitmap!!, rotation, false) // tmpBitmap!! //
        tmpBitmap =
            BitmapUtil.crop(tmpBitmap!!, tmpBitmap!!.width, (tmpBitmap!!.width * 0.7f).toInt(), 0.5f, 0.5f)
        ivPreviewImage.visibility = View.VISIBLE
        ivPreviewImage.setImageBitmap(tmpBitmap)
    }

    private fun renewSession() {
        currError = ""
        detector?.restart(false)
        ivPreviewImage.visibility = View.INVISIBLE
        tmpBitmap = null
    }

    override fun onResume() {
        super.onResume()
        renewSession()
    }

    override fun verifyImage() {
        ProgressView.showProgress(this@CameraXAutoCaptureActivity)
        (KalapaSDK.handler as KalapaCaptureHandler).process(
            BitmapUtil.convertBitmapToBase64(tmpBitmap!!),
            documentType,
            this@CameraXAutoCaptureActivity
        )
    }

    override fun onBackBtnClicked() {
        if (showingGuide) {
            supportFragmentManager.popBackStack()
            showingGuide = false
        } else
            showEndEkyc()
    }

    private var showingGuide = false

    override fun onInfoBtnClicked() {
        showingGuide = true
        Helpers.printLog("On Info Btn Clicked $documentType")
        val bottomFragment = BottomGuideFragment(
            when (documentType) {
                KalapaSDKMediaType.FRONT -> GuideType.FRONT
                KalapaSDKMediaType.BACK -> GuideType.BACK
                KalapaSDKMediaType.PASSPORT -> GuideType.PASSPORT
                KalapaSDKMediaType.PORTRAIT -> GuideType.SELFIE
                else -> GuideType.FRONT
            }
        )
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_in_bottom,
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom
            )
            .replace(R.id.fragment_container, bottomFragment)
            .addToBackStack(null)
            .commit()
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

    override fun onImageDetected() {
        takePhoto()
        Handler(Looper.getMainLooper()).postDelayed({
            vibratePhone(this)
            runOnUiThread {
                tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                tvError.visibility = View.VISIBLE
                tvError.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_auto_capture_success))
            }
        }, 200)
    }

    @SuppressLint("ResourceType")
    override fun onImageOutOfMask() {
        runOnUiThread {
            sendError(KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.please_place_card_into_mask)))
            Helpers.setBackgroundColorTintList(ivCardInMask, resources.getString(R.color.ekyc_red))
        }
    }

    @SuppressLint("ResourceType")
    override fun onImageInMask() {
        runOnUiThread {
            sendError("")
            Helpers.setBackgroundColorTintList(ivCardInMask, resources.getString(R.color.ekyc_green))
        }
    }

    @SuppressLint("ResourceType")
    override fun onImageNotDetected() {
        runOnUiThread {
            overlay.clear()
            overlay.invalidate()
            if (currError == "")
                Helpers.setBackgroundColorTintList(ivCardInMask, resources.getString(R.color.white))
        }
    }

    @SuppressLint("ResourceType")
    override fun onImageTooSmall() {
        runOnUiThread {
            sendError(KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_autocapture_too_small)))
            Helpers.setBackgroundColorTintList(ivCardInMask, resources.getString(R.color.ekyc_red))
        }
    }


}
typealias InputImageListener = (inputImage: Bitmap) -> Unit
