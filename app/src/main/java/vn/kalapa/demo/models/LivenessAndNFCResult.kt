package vn.kalapa.demo.models

import com.google.gson.Gson
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.nfcsdk.models.NFCResultData


data class NFCVerificationResponse(
    var error: KalapaError? = null,
    var data: NFCVerificationData? = NFCVerificationData()
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): NFCVerificationResponse = Gson().fromJson(json, NFCVerificationResponse::class.java)//klaxon.parse<NFCVerificationResponse>(json)
    }
}

data class NFCVerificationData(
    var nfc_data: NFCCardData? = NFCCardData(),
    var is_match: Boolean? = null,
    var matching_score: Int? = null
) {
    fun toJson() = Gson().toJson(this) //klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): NFCVerificationData = Gson().fromJson(json, NFCVerificationData::class.java)// klaxon.parse<NFCVerificationData>(json)
    }
}

data class NFCCardData(
    var card_data: NFCResultData? = NFCResultData(),
    var is_valid: Boolean? = null
) {
    fun toJson() = Gson().toJson(this)//klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): NFCCardData = Gson().fromJson(json, NFCCardData::class.java) // klaxon.parse<NFCCardData>(json)
    }
}