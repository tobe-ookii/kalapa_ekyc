package vn.kalapa.ekyc.models

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import vn.kalapa.ekyc.nfcsdk.models.NFCResultData

data class KalapaResult(
    var session: String? = "",
    var idNumber: String? = "",
    var name: String? = "",
    var gender: String? = "",
    var home: String? = "",
    var type: String? = "",
    var decision: String? = "",
    var national: String? = "",
    var resident: String? = "",

    var doe: String? = "",
    var doi: String? = "",

    var religion: String? = "",
    var ethnicity: String? = "",
    var country: String? = "",
    var poi: String? = "",
    var code: String? = "",
    var pp_number: String? = "",
    var pob: String? = "",
    var features: String? = "",
    var birthday: String? = "",
    var home_entities: Entities? = Entities(),
    var resident_entities: Entities? = Entities(),
    var qr_code: QrCode? = QrCode(),
    var mrz_data: MRZ? = MRZ(),
    var nfc_data: NFCResultData? = NFCResultData(),
    var selfie_data: SelfieResultData? = SelfieResultData(),
    var decision_detail: List<DecisionDetail>? = null
) {
    fun toJson() = Gson().toJson(this)
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "gender" to gender,
            "home" to home,
            "type" to type,
            "decision" to decision,
            "national" to national,
            "resident" to resident,
            "session" to session,
            "idNumber" to idNumber,
            "doe" to doe,
            "doi" to doi,
            "name" to name,
            "religion" to religion,
            "ethnicity" to ethnicity,
            "country" to country,
            "poi" to poi,
            "features" to features,
            "birthday" to birthday,
            "pob" to pob,
            "pp_number" to pp_number,
            "code" to code,
            "home_entities" to (home_entities?.toMap() ?: ""),
            "resident_entities" to (resident_entities?.toMap() ?: ""),
            "qr_code" to (qr_code?.toMap() ?: ""),
            "mrz_data" to (mrz_data?.toMap() ?: "")
        )
    }

    companion object {
        fun fromJson(json: String): KalapaResult = Gson().fromJson(json, KalapaResult::class.java)// klaxon.parse<KalapaResult>(json)
    }
}


data class ConfirmResult(
    val session: String? = "",
    val decision_detail: Decision? = Decision(),
    val recorrect_data: KLPFields? = KLPFields(),
    val nfc_data: NFCResultData? = null,
    val selfie_data: SelfieResult? = null
) {
    fun toJson() = Gson().toJson(this) // klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): ConfirmResult = Gson().fromJson(json, ConfirmResult::class.java)//  = klaxon.parse<ConfirmResult>(json)
    }
}

data class Decision(
    val decision: String? = "",
    val details: List<DecisionDetail>? = listOf()
)

data class DecisionDetail(
    val code: String? = "",
    val description: String? = "",
    val description_vi: String? = "",
    val info: Info? = null,
    val is_pass: Int? = 0,
    val alias: String? = ""
)

data class Info(
    val level: Int,
    val sensitivity: Int? = null
)

data class Entities(
    val district: String? = "",
    val province: String? = "",
    val ward: String? = "",
    val unknown: String? = ""
) {
    override fun toString(): String {
        return "{\n \"district\": \" ${this.district}\", \"province\": \"${this.province}\", \"ward\": \"${this.ward}\", \"unknown\": \"${this.unknown}\" \n}"
    }

    fun toMap(): Map<String, String?> {
        return mapOf(
            "district" to district,
            "province" to province,
            "ward" to ward,
            "unknown" to unknown
        )
    }

    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): Entities = Gson().fromJson(json, Entities::class.java) // klaxon.parse<Entities>(json)
    }
}

data class KLPFields(
    val birthday: String? = "",
    val doe: String? = "",
    val doi: String? = "",
    val ethnicity: String? = "",
    val features: String? = "",
    val gender: String? = "",
    val home: String? = "",

    val id_number: String? = "",
    val name: String? = "",
    val poi: String? = "",
    val religion: String? = "",
    val resident: String? = "",
    val type: String? = "",
    val national: String? = "",
    val country: String? = "",
    val code: String? = "",
    val dob: String? = "",
    val nationality: String? = "",
    val pob: String? = "",

    val pp_number: String? = "",
    val home_entities: Entities? = Entities(),

    val resident_entities: Entities = Entities()
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): KLPFields = Gson().fromJson(json, KLPFields::class.java) //klaxon.parse<KLPFields>(json)
    }
}

data class QrCode(
    val data: QrCodeData? = QrCodeData(),
    val error: MyError? = MyError()
) {
    override fun toString(): String {
        return "{\n \"data\":{\n \"decoded_text\": \"${this.data?.decoded_text}\",\"stage\": ${this.data?.stage}\n}, " +
                "{\n \"error\": ${this.error?.code}, \"message\": \"${this.error?.message}\"\n} " +
                "\n}"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "data" to (data?.toMap() ?: ""),
            "error" to (error?.toMap() ?: "")
        )
    }

    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): QrCode = Gson().fromJson(json, QrCode::class.java) //  klaxon.parse<QrCode>(json)
    }
}

data class QrCodeData(
    val decoded_text: String? = null,
    val stage: Int? = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "decoded_text" to decoded_text,
            "stage" to stage
        )
    }

    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): QrCodeData = Gson().fromJson(json, QrCodeData::class.java)
    }

}


data class MRZ(
    val data: MRZData? = MRZData(),
    val error: MyError? = MyError()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "data" to (data?.toMap() ?: ""),
            "error" to (error?.toMap() ?: "")
        )
    }

    fun toJson() = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): MRZ = Gson().fromJson(json, MRZ::class.java) //  = klaxon.parse<MRZ>(json)
    }
}

