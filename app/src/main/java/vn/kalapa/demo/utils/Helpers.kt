package vn.kalapa.demo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.widget.CompoundButtonCompat
import com.google.android.material.slider.Slider
import vn.kalapa.demo.ExampleGlobalClass
import vn.kalapa.demo.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.models.PreferencesConfig
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ACCEPTED_DOCUMENT_1
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ACCEPTED_DOCUMENT_2
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ACCEPTED_DOCUMENT_3
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ACCEPTED_DOCUMENT_4
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ACCEPTED_DOCUMENT_5
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_CAPTURE_IMAGE
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_CARD_SIDE_CHECK
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_CUSTOM_CAPTURE
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_CUSTOM_LIVENESS
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_CUSTOM_NFC
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_ENABLE_NFC
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_FACE_MATCHING_THRESHOLD
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_FRAUD_CHECK
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_NORMAL_CHECK_ONLY
import vn.kalapa.ekyc.utils.Common.Companion.MY_KEY_VERIFY_CHECK
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

internal class Helpers {
    companion object {
        private const val TAG = "KLP"
        lateinit var activity: Activity
        private var prefs: SharedPreferences? = null
        private var backgroundColor: String? = null
        private var mainColor: String? = null
        private var mainTextColor: String? = null
        private var btnTextColor: String? = null

        fun setHelpersUI(backgroundColor: String? = null, mainColor: String? = null, mainTextColor: String? = null, btnTextColor: String? = null) {
            LogUtils.printLog("setHelpersUI $backgroundColor $mainColor $mainTextColor $btnTextColor")
            backgroundColor?.let { this.backgroundColor = backgroundColor }
            mainColor?.let { this.mainColor = mainColor }
            mainTextColor?.let { this.mainTextColor = mainTextColor }
            btnTextColor?.let { this.btnTextColor = btnTextColor }
        }

        fun getSharedPreferencesConfig(activity: Activity): PreferencesConfig? {
            init(activity)
            val token = getValuePreferences(Common.MY_KEY_TOKEN) ?: ""
            val lang = getValuePreferences(Common.MY_KEY_LANGUAGE)
            val livenessVersion = getIntPreferences(Common.MY_KEY_LIVENESS_VERSION, Common.LIVENESS_VERSION.PASSIVE.version)
            val backgroundColor = getValuePreferences(Common.MY_KEY_BACKGROUND_COLOR)
            this.backgroundColor = backgroundColor
            val mainColor = getValuePreferences(Common.MY_KEY_MAIN_COLOR)
            this.mainColor = mainColor
            val mainTextColor = getValuePreferences(Common.MY_KEY_MAIN_TEXT_COLOR)
            this.mainTextColor = mainTextColor
            val btnTextColor = getValuePreferences(Common.MY_KEY_BTN_TEXT_COLOR)
            this.btnTextColor = btnTextColor
            val env = getValuePreferences(Common.MY_KEY_ENV)
            val leftoverSession = getValuePreferences(Common.MY_KEY_LEFTOVER_SESSION) ?: ""
            val mrz = getValuePreferences(Common.MY_KEY_MRZ) ?: ""
            val faceMatchingThreshold: Int =
                getIntPreferences(MY_KEY_FACE_MATCHING_THRESHOLD, 50)
            val scenarioPlan = getBooleanPreferences(Common.MY_KEY_UPGRADE_PLAN_FROM_SESSION_ID, false)
            val scenario = getValuePreferences(Common.MY_KEY_SCENARIO) ?: ""
            val accept9DigitsIdCard = getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_1, true)
            val accept12DigitIdCard = getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_2, true)
            val acceptEidWithoutChip = getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_3, true)
            val acceptEidWithChip = getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_4, true)
            val acceptEid2024 = getBooleanPreferences(MY_KEY_ACCEPTED_DOCUMENT_5, true)

            val enableNFC = getBooleanPreferences(MY_KEY_ENABLE_NFC, true)
            val captureImage =
                getBooleanPreferences(MY_KEY_CAPTURE_IMAGE, true)
            val verifyCheck =
                getBooleanPreferences(MY_KEY_VERIFY_CHECK, false)
            val fraudCheck =
                getBooleanPreferences(MY_KEY_FRAUD_CHECK, true)
            val normalCheckOnly = getBooleanPreferences(MY_KEY_NORMAL_CHECK_ONLY, true)
            val hasCustomCaptureScreen = getBooleanPreferences(MY_KEY_CUSTOM_CAPTURE, true)
            val hasCustomLivenessScreen = getBooleanPreferences(MY_KEY_CUSTOM_LIVENESS, true)
            val hasCustomNFCScreen = getBooleanPreferences(MY_KEY_CUSTOM_NFC, true)
            val cardSideMatchesCheck = getBooleanPreferences(MY_KEY_CARD_SIDE_CHECK, true)
            LogUtils.printLog("Preferences: ", token.isEmpty(), lang == null, backgroundColor == null, mainColor == null, mainTextColor == null, btnTextColor == null, env == null)
            return if (lang == null || backgroundColor == null || mainColor == null || mainTextColor == null || btnTextColor == null || env == null) null
            else {
                PreferencesConfig(token, livenessVersion, backgroundColor, mainColor, mainTextColor, btnTextColor, lang, env, enableNFC, captureImage, verifyCheck, fraudCheck, normalCheckOnly, cardSideMatchesCheck, faceMatchingThreshold, accept9DigitsIdCard, accept12DigitIdCard, acceptEidWithoutChip, acceptEidWithChip, acceptEid2024, leftoverSession, mrz, scenario, scenarioPlan, hasCustomCaptureScreen, hasCustomLivenessScreen, hasCustomNFCScreen)
            }
        }

        fun isAllDigits(input: String): Boolean {
            return input.all { it.isDigit() }
        }

        fun formatMoney(number: Int): String {
            val formatter = DecimalFormat("#,###")
            return formatter.format(number)
        }

        fun formatDateToHHmmssddMMyyyy(date: Date): String {
            val format = SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
            return format.format(date)
        }


        fun getValuePreferences(key: String): String? {
            if (prefs == null) initPrefs()
            return prefs?.getString(key, null)
        }

        fun savePrefs(key: String, value: Any) {
//            LogUtils.printLog("Save Prefs String: $key $value")
            val editor = prefs?.edit()
            editor?.putString(key, value.toString())
            editor?.apply()
        }

        fun getBooleanPreferences(key: String, defaultValue: Boolean): Boolean {
            if (getValuePreferences(key) != null)
                return getValuePreferences(key) == "true"
            return defaultValue
        }

        fun getIntPreferences(key: String, defaultValue: Int): Int {
            val intPrefs = getValuePreferences(key)
            if (intPrefs != null)
                return Integer.parseInt(intPrefs.replace(".0", ""))
            return defaultValue
        }

        private fun initPrefs() {
            LogUtils.printLog("initPrefs")
            this.prefs = this.activity.getSharedPreferences(Common.MY_PREFERENCES, Context.MODE_PRIVATE)
            LogUtils.printLog("done initPrefs")
        }

        fun init(activity: Activity) {
            this.activity = activity
            initPrefs()
        }

        fun getPackageName(activity: Activity): String {

            val appInfoPackageName = this.activity.applicationInfo.packageName
            val actPackageName = this.activity.javaClass.`package`?.name ?: this.activity.applicationInfo.packageName
            if (appInfoPackageName == actPackageName) return appInfoPackageName
            var i = 0
            var count = 0
            while (i < appInfoPackageName.length && i < actPackageName.length) {
                if (appInfoPackageName[i] === actPackageName[i]) {
                    count++
                }
                i++
            }
            return appInfoPackageName.substring(0, count)
        }

        fun getString(id: Int): String {
            return activity.getString(id)
        }

        fun setBackgroundColorTintList(it: View, color: String) {
            ViewCompat.setBackgroundTintList(
                it, ColorStateList.valueOf(
                    Color.parseColor(
                        color
                    )
                )
            )
        }

        fun setColorTintList(it: ImageView, color: String) {
            var normalizedColor = color
            if (color.length > 7) {
                normalizedColor = color.replace("#FF", "#")
            }
            it.setColorFilter(Color.parseColor(normalizedColor))
        }


        fun setColorStroke(view: View, color: String) {
            val drawable = view.background as GradientDrawable
            drawable.mutate()
            drawable.setStroke(1, Color.parseColor(color))
        }


        @SuppressLint("UseCompatLoadingForDrawables")
        fun showDialog(myActivity: Activity, myTitle: String?, myBody: String, yesTxt: String?, noTxt: String?, drawableIcon: Int?, listener: DialogListener?) {
            activity = myActivity
            val dialog = Dialog(activity, R.style.full_screen_dialog)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)

            dialog.setContentView(R.layout.custom_layout)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val body = dialog.findViewById(R.id.custom_dialog_body) as TextView
            body.text = myBody

            val yesBtn = dialog.findViewById(R.id.custom_dialog_btn_yes) as Button
            val tvTitle = dialog.findViewById(R.id.custom_dialog_title) as TextView
