package vn.kalapa.demo.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import vn.kalapa.demo.R
import vn.kalapa.demo.utils.Helpers
import vn.kalapa.demo.utils.LogUtils
import vn.kalapa.ekyc.FaceOTPFlowType
import vn.kalapa.ekyc.KalapaSDKConfig
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_BACKGROUND_COLOR
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_BTN_TEXT_COLOR
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ENV
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_LANGUAGE
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_LIVENESS_VERSION
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_MAIN_COLOR
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_MAIN_TEXT_COLOR
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_SCENARIO
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_TOKEN
import vn.kalapa.ekyc.utils.LocaleHelper
import vn.kalapa.ekyc.views.KLPCustomMultipleChoices
import vn.kalapa.ekyc.views.KLPCustomSwitch

// prod: api-ekyc.kalapa.vn/face-otp
// dev: faceotp-dev.kalapa.vn/api
class SettingActivity : AppCompatActivity(), TextView.OnEditorActionListener {
//    private val KLP_PROD = "https://api-ekyc.kalapa.vn/face-otp"
//    private val KLP_DEV = "https://faceotp-dev.kalapa.vn/api"
    private val KLP_PROD = "https://ekyc-api.kalapa.vn"
    private val KLP_DEV = "https://ekyc-dev-internal.kalapa.vn"
    private val defaultConfig = KalapaSDKConfig(this@SettingActivity, language = "en")
    private lateinit var rgLanguage: KLPCustomMultipleChoices

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

    private fun findViewById() {
        rootContainer = findViewById(R.id.root_container)
        container = findViewById(R.id.ln_container)
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
//        tvScenario = findViewById(R.id.tv_scenario)
        rgEnvironment = findViewById(R.id.sw_enviroment)
//        rgScenario = findViewById(R.id.sw_scenario)

        btnSaveConfig = findViewById(R.id.btn_next)

        tvLanguage = findViewById(R.id.tv_language)
        tvLivenessVersion = findViewById(R.id.tv_liveness_version)

        btnSaveConfig.setOnClickListener {
            setConfigBeforeExit()
        }
        rgLanguage.listener = KLPCustomMultipleChoices.KLPCustomMultipleChoicesChangeListener {
            LogUtils.printLog("Current language is $it")
            LocaleHelper.setLocale(this@SettingActivity, if (it == 0) "vi" else if (it == 1) "en" else "ko")
            refreshUI()
        }
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

        edtMainColor.addTextChangedListener(EditTextWatcher(edtMainColor, object : EditTextWatcherListener {
            override fun completion(colorStr: String) {
                refreshMainColor(colorStr)
            }
        }))
        edtMainTextColor.addTextChangedListener(EditTextWatcher(edtMainTextColor, object : EditTextWatcherListener {
            override fun completion(colorStr: String) {
                refreshMainTextColor(colorStr)
            }
        }))
        edtBackgroundColor.addTextChangedListener(EditTextWatcher(edtBackgroundColor, object : EditTextWatcherListener {
            override fun completion(colorStr: String) {
                refreshBackgroundColor(colorStr)
            }
        }))
        edtButtonTextColor.addTextChangedListener(EditTextWatcher(edtButtonTextColor, object : EditTextWatcherListener {
            override fun completion(colorStr: String) {
                refreshBtnTextColor(colorStr)
            }
        }))
    }


    private fun refreshColor(strMainColor: String, strMainTextColor: String, strButtonTextColor: String, strBackgroundColor: String) {
        refreshMainColor(strMainColor)
        refreshBtnTextColor(strButtonTextColor)
        refreshBackgroundColor(strBackgroundColor)
        refreshMainTextColor(strMainTextColor)
    }

    private fun refreshMainColor(mainColor: String) {
        Helpers.setBackgroundColorTintList(btnMainColor, mainColor)
        Helpers.setBackgroundColorTintList(btnButtonTextColor, mainColor)
        Helpers.setBackgroundColorTintList(btnSaveConfig, mainColor)
        rgLanguage.setMainColor(mainColor)
        rgLivenessVersion.setMainColor(mainColor)
//        rgScenario.setMainColor(mainColor)
        rgEnvironment.setMainColor(mainColor)
    }

