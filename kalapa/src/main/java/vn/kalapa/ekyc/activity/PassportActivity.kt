package vn.kalapa.ekyc.activity

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.*
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.fragment.BottomGuideFragment
import vn.kalapa.ekyc.fragment.GuideType
import vn.kalapa.ekyc.utils.*
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView

class PassportActivity : CameraActivity(R.layout.activity_camera, LENS_FACING.REAR, hideAutoCapture = true, refocusFrequency = 30), KalapaSDKCallback {
    private var showingGuide = false
    private val TEXT_SIZE_DIP = 10f
    private var computingDetection: Boolean = false
    private lateinit var res: Res
    lateinit var tvInstruction: TextView
    private lateinit var ivError: ImageView
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
                    KalapaSDK.captureHandler.onError(KalapaCaptureResultCode.USER_LEAVE)
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
            this@PassportActivity,
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_warning_title)),
            KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_emulator_warning_body)), R.drawable.frowning_face
        ) {
            KalapaSDK.nfcHandler.onError(KalapaNFCResultCode.EMULATOR_DETECTED)
            finish()
        }
    }

    private fun renewSession() {
        computingDetection = false
        this.ivError.visibility = View.INVISIBLE
    }

    override fun onRetryClicked() {
        renewSession()
        Helpers.printLog("on retry clicked computingDetection $computingDetection")
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

    override fun updateActiveModel() {
        TODO("Not yet implemented")
    }

    override fun setupCustomUI() {
        tvInstruction = findViewById(R.id.tv_instruction)
        ivError = findViewById(R.id.iv_error)
        tvInstruction.text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_instruction))
        tvInstruction.setOnClickListener(this)
        this.tvError.text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_passport_guide_title))
        this.tvGuide.text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_capture_note))
        this.ivError.visibility = View.INVISIBLE
        this.btnRetry.visibility = View.INVISIBLE
        this.btnNext.visibility = View.INVISIBLE
        Helpers.setBackgroundColorTintList(this.btnNext, KalapaSDK.config.mainColor)
        this.btnNext.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
        Helpers.setBackgroundColorTintList(this.btnCapture, KalapaSDK.config.mainColor)
        findViewById<TextView>(R.id.tv_title).text = KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_title_passport))
        tvGuide.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
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
    }

    override fun verifyImage() {
        runOnUiThread {
            ProgressView.showProgress(this@PassportActivity)
        }
        Handler().postDelayed({
            KalapaSDK.captureHandler.process(BitmapUtil.convertBitmapToBase64(BitmapUtil.resizeBitmapToBitmap(tmpBitmap)), KalapaSDKMediaType.PASSPORT, this)
        }, 100)

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
        showingGuide = true
        Helpers.printLog("On Info Btn Clicked")
        val bottomFragment = BottomGuideFragment(GuideType.PASSPORT)
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

    override fun sendError(message: String?) {
        Helpers.printLog("Failed! ")
        ProgressView.hideProgress()
        this.runOnUiThread {
            tvError.visibility = View.VISIBLE
            tvError.setTextColor(res.getColor(R.color.ekyc_red))
            cardMaskView.dashColor = res.getColor(R.color.ekyc_red)
            btnNext.visibility = View.INVISIBLE
            ivError.setImageDrawable(res.getDrawable(R.drawable.ic_failed_solid))
            ivError.visibility = View.VISIBLE
            tvError.text = message ?: KalapaSDK.config.languageUtils.getLanguageString(res.getString(R.string.klp_liveness_processing_failed))
        }
        Helpers.printLog("onError message: $message")
    }

    override fun sendDone(nextAction: () -> Unit) {
        nextAction()
        runOnUiThread {
            ProgressView.hideProgress()
        }
        KalapaSDK.captureHandler.onError(KalapaCaptureResultCode.SUCCESS)
        finish()
    }


}
