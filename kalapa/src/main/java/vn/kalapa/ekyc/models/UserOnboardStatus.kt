package vn.kalapa.ekyc.models


import com.beust.klaxon.*

private val klaxon = Klaxon()

data class UserOnboardStatus(
    @Json(name = "has_nfc")
    val hasNFC: Boolean?,
    @Json(name = "has_ekyc")
    val hasEKYC: Boolean?,
    val indexed: Boolean?
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<UserOnboardStatus>(json)
    }
}