    private fun refreshBtnTextColor(txtColor: String) {
        btnButtonTextColor.text = txtColor
        btnButtonTextColor.setTextColor(Color.parseColor(txtColor))
        btnSaveConfig.setTextColor(Color.parseColor(txtColor))
        rgLanguage.setTextColor(txtColor)
        rgLivenessVersion.setTextColor(txtColor)
//        rgScenario.setTextColor(txtColor)
        rgEnvironment.setTextColor(txtColor)
    }

    private fun refreshBackgroundColor(backgroundColor: String) {
        Helpers.setBackgroundColorTintList(btnBackgroundColor, backgroundColor)
        Helpers.setBackgroundColorTintList(btnMainTextColor, backgroundColor)
    }

    private fun refreshMainTextColor(txtColor: String) {
        btnMainTextColor.text = txtColor
        btnMainTextColor.setTextColor(Color.parseColor(txtColor))
    }

    private fun refreshUI() {
//        tvScenario.text = resources.getString(R.string.klp_index_scenario)
        tvLanguage.text = resources.getString(R.string.klp_index_language)
        rgLanguage.rbOne.text = resources.getString(R.string.klp_faceOTP_language_vi)
        rgLanguage.rbSecond.text = resources.getString(R.string.klp_faceOTP_language_en)
        rgLanguage.rbThird.text = resources.getString(R.string.klp_faceOTP_language_ko)

        rgEnvironment.rbOne.text = resources.getString(R.string.klp_environment_production)
        rgEnvironment.rbOther.text = resources.getString(
            R.string.klp_environment_dev
        )
        tvLivenessVersion.text = resources.getString(R.string.klp_index_faceOTP_liveness)
        rgLivenessVersion.rbOne.text = resources.getString(R.string.klp_liveness_passive)
        rgLivenessVersion.rbSecond.text = resources.getString(R.string.klp_liveness_semi_activate)
        rgLivenessVersion.rbThird.text = resources.getString(R.string.klp_liveness_activate)

//        rgScenario.rbOne.text = "nfc_ekyc"
//        rgScenario.rbSecond.text = "verify"
//        rgScenario.rbThird.text = "passport"

        btnSaveConfig.text = resources.getString(R.string.klp_save_setting_title)
        tvMainColor.text = resources.getString(R.string.klp_index_main_color)
        tvMainTextColor.text = resources.getString(R.string.klp_index_main_text_color)
        tvButtonTextColor.text = resources.getString(R.string.klp_index_button_text_color)
        tvBackgroundColor.text = resources.getString(R.string.klp_index_background_color)
    }

    private fun setConfigBeforeExit() {
        if (edtToken.text.toString().isNotEmpty()) {
            Helpers.savePrefs(
                MY_KEY_LIVENESS_VERSION, if (rgLivenessVersion.selectedIndex == 0) Common.LIVENESS_VERSION.PASSIVE.version
                else if (rgLivenessVersion.selectedIndex == 1) Common.LIVENESS_VERSION.SEMI_ACTIVE.version else Common.LIVENESS_VERSION.ACTIVE.version
            )
            Helpers.savePrefs(MY_KEY_LANGUAGE, if (rgLanguage.selectedIndex == 0) "vi" else if (rgLanguage.selectedIndex == 1) "en" else "ko")
//        Helpers.savePrefs(MY_KEY_LANGUAGE, if (rgLanguage.isPostitiveCheck) "vi" else "en")
            Helpers.savePrefs(MY_KEY_TOKEN, edtToken.text.toString())

//            Helpers.savePrefs(
//                MY_KEY_SCENARIO, if (rgScenario.selectedIndex == 0) FaceOTPFlowType.ONBOARD.name
//                else if (rgScenario.selectedIndex == 1) FaceOTPFlowType.VERIFY.name else FaceOTPFlowType.PASSPORT.name
//            )

            Helpers.savePrefs(MY_KEY_ENV, if (rgEnvironment.isPostitiveCheck) KLP_PROD else KLP_DEV)

            if (Helpers.getValuePreferences(MY_KEY_MAIN_COLOR) == null || edtMainColor.text.toString() != Helpers.getValuePreferences(MY_KEY_MAIN_COLOR)!!)
                Helpers.savePrefs(MY_KEY_MAIN_COLOR, edtMainColor.text.toString())

            if (Helpers.getValuePreferences(MY_KEY_BTN_TEXT_COLOR) == null || edtButtonTextColor.text.toString() != Helpers.getValuePreferences(MY_KEY_BTN_TEXT_COLOR)!!)
                Helpers.savePrefs(MY_KEY_BTN_TEXT_COLOR, edtButtonTextColor.text.toString())

            if (Helpers.getValuePreferences(MY_KEY_MAIN_TEXT_COLOR) == null || edtMainTextColor.text.toString() != Helpers.getValuePreferences(MY_KEY_MAIN_TEXT_COLOR)!!)
                Helpers.savePrefs(MY_KEY_MAIN_TEXT_COLOR, edtMainTextColor.text.toString())

            if (Helpers.getValuePreferences(MY_KEY_BACKGROUND_COLOR) == null || edtBackgroundColor.text.toString() != Helpers.getValuePreferences(MY_KEY_BACKGROUND_COLOR)!!)
                Helpers.savePrefs(MY_KEY_BACKGROUND_COLOR, edtBackgroundColor.text.toString())

            finish()
        } else {
            Helpers.showDialog(this@SettingActivity, resources.getString(R.string.klp_faceOTP_alert_title), "Token " + resources.getString(R.string.klp_faceotp_can_not_leave_empty), R.drawable.frowning_face)
        }
    }

