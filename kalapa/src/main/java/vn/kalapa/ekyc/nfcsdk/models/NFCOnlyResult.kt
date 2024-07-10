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

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "mrz" to mrz,
            "id_number" to id_number,
            "old_id_number" to old_id_number,
            "name" to name,
            "date_of_birth" to date_of_birth,
            "gender" to gender,
            "nationality" to nationality,
            "nation" to nation,
            "religion" to religion,
            "hometown" to hometown,
            "address" to address,
            "personal_identification" to personal_identification,
            "date_of_issuance" to date_of_issuance,
            "date_of_expiry" to date_of_expiry,
            "mother_name" to mother_name,
            "father_name" to father_name,
            "spouse_name" to spouse_name,
            "serial" to serial,
            "transID" to transID,
            "face_image" to face_image
        )
    }
    companion object {
        fun fromJson(json: String): NFCResultData = Gson().fromJson(json, NFCResultData::class.java)// klaxon.parse<NFCResultData>(json)
    }
}