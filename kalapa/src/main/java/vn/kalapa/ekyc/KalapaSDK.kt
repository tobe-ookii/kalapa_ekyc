package vn.kalapa.ekyc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fis.ekyc.nfc.build_in.model.ResultCode
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils.NFCListener
import org.json.JSONObject
import vn.kalapa.R
import vn.kalapa.ekyc.capturesdk.CameraXPassportActivity
import vn.kalapa.ekyc.activity.CameraXSelfieActivity
import vn.kalapa.ekyc.activity.ConfirmActivity
import vn.kalapa.ekyc.capturesdk.CameraXAutoCaptureActivity
import vn.kalapa.ekyc.handlers.GetDynamicLanguageHandler
import vn.kalapa.ekyc.models.BackResult
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.FrontResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.KalapaLanguageModel
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.models.MRZData
import vn.kalapa.ekyc.models.NFCRawData
import vn.kalapa.ekyc.networks.Client
import vn.kalapa.ekyc.networks.KalapaAPI
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.LanguageUtils
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

    class KalapaSDKBuilder(private val activity: Activity, private val config: KalapaSDKConfig) {
        private var mrz = ""
        private var faceData = ""
        private var leftoverSession = ""

        fun build(): KalapaSDK {
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
        private val VERSION = "2.10.0.1"

        fun getSDKVersion(): String {
            return VERSION
        }

        var kalapaResult = KalapaResult()
        lateinit var frontResult: FrontResult
        private lateinit var backResult: BackResult
        internal lateinit var config: KalapaSDKConfig
        internal lateinit var handler: KalapaHandler
        internal lateinit var session: String

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
        val complete = { kalapaCustomHandler.onComplete(KalapaResult()) }

        val nfcScreen = {
            startNFCForResult(mrz, object : KalapaNFCHandler() {
                override fun process(idCardNumber: String, nfcData: String, callback: KalapaSDKCallback) {
                    callback.sendDone(complete)
                }

                override fun onExpired() {
                }

            })
        }

        val livenessScreen = {
            startLivenessForResult(faceData, object : KalapaCaptureHandler() {
                override fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback) {
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
                    startCaptureForResult(KalapaSDKMediaType.BACK, object : KalapaCaptureHandler() {
                        override fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback) {
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
        kalapaCustomHandler: IKalapaRawDataProcessor
    ) {
        Companion.session = session
        var leftoverSessionMRZ: String? = null
        val sessionFlow = KalapaFlowType.ofFlow(flow)
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
                    handler.onExpired()
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
                        config.languageUtils.getLanguageString(activity.getString(R.string.klp_error_timeout))
                    }

                })
        }

        /*****-STEP 4-*****/
        val localStartLivenessForResult = {
            startLivenessForResult(faceData, object : KalapaCaptureHandler() {
                override fun onExpired() {
                    handler.onExpired()
                }

                override fun process(
                    base64: String,
                    mediaType: KalapaSDKMediaType,
                    callback: KalapaSDKCallback
                ) {
                    faceBitmap = BitmapUtil.base64ToBitmap(base64)
                    kalapaCustomHandler.processLivenessData(base64, object : Client.RequestListener {
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
                            callback.sendError(
                                config.languageUtils.getLanguageString(
                                    activity.getString(R.string.klp_error_timeout)
                                )
                            )
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
            Helpers.printLog("localStartNFCForResult $mrz")
            startNFCForResult(
                if (mrz.isNotEmpty()) mrz else
                    if (!leftoverSessionMRZ.isNullOrEmpty()) leftoverSessionMRZ!! else
                        if (!isFrontAndBackResultInitialized()) "" else frontResult.fields?.id_number
                            ?: frontResult.mrz_data?.data?.raw_mrz ?: "",
                object : KalapaNFCHandler() {
                    override fun onExpired() {
                        handler.onExpired()
                    }

                    override fun process(
                        idCardNumber: String,
                        nfcData: String,
                        callback: KalapaSDKCallback
                    ) {
                        kalapaCustomHandler.processNFCData(idCardNumber, nfcData, object : Client.RequestListener {
                            override fun success(jsonObject: JSONObject) {
                                // Set NFC. Call liveness.
                                Helpers.printLog("nfcCheck $jsonObject")

                                if (jsonObject.has("data") && jsonObject.getJSONObject("data")
                                        .has("is_nfc_face_match_selfie")
                                ) {
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
                                callback.sendError(error.getMessageError())
                            }

                            override fun timeout() {
                                callback.sendError(
                                    config.languageUtils.getLanguageString(
                                        activity.getString(R.string.klp_error_timeout)
                                    )
                                )
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
                    handler.onExpired()
                }

                override fun process(
                    base64: String,
                    mediaType: KalapaSDKMediaType,
                    callback: KalapaSDKCallback
                ) {
                    backBitmap = BitmapUtil.base64ToBitmap(base64)
                    kalapaCustomHandler.processCaptureData(base64, KalapaSDKMediaType.BACK, object : Client.RequestListener {
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
                            callback.sendError(
                                config.languageUtils.getLanguageString(
                                    activity.getString(
                                        R.string.klp_error_timeout
                                    )
                                )
                            )
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
                    handler.onExpired()
                }

                override fun process(
                    base64: String,
                    mediaType: KalapaSDKMediaType,
                    callback: KalapaSDKCallback
                ) {
                    // Check Front!
                    frontBitmap = BitmapUtil.base64ToBitmap(base64)
                    kalapaCustomHandler.processCaptureData(base64, KalapaSDKMediaType.FRONT, object : Client.RequestListener {
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
                            callback.sendError(
                                config.languageUtils.getLanguageString(
                                    activity.getString(
                                        R.string.klp_error_timeout
                                    )
                                )
                            )
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
    var languageUtils: LanguageUtils,
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

        fun build(): KalapaSDKConfig {
            return KalapaSDKConfig(
                backgroundColor,
                mainColor,
                mainTextColor,
                btnTextColor,
                livenessVersion,
                language,
                minNFCRetry,
                baseURL,
                pullLanguage(context)
            )
        }


        private fun pullLanguage(context: Context): LanguageUtils {
            val start = System.currentTimeMillis()
            Helpers.printLog("pullLanguage $start | $language")
            val languageUtils = LanguageUtils(context)
            val languageJsonBody: String? =
                GetDynamicLanguageHandler(context).execute(baseURL, language).get() // null //
            Helpers.printLog("pullLanguage Done ${System.currentTimeMillis() - start} | $language")
            if (!languageJsonBody.isNullOrEmpty() && languageJsonBody != "-1") {
                Helpers.printLog("pullLanguage $languageJsonBody")
                val klpLanguageModel = KalapaLanguageModel.fromJson(languageJsonBody)
                Helpers.printLog("pullLanguage ${klpLanguageModel.error} ${klpLanguageModel.data}")
                if ((klpLanguageModel.error != null) && (klpLanguageModel.error.code == 200) && klpLanguageModel.data != null) {
                    // Thành công
                    if (klpLanguageModel.data.content?.SDK?.isNotEmpty() == true) {
                        Helpers.printLog("setLanguage ${klpLanguageModel.data.content.SDK}")
                        languageUtils.setLanguage(klpLanguageModel.data.content.SDK, klpLanguageModel.data.content.APP_DEMO)
                    }
                }
            }
            return languageUtils
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
    UNKNOWN("lỗi không xác định", "unknown error"),
    SUCCESS("thành công", "success"),
    PERMISSION_DENIED("không cung cấp quyền truy cập", "user permission not granted"),
    USER_CONSENT_DECLINED("lhông đồng ý điều khoản", "user declines consent"),
    SUCCESS_WITH_WARNING("thành công", "success with warning"),
    CANNOT_OPEN_DEVICE("lỗi phần cứng", "device issues"),
    CARD_NOT_FOUND(
        "không tìm thấy giấy tờ hoặc giấy tờ không hợp lệ",
        "document not found or invalid"
    ),
    WRONG_CCCDID("giấy tờ không hợp lệ", "document invalid"),
    CARD_LOST_CONNECTION("mất kết nối tới thẻ", "card lost connection"),
    USER_LEAVE("người dùng hủy bỏ xác thực", "user leave ekyc process"),
    EMULATOR_DETECTED("Phát hiện máy ảo", "emulator detection"),
    DEVICE_NOT_SUPPORTED("Thiết bị không hỗ trợ", "device does not support"),
    CONFIGURATION_NOT_ACCEPTABLE(
        "cấu hình chưa đúng, vui lòng kiểm tra lại",
        "configuration not acceptable, please try again"
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

}

//sealed class KalapaFlowType(val flow: String) {
//    class EKYC : KalapaFlowType("ekyc") {
//
//    }
//
//    class NFC_EKYC : KalapaFlowType("nfc_ekyc") {
//
//    }
//
//    class NFC_ONLY : KalapaFlowType("nfc_only") {
//
//    }
//
//}


enum class KalapaFlowType(val flow: String?) {
    EKYC("ekyc") {
        val session: String = ""
    },
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