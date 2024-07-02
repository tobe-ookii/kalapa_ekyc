package vn.kalapa.ekyc.models

import com.google.gson.Gson

// To parse the JSON, install Klaxon and do:
//
//   val welcome8 = Welcome8.fromJson(jsonString)


data class KalapaLanguageModel(
    val data: LanguageModelData? = null,
    val error: MyError?,
) {
    fun toJson() = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): KalapaLanguageModel = Gson().fromJson(json, KalapaLanguageModel::class.java)// klaxon.parse<KalapaLanguageModel>(json)
    }
}

data class LanguageModelData(
    val data: LanguageModelDataData? = null,
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): LanguageModelData = Gson().fromJson(json, LanguageModelData::class.java)// klaxon.parse<LanguageModelData>(json)
    }
}

data class LanguageModelDataData(
    val APP_DEMO: Map<String, String>,
    val SDK: Map<String, String>
) {
    fun toJson() = Gson().toJson(this)// klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): LanguageModelDataData = Gson().fromJson(json, LanguageModelDataData::class.java)//klaxon.parse<LanguageModelDataData>(json)
    }
}
