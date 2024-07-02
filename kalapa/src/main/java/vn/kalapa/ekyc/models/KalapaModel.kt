package vn.kalapa.ekyc.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class KalapaModel {
}

data class MyError(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null
) {
    fun toJson() = Gson().toJson(this)
    companion object { fun fromJson(json: String) = Gson().fromJson(json, MyError::class.java) }
    fun toMap(): Map<String, Any?> { return mapOf("code" to code, "message" to message) }

}

