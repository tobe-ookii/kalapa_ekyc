package vn.kalapa.kalapasdk.nfcsdk.activities

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import org.json.JSONObject
import pl.droidsonroids.gif.GifDrawable
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.activity.BaseActivity
import vn.kalapa.ekyc.handlers.GetModelPositionHandler
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView


abstract class BaseNFCActivity : BaseActivity {
    lateinit var bottomSheetDialog: Dialog
    var isNFCSucceed = false
    var isNFCFinished = false

    private val NFC_LOCATION_PATH = "/api/nfc/position"
    lateinit var btnScanNFC: Button
    lateinit var tvError: TextView
    lateinit var tvShowPosition: TextView
    lateinit var llShowPosition: LinearLayout
    var isBottomSheetGuide = false

    lateinit var gifSucess: GifDrawable
    lateinit var gifError: GifDrawable
    private var errorMessage = ""
    var isNFCSupport: Boolean = false
    var isNFCNotEnabled: Boolean = false
    private var nfcFailedTimes = 0
    lateinit var scanGif: GifDrawable
    lateinit var readingGif: GifDrawable
    lateinit var ivCloseEkyc: ImageView
    lateinit var ibNote1: ImageView
    lateinit var ibNote2: ImageView
    lateinit var ibNote3: ImageView
    lateinit var ibNote4: ImageView
    lateinit var tvNote0: TextView

    var nfcUnderScanning = false


    constructor()

    abstract fun onButtonScanClicked()
    abstract fun onUserSkip()
    abstract fun onSucceeded()
    abstract fun onNotAvailable()

    open fun onPostCreated() {
        Helpers.printLog("onPostCreated")
    }

    open fun onNFCAvailable() {
        Helpers.printLog("NFC Supported")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfcactivity)
        initView()
        onPostCreated()
    }

    fun initView() {
        val rootContainer = findViewById<View>(R.id.container)
        rootContainer.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
        ibNote1 = findViewById(R.id.ib_note_1)
        ibNote2 = findViewById(R.id.ib_note_2)
        ibNote3 = findViewById(R.id.ib_note_3)
        ibNote4 = findViewById(R.id.ib_note_4)
        tvNote0 = findViewById(R.id.tv_note_0)
        tvNote0.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_note))
        findViewById<TextView>(R.id.tv_note_1).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_nfc_1))
        findViewById<TextView>(R.id.tv_note_2).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_nfc_2))
        findViewById<TextView>(R.id.tv_note_3).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_nfc_3))
        findViewById<TextView>(R.id.tv_note_4).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_nfc_4))
        scanGif = GifDrawable(resources, R.drawable.scan_nfc)
        readingGif = GifDrawable(resources, R.drawable.reading_nfc_small)
        scanGif.setSpeed(2.0f)
        readingGif.setSpeed(2.0f)
        gifSucess = GifDrawable(resources, R.drawable.gif_success)
        gifError = GifDrawable(resources, R.drawable.gif_error)
        gifSucess.setSpeed(2.0f)
        btnScanNFC = findViewById(R.id.btn_scan_nfc)
        tvError = findViewById(R.id.tv_compatibility_result)
        initBottomSheetDialog()
        btnScanNFC.setOnClickListener {
            onButtonScanClicked()
        }
        tvShowPosition = findViewById(R.id.tv_show_nfc_location)
        btnScanNFC.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_button_start))
        tvShowPosition.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_button_nfc_location))
        llShowPosition = findViewById(R.id.ll_show_nfc_location)
        tvShowPosition.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        llShowPosition.setOnClickListener {
            if (nfcUnderScanning) {
                Toast.makeText(this@BaseNFCActivity, KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_please_wait)), Toast.LENGTH_SHORT).show()
            } else {
                ProgressView.showProgress(this)
                Handler().postDelayed({
                    val model = GetModelPositionHandler(this@BaseNFCActivity).execute(Build.MODEL).get()
                    var image = ""
                    if (model != null && model.isNotEmpty()) {
                        val json = JSONObject(model).getJSONObject("data")
                        image = if (json.has("image")) json.getString("image") else ""
                    }
                    ProgressView.hideProgress()
                    showBottomNFCPosition(if (image.isEmpty()) null else BitmapUtil.base64ToBitmap(image))
                }, 100)
            }
        }
        setCustomUI()
    }

    fun setCustomUI() {
        this.btnScanNFC.let {
            ViewCompat.setBackgroundTintList(
                it,
                ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
            )
        }
        ivCloseEkyc = findViewById(R.id.iv_close_ekyc)
        ivCloseEkyc.setColorFilter(Color.parseColor(KalapaSDK.config.mainTextColor))
        ivCloseEkyc.setOnClickListener {
            Helpers.showEndKYC(this, object : DialogListener {
                override fun onYes() {
                    KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
                    finish()
                }

                override fun onNo() {

                }
            })
        }
        Helpers.setBackgroundColorTintList(ibNote1, KalapaSDK.config.mainColor)
        Helpers.setBackgroundColorTintList(ibNote2, KalapaSDK.config.mainColor)
        Helpers.setBackgroundColorTintList(ibNote3, KalapaSDK.config.mainColor)
        Helpers.setBackgroundColorTintList(ibNote4, KalapaSDK.config.mainColor)
        tvNote0.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
    }

    fun moveToNextScreen() {
//        val intent = Intent(this, ConfirmActivity::class.java)
//        val intent = Intent(this, LivenessActivity::class.java)
//        this.startActivity(intent)
        finish()
    }

    private fun initBottomSheetDialog() {
        bottomSheetDialog = Dialog(this@BaseNFCActivity)
        bottomSheetDialog.setOnDismissListener { isBottomSheetGuide = false }
//        bottomSheetDialog.requestWindowFeature(1)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_nfc)
        bottomSheetDialog.window?.setLayout(-1, -2)
        bottomSheetDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        bottomSheetDialog.window?.setGravity(80)
    }

    open fun hideBottomSheet() {
        this.bottomSheetDialog.dismiss()
    }

    private fun showBottomNFCPosition(bitmap: Bitmap?) {
        if (!this.bottomSheetDialog.isShowing) {
            hideBottomSheet()
            val defaultPosition = bitmap != null
            isBottomSheetGuide = true
            bottomSheetDialog = Dialog(this@BaseNFCActivity)
            bottomSheetDialog.setOnDismissListener { isBottomSheetGuide = false }
//            bottomSheetDialog.requestWindowFeature(1)
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_nfc_position)
            bottomSheetDialog.window?.setLayout(-1, -2)
            bottomSheetDialog.window?.setBackgroundDrawable(ColorDrawable(0))
            bottomSheetDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            bottomSheetDialog.window?.setGravity(80)
            val tvTitle = this.bottomSheetDialog.findViewById<TextView>(R.id.tv_title)
            this.bottomSheetDialog.findViewById<TextView>(R.id.tv_klp_guide_nfc_position_1).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_nfc_position_1))
            this.bottomSheetDialog.findViewById<TextView>(R.id.tv_klp_guide_nfc_position_2).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_nfc_position_2))
            tvTitle.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_location_title)) // nfc_location_title
            val btnUnderstand = bottomSheetDialog.findViewById<Button>(R.id.btn_understand)
            btnUnderstand.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_guide_button_close))
            btnUnderstand.setOnClickListener {
                hideBottomSheet()
                isBottomSheetGuide = false
            }
            Helpers.setBackgroundColorTintList(btnUnderstand, KalapaSDK.config.mainColor)
            btnUnderstand.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))

            if (bitmap != null) {
                bottomSheetDialog.findViewById<LinearLayout>(R.id.ll_description_1).visibility = View.GONE
                bottomSheetDialog.findViewById<LinearLayout>(R.id.ll_description_2).visibility = View.GONE
                bottomSheetDialog.findViewById<ImageView>(R.id.iv_nfc_position)
                    .setImageBitmap(bitmap)
            } else {
                bottomSheetDialog.findViewById<ImageView>(R.id.iv_nfc_position)
                    .setImageDrawable(resources.getDrawable(R.drawable.sample_position))
            }

            this.bottomSheetDialog.show()
        }
    }


    fun showBottomSheet() {
        initBottomSheetDialog()
        try {
            (this.bottomSheetDialog.findViewById(R.id.text_des) as TextView).text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_reading_message_1))
            val btnCancel = this.bottomSheetDialog.findViewById<Button>(R.id.btn_cancel)
            val tvStatus = this.bottomSheetDialog.findViewById<TextView>(R.id.text_status)
            val tvDescription = this.bottomSheetDialog.findViewById<TextView>(R.id.text_des)
            val tvTitle = this.bottomSheetDialog.findViewById<TextView>(R.id.tv_title)
            btnCancel.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_button_cancel))
            btnCancel.setOnClickListener { hideBottomSheet() }
            Helpers.setBackgroundColorTintList(btnCancel, KalapaSDK.config.mainColor)
            btnCancel.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
            tvDescription.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvStatus.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            tvStatus.visibility = View.VISIBLE
            tvStatus.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_reading_title_ready))
            tvTitle.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_title))
            tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
