package vn.kalapa.ekyc.models



class PreferencesConfig constructor(
    val token: String,
    val livenessVersion: Int,
    val scenario: String,
    val backgroundColor: String,
    val mainColor: String,
    val mainTextColor: String,
    val btnTextColor: String,
    val language: String,
    val env: String
) {
    fun parserObj(root: PreferencesConfig): PreferencesConfig {
        return PreferencesConfig(
            token = root.token,
            livenessVersion = root.livenessVersion,
            scenario = root.scenario,
            backgroundColor = root.backgroundColor,
            mainColor = root.mainColor,
            mainTextColor = root.mainTextColor,
            btnTextColor = root.btnTextColor,
            language = root.language,
            env = root.env
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "env" to env,
            "token" to token,
            "livenessVersion" to livenessVersion,
            "background" to backgroundColor,
            "mainColor" to mainColor,
            "mainTextColor" to mainTextColor,
            "btnTextColor" to btnTextColor,
            "language" to language,
            "livenessVersion" to livenessVersion,
            "scenario" to scenario,
            "env" to env
        )
    }

    override fun toString(): String {
        return "PreferencesConfig token - $token env - $env, livenessVersion - $livenessVersion, background - $backgroundColor, mainColor - $mainColor," +
                " mainTextColor - $mainTextColor, btnTextColor - $btnTextColor, language - $language, scenario - $scenario livenessVersion $livenessVersion"
    }
}