//"fields": {
//        "birthday": "02/09/1997",
//        "doe": "02/09/2037",
//        "gender": "Nam",
//        "id_number": "035097001154",
//        "name": "TA MINH PHUC"
//},
//"raw_mrz": "IDVNM0970011541035097001154<<5\n9709029M3709027VNM<<<<<<<<<<<8\nTA<<MINH<PHUC<<<<<<<<<<<<<<<<<"
data class MRZData(
    val fields: MRZDataField? = MRZDataField(),
    val raw_mrz: String? = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fields" to (fields?.toMap() ?: ""),
            "raw_mrz" to raw_mrz
        )
    }

    fun toJson() = Gson().toJson(this) // klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): MRZData = Gson().fromJson(json, MRZData::class.java)//  = klaxon.parse<MRZData>(json)
    }
}

data class MRZDataField(
    val birthday: String? = "",
    val doe: String? = "",
    val gender: String? = "",
    val id_number: String? = "",
    val name: String? = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "birthday" to birthday,
            "doe" to doe,
            "gender" to gender,
            "id_number" to id_number,
            "name" to name
        )
    }

    fun toJson() = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): MRZDataField = Gson().fromJson(json, MRZDataField::class.java)// klaxon.parse<MRZDataField>(json)
    }
}

//data class NFCResult(
//    var mrz: String? = "",
//    @Json(name = "face_image")
//    var image: String? = "", // base64
//    @Json(name = "id_number")
//    var idCardNo: String? = "",
//    @Json(name = "old_id_number")
//    val oldIdCardNo: String? = "",
//    val name: String? = "",
//
//    @Json(name = "date_of_birth")
//    val dateOfBirth: String? = "",
//    val gender: String? = "",
//    val nationality: String? = "",
//    @Json(name = "nation")
//    val ethnic: String? = "",
//    val religion: String? = "",
//    @Json(name = "hometown")
//    val placeOfOrigin: String? = "",
//    @Json(name = "address")
//    val residenceAddress: String? = "",
//    @Json(name = "personal_identification")
//    val personalSpecificIdentification: String? = "",
//    @Json(name = "date_of_issuance")
//    val dateOfIssuance: String? = "",
//    @Json(name = "date_of_expiry")
//    val dateOfExpiry: String? = "",
//    @Json(name = "mother_name")
//    val motherName: String? = "",
//    @Json(name = "father_name")
//    val fatherName: String? = "",
//    @Json(name = "spouse_name")
//    val spouseName: String? = "",
//    val serial: String? = "",
//    val transID: String? = ""
//) {
//    fun toJson() = klaxon.toJsonString(this)
//
//    companion object {
//        fun fromJson(json: String) = klaxon.parse<NFCResult>(json)
//    }
//}

data class CreateSessionResult(
    @SerializedName("token")
    val token: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("flow")
    val flow: String
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): CreateSessionResult = Gson().fromJson(json, CreateSessionResult::class.java) // klaxon.parse<CreateSessionResult>(json)
    }
}

data class FrontResult(
    val fields: KLPFields? = KLPFields(),
    val qr_code: QrCode? = QrCode(),
    val mrz_data: MRZ? = MRZ(),
    val card_type: String
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): FrontResult = Gson().fromJson(json, FrontResult::class.java)// klaxon.parse<FrontResult>(json)
    }
}


data class PassportResult(
    val fields: PassportFields

) {
    fun toJson() = Gson().toJson(this)//klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): PassportResult = Gson().fromJson(json, PassportResult::class.java) //  klaxon.parse<PassportResult>(json)
    }
}

data class PassportFields(
    val code: String,
    val dob: String,
    val doe: String,
    val doi: String,
    val gender: String,

    val id_number: String,

    val name: String,
    val nationality: String,
    val pob: String,
    val poi: String,

    val pp_number: String,

    val type: String
)

data class SelfieResult(
    var error: KalapaError? = null,
    var data: SelfieResultData? = SelfieResultData()
) {
    fun toJson() = Gson().toJson(this) //klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): SelfieResult = Gson().fromJson(json, SelfieResult::class.java)// klaxon.parse<SelfieResult>(json)
    }
}

data class SelfieResultData(
    val is_matched: Boolean? = null,
    val matching_score: Int? = null
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): SelfieResultData = Gson().fromJson(json, SelfieResultData::class.java)// //klaxon.parse<SelfieResultData>(json)
    }
}

data class BackResult(
    val card_type: String?
) {
    fun toJson() = Gson().toJson(this)//klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): BackResult = Gson().fromJson(json, BackResult::class.java)//= klaxon.parse<BackResult>(json)
    }
}

data class NFCRawData(
    val sod: String,
    val dg1: String,
    val dg2: String,
    val dg3: String,
    val dg4: String,
    val dg5: String,
    val dg6: String,
    val dg7: String,
    val dg8: String,
    val dg9: String,
    val dg10: String,
    val dg11: String,
    val dg12: String,
    val dg13: String,
    val dg14: String,
    val dg15: String,
    val dg16: String
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): NFCRawData = Gson().fromJson(json, NFCRawData::class.java)// klaxon.parse<NFCRawData>(json)
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "sod" to sod,
            "dg1" to dg1,
            "dg2" to dg2,
            "dg3" to dg3,
            "dg4" to dg4,
            "dg5" to dg5,
            "dg6" to dg6,
            "dg7" to dg7,
            "dg8" to dg8,
            "dg9" to dg9,
            "dg10" to dg10,
            "dg11" to dg11,
            "dg12" to dg12,
            "dg13" to dg13,
            "dg14" to dg14,
            "dg15" to dg15,
            "dg16" to dg16

        )
    }
}
