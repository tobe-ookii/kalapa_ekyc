package vn.kalapa.demo.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.slider.Slider
import vn.kalapa.demo.MainActivityJava
import vn.kalapa.demo.R
import vn.kalapa.demo.utils.Common.MY_KEY_TOKEN
import vn.kalapa.demo.utils.Common.MY_KEY_LANGUAGE
import vn.kalapa.demo.utils.Common.MY_KEY_LIVENESS_VERSION
import vn.kalapa.demo.utils.Common.MY_KEY_MAIN_COLOR
import vn.kalapa.demo.utils.Common.MY_KEY_BTN_TEXT_COLOR
import vn.kalapa.demo.utils.Common.MY_KEY_MAIN_TEXT_COLOR
import vn.kalapa.demo.utils.Common.MY_KEY_BACKGROUND_COLOR
import vn.kalapa.demo.utils.Common.MY_KEY_ENV

import vn.kalapa.demo.utils.Common.MY_KEY_CUSTOM_CAPTURE
import vn.kalapa.demo.utils.Common.MY_KEY_CUSTOM_LIVENESS
import vn.kalapa.demo.utils.Common.MY_KEY_CUSTOM_NFC

import vn.kalapa.demo.utils.Common.MY_KEY_UPGRADE_PLAN_FROM_SESSION_ID

import vn.kalapa.demo.utils.Common.MY_KEY_SCENARIO
import vn.kalapa.demo.utils.Common.MY_KEY_MRZ
import vn.kalapa.demo.utils.Common.MY_KEY_LEFTOVER_SESSION
import vn.kalapa.demo.utils.Common.MY_KEY_FACE_DATA_URI
import vn.kalapa.demo.utils.Common.MY_KEY_ACCEPTED_DOCUMENT_1
import vn.kalapa.demo.utils.Common.MY_KEY_ACCEPTED_DOCUMENT_2
import vn.kalapa.demo.utils.Common.MY_KEY_ACCEPTED_DOCUMENT_3
import vn.kalapa.demo.utils.Common.MY_KEY_ACCEPTED_DOCUMENT_4
import vn.kalapa.demo.utils.Common.MY_KEY_ACCEPTED_DOCUMENT_5
import vn.kalapa.demo.utils.Common.MY_KEY_NFC_TIMEOUT_IN_SECONDS
import vn.kalapa.demo.utils.Common.MY_KEY_ENABLE_NFC
import vn.kalapa.demo.utils.Common.MY_KEY_CAPTURE_IMAGE
import vn.kalapa.demo.utils.Common.MY_KEY_VERIFY_CHECK
import vn.kalapa.demo.utils.Common.MY_KEY_FRAUD_CHECK
import vn.kalapa.demo.utils.Common.MY_KEY_NORMAL_CHECK_ONLY
import vn.kalapa.demo.utils.Common.MY_KEY_CARD_SIDE_CHECK

import vn.kalapa.demo.utils.Common.MY_KEY_FACE_MATCHING_THRESHOLD
import vn.kalapa.demo.utils.Helpers
import vn.kalapa.demo.utils.LogUtils

import vn.kalapa.ekyc.KalapaFlowType
import vn.kalapa.ekyc.KalapaSDKConfig
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Common.nfcAvailable
import vn.kalapa.ekyc.utils.LIVENESS_VERSION
import vn.kalapa.ekyc.utils.LocaleHelper
import vn.kalapa.ekyc.utils.SCENARIO
import vn.kalapa.ekyc.views.KLPCustomMultipleChoices
import vn.kalapa.ekyc.views.KLPCustomSwitch
import java.util.Locale

// prod: api-ekyc.kalapa.vn/face-otp
// dev: faceotp-dev.kalapa.vn/api
class SettingActivity : AppCompatActivity(), TextView.OnEditorActionListener {
//    private val KLP_PROD = "https://api-ekyc.kalapa.vn/face-otp"
//    private val KLP_DEV = "https://faceotp-dev.kalapa.vn/api"

    private val KLP_PROD = "https://ekyc-api.kalapa.vn"
    private val KLP_DEV = "https://ekyc-dev-internal.kalapa.vn"
    private val defaultConfig = KalapaSDKConfig.KalapaSDKConfigBuilder(this@SettingActivity).build()

    private lateinit var tvMRZ: TextView
    private lateinit var edtMRZ: EditText
    private lateinit var tvLeftoverSession: TextView
    private lateinit var edtLeftoverSession: EditText
    private lateinit var tvFaceData: TextView
    private lateinit var tvScenario: TextView
    private lateinit var rgScenario: KLPCustomMultipleChoices
    private lateinit var containerMrz: LinearLayout
    private lateinit var containerFaceData: LinearLayout
    private lateinit var containerUpgrade: LinearLayout
    private lateinit var containerLeftoverSession: LinearLayout
    private lateinit var containerRegister: LinearLayout
    private lateinit var containerCustom: LinearLayout
    private lateinit var ivFaceData: ImageView
    private lateinit var btnFaceData: Button
    private lateinit var rgUpgradePlan: KLPCustomSwitch

    private lateinit var tvLivenessDescription: TextView
    private lateinit var tvScenarioDescription: TextView
    private var faceBitmap: Bitmap? = null

