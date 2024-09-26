package vn.kalapa.ekyc.managers

import android.content.Context
import android.content.res.Resources
import vn.kalapa.R
import vn.kalapa.ekyc.handlers.GetAllLanguageHandler
import vn.kalapa.ekyc.handlers.GetDynamicLanguageHandler
import vn.kalapa.ekyc.models.KalapaAllLanguageModel
import vn.kalapa.ekyc.models.KalapaLanguageModel
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import java.util.Dictionary

// Singleton way
object KLPLanguageManager {
    private val languageMap = HashMap<String, HashMap<String, String>>()
    private lateinit var language: KLPLanguage

    //    private val resourceMap = HashMap<Int, String>()
    fun get(key: String): String {
        if (this::language.isInitialized) {
//            Helpers.printLog("get $language $key ${(languageMap[language.name])?.get(key) ?: key}")
            return (languageMap[language.name])?.get(key) ?: key
        } else return key
    }

    private fun loadResourcesMap(context: Context) {
        val resources: Resources = context.resources
        val packageName = context.packageName
        val stringResIds = resources.getIdentifier("string", "values", packageName)
        val fields = R.string::class.java.fields

        val allStrings = fields.associate { field ->
            val id = field.getInt(null)
            field.name to resources.getString(id)
        }

        allStrings.forEach { (name, value) ->
            Helpers.printLog("Resources: $name: $value")
        }
    }


    fun pullLanguage(baseURL: String) {
        Helpers.printLog(this@KLPLanguageManager, "Start Pulling Language")
        var languageJsonBody = GetAllLanguageHandler().execute(baseURL).get()
        Helpers.printLog(this@KLPLanguageManager, "End Pulling Language - $languageJsonBody")

        if (!languageJsonBody.isNullOrEmpty() && languageJsonBody != "-1") {
            val klpAllLanguageModel = KalapaAllLanguageModel.fromJson(languageJsonBody)
            if ((klpAllLanguageModel.error != null) && (klpAllLanguageModel.error.code == 200) && klpAllLanguageModel.data != null) {
                klpAllLanguageModel.data.forEach {
                    if (it.content?.SDK?.isNotEmpty() == true) {
                        setDictionary(KLPLanguage.fromCountryCode(it.code), it.content.SDK, it.content.APP_DEMO)
                    }
                }
            }
        }
    }

    private fun setDictionary(language: KLPLanguage, vararg dictionaries: Map<String, String>) {
        var keyMap = languageMap[language.name] ?: HashMap()
        dictionaries.forEach {
            for (key in it.keys) {
//                Helpers.printLog("$key ${it[key]}")
                keyMap[key] = it[key] ?: key
            }
        }
        languageMap[language.name] = keyMap
//        Helpers.printLog(languageMap)
    }

    fun setLanguage(language: KLPLanguage): KLPLanguageManager {
        this.language = language
        return this
    }

    fun setLanguage(language: String): KLPLanguageManager {

        Helpers.printLog("setLanguage from ${if (this::language.isInitialized) this.language else "null"} to ${KLPLanguage.fromCountryCode(language)}")
        this.language = KLPLanguage.fromCountryCode(language)
        return this
    }

    enum class KLPLanguage {
        ENGLISH, VIETNAMESE, KOREAN;

        companion object {
            fun fromCountryCode(code: String): KLPLanguage {
                return when (code) {
                    "en" -> ENGLISH
                    "vi" -> VIETNAMESE
                    "ko" -> KOREAN
                    else -> ENGLISH
                }
            }
        }
    }

}