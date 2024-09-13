package vn.kalapa.ekyc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
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
import vn.kalapa.ekyc.models.PassportResult
import vn.kalapa.ekyc.networks.Client
import vn.kalapa.ekyc.networks.KalapaAPI
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.LanguageUtils
import vn.kalapa.ekyc.nfcsdk.activities.NFCActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.views.ProgressView
import java.io.ByteArrayOutputStream

class KalapaSDK {
    companion object {
        private val VERSION = "2.9.1.2"
        lateinit var session: String

        @SuppressLint("StaticFieldLeak")
        internal lateinit var config: KalapaSDKConfig
        lateinit var handler: KalapaHandler
        lateinit var kalapaResult: KalapaResult
        lateinit var frontResult: FrontResult
        private lateinit var passportResult: PassportResult
        private lateinit var backResult: BackResult
        var faceBitmap: Bitmap? = null
        var frontBitmap: Bitmap? = null
        var backBitmap: Bitmap? = null

        fun isHandlerInitialized(): Boolean {
            return this::handler.isInitialized
        }

        fun getSDKVersion(): String {
            return VERSION
        }

        fun isConfigInitialized(): Boolean {
            return this::config.isInitialized
        }


        private fun isFrontAndBackResultInitialized(): Boolean {
            return this::frontResult.isInitialized
        }

        private fun configure(sdkConfig: KalapaSDKConfig) {
            this.config = sdkConfig
            KalapaAPI.configure(config.baseURL)
        }

        private fun startLivenessForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            faceData: String = "",
            handler: KalapaCaptureHandler
        ) {
            configure(config)
            isFoldOpen(activity)
            this.handler = handler
            val intent = Intent(activity, CameraXSelfieActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("face_data", faceData)
            activity.startActivity(intent)
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


        private fun startCaptureForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            documentType: KalapaSDKMediaType = KalapaSDKMediaType.FRONT,
            handler: KalapaCaptureHandler
        ) {
            val metrics = activity.resources.displayMetrics
            configure(config)
            this.handler = handler
            val intent = Intent(activity, CameraXAutoCaptureActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("document_type", documentType.name)
            activity.startActivity(intent)
        }


        private fun startConfirmForResult(
            activity: Activity,
            session: String,
            config: KalapaSDKConfig,
            leftoverSession: String,
            handler: KalapaHandler
        ) {
            this.session = session
            configure(config)
            this.handler = handler
            val intent = Intent(activity, ConfirmActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("leftover_session", leftoverSession)
            activity.startActivity(intent)
        }


        fun startCapturingPassportForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            handler: KalapaCaptureHandler
        ) {
            configure(config)
            this.handler = handler
            val intent = Intent(activity, CameraXPassportActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }

        private fun startNFCForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            mrz: String = "",
            nfcHandler: KalapaNFCHandler,
        ) {
            Helpers.printLog("mrzzz: $mrz")
            configure(config)
            this.handler = nfcHandler
            val intent = Intent(activity, NFCActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("mrz", mrz)
            activity.startActivity(intent)
        }

        fun startFullEKYC(
            activity: Activity,
            session: String,
            flow: String,
            config: KalapaSDKConfig,
            leftoverSession: String = "",
            mrz: String = "",
            faceData: String = "",
            kalapaCustomHandler: KalapaHandler
        ) {
            this.session = session
            this.kalapaResult = KalapaResult()
            this.frontResult = FrontResult()
            this.backResult = BackResult()
            this.faceBitmap = null
            this.backBitmap = null
            this.frontBitmap = null

            var leftoverSessionMRZ: String? = null
            val sessionFlow = KalapaFlowType.ofFlow(flow)
            if (config.baseURL.isEmpty() || !config.baseURL.contains("http") || sessionFlow == KalapaFlowType.NA) {
                kalapaCustomHandler.onError(KalapaSDKResultCode.CONFIGURATION_NOT_ACCEPTABLE)
                return
            }
            config.withFlow(sessionFlow)
            val onGeneralError: (resultCode: KalapaSDKResultCode) -> Unit = {
                kalapaCustomHandler.onError(it)
            }
            configure(config)
            /*****-STEP 5-*****/
            val localStartConfirmForResult = {
                startConfirmForResult(activity, session, config, leftoverSession, object : KalapaHandler() {
                    override fun onExpired() {
                        handler.onExpired()
                    }

                    override fun onError(resultCode: KalapaSDKResultCode) {
                        onGeneralError(resultCode)
                    }

                    override fun onComplete(kalapaResult: KalapaResult) {
                        super.onComplete(kalapaResult)
                        Helpers.printLog("startFullEKYC localStartConfirmForResult onComplete KalapaResult: $kalapaResult \n ${kalapaResult.decision} ${kalapaResult.decision_detail} ")
                        kalapaCustomHandler.onComplete(kalapaResult)
                    }


                })
            }

            fun backgroundConfirm(callback: KalapaSDKCallback) {
                val path = "${Companion.config.baseURL}/api/kyc/confirm?lang=${config.language}"
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
                                kalapaCustomHandler.onComplete(kalapaResult)
                            }
                        }

                        override fun fail(error: KalapaError) {
                            callback.sendError(error.getMessageError())
                        }

                        override fun timeout() {
                            config.languageUtils.getLanguageString(activity.getString(R.string.klp_timeout_body))
                        }

                    })
            }

