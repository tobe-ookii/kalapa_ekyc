package vn.kalapa.ekyc.models

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.google.gson.annotations.SerializedName
import vn.kalapa.ekyc.nfcsdk.models.NFCResultData

private val klaxon = Klaxon()

//class KalapaModelFactory{}


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
    @Json(name = "decision_detail")
    var decisionDetail: List<DecisionDetail>? = null
) {
    public fun toJson() = klaxon.toJsonString(this)
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
        public fun fromJson(json: String) = klaxon.parse<KalapaResult>(json)
    }
}


data class ConfirmResult(
    @Json(ignored = false)
    val session: String? = "",
    @Json(ignored = false)
    val decision_detail: Decision? = Decision(),
    @Json(ignored = false)
    val recorrect_data: KLPFields? = KLPFields(),
    @Json(ignored = false)
    val nfc_data: NFCResultData? = null,
    @Json(ignored = false)
    val selfie_data: SelfieResult? = null
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<ConfirmResult>(json)
    }
}

data class Decision(
    @Json(ignored = false)
    val decision: String? = "",
    @Json(ignored = false)
    val details: List<DecisionDetail>? = listOf()
)

data class DecisionDetail(
    @Json(ignored = false)
    val code: String? = "",
    @Json(ignored = false)
    val description: String? = "",

    @Json(name = "description_vi")
    val descriptionVi: String? = "",
    @Json(ignored = false)
    val info: Info? = null,

    @Json(name = "is_pass")
    val isPass: Int? = 0,

    @Json(name = "alias")
    val alias: String? = ""
)

data class Info(
    @Json(ignored = false)
    val level: Int,
    @Json(ignored = false)
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

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Entities>(json)
    }
}

data class KLPFields(
    @Json(name = "birthday", ignored = false)
    val birthday: String? = "",
    @Json(name = "doe", ignored = false)
    val doe: String? = "",
    @Json(name = "doi", ignored = false)
    val doi: String? = "",
    @Json(name = "ethnicity", ignored = false)
    val ethnicity: String? = "",
    @Json(name = "features", ignored = false)
    val features: String? = "",
    @Json(name = "gender", ignored = false)
    val gender: String? = "",
    @Json(name = "home", ignored = false)
    val home: String? = "",

    @Json(name = "id_number", ignored = false)
    @SerializedName("id_number")
    val idNumber: String? = "",
    @Json(name = "name", ignored = false)
    val name: String? = "",
    @Json(name = "poi", ignored = false)
    val poi: String? = "",
    @Json(name = "religion", ignored = false)
    val religion: String? = "",
    @Json(name = "resident", ignored = false)
    val resident: String? = "",
    @Json(name = "type", ignored = false)
    val type: String? = "",
    @Json(name = "national", ignored = false)
    val national: String? = "",
    @Json(name = "country", ignored = false)
    val country: String? = "",
    @Json(name = "code", ignored = false)
    val code: String? = "",
    @Json(name = "dob", ignored = false)
    val dob: String? = "",
    @Json(name = "nationality", ignored = false)
    val nationality: String? = "",
    @Json(name = "pob", ignored = false)
    val pob: String? = "",

    @Json(name = "pp_number", ignored = false)
    @SerializedName("pp_number")
    val ppNumber: String? = "",

    @Json(name = "home_entities", ignored = false)
    @SerializedName("home_entities")
    val homeEntities: Entities? = Entities(),

    @Json(name = "resident_entities", ignored = false)
    @SerializedName("resident_entities")
    val residentEntities: Entities = Entities()
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<KLPFields>(json)
    }
}

data class QrCode(
    @SerializedName("data")
    @Json(name = "data", ignored = false)
    val data: QrCodeData? = QrCodeData(),
    @SerializedName("error")
    @Json(name = "error", ignored = false)
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

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<QrCode>(json)
    }
}

data class QrCodeData(
    @SerializedName("decoded_text") val decoded_text: String? = null,
    @SerializedName("stage") val stage: Int? = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "decoded_text" to decoded_text,
            "stage" to stage
        )
    }

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<QrCodeData>(json)
    }

}


data class MRZ(
    val data: MRZData? = MRZData(),
    @Json(name = "error", ignored = false)
    val error: MyError? = MyError()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "data" to (data?.toMap() ?: ""),
            "error" to (error?.toMap() ?: "")
        )
    }

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<MRZ>(json)
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
    @Json(name = "fields", ignored = false)
    val mrzDataFields: MRZDataField? = MRZDataField(),
    @Json(name = "raw_mrz", ignored = false)
    val rawMRZ: String? = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fields" to (mrzDataFields?.toMap() ?: ""),
            "raw_mrz" to rawMRZ
        )
    }

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<MRZData>(json)
    }
}

data class MRZDataField(
    @Json(name = "birthday", ignored = false)
    val birthday: String? = "",
    @Json(name = "doe", ignored = false)
    val doe: String? = "",
    @Json(name = "gender", ignored = false)
    val gender: String? = "",
    @Json(name = "id_number", ignored = false)
    val id_number: String? = "",
    @Json(name = "name", ignored = false)
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

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<MRZDataField>(json)
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
    val token: String,
    @Json(name = "client_id")
    val username: String,
    val flow: String
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<CreateSessionResult>(json)
    }
}

data class FrontResult(
    @Json(name = "fields", ignored = false)
    val myFields: KLPFields? = KLPFields(),
    @Json(name = "qr_code", ignored = false)
    val qrCode: QrCode? = QrCode(),
    @Json(name = "mrz_data", ignored = false)
    val mrzData: MRZ? = MRZ(),
    @Json(name = "card_type", ignored = false)
    val cardType: String
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<FrontResult>(json)
    }
}


data class PassportResult(
    @Json("fields")
    val fields: PassportFields

) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<PassportResult>(json)
    }
}

data class PassportFields(
    val code: String,
    val dob: String,
    val doe: String,
    val doi: String,
    val gender: String,

    @Json(name = "id_number")
    val idNumber: String,

    val name: String,
    val nationality: String,
    val pob: String,
    val poi: String,

    @Json(name = "pp_number")
    val ppNumber: String,

    val type: String
)

data class SelfieResult(
    var error: KalapaError? = null,
    var data: SelfieResultData? = SelfieResultData()
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<SelfieResult>(json)
    }
}

data class SelfieResultData(
    val is_matched: Boolean? = null,
    val matching_score: Int? = null
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<SelfieResultData>(json)
    }
}

data class BackResult(
    @Json(name = "card_type")
    val cardType: String?
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<BackResult>(json)
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
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<NFCRawData>(json)
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