    private lateinit var rgLanguage: KLPCustomSwitch
    private lateinit var tvEnvironment: TextView
    private lateinit var rgEnvironment: KLPCustomSwitch
    private lateinit var rgLivenessVersion: KLPCustomMultipleChoices
    private lateinit var tvLanguage: TextView
    private lateinit var tvLivenessVersion: TextView
    private lateinit var btnSaveConfig: Button
    private lateinit var rootContainer: View
    private lateinit var container: View
    private lateinit var btnMainColor: ImageView
    private lateinit var edtMainColor: EditText
    private lateinit var btnMainTextColor: Button
    private lateinit var edtMainTextColor: EditText
    private lateinit var btnButtonTextColor: Button
    private lateinit var edtButtonTextColor: EditText
    private lateinit var btnBackgroundColor: ImageView
    private lateinit var edtBackgroundColor: EditText
    private lateinit var tvMainColor: TextView
    private lateinit var tvMainTextColor: TextView
    private lateinit var tvButtonTextColor: TextView
    private lateinit var tvBackgroundColor: TextView
    private lateinit var tvScanNFC: TextView
    private lateinit var tvCaptureImage: TextView
    private lateinit var containerToken: LinearLayout

    private lateinit var containerFaceDataBitmap: CardView
    private lateinit var containerCaptureConfig: LinearLayout
    private lateinit var ivRemoveFaceDataUri: ImageView

    private lateinit var tvVerifyCheck: TextView
    private lateinit var tvFraudCheck: TextView
    private lateinit var tvStrictQualityCheck: TextView
    private lateinit var tvCardSidesMatchCheck: TextView // tv_card_sides_match_check


    private lateinit var tvAcceptanceDocument: TextView //tv_acceptance_document
    private lateinit var tvAcceptanceDocument1: TextView // tv_acceptance_document_1
    private lateinit var tvAcceptanceDocument2: TextView // tv_acceptance_document_2
    private lateinit var tvAcceptanceDocument3: TextView // tv_acceptance_document_3
    private lateinit var tvAcceptanceDocument4: TextView // tv_acceptance_document_4
    private lateinit var tvAcceptanceDocument5: TextView // tv_acceptance_document_5
    private lateinit var tvAcceptanceFaceMatchingThreshold: TextView // tv_acceptance_face_matching_threshold

    private lateinit var rgVerifyCheck: KLPCustomSwitch
    private lateinit var rgFraudCheck: KLPCustomSwitch
    private lateinit var rgStrictQualityCheck: KLPCustomSwitch
    private lateinit var rgCardSidesMatchCheck: KLPCustomSwitch
    private lateinit var rgCaptureImage: KLPCustomSwitch
    private lateinit var rgScanNFC: KLPCustomSwitch

    private lateinit var cbAcceptedOldIdCard: CheckBox
    private lateinit var cbAcceptedOld12DigitsIdCard: CheckBox
    private lateinit var cbAcceptedEidWithoutChip: CheckBox
    private lateinit var cbAcceptedEidWithChip: CheckBox
    private lateinit var cbAcceptedEid2024: CheckBox

    private lateinit var cbCaptureIdScreen: CheckBox
    private lateinit var cbLivenessScreen: CheckBox
    private lateinit var cbNFCScreen: CheckBox

    private lateinit var tvScreenCapture: TextView
    private lateinit var tvScreenLiveness: TextView
    private lateinit var tvScreenNFC: TextView
    private lateinit var tvScreen: TextView
    private lateinit var sliderFaceMatchingThreshold: Slider

    private lateinit var tvFaceDataDescription: TextView
    private lateinit var tvMRZDescription: TextView
    private lateinit var tvEnabledNFCDescription: TextView

