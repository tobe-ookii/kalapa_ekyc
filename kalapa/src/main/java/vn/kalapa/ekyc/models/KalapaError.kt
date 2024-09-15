package vn.kalapa.ekyc.models

import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.R


data class KalapaError(val code: Int, var message: String = "") {
    companion object {
        val NetworkError = KalapaError(
            -1,
            KalapaSDK.config.languageUtils.getLanguageString("klp_error_network")
        ) // "Xảy ra lỗi, vui lòng thử lại..."
        val UnknownError = KalapaError(
            -1,
            KalapaSDK.config.languageUtils.getLanguageString("klp_error_unknown_short")
        )
        val ExpiredError = KalapaError(
            -1,
            KalapaSDK.config.languageUtils.getLanguageString("klp_timeout_body")
        )
    }


    constructor(code: Int) : this(code, "") {
//        Helpers.printLog("$code Get String: ${rootActivity.getString(R.string.klp_error_doc_not_found)}")
        message = when (code) {
            400 -> KalapaSDK.config.languageUtils.getLanguageString("klp_error_input_invalid")

            500 -> KalapaSDK.config.languageUtils.getLanguageString("klp_error_service_temporary_down")

            else -> KalapaSDK.config.languageUtils.getLanguageString("klp_error_service_temporary_down")
        }
    }

    fun getMessageError(): String {
        when (code) {
            -1 -> {
                if (message == "SSL handshake timed out" || message.contains("SSL handshake aborted")
                    || message.contains("Unable to resolve host") || message.contains("Software caused connection abort")
                )
                    return KalapaSDK.config.languageUtils.getLanguageString("klp_error_network")
                if (message == "Wrong Token")
                    return KalapaSDK.config.languageUtils.getLanguageString("klp_error_token")
                if (message == "Session Expired")
                    return KalapaSDK.config.languageUtils.getLanguageString("klp_timeout_body")
                if (message == "timeout")
                    return KalapaSDK.config.languageUtils.getLanguageString("klp_error_service_temporary_down")
            }
        }
        return message
    }
}