package vn.kalapa.ekyc.capturesdk

import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.KalapaCaptureHandler
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.KalapaSDKCallback
import vn.kalapa.ekyc.KalapaSDKMediaType
import vn.kalapa.ekyc.KalapaSDKResultCode
import vn.kalapa.ekyc.activity.CameraXActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView

class CameraXPassportActivity :
    CameraXActivity(activityLayoutId = R.layout.activity_camera_x_passport_solid),
    KalapaSDKCallback {
    //    private lateinit var cardMaskView: CardMaskView
    private lateinit var ivPreviewImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvGuide0: TextView
    private lateinit var tvGuide1: TextView
    private lateinit var ivGuide: ImageView
    private lateinit var btnPickImage: ImageButton
    private lateinit var tvPickImage: TextView

    //    private val changeImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//        if (it.resultCode == Activity.RESULT_OK) {
//            processFromActivityResult(it)
//        }
//    }
    private val changeImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Helpers.printLog("PhotoPicker", "Selected URI: $uri")
            processFromActivityResult(uri)
        } else {
            Helpers.printLog("PhotoPicker", "No media selected")
        }
    }

    private fun processFromActivityResult(imgUri: Uri?) {
        if (imgUri != null) {
            val inputStream = contentResolver.openInputStream(imgUri)
            if (inputStream != null) {
                Helpers.printLog("Picked image from inputStream $imgUri")
                val exif = ExifInterface(inputStream)
                val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    6 -> 90
                    3 -> 180
                    8 -> 270
                    else -> 0
                }
                tmpBitmap = BitmapUtil.rotateBitmapToStraight(MediaStore.Images.Media.getBitmap(contentResolver, imgUri), rotation)
                Helpers.printLog("Picked image: $imgUri with Rotation $rotation ${tmpBitmap!!.width} ${tmpBitmap!!.height} ${tmpBitmap!!.byteCount}")
                tmpBitmap = BitmapUtil.resizeImageFromGallery(tmpBitmap!!)
                Helpers.printLog("Picked image: Compress ${tmpBitmap!!.byteCount} ${tmpBitmap!!.width} ${tmpBitmap!!.height}")
                Handler(Looper.getMainLooper()).postDelayed({
                    stopCamera()
                }, 100)
            }
        }
    }

    //    private var backFromPickedImage = false
    private fun pickImageFromGallery() {
        changeImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun setupCustomUI() {
//        cardMaskView = findViewById(R.id.cardMaskView)
        ivPreviewImage = findViewById(R.id.iv_preview_image)
        ivPreviewImage.isDrawingCacheEnabled = false
        tvTitle = findViewById(R.id.tv_title)
        tvGuide0 = findViewById(R.id.tv_guide_0)
        tvGuide1 = findViewById(R.id.tv_guide)
        ivGuide = findViewById(R.id.iv_action)
        btnPickImage = findViewById(R.id.btn_pick_image)
        tvPickImage = findViewById(R.id.tv_pick_image)
        tvPickImage.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvPickImage.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_upload_passport))
        Helpers.setBackgroundColorTintList(btnPickImage, KalapaSDK.config.mainTextColor)
        btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }
        tvTitle.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_passport_title))
        tvGuide0.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_passport_guide_1))
        tvGuide1.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_passport_guide_2))

        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvGuide0.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        tvGuide1.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        ivCloseEkyc.setColorFilter(Color.parseColor(KalapaSDK.config.mainTextColor))
        ivGuide.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
//        btnCapture.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
        Helpers.setBackgroundColorTintList(btnCapture, KalapaSDK.config.mainColor)
        Helpers.setBackgroundColorTintList(btnRetry, KalapaSDK.config.mainColor)
    }

    override fun onRetryClicked() {
        super.onRetryClicked()
        runOnUiThread {
            tvGuide0.visibility = View.VISIBLE
            tvGuide1.visibility = View.VISIBLE
        }
    }

    override fun previewViewLayerMode(isCameraMode: Boolean) {
        super.previewViewLayerMode(isCameraMode)
        if (isCameraMode) {
            ivPreviewImage.visibility = View.INVISIBLE
//            cardMaskView.setBackgroundColor(resources.getColor(R.color.black40))
//            cardMaskView.visibility = View.VISIBLE
//            tvTitle.setTextColor(resources.getColor(R.color.white))
//            tvGuide0.setTextColor(resources.getColor(R.color.white))
//            tvGuide1.setTextColor(resources.getColor(R.color.white))
//            ivCloseEkyc.setColorFilter(resources.getColor(R.color.white))
//            ivGuide.setColorFilter(resources.getColor(R.color.white))
        } else {
            val mainColor = Color.parseColor(KalapaSDK.config.mainColor)
            val mainTextColor = Color.parseColor(KalapaSDK.config.mainTextColor)
            ivPreviewImage.visibility = View.VISIBLE
//            tvTitle.setTextColor(mainColor)
//            tvGuide0.setTextColor(mainTextColor)
//            tvGuide1.setTextColor(mainTextColor)
//            ivCloseEkyc.setColorFilter(mainColor)
//            ivGuide.setColorFilter(mainColor)
//            cardMaskView.setBackgroundColor(resources.getColor(R.color.black40))
            if (tmpBitmap != null) {
                ivPreviewImage.visibility = View.VISIBLE
                ivPreviewImage.setImageBitmap(tmpBitmap)
            }
//            cardMaskView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
        }
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
        tmpBitmap = BitmapUtil.crop(
            BitmapUtil.rotateBitmapToStraight(tmpBitmap!!, rotationDegree),
            tmpBitmap!!.width,
            tmpBitmap!!.width * 5 / 8,
            0.5f,
            0.5f
        )
        tmpBitmap = BitmapUtil.resizeImageFromGallery(tmpBitmap!!)
        Helpers.printLog("opticalResolution onCaptureSuccess ${tmpBitmap?.width} ${tmpBitmap?.height} ${tmpBitmap?.byteCount}")
        //  cardMaskView.crop(tmpBitmap!!, rotationDegree.toFloat())
    }

    override fun verifyImage() {
        runOnUiThread {
            ProgressView.showProgress(this@CameraXPassportActivity)
        }
        Handler(Looper.getMainLooper()).postDelayed({

            (KalapaSDK.handler as KalapaCaptureHandler).process(
                BitmapUtil.convertBitmapToBase64(tmpBitmap!!),
                KalapaSDKMediaType.PASSPORT,
                this
            )
        }, 100)
    }

    override fun onBackBtnClicked() {
        showEndEkyc()
    }

    override fun onInfoBtnClicked() {
    }

    override fun sendError(message: String?) {
        Helpers.printLog("Failed! ")
        ProgressView.hideProgress()
        this.runOnUiThread {
            if (KalapaSDK.isFoldOpen(this@CameraXPassportActivity)) {
                tvGuide0.visibility = View.INVISIBLE
                tvGuide1.visibility = View.INVISIBLE
            }
            tvError.visibility = View.VISIBLE
            tvError.setTextColor(resources.getColor(R.color.ekyc_red))
            btnNext.visibility = View.INVISIBLE
            tvError.text = message
                ?: KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_liveness_processing_failed))
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

}