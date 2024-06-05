package vn.kalapa.ekyc

//
//
//class KLPFaceOTP {
//    companion object {
//        internal var isTablet: Boolean = false
//
//        var userId: String? = null
//        var bitmapFace: Bitmap? = null
//
//        lateinit var klpListener: KLPFaceOTPListener
//        lateinit var baseURL: String
//        internal lateinit var sdkConfig: SDKConfig
//        lateinit var flowType: FaceOTPFlowType
//        private fun isActivated(activity: Activity): Boolean {
//            return Helpers.getSecretKey(activity) != null
//        }
//
//        @JvmStatic
//        fun configure(activity: Activity, baseURL: String, sdkConfig: SDKConfig?) {
//            Helpers.printLog("KLPFaceOTP.configure")
//            isTablet = activity.resources.getBoolean(R.bool.isTablet)
//            this.baseURL = baseURL
//            this.sdkConfig = sdkConfig ?: SDKConfig()
//        }
//
//        @JvmStatic
//        private fun isConfiguredYet(): Boolean {
//            return this::baseURL.isInitialized && this::sdkConfig.isInitialized
//        }
//
//        @JvmStatic
//        fun setLanguage(
//            context: Context,
//            language: String
//        ) {
//            if (isConfiguredYet()) {
//                if (language == "vi-VN" || language == "vi") {
//                    LocaleHelper.setLocale(context, LocaleHelper.VIETNAMESE)
//                    this.sdkConfig.language = "vi"
//                }
//                if (language == "en-US" || language == "en") {
//                    LocaleHelper.setLocale(context, LocaleHelper.ENGLISH)
//                    this.sdkConfig.language = "en"
//                }
//            }
//        }
//
//        @JvmStatic
//        fun setColor(
//            backgroundColor: String? = null,
//            mainColor: String? = null,
//            mainTextColor: String? = null,
//            btnTextColor: String? = null,
//        ) {
//            if (isConfiguredYet()) {
//                if (backgroundColor != null) this.sdkConfig.backgroundColor = backgroundColor
//                if (mainTextColor != null) this.sdkConfig.mainTextColor = mainTextColor
//                if (mainColor != null) this.sdkConfig.mainColor = mainColor
//                if (btnTextColor != null) this.sdkConfig.btnTextColor = btnTextColor
//            }
//        }
//
//        @JvmStatic
//        fun setLivenessVersion(livenessVersion: Int) {
//            if (isConfiguredYet()) {
//                this.sdkConfig.livenessVersion = livenessVersion
//            }
//        }
//
//        private var startTime: Long = 0
//
//
//        @JvmStatic
//        fun start(context: Context, userId: String, flowType: FaceOTPFlowType, klpFaceOTPListener: KLPFaceOTPListener) {
//            this.userId = userId
//            this.klpListener = klpFaceOTPListener
//            if (isConfiguredYet()) {
//                this.flowType = flowType
//                KalapaNFC.configure(
//                    KalapaNFCConfig.Builder(context)
//                        .withLanguage(if (sdkConfig.language.contains("en")) Language.EN else Language.VI)
//                        .build(),
//                    object : KalapaNFCListener {
//                        override fun onSuccess(result: String) {
//                            klpFaceOTPListener.complete(flowType, userId, result)
//                        }
//
//                        override fun onFail(reason: String) {
//                            klpFaceOTPListener.cancel(flowType, userId, reason)
//                        }
//
//                        override fun onError(errorCode: NFCResultCode, message: String) {
//                            klpFaceOTPListener.cancel(flowType, userId, message)
//                        }
//
//                        override fun onCheckNFCAvailable(status: NFCAvailablityStatus) {
//                            if (status == NFCAvailablityStatus.NOT_SUPPORTED) {
//                                klpFaceOTPListener.cancel(
//                                    flowType,
//                                    userId,
//                                    NFCAvailablityStatus.NOT_SUPPORTED.name
//                                )
//                            }
//                        }
//
//                    }
//                )
//                KalapaNFC.start(context, null, flowType)
//            }
//        }
//
//
//    }
//
//
//    enum class FaceOTPFlowType(val flow: String?) {
//        EKYC("ekyc"),
//        NFC("nfc"),
//        EKYC_NFC("nfc_ekyc"),
//        VERIFY(null)
//    }
//
//}
//
