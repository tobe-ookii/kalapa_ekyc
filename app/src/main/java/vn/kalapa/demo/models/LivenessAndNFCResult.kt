package vn.kalapa.demo.models

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.nfcsdk.models.NFCData

private val klaxon = Klaxon()

data class NFCVerificationResponse(
    var error: KalapaError? = null,
    var data: NFCVerificationData? = NFCVerificationData()
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<NFCVerificationResponse>(json)
    }
}

data class NFCVerificationData(
    @Json(name = "nfc_data")
    var data: NFCCardData? = NFCCardData(),
    @Json(name = "is_match")
    var isMatch: Boolean? = null,
    @Json(name = "matching_score")
    var matchingScore: Int? = null
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<NFCVerificationData>(json)
    }
}

data class NFCCardData(
    @Json(name = "card_data")
    var data: NFCData? = NFCData(),
    @Json(name = "is_valid")
    var isValid: Boolean? = null
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<NFCCardData>(json)
    }
}