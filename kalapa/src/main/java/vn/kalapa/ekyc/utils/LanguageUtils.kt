package vn.kalapa.ekyc.utils

import android.content.Context
import vn.kalapa.ekyc.KalapaSDK
import java.util.Locale
import kotlin.collections.HashMap

class LanguageUtils(val activity: Context, private var mapLanguage: HashMap<String, String> = HashMap()) {
    fun setLanguage(map: Map<String, String>, map2: Map<String, String>? = null) {
        for (k in map.keys) {
            mapLanguage[k] = map[k] ?: k
        }
        map2?.let {
            for (k in map2.keys) {
                mapLanguage[k] = map2[k] ?: k
            }
        }
    }

    fun getLanguageString(tag: String): String {
        val k = tag.lowercase(Locale.ROOT)
        return if (mapLanguage[k] != null) mapLanguage[k]!!
        else if (KalapaSDK.config.language.contains("vi") && VI_DEFAULT[k] != null)
            VI_DEFAULT[k]!!
        else if (KalapaSDK.config.language.contains("ko") && KO_DEFAULT[k] != null)
            KO_DEFAULT[k]!!
        else if (EN_DEFAULT[k] != null)
            EN_DEFAULT[k]!!
        else
            tag
    }

    private val EN_DEFAULT = mapOf(
        "" to ""
    )
    private val VI_DEFAULT = mapOf(
        "" to ""
    )

    private val KO_DEFAULT = mapOf(
        "" to ""
    )
}