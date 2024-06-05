package vn.kalapa.ekyc.models
// To parse the JSON, install Klaxon and do:
//
//   val welcome8 = Welcome8.fromJson(jsonString)

import com.beust.klaxon.*

private val klaxon = Klaxon()


data class KalapaLanguageModel(
    @Json(name = "data")
    val data: LanguageModelData? = null,
    @Json(name = "error", ignored = false)
    val error: MyError?,
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<KalapaLanguageModel>(json)
    }
}

data class LanguageModelData(
    @Json(name = "data", ignored = false)
    val data: LanguageModelDataData? = null,
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<LanguageModelData>(json)
    }
}

data class LanguageModelDataData(
    @Json(name = "APP_DEMO", ignored = false)
    val appDemo: Map<String, String>,
    @Json(name = "SDK", ignored = false)
    val sdk:  Map<String, String>
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<LanguageModelDataData>(json)
    }
}