    //    private lateinit var tvScenario: TextView
//    private lateinit var rgScenario: KLPCustomMultipleChoices
    private lateinit var edtToken: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        findViewById()
        onInitValue()
        refreshUI()
    }

    private fun pickImageFromGallery() {
        changeImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val changeImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            LogUtils.printLog("PhotoPicker", "Selected URI: $uri")
            processFromActivityResult(uri)
        } else {
            LogUtils.printLog("PhotoPicker", "No media selected")
        }
    }

    private fun processFromActivityResult(imgUri: Uri?) {
        if (imgUri != null) {
            val inputStream = contentResolver.openInputStream(imgUri)
            if (inputStream != null) {
                LogUtils.printLog("Picked image from inputStream $imgUri")
                faceBitmap = BitmapUtil.getBitmapFromUri(contentResolver, imgUri)
                faceBitmap?.let {
                    MainActivityJava.faceDataBase64 = BitmapUtil.convertBitmapToBase64(it)
                    ivFaceData.setImageBitmap(it)
                    containerFaceDataBitmap.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun findViewById() {
        rootContainer = findViewById(R.id.root_container)
        containerUpgrade = findViewById(R.id.container_upgrade)
        tvScreenNFC = findViewById(R.id.tv_screen_nfc)
        ivRemoveFaceDataUri = findViewById(R.id.iv_remove_face_data_uri)
        tvFaceDataDescription = findViewById(R.id.tv_face_data_description)
        tvMRZDescription = findViewById(R.id.tv_mrz_description)
        tvEnabledNFCDescription = findViewById(R.id.tv_enable_nfc_description)
        tvScreen = findViewById(R.id.tv_screen)
        tvScreenCapture = findViewById(R.id.tv_screen_capture)
        tvScreenLiveness = findViewById(R.id.tv_screen_liveness)
        container = findViewById(R.id.ln_container)
        containerCustom = findViewById(R.id.container_custom)
        tvMRZ = findViewById(R.id.tv_mrz)
        edtMRZ = findViewById(R.id.edt_mrz)
        tvLeftoverSession = findViewById(R.id.tv_session_id)
        edtLeftoverSession = findViewById(R.id.edt_leftover_session)
        tvFaceData = findViewById(R.id.tv_face_data)
        tvScenario = findViewById(R.id.tv_scenario)
        rgScenario = findViewById(R.id.sw_scenario)
        containerToken = findViewById(R.id.container_token)
        containerFaceDataBitmap = findViewById(R.id.container_face_data_bitmap)
        containerCaptureConfig = findViewById(R.id.container_capture_config)
        containerToken.visibility = if ((Helpers.getValuePreferences(MY_KEY_SCENARIO) ?: "") == SCENARIO.CUSTOM.name) View.GONE else View.VISIBLE
        rgScenario.listener = KLPCustomMultipleChoices.KLPCustomMultipleChoicesChangeListener {
            containerRegister.visibility = if (it == 0) View.VISIBLE else View.GONE
            containerUpgrade.visibility = if (it > 0) View.VISIBLE else View.GONE
            containerCustom.visibility = if (it == 2) View.VISIBLE else View.GONE
            rgUpgradePlan.visibility = if (it == 2) View.GONE else View.VISIBLE
            containerToken.visibility = if (it == 2) View.GONE else View.VISIBLE
            if (it == 2) {
                containerMrz.visibility = View.VISIBLE
                containerFaceData.visibility = View.GONE
                containerLeftoverSession.visibility = View.GONE
            } else {
                rgUpgradePlan.switchChangeListener(rgUpgradePlan.isPositiveCheck)
            }
        }
        containerMrz = findViewById(R.id.container_mrz)
        containerFaceData = findViewById(R.id.container_face_data)
        containerLeftoverSession = findViewById(R.id.container_leftover_session)
        containerRegister = findViewById(R.id.container_register)
        btnFaceData = findViewById(R.id.btn_choose_face_data)
        btnFaceData.setOnClickListener {
            if (Common.checkIfImageOrStorageIsGranted(this@SettingActivity, true))
                pickImageFromGallery()
            else
                Toast.makeText(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_check_permission_camera)), Toast.LENGTH_SHORT).show()
        }
        ivFaceData = findViewById(R.id.iv_face_data)

        ivRemoveFaceDataUri.setOnClickListener {
            containerFaceDataBitmap.visibility = View.GONE
            faceBitmap = null
        }

        rootContainer.setBackgroundColor(resources.getColor(R.color.ekyc_demo_color))
        rootContainer.setOnClickListener {
            hideKeyboard()
        }
        container.setOnClickListener {
            hideKeyboard()
        }

        edtToken = findViewById(R.id.edt_token)
        rgLanguage = findViewById(R.id.sw_language)
        rgLivenessVersion = findViewById(R.id.sw_liveness_version)
        rgUpgradePlan = findViewById(R.id.sw_upgrade_plan)
        rgEnvironment = findViewById(R.id.sw_enviroment)
        tvEnvironment = findViewById(R.id.tv_enviroment)
        btnSaveConfig = findViewById(R.id.btn_next)

        tvLanguage = findViewById(R.id.tv_language)
        tvLivenessVersion = findViewById(R.id.tv_liveness_version)
        tvLivenessDescription = findViewById(R.id.tv_liveness_version_description)
        tvScenarioDescription = findViewById(R.id.tv_scenario_description)
        btnSaveConfig.setOnClickListener {
            setConfigBeforeExit()
        }
        rgUpgradePlan.listener = KLPCustomSwitch.KLPCustomSwitchChangeListener {
            if (it) { // From SessionID
                containerMrz.visibility = View.GONE
                containerFaceData.visibility = View.GONE
                containerLeftoverSession.visibility = View.VISIBLE
                containerToken.visibility = if (rgScenario.selectedIndex > 0) View.GONE else View.VISIBLE
            } else { // Provided Data
                containerMrz.visibility = View.VISIBLE
                containerFaceData.visibility = View.VISIBLE
                containerLeftoverSession.visibility = View.GONE
                containerToken.visibility = View.VISIBLE
            }
        }
        rgLanguage.listener = KLPCustomSwitch.KLPCustomSwitchChangeListener {
            LocaleHelper.setLocale(this@SettingActivity, if (it) "vi" else "en")
            val locale = Locale(if (it) LocaleHelper.VIETNAMESE else LocaleHelper.ENGLISH)
            KLPLanguageManager.setLanguage(if (it) "vi" else "en")
            Locale.setDefault(locale)
            refreshUI()
        }
        rgScanNFC = findViewById(R.id.sw_enable_nfc)
        rgCaptureImage = findViewById(R.id.sw_capture_image)
        cbAcceptedEidWithChip = findViewById(R.id.cb_acceptance_document_4)
        cbAcceptedEid2024 = findViewById(R.id.cb_acceptance_document_5)
        cbCaptureIdScreen = findViewById(R.id.cb_screen_capture)
        cbLivenessScreen = findViewById(R.id.cb_screen_liveness)
        cbNFCScreen = findViewById(R.id.cb_screen_nfc)

        rgScanNFC.listener = KLPCustomSwitch.KLPCustomSwitchChangeListener {
            if (it && !nfcAvailable(this@SettingActivity)) {
                Toast.makeText(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_nfc_not_support)), Toast.LENGTH_SHORT).show()
                this.rgScanNFC.switchChangeListener(false)
            } else {
                if (it) {
                    cbAcceptedEidWithChip.isChecked = true
                    cbAcceptedEid2024.isChecked = true
                } else {
                    if (this::rgCaptureImage.isInitialized) if (!rgCaptureImage.isPositiveCheck)
                        this.rgCaptureImage.switchChangeListener(true)

                }
            }
        }
        rgCaptureImage.listener = KLPCustomSwitch.KLPCustomSwitchChangeListener {
            if (!it) {
                cbAcceptedEidWithChip.isChecked = true // Only work with eid
                cbAcceptedEid2024.isChecked = true
                if (this::rgScanNFC.isInitialized) if (!rgScanNFC.isPositiveCheck) rgScanNFC.switchChangeListener(true)
            }
            containerCaptureConfig.visibility = if (it) View.VISIBLE else View.GONE
        }
        cbAcceptedEidWithChip.setOnCheckedChangeListener { _, b ->
            LogUtils.printLog("cbAcceptedEidWithChip onCheckedChanged $b")
            if (!b && !cbAcceptedEid2024.isChecked) {
                rgScanNFC.switchChangeListener(false)
                rgCaptureImage.switchChangeListener(true)
            }
        }
        cbAcceptedEid2024.setOnCheckedChangeListener { _, b ->
            LogUtils.printLog("cbAcceptedEid2024 onCheckedChanged $b")
            if (!b && !cbAcceptedEidWithChip.isChecked) {
                rgScanNFC.switchChangeListener(false)
                rgCaptureImage.switchChangeListener(true)
            }
        }

//        rgLanguage.listener = KLPCustomMultipleChoices.KLPCustomMultipleChoicesChangeListener {
//            LogUtils.printLog("Current language is $it")
//            LocaleHelper.setLocale(
//                this@SettingActivity,
//                if (it == 0) "vi" else if (it == 1) "en" else "ko"
//            )
//            refreshUI()
//        }
        btnMainColor = findViewById(R.id.btn_main_color)
        btnMainTextColor = findViewById(R.id.btn_main_text_color)
        btnBackgroundColor = findViewById(R.id.btn_background_color)
        btnButtonTextColor = findViewById(R.id.btn_button_text_color)
        edtMainColor = findViewById(R.id.edt_main_color)

        edtMainTextColor = findViewById(R.id.edt_main_text_color)
        edtBackgroundColor = findViewById(R.id.edt_background_color)
        edtButtonTextColor = findViewById(R.id.edt_button_text_color)
        tvMainColor = findViewById(R.id.tv_main_color)
        tvMainTextColor = findViewById(R.id.tv_main_text_color)
        tvButtonTextColor = findViewById(R.id.tv_button_text_color)
        tvBackgroundColor = findViewById(R.id.tv_background_color)

        cbAcceptedOldIdCard = findViewById(R.id.cb_acceptance_document_1)
        cbAcceptedOld12DigitsIdCard = findViewById(R.id.cb_acceptance_document_2)
        cbAcceptedEidWithoutChip = findViewById(R.id.cb_acceptance_document_3)
        sliderFaceMatchingThreshold = findViewById(R.id.slider_face_matching_threshold)

        edtMainColor.addTextChangedListener(
            EditTextWatcher(
                edtMainColor,
                object : EditTextWatcherListener {
                    override fun completion(colorStr: String) {
                        refreshMainColor(colorStr)
                    }
                })
        )
        edtMainTextColor.addTextChangedListener(
            EditTextWatcher(
                edtMainTextColor,
                object : EditTextWatcherListener {
                    override fun completion(colorStr: String) {
                        refreshMainTextColor(colorStr)
                    }
                })
        )
        edtBackgroundColor.addTextChangedListener(
            EditTextWatcher(
                edtBackgroundColor,
                object : EditTextWatcherListener {
                    override fun completion(colorStr: String) {
                        refreshBackgroundColor(colorStr)
                    }
                })
        )
        edtButtonTextColor.addTextChangedListener(
            EditTextWatcher(
                edtButtonTextColor,
                object : EditTextWatcherListener {
                    override fun completion(colorStr: String) {
                        refreshBtnTextColor(colorStr)
                    }
                })
        )

        // New
        tvScanNFC = findViewById(R.id.tv_enable_nfc)
        tvCaptureImage = findViewById(R.id.tv_capture_image)

        tvVerifyCheck = findViewById(R.id.tv_verify_check)
        tvFraudCheck = findViewById(R.id.tv_fraud_check)
        tvStrictQualityCheck = findViewById(R.id.tv_strict_quality_check)
        tvCardSidesMatchCheck = findViewById(R.id.tv_card_sides_match_check)


        tvAcceptanceDocument = findViewById(R.id.tv_acceptance_document)
        tvAcceptanceDocument1 = findViewById(R.id.tv_acceptance_document_1)
        tvAcceptanceDocument2 = findViewById(R.id.tv_acceptance_document_2)
        tvAcceptanceDocument3 = findViewById(R.id.tv_acceptance_document_3)
        tvAcceptanceDocument4 = findViewById(R.id.tv_acceptance_document_4)
        tvAcceptanceDocument5 = findViewById(R.id.tv_acceptance_document_5)
        tvAcceptanceFaceMatchingThreshold = findViewById(R.id.tv_acceptance_face_matching_threshold)

        rgVerifyCheck = findViewById(R.id.sw_verify_check)
        rgFraudCheck = findViewById(R.id.sw_fraud_check)
        rgStrictQualityCheck = findViewById(R.id.sw_strict_quality_check)
        rgCardSidesMatchCheck = findViewById(R.id.sw_card_sides_match_check)

    }


    private fun refreshColor(strMainColor: String, strMainTextColor: String, strButtonTextColor: String, strBackgroundColor: String) {
        refreshMainColor(strMainColor)
        refreshBtnTextColor(strButtonTextColor)
        refreshBackgroundColor(strBackgroundColor)
        refreshMainTextColor(strMainTextColor)
    }

    private fun refreshMainColor(mainColor: String) {
        Helpers.setHelpersUI(mainColor = mainColor)
        Helpers.setBackgroundColorTintList(btnMainColor, mainColor)
        Helpers.setBackgroundColorTintList(btnButtonTextColor, mainColor)
        Helpers.setBackgroundColorTintList(btnSaveConfig, mainColor)
        Helpers.setBackgroundColorTintList(btnFaceData, mainColor)
        rgLanguage.setMainColor(mainColor)
        rgLivenessVersion.setMainColor(mainColor)
        rgUpgradePlan.setMainColor(mainColor)
        rgScenario.setMainColor(mainColor)
        rgEnvironment.setMainColor(mainColor)
        rgVerifyCheck.setMainColor(mainColor)
        rgFraudCheck.setMainColor(mainColor)
        rgStrictQualityCheck.setMainColor(mainColor)
        rgCardSidesMatchCheck.setMainColor(mainColor)
        rgCaptureImage.setMainColor(mainColor)
        rgScanNFC.setMainColor(mainColor)

        btnFaceData.setTextColor(Color.parseColor(mainColor))
        Helpers.setBackgroundColorTintList(btnFaceData, mainColor)
        Helpers.setBackgroundColorTintList(ivRemoveFaceDataUri, "#FFFFFF")
        Helpers.setCheckboxTintList(cbAcceptedOldIdCard, Color.parseColor(mainColor))
        Helpers.setCheckboxTintList(cbAcceptedEidWithoutChip, Color.parseColor(mainColor))
        Helpers.setCheckboxTintList(cbAcceptedOld12DigitsIdCard, Color.parseColor(mainColor))
        Helpers.setCheckboxTintList(cbAcceptedEidWithChip, Color.parseColor(mainColor))
        Helpers.setCheckboxTintList(cbAcceptedEid2024, Color.parseColor(mainColor))
        Helpers.setSliderTintList(sliderFaceMatchingThreshold, Color.parseColor(mainColor))

        Helpers.setCheckboxTintList(cbCaptureIdScreen, Color.parseColor(mainColor))
        Helpers.setCheckboxTintList(cbLivenessScreen, Color.parseColor(mainColor))
        Helpers.setCheckboxTintList(cbNFCScreen, Color.parseColor(mainColor))
    }

    private fun refreshBtnTextColor(txtColor: String) {
        Helpers.setHelpersUI(btnTextColor = txtColor)
        btnButtonTextColor.text = txtColor
        btnButtonTextColor.setTextColor(Color.parseColor(txtColor))
        btnSaveConfig.setTextColor(Color.parseColor(txtColor))
        rgLanguage.setTextColor(txtColor)
        rgUpgradePlan.setTextColor(txtColor)
        rgLivenessVersion.setTextColor(txtColor)
        rgScenario.setTextColor(txtColor)
        rgEnvironment.setTextColor(txtColor)
        rgVerifyCheck.setTextColor(txtColor)
        rgFraudCheck.setTextColor(txtColor)
        rgStrictQualityCheck.setTextColor(txtColor)
        rgCardSidesMatchCheck.setTextColor(txtColor)
        rgCaptureImage.setTextColor(txtColor)
        rgScanNFC.setTextColor(txtColor)
    }

    private fun refreshBackgroundColor(backgroundColor: String) {
        Helpers.setHelpersUI(backgroundColor = backgroundColor)
        Helpers.setBackgroundColorTintList(btnBackgroundColor, backgroundColor)
        Helpers.setBackgroundColorTintList(btnMainTextColor, backgroundColor)
    }

    private fun refreshMainTextColor(txtColor: String) {
        Helpers.setHelpersUI(mainTextColor = txtColor)
        btnMainTextColor.text = txtColor
        btnMainTextColor.setTextColor(Color.parseColor(txtColor))
    }

    private fun refreshUI() {
//        tvScenario.text = KLPLanguageManager.get(resources.getString(R.string.klp_index_scenario)
//        ivRemoveFaceDataUri.visibility = ivFaceData.visibility
        tvLanguage.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lang))
        rgLanguage.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lang_vi))
        rgLanguage.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lang_en))
        tvScreenNFC.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_nfc))
        tvScreen.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_screen))
        tvScreenLiveness.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_custom_screen_selfie))
        tvScreenCapture.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_custom_screen_id))
        tvMRZDescription.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_data_mrz_note))
        tvFaceDataDescription.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_data_face_note))
        tvEnabledNFCDescription.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_nfc_note))
