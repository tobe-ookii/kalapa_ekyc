package vn.kalapa.ekyc.activity

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.*
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.utils.*
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.nfcsdk.activities.NFCActivity

class MRZScannerActivity : CameraActivity(R.layout.activity_camera_mrz, LENS_FACING.REAR, hideAutoCapture = true, refocusFrequency = 30) {
    private var showingGuide = false
    private val TEXT_SIZE_DIP = 10f
    private var rgbFrameBitmap: Bitmap? = null
    private var faceDetected = false
    private val TAG = "MRZScannerActivity"
    private var computingDetection: Boolean = false
    private lateinit var res: Res
    private lateinit var ivGuide: ImageView
    //    private lateinit var ivBitmapPreview: ImageView
    override fun getResources(): Resources? {
        res = Res(
            super.getResources(),
            KalapaSDK.config.mainColor,
            KalapaSDK.config.mainTextColor,
            KalapaSDK.config.backgroundColor
        )
        return res
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
                    KalapaSDK.nfcHandler.onError(KalapaNFCResultCode.USER_LEAVE)
                    finish()
                }

                override fun onNo() {

                }
            })
        } else {
            previewViewLayerMode(true)
            Helpers.printLog("On Back Clicked & Preview Mode... Finish Activity...")
        }
    }

    private val DESIRED_PREVIEW_SIZE = Size(320, 320)

    override fun onBackPressed() {
        onBackBtnClicked()
    }


    override fun onResume() {
        super.onResume()
        renewSession()
    }

    override fun onPause() {
        super.onPause()
        computingDetection = false
    }

    override fun onEmulatorDetected() {
        Helpers.showDialog(
            this@MRZScannerActivity,
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_warning_title)),
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_emulator_warning_body)), R.drawable.frowning_face
        ) {
            KalapaSDK.nfcHandler.onError(KalapaNFCResultCode.EMULATOR_DETECTED)
            finish()
        }
    }

    private fun renewSession() {
        cardMaskView.dashColor = res.getColor(R.color.white)
        idCardNumbersMap.clear()
        faceDetected = false
        computingDetection = false
    }

    override fun onRetryClicked() {
        renewSession()
        Helpers.printLog("on retry clicked computingDetection $computingDetection")
    }

    override fun showEndEkyc() {
        Helpers.showEndKYC(this, object : DialogListener {
            override fun onYes() {
                KalapaSDK.nfcHandler.onError(KalapaNFCResultCode.USER_LEAVE)
                finish()
            }

            override fun onNo() {

            }
        })
    }

    override fun updateActiveModel() {
        TODO("Not yet implemented")
    }

    override fun setupCustomUI() {
//        ivBitmapPreview = findViewById(R.id.iv_test_image)
        ivGuide = findViewById(R.id.iv_action)
        this.tvError.text = ""
        this.tvGuide.text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_scan_back_document))

//        this.ivError.visibility = View.INVISIBLE
        this.btnRetry.visibility = View.INVISIBLE
        this.btnNext.visibility = View.INVISIBLE
        cardMaskView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
        Helpers.setBackgroundColorTintList(this.btnNext, KalapaSDK.config.mainColor)
        this.btnNext.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
//        Helpers.setBackgroundColorTintList(this.btnCapture, KalapaSDK.config.mainColor)
        findViewById<TextView>(R.id.tv_title).text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_scan_mrz))
        tvGuide.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))

        //        Helpers.setBackgroundColorTintList(this.btnCapture, KalapaSDK.config.mainColor)
        btnCapture.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
        ivCloseEkyc.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
        ivGuide.setColorFilter(Color.parseColor(KalapaSDK.config.mainColor))
    }

    private fun setCircleViewAnimation(status: AnimStatus) {
//        Helpers.printLog("Set Anim: ${status.name}")
        runOnUiThread {
            when (status) {
                AnimStatus.ANIM_SUCCESS -> cardMaskView.dashColor = res.getColor(R.color.ekyc_green)
                AnimStatus.ANIM_WARNING -> cardMaskView.dashColor = res.getColor(R.color.ekyc_red)
                AnimStatus.ANIM_FAILED -> cardMaskView.dashColor = res.getColor(R.color.ekyc_red)
                AnimStatus.ANIM_LOADING -> cardMaskView.dashColor = res.getColor(R.color.white)
                else -> cardMaskView.dashColor = Color.parseColor(KalapaSDK.config.mainColor) // PROCESSING
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

            val matrix = Matrix()
            // Mirror is basically a rotation
            matrix.setScale(1f, 1f)
            matrix.postRotate(getCameraRotationDegree().toFloat())
            // so you got to move your bitmap back to it's place. otherwise you will not see it
//            ivError.setGifImageResource(R.drawable.gif_success)
            tmpBitmap = Bitmap.createBitmap(
                rgbFrameBitmap!!,
                rgbFrameBitmap!!.width / 2 - 1,
                0,
                rgbFrameBitmap!!.width / 2,
                rgbFrameBitmap!!.height,
                matrix,
                false
            )
//            runOnUiThread {
//                ivBitmapPreview.setImageBitmap(tmpBitmap)
//            }
            processFrame(tmpBitmap)
        }
    }

    override fun verifyImage() {

    }

    private fun processFrame(bitmap: Bitmap): String {
        // When using Latin script library
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, getCameraRotationDegree()))
            .addOnSuccessListener {
                if (it.textBlocks.size > 0) {
                    for (line in it.textBlocks) {
//                        Helpers.printLog("$TAG text blocks ${line.text}")
                        val idCardNumber = getIdCardNumber(line.text)
                        if (idCardNumber != null && idCardNumber.isNotEmpty()) {
                            setCircleViewAnimation(AnimStatus.ANIM_PROCESSING)
                            returnToNFCActivity(line.text)
                        }
                    }
                }
                computingDetection = false
            }.addOnFailureListener {
                Helpers.printLog("$TAG addOnFailureListener $it")
                computingDetection = false
            }
//        val cardReader = IDCardReader()
//        cardReader.parseMRZImage("", bitmap)
        return ""
    }

    private var enteredNFCActivity = false
    private fun returnToNFCActivity(ocrMRZ: String) {
        if (!enteredNFCActivity) {
            enteredNFCActivity = true
            val intent = Intent(this@MRZScannerActivity, NFCActivity::class.java)
            intent.putExtra("mrz", ocrMRZ)
            startActivity(intent)
            finish()
        }

    }

    var idCardNumbersMap = HashMap<String, Int>()

    private fun getIdCardNumber(input: String): String? {
        return Common.getIdCardNumberFromMRZ(input)
//        if (idCard != null && idCard.isNotEmpty()) {
//            if (idCardNumbersMap.contains(idCard)) {
//                idCardNumbersMap[idCard] = idCardNumbersMap[idCard]!! + 1
//                if (idCardNumbersMap[idCard]!! > 0) {
//                    return idCard
//                }
//            } else
//                idCardNumbersMap[idCard] = 1
//            Helpers.printLog("$TAG Matched value: $idCard - ${idCardNumbersMap[idCard]}")
//        }
//        return null
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
//        val bottomFragment = BottomGuideFragment(GuideType.MRZ)
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

}
