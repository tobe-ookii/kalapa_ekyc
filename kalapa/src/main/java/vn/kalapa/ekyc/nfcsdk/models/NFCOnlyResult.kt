package vn.kalapa.ekyc.nfcsdk.models

import com.google.gson.Gson
import vn.kalapa.ekyc.models.KalapaError


data class NFCOnlyResult(
    var error: KalapaError? = null,
    var data: NFCResultData? = NFCResultData()
) {
    fun toJson() = Gson().toJson(this)//klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): NFCOnlyResult = Gson().fromJson(json, NFCOnlyResult::class.java)// klaxon.parse<NFCOnlyResult>(json)
    }
}

data class NFCResultData(
    var mrz: String? = "",

    var id_number: String? = "",
    val old_id_number: String? = "",
    val name: String? = "",

    val date_of_birth: String? = "",
    val gender: String? = "",
    val nationality: String? = "",
    val nation: String? = "",
    val religion: String? = "",
    val hometown: String? = "",
    val address: String? = "",
    val personal_identification: String? = "",
    val date_of_issuance: String? = "",
    val date_of_expiry: String? = "",
    val mother_name: String? = "",
    val father_name: String? = "",
    val spouse_name: String? = "",
    val serial: String? = "",
    val transID: String? = "",
    var face_image: String? = "", // base64
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): NFCResultData = Gson().fromJson(json, NFCResultData::class.java)// klaxon.parse<NFCResultData>(json)
    }
}