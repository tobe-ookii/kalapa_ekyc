package vn.kalapa.ekyc.nfcsdk.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.fis.ekyc.nfc.build_in.model.ResultCode
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.capturesdk.CameraXMRZActivity
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.managers.KLPNFCUtils
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView
import vn.kalapa.kalapasdk.nfcsdk.activities.BaseNFCActivity

class NFCActivity : BaseNFCActivity(), DialogListener, KalapaSDKCallback {
    private val TAG = NFCActivity::class.java.simpleName
    private val nfcUtils: KLPNFCUtils = KLPNFCUtils(this@NFCActivity)
    private var mrz: String? = null
    private var idCardNumber: String? = null
    private var transactionId: String? = null
    private lateinit var llPleaseMakeSure: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var scrollview: ScrollView

    private fun validateMRZOrOpenMRZScanner() {
        if (intent.getStringExtra("mrz") != null) {
            mrz = intent.getStringExtra("mrz")
            idCardNumber = Common.getIdCardNumberFromMRZ(mrz ?: "")
            Helpers.printLog("idCardNumber: $idCardNumber from mrz $mrz")
        }
        if (idCardNumber == null || idCardNumber == "-1") {
            if (idCardNumber == "-1") {
                Helpers.showDialog(
                    this@NFCActivity,
                    KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown)),
                    KLPLanguageManager.get(resources.getString(R.string.klp_error_invalid_format)), null
                ) {
                    openMRZScanner()
                }
            } else
                openMRZScanner()
        } else
            nfcUtils.setIdCardNumber(idCardNumber)
    }

    override fun onDestroy() {
        super.onDestroy()
        mrz = ""
    }

    private fun getIntentData() {
        KLPNFCUtils.checkNFCCapacity(this@NFCActivity, {
            validateMRZOrOpenMRZScanner()
        }, {
            // Do nothing
        })
    }

    private fun openMRZScanner() {
        val intent = Intent(this@NFCActivity, CameraXMRZActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onPostCreated() {
        super.onPostCreated()
        llPleaseMakeSure = findViewById(R.id.ll_please_make_sure)
        initNFC()
    }

    override fun onButtonScanClicked() {
        if (nfcUnderScanning) Toast.makeText(
            this@NFCActivity,
            KLPLanguageManager.get(resources.getString(R.string.klp_please_wait)),
            Toast.LENGTH_LONG
        ).show()
        isNFCFinished = false
        isNFCSucceed = false
        nfcUtils.forceCall = true
        nfcUtils.callOnPause()
        nfcUtils.callOnResume()
        showBottomSheet()
    }

    override fun onUserSkip() {
        KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
        finish()
    }

    override fun onSucceeded() {
//        TODO("Not yet implemented")
//        if (KalapaSDK.flowType == FaceOTPFlowType.ONBOARD) {
//            startLivenessForFaceData()
//        } else {
//            KalapaNFC.nfcListener?.onSuccess("")
//        }
    }

    override fun onNotAvailable() {
//        TODO("Not yet implemented")
//        KalapaNFC.nfcListener?.onCheckNFCAvailable(NFCAvailablityStatus.NOT_ENABLED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tvTitle = findViewById(R.id.tv_title)
        tvTitle.text = KLPLanguageManager.get(resources.getString(R.string.klp_nfc_title))
        tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
        scrollview = findViewById(R.id.scrollView)
        scrollview.post {
            scrollview.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun getMesageFromErrorCode(errorCode: ResultCode?): String {
        return when (errorCode) {
            ResultCode.CANNOT_OPEN_DEVICE -> KLPLanguageManager.get(
                resources.getString(R.string.klp_nfc_not_support)
            )

            ResultCode.CARD_LOST_CONNECTION -> KLPLanguageManager.get(
                resources.getString(R.string.klp_nfc_error_card_lost_connection)
            )

            ResultCode.CARD_NOT_FOUND -> KLPLanguageManager.get(
                resources.getString(
                    R.string.klp_nfc_error_wrong_id_card
                )
            )

            ResultCode.SUCCESS_WITH_WARNING -> KLPLanguageManager.get(
                resources.getString(R.string.klp_nfc_read_successfully)
            )

            ResultCode.UNKNOWN -> KLPLanguageManager.get(
                resources.getString(
                    R.string.klp_error_unknown
                )
            )

            ResultCode.WRONG_CCCDID -> KLPLanguageManager.get(
                resources.getString(R.string.klp_nfc_invalid_mrz)
            )

            else -> ""
        }
    }

    private fun initNFC() {
        nfcUtils.init(this@NFCActivity)
            .setListener(object : NFCUtils.NFCListener {
                override fun OnSuccess(p0: String?) {
                    runOnUiThread {
                        if (bottomSheetDialog.isShowing && !isBottomSheetGuide)
                            (bottomSheetDialog.findViewById(R.id.iv_gif) as ImageView).setImageDrawable(
                                gifSucess
                            )
                    }
                    isNFCFinished = true
                    ProgressView.hideProgress()
                    //data result here
                    Helpers.printLog("NFCActivity initNFC OnSuccess $p0")
                    btnScanNFC.visibility = View.VISIBLE
                    runOnUiThread {
                        Toast.makeText(
                            this@NFCActivity,
                            KLPLanguageManager.get(resources.getString(R.string.klp_nfc_read_successfully)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    runOnUiThread {
                        ProgressView.showProgress(this@NFCActivity)
                    }
                    (KalapaSDK.handler as KalapaNFCHandler).process(
                        Common.getIdCardNumberFromMRZ(mrz)!!,
                        p0!!,
                        this@NFCActivity
                    )
                }

                override fun OnFail(p0: String?) {
                    //message on fail
                    Helpers.printLog("NFCActivity initNFC  OnFail $p0")
                    onNFCErrorHandleUI(p0)
                    (bottomSheetDialog.findViewById<ImageView>(R.id.iv_gif)!!).setImageDrawable(
                        gifError
                    )
                    (bottomSheetDialog.findViewById<TextView>(R.id.tv_title)!!).text =
                        KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown))
                }

                override fun OnError(p0: ResultCode?) {
                    //error with code here
                    runOnUiThread {
                        Helpers.printLog("NFCActivity initNFC  OnError $p0")
                        ProgressView.hideProgress()
                        if (bottomSheetDialog.isShowing && !isBottomSheetGuide)
                            (bottomSheetDialog.findViewById<ImageView>(R.id.iv_gif)!!).setImageDrawable(
                                gifError
                            )
                        onNFCErrorHandleUI(getMesageFromErrorCode(p0))
                    }
                    p0?.let {
                        when (p0) {
                            ResultCode.CARD_NOT_FOUND -> KalapaSDKResultCode.CARD_NOT_FOUND
                            ResultCode.CARD_LOST_CONNECTION -> KalapaSDKResultCode.CARD_LOST_CONNECTION
                            ResultCode.SUCCESS -> KalapaSDKResultCode.SUCCESS
                            ResultCode.CANNOT_OPEN_DEVICE -> KalapaSDKResultCode.CANNOT_OPEN_DEVICE
                            ResultCode.UNKNOWN -> KalapaSDKResultCode.UNKNOWN
                            ResultCode.WRONG_CCCDID -> KalapaSDKResultCode.WRONG_CCCDID
                            ResultCode.SUCCESS_WITH_WARNING -> KalapaSDKResultCode.SUCCESS_WITH_WARNING
                        }
                    }
                }

                @SuppressLint("CutPasteId")
                override fun OnStartProcess() {
                    if (!bottomSheetDialog.isShowing)
                        showBottomSheet()
                    else {
                        bottomSheetDialog.dismiss()
                        showBottomSheet()
                    }
                    (bottomSheetDialog.findViewById(R.id.iv_gif) as ImageView).setImageDrawable(
                        readingGif
                    )
                    //show progress or something with your UI here
                    Helpers.printLog("NFCActivity initNFC  onStartProcess")
                    nfcUnderScanning = true
                    (bottomSheetDialog.findViewById(R.id.text_status) as TextView).visibility =
                        View.VISIBLE
                    (bottomSheetDialog.findViewById(R.id.text_status) as TextView).text =
                        KLPLanguageManager.get(resources.getString(R.string.klp_nfc_reading_title_reading))
                    (bottomSheetDialog.findViewById(R.id.text_des) as TextView).text =
                        KLPLanguageManager.get(resources.getString(R.string.klp_nfc_reading_message_2))
                    (bottomSheetDialog.findViewById(R.id.tv_title) as TextView).text =
                        KLPLanguageManager.get(resources.getString(R.string.klp_nfc_reading_title_reading))
                    (bottomSheetDialog.findViewById(R.id.text_des) as TextView).setTextColor(
                        Color.parseColor(KalapaSDK.config.mainTextColor)
                    )
                    (bottomSheetDialog.findViewById(R.id.text_status) as TextView).setTextColor(
                        Color.parseColor(KalapaSDK.config.mainTextColor)
                    )

                }

                override fun OnProcessFinished() {
                    //hide progress or something with your UI here
                    nfcUnderScanning = false
                    Helpers.printLog("NFCActivity initNFC onProcessFinished")
                }

                override fun CheckNFCAvailable(p0: Int) {
                    //result of NFC status
                    Helpers.printLog("NFCActivity initNFC OnNFCAvailable $p0")
                    runOnUiThread {
                        isNFCSupport = p0 >= 0
                        isNFCNotEnabled = p0 == 0
                        Helpers.printLog("NFCActivity initNFC isNFCSupport $isNFCSupport isNFCNotEnabled $isNFCNotEnabled")
                        if (!isNFCNotEnabled && isNFCSupport) {
                            tvError.setTextColor(resources.getColor(R.color.ekyc_green))
                            tvError.visibility = View.INVISIBLE
                            btnScanNFC.visibility = View.VISIBLE
                            llPleaseMakeSure.visibility = View.VISIBLE
                            llShowPosition.visibility = View.VISIBLE
                            btnScanNFC.text = KLPLanguageManager.get(resources.getString(R.string.klp_nfc_button_start))
                            btnScanNFC.setOnClickListener {
                                onButtonScanClicked()
                            }
                        } else {
                            llPleaseMakeSure.visibility = View.GONE
                            llShowPosition.visibility = View.GONE
                            nfcUtils.forceCall = false
                            btnScanNFC.visibility = View.GONE
                            tvError.text =
                                if (isNFCNotEnabled) {
                                    btnScanNFC.visibility = View.VISIBLE
                                    btnScanNFC.text = KLPLanguageManager.get(resources.getString(R.string.klp_button_confirm))
                                    btnScanNFC.setOnClickListener {
                                        KLPNFCUtils.openNFCSetting(this@NFCActivity)
                                    }
                                    KLPLanguageManager.get(
                                        resources.getString(
                                            R.string.klp_nfc_error_not_enabled
                                        )
                                    )
                                } else {
                                    btnScanNFC.visibility = View.VISIBLE
                                    btnScanNFC.text = KLPLanguageManager.get(resources.getString(R.string.klp_button_confirm))
                                    btnScanNFC.setOnClickListener {
                                        KalapaSDK.handler.onError(KalapaSDKResultCode.DEVICE_NOT_SUPPORTED)
                                        finish()
                                    }
                                    KLPLanguageManager.get(resources.getString(R.string.klp_nfc_not_support))
                                }

                            tvError.setTextColor(resources!!.getColor(R.color.ekyc_red))
                            tvError.visibility = View.VISIBLE
                            bottomSheetDialog.dismiss()
                        }
                    }

                }

            })

    }

    override fun sendError(errorMess: String?) {
        Helpers.printLog("callback onError $errorMess")
        this.runOnUiThread {
            val message = errorMess
                ?: KLPLanguageManager.get(resources.getString(R.string.klp_liveness_result_fail))
            Helpers.showDialog(
                this@NFCActivity,
                KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown)),
                message,
                R.drawable.ic_warning
            ) {
                ProgressView.hideProgress()
                if (bottomSheetDialog.isShowing)
                    bottomSheetDialog.dismiss()
            }
            tvError.text = message
        }
    }

//    override fun onDestroy() {
//        Helpers.dismissDialogIfNeeded()
//        super.onDestroy()
//    }

    override fun sendDone(nextAction: () -> Unit) {
        Helpers.printLog("callback sendDone")
        nextAction()
        runOnUiThread {
            ProgressView.hideProgress()
            if (bottomSheetDialog.isShowing) bottomSheetDialog.dismiss()
        }
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        nfcUtils.callOnNewIntent(intent)
    }

    override fun onResume() {
        Helpers.printLog("$TAG - onResume")
        super.onResume()
//        nfcUtils.isOnBackground = false
        getIntentData()
        nfcUtils.callOnResume()
    }

    override fun onPause() {
        super.onPause()
//        nfcUtils.isOnBackground = true
//        Helpers.printLog("$TAG - NFCActivity - onPause - isOnBackground ${nfcUtils.isOnBackground}")
        nfcUtils.callOnPause()
    }

    override fun hideBottomSheet() {
        super.hideBottomSheet()
        nfcUtils.callOnPause()
    }

    override fun onEmulatorDetected() {
        Helpers.showDialog(
            this@NFCActivity,
            KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown)),
            KLPLanguageManager.get(resources.getString(R.string.klp_error_emulator)),
            R.drawable.frowning_face
        ) {
            KalapaSDK.handler.onError(KalapaSDKResultCode.EMULATOR_DETECTED)
            finish()
        }
    }

    override fun onYes() {
        KalapaSDK.handler.onError(KalapaSDKResultCode.USER_LEAVE)
        finish()
    }

    override fun onNo() {
    }
}