    override fun onBackPressed() {
        setConfigBeforeExit()
    }

    private fun onInitValue() {
        val token = Helpers.getValuePreferences(MY_KEY_TOKEN) ?: ""
        val env = Helpers.getValuePreferences(MY_KEY_ENV) ?: KLP_DEV
        val secnario = Helpers.getValuePreferences(MY_KEY_SCENARIO) ?: FaceOTPFlowType.ONBOARD.name
        val lang = Helpers.getValuePreferences(MY_KEY_LANGUAGE)
        val livenessVersion = Helpers.getIntPreferences(MY_KEY_LIVENESS_VERSION, 2)

        val mainTextColor = Helpers.getValuePreferences(MY_KEY_MAIN_TEXT_COLOR) ?: defaultConfig.mainTextColor
        val mainColor = Helpers.getValuePreferences(MY_KEY_MAIN_COLOR) ?: defaultConfig.mainColor
        val backgroundColor = Helpers.getValuePreferences(MY_KEY_BACKGROUND_COLOR) ?: defaultConfig.backgroundColor
        val btnTextColor = Helpers.getValuePreferences(MY_KEY_BTN_TEXT_COLOR) ?: defaultConfig.btnTextColor
        refreshColor(mainColor, mainTextColor, btnTextColor, backgroundColor)

        edtButtonTextColor.setText(btnTextColor)
        edtMainTextColor.setText(mainTextColor)
        edtMainColor.setText(mainColor)
        edtBackgroundColor.setText(backgroundColor)
        edtToken.setText(token)
        LogUtils.printLog("lang $lang liveness $livenessVersion")

//        rgScenario.switchChangeListener(secnario == FaceOTPFlowType.ONBOARD.name)
        rgLivenessVersion.switchChangeListener(if (livenessVersion == Common.LIVENESS_VERSION.PASSIVE.version) 0 else if (livenessVersion == Common.LIVENESS_VERSION.SEMI_ACTIVE.version) 1 else 2)
//        rgScenario.switchChangeListener(if (secnario == FaceOTPFlowType.ONBOARD.name) 0 else if (secnario == FaceOTPFlowType.VERIFY.name) 1 else 2)

        rgLanguage.switchChangeListener(if (lang == "vi") 0 else if (lang == "en") 1 else 2)
        rgEnvironment.switchChangeListener(env == KLP_PROD)
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
        LogUtils.printLog(" beforeTextChanged $p0 $p1 $p2 $p3")
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        LogUtils.printLog("onTextChanged $p0 $p1 $p2 $p3")
    }

    @SuppressLint("SetTextI18n")
    override fun afterTextChanged(p0: Editable?) {
        LogUtils.printLog("afterTextChanged ${p0.toString()}")
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