            /*****-STEP 4-*****/
            val localStartLivenessForResult = {
                startLivenessForResult(activity, config, faceData, object : KalapaCaptureHandler() {
                    override fun onExpired() {
                        handler.onExpired()
                    }

                    private val endpoint = "/api/kyc/app/check-selfie?lang=${config.language}"
                    override fun process(
                        base64: String,
                        mediaType: KalapaSDKMediaType,
                        callback: KalapaSDKCallback
                    ) {
                        faceBitmap = BitmapUtil.base64ToBitmap(base64)
                        KalapaAPI.selfieCheck(
                            endpoint,
                            faceBitmap!!,
                            leftoverSession,
                            object : Client.RequestListener {
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
                                            activity.getString(R.string.klp_timeout_body)
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
                startNFCForResult(
                    activity,
                    config,
                    if (mrz.isNotEmpty()) mrz else
                        if (!leftoverSessionMRZ.isNullOrEmpty()) leftoverSessionMRZ!! else
                            if (!isFrontAndBackResultInitialized()) "" else frontResult.fields?.id_number
                                ?: frontResult.mrz_data?.data?.raw_mrz ?: "",
                    object : KalapaNFCHandler() {
                        override fun onExpired() {
                            handler.onExpired()
                        }

                        private val endpoint = "/api/nfc/verify"
                        override fun process(
                            idCardNumber: String,
                            nfcData: String,
                            callback: KalapaSDKCallback
                        ) {
                            // Submit NFC.
                            KalapaAPI.nfcCheck(
                                endPoint = endpoint,
                                body = NFCRawData.fromJson(nfcData),
                                leftoverSession,
                                object : Client.RequestListener {
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
                                            callback.sendDone {
                                                localStartLivenessForResult()
                                            }
                                    }

                                    override fun fail(error: KalapaError) {
                                        callback.sendError(error.getMessageError())
                                    }

                                    override fun timeout() {
                                        callback.sendError(
                                            config.languageUtils.getLanguageString(
                                                activity.getString(R.string.klp_timeout_body)
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
                startCaptureForResult(activity, config, KalapaSDKMediaType.BACK, object : KalapaCaptureHandler() {
                    override fun onExpired() {
                        handler.onExpired()
                    }

                    private val endpoint = "/api/kyc/app/scan-back?lang=${config.language}"
                    override fun process(
                        base64: String,
                        mediaType: KalapaSDKMediaType,
                        callback: KalapaSDKCallback
                    ) {
                        backBitmap = BitmapUtil.base64ToBitmap(base64)
                        KalapaAPI.imageCheck(endpoint, backBitmap!!, leftoverSession, object : Client.RequestListener {
                            override fun success(jsonObject: JSONObject) {
                                backResult = BackResult.fromJson(jsonObject.toString())!!
                                Helpers.printLog("imageCheck $endpoint $jsonObject")
                                callback.sendDone {
                                    // Call NFC if needed!
                                    if (backResult.card_type?.contains("eid") == true && KalapaSDK.config.getUseNFC()) {
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
                                            R.string.klp_timeout_body
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
                startCaptureForResult(activity, config, KalapaSDKMediaType.FRONT, object : KalapaCaptureHandler() {
                    override fun onExpired() {
                        handler.onExpired()
                    }

                    private val endpoint = "/api/kyc/app/scan-front?lang=${config.language}"
                    override fun process(
                        base64: String,
                        mediaType: KalapaSDKMediaType,
                        callback: KalapaSDKCallback
                    ) {
                        // Check Front!
                        frontBitmap = BitmapUtil.base64ToBitmap(base64)
                        KalapaAPI.imageCheck(
                            endpoint,
                            frontBitmap!!,
                            leftoverSession,
                            object : Client.RequestListener {
                                override fun success(jsonObject: JSONObject) {
                                    // Call Back!
                                    frontResult = FrontResult.fromJson(jsonObject.toString())!!
                                    Helpers.printLog("imageCheck $endpoint $jsonObject")
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
                                                R.string.klp_timeout_body
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
                Helpers.printLog("leftoverSession: ${leftoverSession}")
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
                    if (KalapaSDK.config.getCaptureImage()) {
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

}

class KalapaSDKConfig private constructor(
    var context: Context,
    var backgroundColor: String = "#FFFFFF",
    var mainColor: String = "#62A583",
    var mainTextColor: String = "#65657B",
    var btnTextColor: String = "#FFFFFF",
    var livenessVersion: Int = 0,
    var language: String,
    var minNFCRetry: Int = 3,
    var baseURL: String = "https://api-ekyc.kalapa.vn",
) {
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
                context,
                backgroundColor,
                mainColor,
                mainTextColor,
                btnTextColor,
                livenessVersion,
                language,
                minNFCRetry,
                baseURL,
            )
        }

        fun withBackgroundColor(color: String): KalapaSDKConfigBuilder {
            this.backgroundColor = color
            return this
        }

//        fun withFaceData(face: String): KalapaSDKConfigBuilder {
//            try {
//                val imageBytes = android.util.Base64.decode(face, android.util.Base64.DEFAULT)
//                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//                KalapaSDK.faceData = face
//            } catch (e: Exception) {
//                throw IllegalArgumentException("Invalid base64 string")
//            }
//            return this
//        }

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

    lateinit var languageUtils: LanguageUtils
    var customTitle: String = ""
    var customSubTitle: String = ""

    init {
        pullLanguage()
    }

    private fun pullLanguage() {
        val start = System.currentTimeMillis()
        Helpers.printLog("pullLanguage $start | $language")
        languageUtils = LanguageUtils(context)
        val languageJsonBody: String? =
            GetDynamicLanguageHandler(context).execute(baseURL, language).get() // null //
        Helpers.printLog("pullLanguage Done ${System.currentTimeMillis() - start} | $language")

        if (!languageJsonBody.isNullOrEmpty() && languageJsonBody != "-1") {
//            Helpers.printLog("pullLanguage $languageJsonBody")
            val klpLanguageModel = KalapaLanguageModel.fromJson(languageJsonBody)
            if ((klpLanguageModel?.error != null) && (klpLanguageModel.error.code == 0) && klpLanguageModel.data != null) {
                // Thành công
                if (klpLanguageModel.data.data?.SDK?.isNotEmpty() == true) {
                    Helpers.printLog("setLanguage ${klpLanguageModel.data.data.SDK}")
                    languageUtils.setLanguage(klpLanguageModel.data.data.SDK)
                }
            }
        }
    }
}


abstract class KalapaCaptureHandler : KalapaHandler() {
    abstract fun process(
        base64: String,
        mediaType: KalapaSDKMediaType,
        callback: KalapaSDKCallback
    )
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

fun ImageProxy.toBitmap(): Bitmap? {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    return yuvImage.toBitmap()
}

private fun YuvImage.toBitmap(): Bitmap? {
    val out = ByteArrayOutputStream()
    if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
        return null
    val imageBytes: ByteArray = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val pixelCount = image.cropRect.width() * image.cropRect.height()
    val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
    val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
    imageToByteBuffer(image, outputBuffer, pixelCount)
    return outputBuffer
}

private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
    assert(image.format == ImageFormat.YUV_420_888)

    val imageCrop = image.cropRect
    val imagePlanes = image.planes

    imagePlanes.forEachIndexed { planeIndex, plane ->
        // How many values are read in input for each output value written
        // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
        //
        // Y Plane            U Plane    V Plane
        // ===============    =======    =======
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        val outputStride: Int

        // The index in the output buffer the next value will be written at
        // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
        //
        // First chunk        Second chunk
        // ===============    ===============
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        var outputOffset: Int

        when (planeIndex) {
            0 -> {
                outputStride = 1
                outputOffset = 0
            }

            1 -> {
                outputStride = 2
                // For NV21 format, U is in odd-numbered indices
                outputOffset = pixelCount + 1
            }

            2 -> {
                outputStride = 2
                // For NV21 format, V is in even-numbered indices
                outputOffset = pixelCount
            }

            else -> {
                // Image contains more than 3 planes, something strange is going on
                return@forEachIndexed
            }
        }

        val planeBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // We have to divide the width and height by two if it's not the Y plane
        val planeCrop = if (planeIndex == 0) {
            imageCrop
        } else {
            Rect(
                imageCrop.left / 2,
                imageCrop.top / 2,
                imageCrop.right / 2,
                imageCrop.bottom / 2
            )
        }

        val planeWidth = planeCrop.width()
        val planeHeight = planeCrop.height()

        // Intermediate buffer used to store the bytes of each row
        val rowBuffer = ByteArray(plane.rowStride)

        // Size of each row in bytes
        val rowLength = if (pixelStride == 1 && outputStride == 1) {
            planeWidth
        } else {
            // Take into account that the stride may include data from pixels other than this
            // particular plane and row, and that could be between pixels and not after every
            // pixel:
            //
            // |---- Pixel stride ----|                    Row ends here --> |
            // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
            //
            // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
            (planeWidth - 1) * pixelStride + 1
        }

        for (row in 0 until planeHeight) {
            // Move buffer position to the beginning of this row
            planeBuffer.position(
                (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
            )

            if (pixelStride == 1 && outputStride == 1) {
                // When there is a single stride value for pixel and output, we can just copy
                // the entire row in a single step
                planeBuffer.get(outputBuffer, outputOffset, rowLength)
                outputOffset += rowLength
            } else {
                // When either pixel or output have a stride > 1 we must copy pixel by pixel
                planeBuffer.get(rowBuffer, 0, rowLength)
                for (col in 0 until planeWidth) {
                    outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                    outputOffset += outputStride
                }
            }
        }
    }
}