//            (this.bottomSheetDialog.findViewById(id.btnCancel) as TextView).setText(CustomSdk.Companion.getTextCancel())

            (this.bottomSheetDialog.findViewById(R.id.iv_gif) as ImageView).setImageDrawable(
                scanGif
            )
            (this.bottomSheetDialog.findViewById(R.id.main_bottom_layout) as ConstraintLayout).visibility =
                View.VISIBLE
            this.bottomSheetDialog.show()
        } catch (var2: Exception) {
        }
    }

    fun onNFCSucceed() {
        isNFCSucceed = true
        (this.bottomSheetDialog.findViewById(R.id.text_des) as TextView).text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_read_successfully))
        tvError.setTextColor(Color.GREEN)
        tvError.text = KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_nfc_read_successfully))
        (this.bottomSheetDialog.findViewById(R.id.text_des) as TextView).setTextColor(Color.GREEN)
        onSucceeded()
    }

    fun onNFCErrorHandleUI(p0: String?) {
        nfcFailedTimes++
        Helpers.printLog("OnError $p0 $nfcFailedTimes Threshold ${KalapaSDK.config.minNFCRetry}")
        errorMessage = p0.toString()
        if (bottomSheetDialog.isShowing && !isBottomSheetGuide) {
            (this.bottomSheetDialog.findViewById(R.id.text_status) as TextView).visibility = View.GONE
            (this.bottomSheetDialog.findViewById(R.id.text_des) as TextView).text = errorMessage
            (this.bottomSheetDialog.findViewById(R.id.text_des) as TextView).setTextColor(
                resources!!.getColor(
                    R.color.ekyc_red
                )
            )
        }
    }


    override fun onBackPressed() {
//        super.onBackPressed()
        if (isBottomSheetGuide) {
            hideBottomSheet()
            isBottomSheetGuide = false
        } else {
            Helpers.showEndKYC(this, object : DialogListener {
                override fun onYes() {
                    KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
                    finish()
                }

                override fun onNo() {

                }
            })
        }
    }
}