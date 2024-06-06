package vn.kalapa.ekyc.models


val OLD_ID_CARD = "CMND9"
val OLD_12DIGITS_ID_CARD = "CMND12"
val EID_WITHOUT_CHIP = "CCCD"
val EID_WITH_CHIP = "EID"

class PreferencesConfig(
    val token: String,
    val livenessVersion: Int,
    val backgroundColor: String,
    val mainColor: String,
    val mainTextColor: String,
    val btnTextColor: String,
    val language: String,
    val env: String,
    val useNFC: Boolean,
    val captureImage: Boolean,
    var verifyCheck: Boolean,
    var fraudCheck: Boolean,
    var normalCheckOnly: Boolean,
    val cardSidesCheck: Boolean,
    val faceMatchThreshold: Int,
    val acceptOldId: Boolean,
    val accept12DigisterOldId: Boolean,
    val acceptEidWithoutChip: Boolean,
    val acceptEidWithChip: Boolean
    ) {
    fun parserObj(root: PreferencesConfig): PreferencesConfig {
        return PreferencesConfig(
            token = root.token,
            livenessVersion = root.livenessVersion,
            backgroundColor = root.backgroundColor,
            mainColor = root.mainColor,
            mainTextColor = root.mainTextColor,
            btnTextColor = root.btnTextColor,
            language = root.language,
            env = root.env,
            useNFC = root.useNFC,
            captureImage = root.captureImage,
            verifyCheck = root.verifyCheck,
            fraudCheck = root.fraudCheck,
            normalCheckOnly = root.normalCheckOnly,
            cardSidesCheck = root.cardSidesCheck,
            faceMatchThreshold = root.faceMatchThreshold,
            acceptOldId = root.acceptOldId,
            accept12DigisterOldId = root.accept12DigisterOldId,
            acceptEidWithoutChip = root.acceptEidWithoutChip,
            acceptEidWithChip = root.acceptEidWithChip
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "env" to env,
            "token" to token,
            "liveness_version" to livenessVersion,
            "background" to backgroundColor,
            "main_color" to mainColor,
            "main_text_color" to mainTextColor,
            "btn_text_color" to btnTextColor,
            "language" to language,
            "liveness_version" to livenessVersion,
            "env" to env,
            "use_nfc" to useNFC,
            "capture_image" to captureImage
        )
    }

    fun getAcceptedDocument(): Array<String> {
        var acceptedDocuments: ArrayList<String> = ArrayList()
        if (acceptOldId) acceptedDocuments.add(OLD_ID_CARD)
        if (accept12DigisterOldId) acceptedDocuments.add(OLD_12DIGITS_ID_CARD)
        if (acceptEidWithoutChip) acceptedDocuments.add(EID_WITHOUT_CHIP)
        if (acceptEidWithChip) acceptedDocuments.add(EID_WITH_CHIP)
        return acceptedDocuments.toTypedArray()
    }


    override fun toString(): String {
        return "PreferencesConfig token - $token env - $env, liveness_version - $livenessVersion, background - $backgroundColor, main_color - $mainColor," +
                " main_text_color - $mainTextColor, btn_text_color - $btnTextColor, language - $language, useNFC - $useNFC, captureImage - $captureImage, liveness_version - $livenessVersion"
    }
}