//        rgLanguage.rbSecond.text = KLPLanguageManager.get(resources.getString(R.string.klp_faceOTP_language_en))
//        rgLanguage.rbThird.text = KLPLanguageManager.get(resources.getString(R.string.klp_faceOTP_language_ko))
        tvFaceData.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_data_face))
        tvLeftoverSession.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_session))
        rgEnvironment.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_env_prod))
        rgEnvironment.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_env_dev))
        tvLivenessDescription.text = " - ${KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv_a_note))} \n" +
                " - ${KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv_sa_note))} \n" +
                " - ${KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv_p_note))}"
        tvScenarioDescription.text = " - ${KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_note_onboard))} \n" +
                " - ${KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_note_upgrade))} \n" +
                " - ${KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_note_custom))}"

        tvLivenessVersion.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv))
        rgLivenessVersion.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv_p))
        rgLivenessVersion.rbSecond.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv_sa))
        rgLivenessVersion.rbThird.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_lv_a))

        rgUpgradePlan.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_session))
        rgUpgradePlan.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_data))
        tvScenario.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow))
        rgScenario.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_onboard))
        rgScenario.rbSecond.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade))
        rgScenario.rbThird.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_custom))

        btnFaceData.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_flow_upgrade_data_face_upload))
        rgScanNFC.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes))
        rgScanNFC.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))

        rgCaptureImage.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes))
        rgCaptureImage.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))

        rgVerifyCheck.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes))
        rgVerifyCheck.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))

        rgFraudCheck.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes))
        rgFraudCheck.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))

        rgCardSidesMatchCheck.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes))
        rgCardSidesMatchCheck.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))

        rgStrictQualityCheck.rbOne.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_yes))
        rgStrictQualityCheck.rbOther.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_no))

        btnSaveConfig.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_save))
        tvMainColor.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_color_main))
        tvMainTextColor.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_color_main_text))
        tvButtonTextColor.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_color_button_text))
        tvBackgroundColor.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_color_background))

        tvAcceptanceDocument1.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac_idold))
        tvAcceptanceDocument2.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac_idnew))
        tvAcceptanceDocument3.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac_idnew_barcode))
        tvAcceptanceDocument4.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac_eid))
        tvAcceptanceDocument5.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac_eid24))
        tvAcceptanceDocument.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac))
        tvAcceptanceFaceMatchingThreshold.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_facematch_threshold))

        tvStrictQualityCheck.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_quality_check))
        tvCardSidesMatchCheck.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_check_sides_match))
        tvVerifyCheck.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_idname))
        tvFraudCheck.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_fraud))

        tvEnvironment.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_env))//KLPLanguageManager.get(resources.getString(R.string.klp_environment))
        tvScanNFC.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_nfc))
        tvCaptureImage.text = KLPLanguageManager.get(resources.getString(R.string.klp_settings_collect_documents))
    }

    private fun setConfigBeforeExit() {
        val isAcceptedDocumentsSelectAtLeastOnce = cbAcceptedEid2024.isChecked || cbAcceptedEidWithChip.isChecked || cbAcceptedEidWithoutChip.isChecked || cbAcceptedEidWithChip.isChecked || cbAcceptedOld12DigitsIdCard.isChecked
        val isTokenEmptied = edtToken.text.isNullOrEmpty()
        val isSessionIdEmptied = edtLeftoverSession.text.isNullOrEmpty()
        val isCustomScreenSelectAtLeastOnce = cbCaptureIdScreen.isChecked || cbNFCScreen.isChecked || cbLivenessScreen.isChecked
        if (rgScenario.selectedIndex == 0 && (isTokenEmptied || !isAcceptedDocumentsSelectAtLeastOnce))
            if (isTokenEmptied)
                Helpers.showDialog(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_error_occurred_title)), "Token ${KLPLanguageManager.get(resources.getString(R.string.klp_error_empty)).lowercase()}", R.drawable.frowning_face)
            else
                Helpers.showDialog(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_error_occurred_title)), "${KLPLanguageManager.get(resources.getString(R.string.klp_settings_ac))} ${KLPLanguageManager.get(resources.getString(R.string.klp_error_must_choose)).lowercase()}", R.drawable.frowning_face)
        else if (rgScenario.selectedIndex == 1 && (rgUpgradePlan.isPositiveCheck && isSessionIdEmptied || !rgUpgradePlan.isPositiveCheck && isTokenEmptied)) // custom flow
            if (rgUpgradePlan.isPositiveCheck && isSessionIdEmptied)
                Helpers.showDialog(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_error_occurred_title)), "Session ID ${KLPLanguageManager.get(resources.getString(R.string.klp_error_empty)).lowercase()}", R.drawable.frowning_face)
            else
                Helpers.showDialog(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_error_occurred_title)), "Token ${KLPLanguageManager.get(resources.getString(R.string.klp_error_empty)).lowercase()}", R.drawable.frowning_face)
        else if (rgScenario.selectedIndex == 2 && !isCustomScreenSelectAtLeastOnce) // custom flow
            Helpers.showDialog(this@SettingActivity, KLPLanguageManager.get(resources.getString(R.string.klp_error_occurred_title)), "${KLPLanguageManager.get(resources.getString(R.string.klp_settings_screen))} ${KLPLanguageManager.get(resources.getString(R.string.klp_error_must_choose)).lowercase()}", R.drawable.frowning_face)
        else {
            Helpers.savePrefs(
                MY_KEY_LIVENESS_VERSION,
                if (rgLivenessVersion.selectedIndex == 0) LIVENESS_VERSION.PASSIVE.version else if (rgLivenessVersion.selectedIndex == 1) LIVENESS_VERSION.SEMI_ACTIVE.version else LIVENESS_VERSION.ACTIVE.version
            )

            Helpers.savePrefs(
                MY_KEY_SCENARIO, if (rgScenario.selectedIndex == 0) SCENARIO.REGISTER.name else if (rgScenario.selectedIndex == 1) SCENARIO.UPGRADE.name else SCENARIO.CUSTOM.name
            )

            Helpers.savePrefs(MY_KEY_LANGUAGE, if (rgLanguage.isPositiveCheck) "vi" else "en")
            edtToken.text.toString().isNotEmpty().let { Helpers.savePrefs(MY_KEY_TOKEN, edtToken.text.toString()) }
            Helpers.savePrefs(MY_KEY_ENV, if (rgEnvironment.isPositiveCheck) KLP_PROD else KLP_DEV)
            Helpers.savePrefs(MY_KEY_MRZ, edtMRZ.text.toString())
            Helpers.savePrefs(MY_KEY_LEFTOVER_SESSION, edtLeftoverSession.text.toString())
            Helpers.savePrefs(MY_KEY_ENABLE_NFC, rgScanNFC.isPositiveCheck)
            Helpers.savePrefs(MY_KEY_CAPTURE_IMAGE, rgCaptureImage.isPositiveCheck)
            Helpers.savePrefs(MY_KEY_VERIFY_CHECK, rgVerifyCheck.isPositiveCheck)
            Helpers.savePrefs(MY_KEY_FRAUD_CHECK, rgFraudCheck.isPositiveCheck)
            Helpers.savePrefs(MY_KEY_NORMAL_CHECK_ONLY, rgStrictQualityCheck.isPositiveCheck)
            Helpers.savePrefs(MY_KEY_CARD_SIDE_CHECK, rgCardSidesMatchCheck.isPositiveCheck)
            if (containerFaceDataBitmap.visibility != View.VISIBLE) MainActivityJava.faceDataBase64 = ""
            Helpers.savePrefs(MY_KEY_CUSTOM_NFC, cbNFCScreen.isChecked)
            Helpers.savePrefs(MY_KEY_CUSTOM_LIVENESS, cbLivenessScreen.isChecked)
            Helpers.savePrefs(MY_KEY_CUSTOM_CAPTURE, cbCaptureIdScreen.isChecked)
            Helpers.savePrefs(MY_KEY_UPGRADE_PLAN_FROM_SESSION_ID, rgUpgradePlan.isPositiveCheck)
            if (Helpers.getValuePreferences(MY_KEY_MAIN_COLOR) == null || edtMainColor.text.toString() != Helpers.getValuePreferences(
                    MY_KEY_MAIN_COLOR
                )!!
            )
                Helpers.savePrefs(MY_KEY_MAIN_COLOR, edtMainColor.text.toString())

            if (Helpers.getValuePreferences(MY_KEY_BTN_TEXT_COLOR) == null || edtButtonTextColor.text.toString() != Helpers.getValuePreferences(
                    MY_KEY_BTN_TEXT_COLOR
                )!!
            )
                Helpers.savePrefs(MY_KEY_BTN_TEXT_COLOR, edtButtonTextColor.text.toString())

            if (Helpers.getValuePreferences(MY_KEY_MAIN_TEXT_COLOR) == null || edtMainTextColor.text.toString() != Helpers.getValuePreferences(
                    MY_KEY_MAIN_TEXT_COLOR
                )!!
            )
                Helpers.savePrefs(MY_KEY_MAIN_TEXT_COLOR, edtMainTextColor.text.toString())

            if (Helpers.getValuePreferences(MY_KEY_BACKGROUND_COLOR) == null || edtBackgroundColor.text.toString() != Helpers.getValuePreferences(
                    MY_KEY_BACKGROUND_COLOR
                )!!
            ) Helpers.savePrefs(MY_KEY_BACKGROUND_COLOR, edtBackgroundColor.text.toString())
            Helpers.savePrefs(MY_KEY_ACCEPTED_DOCUMENT_1, cbAcceptedOldIdCard.isChecked)
            Helpers.savePrefs(MY_KEY_ACCEPTED_DOCUMENT_2, cbAcceptedOld12DigitsIdCard.isChecked)
            Helpers.savePrefs(MY_KEY_ACCEPTED_DOCUMENT_3, cbAcceptedEidWithoutChip.isChecked)
            Helpers.savePrefs(MY_KEY_ACCEPTED_DOCUMENT_4, cbAcceptedEidWithChip.isChecked)

            Helpers.savePrefs(MY_KEY_ACCEPTED_DOCUMENT_5, cbAcceptedEid2024.isChecked)
            Helpers.savePrefs(
                MY_KEY_FACE_MATCHING_THRESHOLD,
                sliderFaceMatchingThreshold.value.toInt().toString()
            )
            finish()
        }
    }

    override fun onBackPressed() {
        setConfigBeforeExit()
        super.onBackPressed()
    }

    private fun onInitValue() {
        val token = Helpers.getValuePreferences(MY_KEY_TOKEN) ?: ""
        val env = Helpers.getValuePreferences(MY_KEY_ENV) ?: defaultConfig.baseURL

        val scenario = Helpers.getValuePreferences(MY_KEY_SCENARIO) ?: KalapaFlowType.EKYC.name
        val lang = Helpers.getValuePreferences(MY_KEY_LANGUAGE)
        val livenessVersion = Helpers.getIntPreferences(MY_KEY_LIVENESS_VERSION, 0)

        var leftoverSession = Helpers.getValuePreferences(MY_KEY_LEFTOVER_SESSION) ?: ""
        var mrz = Helpers.getValuePreferences(MY_KEY_MRZ) ?: ""
        edtMRZ.setText(mrz)
        edtLeftoverSession.setText(leftoverSession)
        var upgradePlanFromSessionID = Helpers.getBooleanPreferences(MY_KEY_UPGRADE_PLAN_FROM_SESSION_ID, false)
        rgUpgradePlan.switchChangeListener(upgradePlanFromSessionID)
        val mainTextColor =
            Helpers.getValuePreferences(MY_KEY_MAIN_TEXT_COLOR) ?: defaultConfig.mainTextColor
        val mainColor = Helpers.getValuePreferences(MY_KEY_MAIN_COLOR) ?: defaultConfig.mainColor
        val backgroundColor =
            Helpers.getValuePreferences(MY_KEY_BACKGROUND_COLOR) ?: defaultConfig.backgroundColor
        val btnTextColor =
            Helpers.getValuePreferences(MY_KEY_BTN_TEXT_COLOR) ?: defaultConfig.btnTextColor
        refreshColor(mainColor, mainTextColor, btnTextColor, backgroundColor)
        val faceMatchingThreshold: Int =
            Helpers.getIntPreferences(MY_KEY_FACE_MATCHING_THRESHOLD, 50)

        val accept9DigitsIdCard = Helpers.getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_1, true)
        val accept12DigitIdCard = Helpers.getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_2, true)
        val acceptEidWithoutChip = Helpers.getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_3, true)
        val acceptEidWithChip = Helpers.getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_4, true)
        val acceptEid2024 = Helpers.getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_5, true)

        val enableNFC = Helpers.getBooleanPreferences(MY_KEY_ENABLE_NFC, nfcAvailable(this))
        val captureImage =
            Helpers.getBooleanPreferences(MY_KEY_CAPTURE_IMAGE, true)
        val verifyCheck =
            Helpers.getBooleanPreferences(MY_KEY_VERIFY_CHECK, false)
        val fraudCheck =
            Helpers.getBooleanPreferences(MY_KEY_FRAUD_CHECK, true)
        val normalCheckOnly = Helpers.getBooleanPreferences(
            MY_KEY_NORMAL_CHECK_ONLY,
            true
        )
        val cardSideMatchesCheck = Helpers.getBooleanPreferences(
            MY_KEY_CARD_SIDE_CHECK,
            true
        )
        sliderFaceMatchingThreshold.value = faceMatchingThreshold.toFloat()
        cbNFCScreen.isChecked = Helpers.getBooleanPreferences(MY_KEY_CUSTOM_NFC, true)
        cbLivenessScreen.isChecked = Helpers.getBooleanPreferences(MY_KEY_CUSTOM_LIVENESS, true)
        cbCaptureIdScreen.isChecked = Helpers.getBooleanPreferences(MY_KEY_CUSTOM_CAPTURE, true)

        cbAcceptedOldIdCard.isChecked = accept9DigitsIdCard
        cbAcceptedOld12DigitsIdCard.isChecked = accept12DigitIdCard
        cbAcceptedEidWithoutChip.isChecked = acceptEidWithoutChip
        cbAcceptedEidWithChip.isChecked = acceptEidWithChip
        cbAcceptedEid2024.isChecked = acceptEid2024
        edtButtonTextColor.setText(btnTextColor)
        edtMainTextColor.setText(mainTextColor)
        edtMainColor.setText(mainColor)
        edtBackgroundColor.setText(backgroundColor)
        edtToken.setText(token)
        LogUtils.printLog("lang $lang liveness $livenessVersion")

        rgLivenessVersion.switchChangeListener(if (livenessVersion == LIVENESS_VERSION.PASSIVE.version) 0 else if (livenessVersion == LIVENESS_VERSION.SEMI_ACTIVE.version) 1 else 2)
        rgScenario.switchChangeListener(if (scenario == SCENARIO.REGISTER.name) 0 else if (scenario == SCENARIO.UPGRADE.name) 1 else 2)
        rgLanguage.switchChangeListener(lang == "vi")
        rgEnvironment.switchChangeListener(env == KLP_PROD)

        rgVerifyCheck.switchChangeListener(verifyCheck)
        rgFraudCheck.switchChangeListener(fraudCheck)
        rgStrictQualityCheck.switchChangeListener(normalCheckOnly)
        rgCardSidesMatchCheck.switchChangeListener(cardSideMatchesCheck)
        rgCaptureImage.switchChangeListener(captureImage)
        rgScanNFC.switchChangeListener(enableNFC)
    }

    override fun onEditorAction(textView: TextView?, actionId: Int, keyEvent: KeyEvent?): Boolean {
        val result: Int = actionId and EditorInfo.IME_MASK_ACTION
        when (result) {
            EditorInfo.IME_ACTION_DONE -> {
                hideKeyboard()
            }

            EditorInfo.IME_ACTION_NEXT -> {
                hideKeyboard()
            }
        }
        return true
    }

    private fun hideKeyboard() {
        LogUtils.printLog("Hide Keyboard")
        val view = this.currentFocus
        if (view != null) {
            LogUtils.printLog("Begin Hide Keyboard")
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } else
            LogUtils.printLog("View is null. Can't Hide Keyboard")
    }

}

interface EditTextWatcherListener {
    fun completion(colorStr: String)
}

class EditTextWatcher(editText: EditText, onColorTriggered: EditTextWatcherListener) : TextWatcher {
    val edtText: EditText = editText
    val onColorTriggered = onColorTriggered
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//        LogUtils.printLog(" beforeTextChanged $p0 $p1 $p2 $p3")
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//        LogUtils.printLog("onTextChanged $p0 $p1 $p2 $p3")
    }

    @SuppressLint("SetTextI18n")
    override fun afterTextChanged(p0: Editable?) {
//        LogUtils.printLog("afterTextChanged ${p0.toString()}")
        val text: String = p0.toString()
        val length = text.length
        if (length > 0 && !text.contains("#")) {
            edtText.setText("#$text")
            edtText.setSelection(edtText.text.length)
        }
        if (length > 1) { // 0 is #
            if ((!Common.isCharacterHexa(text[length - 1]) || length > 7))
                p0?.delete(length - 1, length)
            if (length == 7) {
                onColorTriggered.completion(text)
            }
        }
    }
}