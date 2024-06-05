package vn.kalapa.ekyc.models

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()

data class GetVersionResult(
    @Json(name = "android_app_version")
    val android_app_version: String,
    @Json(name = "android_build_version")
    val android_build_version: String,
    @Json(name = "android_update_url")
    val android_update_url: String
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<GetVersionResult>(json)
    }
}