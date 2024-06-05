package vn.kalapa.ekyc.nfcsdk


//class KalapaNFC private constructor() {
//    companion object {
//        private var mrz: String? = null
//        private var session: String? = null
//        var nfcListener: KalapaNFCListener? = null
//        lateinit var nfcConfig: KalapaNFCConfig
//        var nfcBitmapFace: Bitmap? = null
//        var faceData: Bitmap? = null
//        fun configure(
//            kalapaNFCConfig: KalapaNFCConfig,
//            nfcListener: KalapaNFCListener?
//        ): KalapaNFC.Companion {
//            this.nfcConfig = kalapaNFCConfig
//            this.nfcListener = nfcListener
//            return this
//        }
//
//        fun scanMRZ(context: Context, mrz: String?) {
//            this.mrz = mrz
//            start(context, null)
//        }
//
//        private fun start(context: Context, flow: FaceOTPFlowType?) {
//            // Nếu là EKYC_NFC và NFC thì vẫn là gọi NFC
//            var intent = Intent(context, NFCActivity::class.java)
//
//            if (flow == FaceOTPFlowType.EKYC || flow == FaceOTPFlowType.VERIFY)
//                intent = Intent(context, LivenessActivityForResult::class.java)
//
//            intent.putExtra("mrz", mrz)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//            context.startActivity(intent)
//        }
//
//        fun start(context: Context, session: String?, flow: FaceOTPFlowType?) {
//            this.session = session
//            start(context, flow)
//        }
//
//        fun start(context: Context, session: String?, flow: String) {
//            this.session = session
//            start(context, getFlowTypeFromString(flow))
//        }
//
//        private fun getFlowTypeFromString(flow: String): FaceOTPFlowType {
//            return when (flow) {
//                FaceOTPFlowType.NFC.name ->FaceOTPFlowType.NFC
//                FaceOTPFlowType.EKYC.name -> FaceOTPFlowType.EKYC
//                FaceOTPFlowType.EKYC_NFC.name -> FaceOTPFlowType.EKYC_NFC
//                else -> FaceOTPFlowType.VERIFY
//            }
//        }
//
//        fun start(
//            context: Context,
//            mrz: String,
//            faceData: Bitmap?,
//            flow: FaceOTPFlowType
//        ) {
//            this.mrz = mrz
//            this.faceData = faceData
//            start(context, flow)
//        }
//
//        fun start(context: Context, mrz: String, faceData: Bitmap?, flow: String) {
//            this.mrz = mrz
//            this.faceData = faceData
//            start(context, getFlowTypeFromString(flow))
//        }
//    }
//
//
//}
//
//interface KalapaNFCListener {
//    fun onSuccess(result: String)
//    fun onFail(reason: String)
//    fun onError(errorCode: NFCResultCode, message: String = "")
//    fun onCheckNFCAvailable(status: NFCAvailablityStatus)
//}
//
//enum class NFCAvailablityStatus(status: Int) {
//    NOT_SUPPORTED(-1),
//    NOT_ENABLED(0),
//    SUPPORTED(1)
//}
//
//enum class NFCResultCode(errorCode: Int) {
//    USER_CANCELED(-2),
//    CANNOT_OPEN_DEVICE(-1),
//    UNKNOWN(0),
//    SUCCESS(1),
//    CARD_LOST_CONNECTION(2),
//    WRONG_ID_NUMBER(3),
//    CARD_NOT_FOUND(4)
//}
//
//class KalapaNFCConfig private constructor(
//    val language: Language,
//    val minNFCRetries: Int = 3
//) {
//
//    class Builder(val context: Context) {
//        private var language: Language = Language.VI
//        private var minNFCRetries: Int = 3
//        fun build(): KalapaNFCConfig {
//            return KalapaNFCConfig(
//                language = language,
//                minNFCRetries = minNFCRetries
//            )
//        }
//
//        fun withMinNFCRetries(minRetries: Int): Builder {
//            this.minNFCRetries = minRetries
//            return this
//        }
//
//        fun withLanguage(lang: Language): Builder {
//            this.language = lang
//            return this
//        }
//
//    }
//}
//
//enum class NFCOptions(val option: Int) {
//    REQUIRED(1),
//    OPTIONAL(2),
//    PREFERED(3)
//}
//
//enum class Language(val lang: String) {
//    VI("vi-VN"),
//    EN("en-US")
//}
