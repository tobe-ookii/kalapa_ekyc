package vn.kalapa.ekyc

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.fis.ekyc.nfc.build_in.model.ResultCode
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils.NFCListener
import org.json.JSONObject
import vn.kalapa.R
import vn.kalapa.ekyc.capturesdk.CameraXPassportActivity
import vn.kalapa.ekyc.activity.CameraXSelfieActivity
import vn.kalapa.ekyc.activity.ConfirmActivity
import vn.kalapa.ekyc.capturesdk.CameraXAutoCaptureActivity
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.models.BackResult
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.FrontResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.models.MRZData
import vn.kalapa.ekyc.models.NFCRawData
import vn.kalapa.ekyc.networks.Client
import vn.kalapa.ekyc.networks.KalapaAPI
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.nfcsdk.activities.NFCActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.views.ProgressView

class KalapaSDK private constructor(
    private val activity: Activity,
    config: KalapaSDKConfig,
    private val mrz: String = "",
    private val faceData: String = "",
    private val leftoverSession: String = ""
) {
    init {
        Companion.config = config
    }

    private var startTime: Long? = null
    private fun sdkIsJustRan(): Boolean {
        if (startTime == null || System.currentTimeMillis() - startTime!! > 2000) {
            startTime = System.currentTimeMillis()
            return false
        } else
            return true
    }

    class KalapaSDKBuilder(private val activity: Activity, private val config: KalapaSDKConfig) {
        private var mrz = ""
        private var faceData = ""
        private var leftoverSession = ""

        fun build(): KalapaSDK {
            Helpers.printLog("KalapaSDKBuilder build() $mrz - $faceData - $leftoverSession")
            return KalapaSDK(activity, config, mrz, faceData, leftoverSession)
        }

        fun withMrz(mrz: String?): KalapaSDKBuilder {
            Helpers.printLog("KalapaSDKBuilder - withMrz $mrz")
            this.mrz = mrz ?: ""
            this.leftoverSession = ""
            return this
        }

        fun withLeftoverSession(leftoverSession: String?): KalapaSDKBuilder {
            Helpers.printLog("KalapaSDKBuilder - withLeftoverSession")
            this.mrz = ""
            this.faceData = ""
            this.leftoverSession = leftoverSession ?: ""
            return this
        }

        fun withFaceData(faceData: String?): KalapaSDKBuilder {
            this.faceData = faceData ?: ""
            this.leftoverSession = ""
            return this
        }

    }


    companion object {
        private val VERSION = "2.10.0"

        fun getSDKVersion(): String {
            return VERSION
        }

        var kalapaResult = KalapaResult()
        lateinit var frontResult: FrontResult
        private lateinit var backResult: BackResult
        internal lateinit var config: KalapaSDKConfig
        internal lateinit var handler: KalapaHandler
        internal var scanNFCCallback: KalapaScanNFCCallback? = null
        internal lateinit var session: String
        internal lateinit var ekycFlow: KalapaFlowType
        private fun refreshSession() {
            Helpers.printLog("Refresh session")
            this.kalapaResult = KalapaResult()
            this.frontResult = FrontResult()
            this.backResult = BackResult()
            this.faceBitmap = null
            this.frontBitmap = null
            this.backBitmap = null
            this.session = ""
        }

        var faceBitmap: Bitmap? = null
        var frontBitmap: Bitmap? = null
        var backBitmap: Bitmap? = null

        internal fun isConfigInitialized(): Boolean {
            return this::config.isInitialized
        }

        internal fun isHandlerInitialized(): Boolean {
            return this::handler.isInitialized
        }

        internal fun isFrontAndBackResultInitialized(): Boolean {
            return this::frontResult.isInitialized
        }

        fun isFoldOpen(activity: Context): Boolean {
            val metrics = activity.resources.displayMetrics
            var isFoldOpen = metrics.heightPixels * 1f / metrics.widthPixels < 1.2f
//            Helpers.printLog("isFoldOpen: $isFoldOpen isFoldDevice ${metrics.heightPixels} ${metrics.widthPixels}")
            return isFoldOpen
        }

        fun isFoldDevice(activity: Context): Boolean {
            val metrics = activity.resources.displayMetrics
            var isFoldDevice =
                metrics.heightPixels * 1f / metrics.widthPixels > 2.4f || metrics.heightPixels * 1f / metrics.widthPixels < 1.2f
            Helpers.printLog("isFoldDevice: isFoldDevice $isFoldDevice ${metrics.heightPixels} ${metrics.widthPixels}")
            return isFoldDevice
        }

        fun isTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        }

        fun checkNFCCapacity(activity: Activity): KalapaSDKNFCStatus {
            val nfcUtils = NFCUtils()
            var status: KalapaSDKNFCStatus? = null
            nfcUtils.init(activity).setListener(object : NFCListener {
                override fun OnSuccess(p0: String?) {
                    TODO("Not yet implemented")
                }

                override fun OnFail(p0: String?) {
                    TODO("Not yet implemented")
                }

                override fun OnError(p0: ResultCode?) {
                    TODO("Not yet implemented")
                }

                override fun OnStartProcess() {
                    TODO("Not yet implemented")
                }

                override fun OnProcessFinished() {
                    TODO("Not yet implemented")
                }

                override fun CheckNFCAvailable(p0: Int) {
                    Helpers.printLog("NFCActivity initNFC OnNFCAvailable $p0")
                    val isNFCSupport = p0 >= 0
                    val isNFCNotEnabled = p0 == 0
                    Helpers.printLog("NFCActivity initNFC isNFCSupport $isNFCSupport isNFCNotEnabled $isNFCNotEnabled")
                    status = if (!isNFCNotEnabled && isNFCSupport) {
                        KalapaSDKNFCStatus.SUPPORTED
                    } else {
                        if (isNFCNotEnabled)
                            KalapaSDKNFCStatus.NOT_ENABLED
                        else
                            KalapaSDKNFCStatus.NOT_SUPPORTED
                    }
                }
            }).callOnResume()
            while (true) {
                if (status != null)
                    return status as KalapaSDKNFCStatus
            }
        }
    }

    private fun startLivenessForResult(
        faceData: String = "",
        handler: KalapaCaptureHandler
    ) {
        Companion.handler = handler
        val intent = Intent(activity, CameraXSelfieActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        Helpers.printLog("FaceData: $faceData")
        CameraXSelfieActivity.faceData = faceData
        activity.startActivity(intent)
    }

    private fun startCaptureForResult(
        documentType: KalapaSDKMediaType = KalapaSDKMediaType.FRONT,
        handler: KalapaCaptureHandler
    ) {
        Companion.handler = handler
        val intent = Intent(activity, CameraXAutoCaptureActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("document_type", documentType.name)
        activity.startActivity(intent)
    }


    private fun startConfirmForResult(
        leftoverSession: String,
        handler: KalapaHandler
    ) {
        Companion.handler = handler
        val intent = Intent(activity, ConfirmActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("leftover_session", leftoverSession)
        activity.startActivity(intent)
    }


    private fun startCapturingPassportForResult(handler: KalapaCaptureHandler) {
        Companion.handler = handler
        val intent = Intent(activity, CameraXPassportActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        activity.startActivity(intent)
    }

    private fun startNFCForResult(mrz: String = "", nfcHandler: KalapaNFCHandler) {
        Helpers.printLog("startNFCForResult $mrz")
        Companion.handler = nfcHandler
        val intent = Intent(activity, NFCActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("mrz", mrz)
        activity.startActivity(intent)
    }

    fun startCustomFlow(withCaptureScreen: Boolean, withLivenessScreen: Boolean, withNFCScreen: Boolean, kalapaCustomHandler: KalapaHandler) {
        if (sdkIsJustRan()) return
        refreshSession()
        ekycFlow = if (withCaptureScreen) KalapaFlowType.NFC_EKYC else KalapaFlowType.NFC_ONLY
        val complete = { kalapaCustomHandler.onComplete(KalapaResult()) }
        val nfcScreen = {
            startNFCForResult(mrz, object : KalapaNFCHandler() {
                override fun process(idCardNumber: String, nfcData: String, callback: KalapaSDKCallback) {
                    if (ekycFlow == KalapaFlowType.NFC_ONLY)
                        callback.sendDone {}
                    else
                        callback.sendDone(complete)
                }

                override fun onExpired() {

                }

                override fun onNFCErrorHandle(activity: Activity, error: KalapaScanNFCError, callback: KalapaScanNFCCallback) {
                    kalapaCustomHandler.onNFCErrorHandle(activity, error, callback)
                }

                override fun onNFCSkipButtonClicked(callback: KalapaScanNFCCallback) {
                    callback.close(complete)
                }

            })
        }

        val livenessScreen = {
            startLivenessForResult(faceData, object : KalapaCaptureHandler() {
                override fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback) {
                    faceBitmap = BitmapUtil.base64ToBitmap(base64)
                    if (withNFCScreen)
                        callback.sendDone(nfcScreen)
                    else
                        callback.sendDone(complete)
                }

                override fun onExpired() {
                }
            })
        }
        val captureScreen = {
            startCaptureForResult(KalapaSDKMediaType.FRONT, object : KalapaCaptureHandler() {
                override fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback) {
                    frontBitmap = BitmapUtil.base64ToBitmap(base64)
                    startCaptureForResult(KalapaSDKMediaType.BACK, object : KalapaCaptureHandler() {
                        override fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback) {
                            backBitmap = BitmapUtil.base64ToBitmap(base64)
                            if (withLivenessScreen)
                                callback.sendDone(livenessScreen)
                            else if (withNFCScreen)
                                callback.sendDone(nfcScreen)
                            else
                                callback.sendDone(complete)
                        }

                        override fun onExpired() {
                        }

                    })
                }

                override fun onExpired() {

                }

            })
        }
        if (withCaptureScreen) captureScreen()
        else if (withLivenessScreen) livenessScreen()
        else nfcScreen()
    }


    private val defaultFlowHandler = object : IKalapaRawDataProcessor {
        override fun processLivenessData(portraitBase64: String, completion: Client.RequestListener) {
            val endpoint = "/api/kyc/app/check-selfie?lang=${Companion.config.language}"
            KalapaAPI.selfieCheck(endpoint, faceBitmap!!, leftoverSession, completion)
        }

        override fun processCaptureData(documentBase64: String, documentType: KalapaSDKMediaType, completion: Client.RequestListener) {
            val frontImageCheckURL = "/api/kyc/app/scan-front?lang=${Companion.config.language}"
            val backImageCheckURL = "/api/kyc/app/scan-back?lang=${Companion.config.language}"
            if (documentType == KalapaSDKMediaType.FRONT || documentType == KalapaSDKMediaType.BACK)
                KalapaAPI.imageCheck(
                    if (documentType == KalapaSDKMediaType.FRONT) frontImageCheckURL else backImageCheckURL,
                    BitmapUtil.base64ToBitmap(documentBase64), leftoverSession, completion
                )
            else
                completion.fail(KalapaError.UnknownError)
        }

        override fun processNFCData(idCardNumber: String, nfcRawData: String, completion: Client.RequestListener) {
            val nfcCheckURL = "/api/nfc/verify"
            KalapaAPI.nfcCheck(endPoint = nfcCheckURL, body = NFCRawData.fromJson(nfcRawData), leftoverSession, completion)
        }
    }

    fun start(session: String, flow: String, kalapaHandler: KalapaHandler) {
        start(session, flow, kalapaHandler, defaultFlowHandler)
    }

    fun start(
        session: String,
        flow: String,
        kalapaHandler: KalapaHandler,
        kalapaRawDataProcessor: IKalapaRawDataProcessor
    ) {
        if (sdkIsJustRan()) return
        refreshSession()
        Companion.session = session
        var leftoverSessionMRZ: String? = null
        val sessionFlow = KalapaFlowType.ofFlow(flow)
        ekycFlow = sessionFlow
        if (config.baseURL.isEmpty() || !config.baseURL.contains("http") || sessionFlow == KalapaFlowType.NA) {
            kalapaHandler.onError(KalapaSDKResultCode.CONFIGURATION_NOT_ACCEPTABLE)
            return
        }
        config.withFlow(sessionFlow)
        val onGeneralError: (resultCode: KalapaSDKResultCode) -> Unit = {
            kalapaHandler.onError(it)
        }

        /*****-STEP 5-*****/
        val localStartConfirmForResult = {
            startConfirmForResult(leftoverSession, object : KalapaHandler() {
                override fun onExpired() {
                    kalapaHandler.onExpired()
                }

                override fun onError(resultCode: KalapaSDKResultCode) {
                    onGeneralError(resultCode)
                }

                override fun onComplete(kalapaResult: KalapaResult) {
                    super.onComplete(kalapaResult)
                    Helpers.printLog("startFullEKYC localStartConfirmForResult onComplete KalapaResult: $kalapaResult \n ${kalapaResult.decision} ${kalapaResult.decision_detail} ")
                    kalapaHandler.onComplete(kalapaResult)
                }


            })
        }

        fun backgroundConfirm(callback: KalapaSDKCallback) {
            val path = "${config.baseURL}/api/kyc/confirm?lang=${config.language}"
            KalapaAPI.confirm(
                path,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                leftoverSession,
                object : Client.ConfirmListener {
                    override fun success(confirmResult: ConfirmResult) {
                        Helpers.printLog("confirmResult")
                        kalapaResult.session = session
                        kalapaResult.decision = confirmResult.decision_detail?.decision
                        kalapaResult.decision_detail = confirmResult.decision_detail?.details
                        kalapaResult.nfc_data = confirmResult.nfc_data
                        if (confirmResult.selfie_data != null)
                            kalapaResult.selfie_data = confirmResult.selfie_data.data
                        callback.sendDone {
                            kalapaHandler.onComplete(kalapaResult)
                        }
                    }

                    override fun fail(error: KalapaError) {
                        callback.sendError(error.getMessageError())
                    }

                    override fun timeout() {
                        callback.sendExpired()
                    }

                })
        }

        /*****-STEP 4-*****/
        val localStartLivenessForResult = {
            startLivenessForResult(faceData, object : KalapaCaptureHandler() {
                override fun onExpired() {
                    kalapaHandler.onExpired()
                }

                override fun process(
                    base64: String,
                    mediaType: KalapaSDKMediaType,
                    callback: KalapaSDKCallback
                ) {
                    faceBitmap = BitmapUtil.base64ToBitmap(base64)
                    kalapaRawDataProcessor.processLivenessData(base64, object : Client.RequestListener {
                        override fun success(jsonObject: JSONObject) {
                            // Set Liveness. Call Confirm
                            if (config.getCaptureImage())
                                callback.sendDone {
                                    localStartConfirmForResult()
                                }
                            else
                                backgroundConfirm(callback)
                        }

                        override fun fail(error: KalapaError) {
                            callback.sendError(error.getMessageError())
                        }

                        override fun timeout() {
                            callback.sendExpired()
                        }

                    })
                }


                override fun onError(resultCode: KalapaSDKResultCode) {
                    onGeneralError(resultCode)
                }

            })
        }

        /*****-STEP 3-*****/
        val localStartNFCForResult = {
            Helpers.printLog("localStartNFCForResult: $mrz")
            startNFCForResult(
                if (mrz.isNotEmpty()) mrz else
                    if (!leftoverSessionMRZ.isNullOrEmpty()) leftoverSessionMRZ!! else
                        if (!isFrontAndBackResultInitialized()) "" else frontResult.fields?.id_number
                            ?: frontResult.mrz_data?.data?.raw_mrz ?: "",
                object : KalapaNFCHandler() {
                    override fun onExpired() {
                        kalapaHandler.onExpired()
                    }

                    override fun onNFCErrorHandle(activity: Activity, error: KalapaScanNFCError, callback: KalapaScanNFCCallback) {
                        kalapaHandler.onNFCErrorHandle(activity, error, callback)
                    }

                    override fun onNFCSkipButtonClicked(callback: KalapaScanNFCCallback) {
                        callback.close(localStartLivenessForResult)
                    }

                    override fun process(
                        idCardNumber: String,
                        nfcData: String,
                        callback: KalapaSDKCallback
                    ) {
                        kalapaRawDataProcessor.processNFCData(idCardNumber, nfcData, object : Client.RequestListener {
                            override fun success(jsonObject: JSONObject) {
                                // Set NFC. Call liveness.
                                Helpers.printLog("nfcCheck $jsonObject")

                                if (jsonObject.has("data") && jsonObject.getJSONObject("data").has("is_nfc_face_match_selfie")) {
                                    // First we need to get SELFIE
                                    val PATH_GET_SELFIE = "/api/data/image?type=SELFIE"
                                    KalapaAPI.getImage(PATH_GET_SELFIE, leftoverSession,
                                        object : Client.RequestImageListener {
                                            override fun success(byteArray: ByteArray) {
                                                try {
                                                    faceBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                                } catch (e: java.lang.Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            override fun fail(error: KalapaError) {
                                                // Do nothing
                                                Helpers.printLog("getData onError $error")
                                            }

                                            override fun timeout() {
                                                Helpers.printLog("getData onTimeout")
                                            }

                                        }) {
                                        callback.sendDone {
                                            backgroundConfirm(callback)
                                        }
                                    }
                                } else
                                    callback.sendDone { localStartLivenessForResult() }
                            }

                            override fun fail(error: KalapaError) {
                                if (error.code == 21 || error.code == 400 || error.code == 51)
                                    callback.sendError("${error.code};${error.getMessageError()}")
                                else callback.sendError(error.getMessageError())
                            }

                            override fun timeout() {
                                callback.sendExpired()
                            }

                        })

                    }

                    override fun onError(resultCode: KalapaSDKResultCode) {
                        onGeneralError(resultCode)
                    }

                })
        }

        /*****-STEP 2-*****/
        val localStartBackForResult = {
            startCaptureForResult(KalapaSDKMediaType.BACK, object : KalapaCaptureHandler() {
                override fun onExpired() {
                    kalapaHandler.onExpired()
                }

                override fun process(
                    base64: String,
                    mediaType: KalapaSDKMediaType,
                    callback: KalapaSDKCallback
                ) {
                    backBitmap = BitmapUtil.base64ToBitmap(base64)
                    kalapaRawDataProcessor.processCaptureData(base64, KalapaSDKMediaType.BACK, object : Client.RequestListener {
                        override fun success(jsonObject: JSONObject) {
                            backResult = BackResult.fromJson(jsonObject.toString())!!
                            callback.sendDone {
                                // Call NFC if needed!
                                if (backResult.card_type?.contains("eid") == true && config.getUseNFC()) {
                                    localStartNFCForResult()
                                } else {
                                    localStartLivenessForResult()
                                }
                            }
                        }

                        override fun fail(error: KalapaError) {
                            Helpers.printLog("error: $error ${error.getMessageError()}")
                            callback.sendError(error.getMessageError())
                        }

                        override fun timeout() {
                            callback.sendExpired()
                        }
                    })
                }

                override fun onError(resultCode: KalapaSDKResultCode) {
                    onGeneralError(resultCode)
                }

            })
        }

        /*****-STEP 1-*****/
        val localStartFrontForResult = {
            startCaptureForResult(KalapaSDKMediaType.FRONT, object : KalapaCaptureHandler() {
                override fun onExpired() {
                    kalapaHandler.onExpired()
                }

                override fun process(
                    base64: String,
                    mediaType: KalapaSDKMediaType,
                    callback: KalapaSDKCallback
                ) {
                    // Check Front!
                    frontBitmap = BitmapUtil.base64ToBitmap(base64)
                    kalapaRawDataProcessor.processCaptureData(base64, KalapaSDKMediaType.FRONT, object : Client.RequestListener {
                        override fun success(jsonObject: JSONObject) {
                            // Call Back!
                            frontResult = FrontResult.fromJson(jsonObject.toString())!!
                            callback.sendDone {
                                localStartBackForResult()
                            }
                        }

                        override fun fail(error: KalapaError) {
                            callback.sendError(error.getMessageError())
                        }

                        override fun timeout() {
                            callback.sendExpired()
                        }

                    })
                }

                override fun onError(resultCode: KalapaSDKResultCode) {
                    // Got Message
                    onGeneralError(resultCode)
                }

            })
        }

        /** STEP 0: Get previous session information if needed
         **/
        if (leftoverSession.isNotEmpty()) {
            ProgressView.showProgress(activity)
            Helpers.printLog("leftoverSession: $leftoverSession")
            val PATH_GET_MRZ = "/api/data/get?type=MRZ"
            KalapaAPI.getData(PATH_GET_MRZ, leftoverSession, object : Client.RequestListener {
                override fun success(jsonObject: JSONObject) {
                    ProgressView.hideProgress()
                    val mrzJSON = MRZData.fromJson(jsonObject.toString())
                    if (mrzJSON != null && mrzJSON.raw_mrz?.isNotEmpty() == true) {
                        Helpers.printLog("leftoverSession MRZ: ${mrzJSON.raw_mrz}")
                        leftoverSessionMRZ = mrzJSON.raw_mrz
                    }
                }

                override fun fail(error: KalapaError) { // Don't care
                    ProgressView.hideProgress()
                }

                override fun timeout() { // Don't care
                    ProgressView.hideProgress()
                }
            }) {
                Helpers.printLog("leftoverSession: End of request")
                if (config.getCaptureImage()) {
                    localStartFrontForResult()
                } else {
                    localStartNFCForResult()
                }
            }
        } else {
            Helpers.printLog("leftoverSession: End of request $flow")
            if (config.getCaptureImage())
                localStartFrontForResult()
            else
                localStartNFCForResult()
        }
    }

}

class KalapaSDKConfig private constructor(
    var backgroundColor: String = "#FFFFFF",
    var mainColor: String = "#62A583",
    var mainTextColor: String = "#65657B",
    var btnTextColor: String = "#FFFFFF",
    var livenessVersion: Int = 0,
    var language: String,
    var minNFCRetry: Int = 3,
    var baseURL: String = "https://api-ekyc.kalapa.vn",
    var nfcTimeoutInSeconds: Int = 180 // 3 min
) {
    init {
        KalapaAPI.configure(baseURL)
    }

    private var useNFC: Boolean = true
    private var captureImage: Boolean = true

    fun withFlow(flow: KalapaFlowType) {
        // ekyc, nfc_ekyc, nfc_only.
        this.useNFC = flow == KalapaFlowType.NFC_EKYC || flow == KalapaFlowType.NFC_ONLY
        this.captureImage = flow == KalapaFlowType.EKYC || flow == KalapaFlowType.NFC_EKYC
    }


    fun getCaptureImage(): Boolean {
        return this.captureImage
    }

    fun getUseNFC(): Boolean {
        return this.useNFC
    }

    class KalapaSDKConfigBuilder(val context: Context) {
        var backgroundColor: String = "#FFFFFF"
        var mainColor: String = "#62A583"
        var mainTextColor: String = "#65657B"
        var btnTextColor: String = "#FFFFFF"
        var livenessVersion: Int = 1
        var language: String = "vi"
        private val minNFCRetry: Int = 3
        var baseURL: String = "https://ekyc-api.kalapa.vn"
        private var nfcTimeoutInSeconds: Int = 180
        fun build(): KalapaSDKConfig {
            KLPLanguageManager.setLanguage(language).pullLanguage(baseURL)
            return KalapaSDKConfig(
                backgroundColor,
                mainColor,
                mainTextColor,
                btnTextColor,
                livenessVersion,
                language,
                minNFCRetry,
                baseURL,
                nfcTimeoutInSeconds
            )
        }

        fun withNFCTimeoutInSeconds(seconds: Int): KalapaSDKConfigBuilder {
            this.nfcTimeoutInSeconds = seconds
            return this
        }

        fun withBackgroundColor(color: String): KalapaSDKConfigBuilder {
            this.backgroundColor = color
            return this
        }

        fun withMainColor(color: String): KalapaSDKConfigBuilder {
            this.mainColor = color
            return this
        }

        fun withMainTextColor(color: String): KalapaSDKConfigBuilder {
            this.mainTextColor = color
            return this
        }

        fun withBtnTextColor(color: String): KalapaSDKConfigBuilder {
            this.btnTextColor = color
            return this
        }

        fun withLivenessVersion(version: Int): KalapaSDKConfigBuilder {
            if (version in 1..3)
                this.livenessVersion = version
            return this
        }

        fun withLanguage(language: String): KalapaSDKConfigBuilder {
            when (language) {
                "vi", "vi-VN" -> this.language = "vi"
                "en", "en-US" -> this.language = "en"
                "ko", "ko-KR" -> this.language = "ko"
            }
            return this
        }

        fun withBaseURL(baseURL: String): KalapaSDKConfigBuilder {
            if (baseURL.startsWith("https"))
                this.baseURL = baseURL
            return this
        }
    }

    var customTitle: String = ""
    var customSubTitle: String = ""

}


abstract class KalapaCaptureHandler : KalapaHandler() {
    abstract fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback)
}

interface IKalapaRawDataProcessor {
    fun processLivenessData(portraitBase64: String, completion: Client.RequestListener)
    fun processCaptureData(documentBase64: String, documentType: KalapaSDKMediaType, completion: Client.RequestListener)
    fun processNFCData(idCardNumber: String, nfcRawData: String, completion: Client.RequestListener)
}

abstract class KalapaHandler {
    open fun onError(resultCode: KalapaSDKResultCode) {
        Helpers.printLog("KalapaHandler onError $resultCode")
    }

    open fun onProcessFinished() {
        Helpers.printLog("KalapaHandler onProcessFinished")
    }

    open fun onComplete(kalapaResult: KalapaResult) {
        Helpers.printLog("KalapaHandler onComplete $kalapaResult")
    }

    open fun onEndSession() {
        Helpers.printLog("KalapaHandler onEndSession")
    }

    abstract fun onExpired()

    open fun onNFCErrorHandle(activity: Activity, error: KalapaScanNFCError, callback: KalapaScanNFCCallback) {
        Helpers.printLog("default onNFCErrorHandle...")
        showDefaultNFCDialog(activity, error, callback)
    }

    open fun onNFCSkipButtonClicked(callback: KalapaScanNFCCallback) {
        Helpers.printLog("onNFCSkipButtonClicked")
    }

    private fun showDefaultNFCDialog(activity: Activity, error: KalapaScanNFCError, callback: KalapaScanNFCCallback) {
        val bottomSheetDialog = Dialog(activity)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_nfc_error)
        bottomSheetDialog.window?.setLayout(-1, -2)
        bottomSheetDialog.window?.setBackgroundDrawable(ColorDrawable(0))
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        bottomSheetDialog.window?.setGravity(80)
        bottomSheetDialog.setCancelable(false)
        val tvTitle = bottomSheetDialog.findViewById<TextView>(R.id.text_status)
        tvTitle.text = KLPLanguageManager.get(activity.getString(R.string.klp_error_unknown)) // nfc_location_title

        val tvBody = bottomSheetDialog.findViewById<TextView>(R.id.text_des)

        tvBody.text = when (error) {
            KalapaScanNFCError.ERROR_NFC_INFO_NOT_MATCH -> KLPLanguageManager.get(activity.getString(R.string.klp_error_nfc_info_not_match))
            KalapaScanNFCError.ERROR_NFC_INVALID -> KLPLanguageManager.get(activity.getString(R.string.klp_error_invalid_format))
            KalapaScanNFCError.ERROR_FACE_NOT_MATCH -> KLPLanguageManager.get(activity.getString(R.string.klp_error_nfc_selfie_not_match))
            KalapaScanNFCError.ERROR_NFC_TIMEOUT -> KLPLanguageManager.get(activity.getString(R.string.klp_liveness_error_timeout))
            else -> KLPLanguageManager.get(activity.getString(R.string.klp_error_unknown))
        }
        val btnCancel = bottomSheetDialog.findViewById<Button>(R.id.btn_cancel)
        Helpers.setBackgroundColorTintList(btnCancel, KalapaSDK.config.mainColor)
        btnCancel.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
        btnCancel.text = KLPLanguageManager.get(activity.getString(R.string.klp_button_cancel))
        btnCancel.setOnClickListener {
            bottomSheetDialog.hide()
            callback.close { Helpers.printLog("User Give Up!") }
        }

        val btnRetry = bottomSheetDialog.findViewById<Button>(R.id.btn_retry)
        Helpers.setBackgroundColorTintList(btnRetry, KalapaSDK.config.mainColor)
        btnRetry.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
        btnRetry.text = KLPLanguageManager.get(activity.getString(R.string.klp_button_retry))
        btnRetry.setOnClickListener {
            callback.onRetry()
            Helpers.printLog("User Tap on Retry!")
            bottomSheetDialog.hide()
        }
        bottomSheetDialog.show()
    }
}

enum class KalapaScanNFCError(val code: Int) {
    ERROR_NFC_INFO_NOT_MATCH(51), ERROR_FACE_NOT_MATCH(21), ERROR_NFC_INVALID(400), ERROR_NFC_TIMEOUT(401), ERROR_NA(-1);

    companion object {
        fun fromErrorCode(code: String): KalapaScanNFCError {
            return when (code) {
                "21" -> ERROR_FACE_NOT_MATCH
                "51" -> ERROR_NFC_INFO_NOT_MATCH
                "400", "500" -> ERROR_NFC_INVALID
                "401" -> ERROR_NFC_TIMEOUT
                else -> ERROR_NA
            }
        }
    }
}

internal abstract class KalapaNFCHandler : KalapaHandler() {
    abstract fun process(
        idCardNumber: String,
        nfcData: String,
        callback: KalapaSDKCallback
    )
}

enum class KalapaSDKNFCStatus(status: Int) {
    NOT_SUPPORTED(-1),
    NOT_ENABLED(0),
    SUPPORTED(1)
}

enum class KalapaSDKResultCode(val vi: String, val en: String) {
    UNKNOWN("Lỗi không xác định", "Unknown error"),
    SUCCESS("Thành công", "Success"),
    PERMISSION_DENIED("Không cung cấp quyền truy cập", "User permission not granted"),
    USER_CONSENT_DECLINED("Không đồng ý điều khoản", "User declines consent"),
    SUCCESS_WITH_WARNING("Thành công", "Success with warning"),
    CANNOT_OPEN_DEVICE("Lỗi phần cứng", "Device issues"),
    CARD_NOT_FOUND("Không tìm thấy giấy tờ hoặc giấy tờ không hợp lệ", "Document not found or invalid"),

    //    WRONG_CCCDID("giấy tờ không hợp lệ", "document invalid"),
    WRONG_CCCDID("MRZ không hợp lệ cho ta liệu này", "Invalid mrz"),
    CARD_LOST_CONNECTION("Mất kết nối tới thẻ", "Card lost connection"),
    USER_LEAVE("Người dùng hủy bỏ xác thực", "User leave ekyc process"),
    EMULATOR_DETECTED("Phát hiện máy ảo", "Emulator detection"),
    DEVICE_NOT_SUPPORTED("Thiết bị không hỗ trợ", "Device does not support"),
    CONFIGURATION_NOT_ACCEPTABLE(
        "Cấu hình chưa đúng, vui lòng kiểm tra lại",
        "Configuration not acceptable, please try again"
    )
}


enum class KalapaSDKMediaType {
    FRONT, BACK, PORTRAIT, PASSPORT, NA;

    companion object {
        fun fromName(mediaType: String): KalapaSDKMediaType {
            return when (mediaType) {
                FRONT.name -> FRONT
                BACK.name -> BACK
                PORTRAIT.name -> PORTRAIT
                PASSPORT.name -> PASSPORT
                else -> NA
            }
        }
    }
}

interface KalapaSDKCallback {
    fun sendError(message: String?)
    fun sendDone(nextAction: () -> Unit)
    fun sendExpired()
}

interface KalapaScanNFCCallback {
    fun close(nextAction: () -> Unit)
    fun onRetry()
}

enum class KalapaFlowType(val flow: String?) {
    EKYC("ekyc"),
    NFC_EKYC("nfc_ekyc"),
    NFC_ONLY("nfc_only"),
    NA("not_applicable");

    companion object {
        fun ofFlow(flow: String): KalapaFlowType {
            return if (flow == EKYC.flow) EKYC
            else if (flow == NFC_EKYC.flow) NFC_EKYC
            else if (flow == NFC_ONLY.flow) NFC_ONLY
            else NA
        }
    }
}