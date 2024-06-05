package vn.kalapa.ekyc.nfcsdk.models

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import vn.kalapa.ekyc.models.KalapaError

private val klaxon = Klaxon()

data class NFCResult(
    var error: KalapaError? = null,
    var data: NFCData? = NFCData()
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<NFCResult>(json)
    }
}

data class NFCData(
    var mrz: String? = "",

    @Json(name = "id_number")
    var idCardNo: String? = "",
    @Json(name = "old_id_number")
    val oldIdCardNo: String? = "",
    val name: String? = "",

    @Json(name = "date_of_birth")
    val dateOfBirth: String? = "",
    val gender: String? = "",
    val nationality: String? = "",
    @Json(name = "nation")
    val ethnic: String? = "",
    val religion: String? = "",
    @Json(name = "hometown")
    val placeOfOrigin: String? = "",
    @Json(name = "address")
    val residenceAddress: String? = "",
    @Json(name = "personal_identification")
    val personalSpecificIdentification: String? = "",
    @Json(name = "date_of_issuance")
    val dateOfIssuance: String? = "",
    @Json(name = "date_of_expiry")
    val dateOfExpiry: String? = "",
    @Json(name = "mother_name")
    val motherName: String? = "",
    @Json(name = "father_name")
    val fatherName: String? = "",
    @Json(name = "spouse_name")
    val spouseName: String? = "",
    val serial: String? = "",
    val transID: String? = "",
    @Json(name = "face_image")
    var image: String? = "", // base64
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<NFCData>(json)
    }
}