//            if (!myTitle.isNullOrEmpty()) dialog.findViewById<TextView>(R.id.tv_alert_title).text =
            //myTitle
//                KalapaSDK.config.languageUtils.getLanguageString(activity.getString(R.string.klp_face_otp_alert_title))
            tvTitle.text = myTitle

            val ivIcon = dialog.findViewById<ImageView>(R.id.iv_dialog_icon)
            if (drawableIcon != null) ivIcon.setImageDrawable(activity.getDrawable(drawableIcon))
            yesBtn.text = yesTxt ?: myActivity.getString(R.string.klp_demo_confirm)
            val noBtn = dialog.findViewById(R.id.custom_dialog_btn_no) as TextView
            noBtn.text = noTxt ?: myActivity.getString(R.string.klp_demo_no)
            if (ExampleGlobalClass.isPreferencesConfigInitialized()) {
                tvTitle.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.mainTextColor))
                noBtn.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.mainTextColor))
                body.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.mainTextColor))
                setBackgroundColorTintList(yesBtn, ExampleGlobalClass.preferencesConfig.mainColor)
                yesBtn.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.btnTextColor))
            } else {
                mainColor?.let {
                    setBackgroundColorTintList(yesBtn, it)
                }
                mainTextColor?.let {
                    tvTitle.setTextColor(Color.parseColor(it))
                    noBtn.setTextColor(Color.parseColor(it))
                    body.setTextColor(Color.parseColor(it))
                }
                btnTextColor?.let {
                    yesBtn.setTextColor(Color.parseColor(it))
                }
            }
            if (listener != null) {
                noBtn.setOnClickListener {
                    dialog.dismiss()
                    listener.onNo()
                }
                yesBtn.setOnClickListener {
                    dialog.dismiss()
                    listener.onYes()
                }
            } else {
                noBtn.visibility = View.GONE
                yesBtn.setOnClickListener {
                    dialog.dismiss()
                }
            }
            dialog.show()
        }

        fun showDialog(myActivity: Activity, myTitle: String?, myBody: String, drawableIcon: Int?, listener: DialogListener?, yesTxt: String? = null) {
            showDialog(myActivity, myTitle, myBody, yesTxt, null, drawableIcon, listener)
        }

        fun showDialog(myActivity: Activity, myBody: String, drawableIcon: Int?) {
            activity = myActivity
            showDialog(activity, null, myBody, drawableIcon, null)
        }

        fun showDialog(myActivity: Activity, myTitle: String, myBody: String, drawableIcon: Int?) {
            activity = myActivity
            showDialog(activity, myTitle, myBody, drawableIcon, null)
        }

        fun showDialog(myActivity: Activity, myTitle: String, myBody: String, yesTxt: String, drawableIcon: Int?) {
            activity = myActivity
            showDialog(activity, myTitle, myBody, drawableIcon, null, yesTxt)
        }

        fun errorLog(
            vararg errors: Any
        ) {
            Log.e(
                TAG, errors.joinToString(
                    " "
                )
            )
        }

        fun startShakingAnimation(context: Context, view: View) {
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shaking))
        }

        fun hideKeyboard(context: Context, view: View) {
            // Only runs if there is a view that is currently focused
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun setTextBoldWithColor(tv: TextView, color: String? = null) {
            val spanString = SpannableString(tv.text)
            spanString.setSpan(StyleSpan(Typeface.BOLD), 0, spanString.length, 0)
            tv.text = spanString
            if (color != null) tv.setTextColor(Color.parseColor(color))
        }

        fun setRadioButtonTintList(radio: RadioButton, checkedColor: Int) {
            val colorStateList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_enabled)), intArrayOf(
                    Color.BLACK,  // disabled
                    checkedColor // enabled
                )
            )
            radio.buttonTintList = colorStateList // set the color tint list
            radio.invalidate() // Could not be necessary
        }

        fun setCheckboxTintList(cb: CheckBox, checkedColor: Int) {
            val colorStateList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_empty), intArrayOf(android.R.attr.state_checked)), intArrayOf(
                    checkedColor,  // disabled
                    checkedColor // enabled
                )
            )
            CompoundButtonCompat.setButtonTintList(cb, colorStateList)
        }

        fun setSliderTintList(sliderFaceMatchingThreshold: Slider, checkedColor: Int) {
            val colorStateList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_empty), intArrayOf(android.R.attr.state_checked)), intArrayOf(
                    checkedColor,  // disabled
                    checkedColor // enabled
                )
            )
            sliderFaceMatchingThreshold.thumbStrokeColor = colorStateList
            sliderFaceMatchingThreshold.trackTintList = colorStateList
            sliderFaceMatchingThreshold.thumbTintList = colorStateList
        }
    }
}