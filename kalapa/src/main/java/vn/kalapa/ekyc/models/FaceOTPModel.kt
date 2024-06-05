package vn.kalapa.ekyc.models
// To parse the JSON, install Klaxon and do:
//
//   val welcome8 = Welcome8.fromJson(jsonString)


import com.google.gson.annotations.SerializedName

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class MyError(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<MyError>(json)
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "code" to code,
            "message" to message
        )

    }

}

data class KalapaOTP(
    @Json(name = "otp", ignored = false)
    val otp: String = "",
    @Json(name = "valid_time", ignored = false)
    val validTime: Int = 30,
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<KalapaOTP>(json)
    }
}

