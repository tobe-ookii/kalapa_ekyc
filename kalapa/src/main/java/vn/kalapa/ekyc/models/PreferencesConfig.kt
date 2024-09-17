package vn.kalapa.ekyc.models

import vn.kalapa.ekyc.utils.Common


val OLD_ID_CARD = "CMND9"
val OLD_12DIGITS_ID_CARD = "CMND12"
val EID_WITHOUT_CHIP = "CCCD"
val EID_WITH_CHIP = "EID"
val EID_24 = "EID24"

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
    val acceptEidWithChip: Boolean,
    val acceptEid2024: Boolean,
    val leftoverSession: String,
    val mrz: String,
    val facedataURI: String,
    scenario: String,
    scenarioPlan: String,
    val hasCustomCaptureScreen: Boolean,
    val hasCustomLivenessScreen: Boolean,
    val hasCustomNFCScreen: Boolean
) {
    val scenarioPlan = Common.SCENARIO_PLAN.getScenarioPlanFromName(scenarioPlan)
    val scenario = Common.SCENARIO.getScenarioFromName(scenario)
    fun getAcceptedDocument(): Array<String> {
        var acceptedDocuments: ArrayList<String> = ArrayList()
        if (acceptOldId) acceptedDocuments.add(OLD_ID_CARD)
        if (accept12DigisterOldId) acceptedDocuments.add(OLD_12DIGITS_ID_CARD)
        if (acceptEidWithoutChip) acceptedDocuments.add(EID_WITHOUT_CHIP)
        if (acceptEidWithChip) acceptedDocuments.add(EID_WITH_CHIP)
        if (acceptEid2024) acceptedDocuments.add(EID_24)
        return acceptedDocuments.toTypedArray()
    }


    override fun toString(): String {
        return "PreferencesConfig token - $token env - $env, liveness_version - $livenessVersion, background - $backgroundColor, main_color - $mainColor," +
                " main_text_color - $mainTextColor, btn_text_color - $btnTextColor, language - $language, useNFC - $useNFC, captureImage - $captureImage, liveness_version - $livenessVersion"
    }
}