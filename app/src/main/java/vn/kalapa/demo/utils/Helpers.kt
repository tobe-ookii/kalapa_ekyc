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
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import vn.kalapa.demo.ExampleGlobalClass
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.R
import vn.kalapa.ekyc.models.KalapaOTP
import vn.kalapa.ekyc.models.PreferencesConfig
import vn.kalapa.ekyc.utils.Common
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

internal class Helpers {
    companion object {
        private const val TAG = "KLP"
        lateinit var activity: Activity
        private var prefs: SharedPreferences? = null

        fun getSharedPreferencesConfig(activity: Activity): PreferencesConfig? {
            init(activity)
            val token = getValuePreferences(Common.MY_KEY_TOKEN) ?: ""
            val lang = getValuePreferences(Common.MY_KEY_LANGUAGE)
            val livenessVersion = getIntPreferences(Common.MY_KEY_LIVENESS_VERSION, Common.LIVENESS_VERSION.PASSIVE.version)
            val backgroundColor = getValuePreferences(Common.MY_KEY_BACKGROUND_COLOR)
            val scenario = getValuePreferences(Common.MY_KEY_SCENARIO)
            val mainColor = getValuePreferences(Common.MY_KEY_MAIN_COLOR)
            val mainTextColor = getValuePreferences(Common.MY_KEY_MAIN_TEXT_COLOR)
            val btnTextColor = getValuePreferences(Common.MY_KEY_BTN_TEXT_COLOR)
            val env = getValuePreferences(Common.MY_KEY_ENV)
            return if (lang == null || scenario == null || backgroundColor == null || mainColor == null || mainTextColor == null || btnTextColor == null || env == null) null
            else {
                PreferencesConfig(token, livenessVersion, backgroundColor, mainColor, mainTextColor, btnTextColor, lang, env, true, true, true, true, true, true, 50, true, true, true, true)
            }
        }

        fun isAllDigits(input: String): Boolean {
            return input.all { it.isDigit() }
        }

        fun getRandomOTP(): KalapaOTP {
            // It will generate 6 digit random Number.
            // from 0 to 999999
            val rnd = Random()
            val number = rnd.nextInt(999999)

            // this will convert any number sequence into 6 character.
            return KalapaOTP(String.format("%06d", number), 30)
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
            LogUtils.printLog("Save Prefs String: $key $value")
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
            this.prefs = this.activity.getSharedPreferences(Common.MY_PREFERENCES, Context.MODE_PRIVATE)
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
//            dialog.findViewById<TextView>(R.id.tv_alert_title).text = myTitle
//                KalapaSDK.config.languageUtils.getLanguageString(activity.getString(R.string.klp_face_otp_alert_title))
            tvTitle.text = myTitle

            val ivIcon = dialog.findViewById<ImageView>(R.id.iv_dialog_icon)
            if (drawableIcon != null) ivIcon.setImageDrawable(activity.getDrawable(drawableIcon))
            if (yesTxt != null) yesBtn.text = yesTxt
            val noBtn = dialog.findViewById(R.id.custom_dialog_btn_no) as TextView
            if (noTxt != null) noBtn.text = noTxt
            if (ExampleGlobalClass.isFaceImageInitialized()) {
                tvTitle.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.mainTextColor))
                noBtn.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.mainTextColor))
                body.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.mainTextColor))
                setBackgroundColorTintList(yesBtn, ExampleGlobalClass.preferencesConfig.mainColor)
                yesBtn.setTextColor(Color.parseColor(ExampleGlobalClass.preferencesConfig.btnTextColor))
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

        fun showDialog(myActivity: Activity, myTitle: String?, myBody: String, drawableIcon: Int?, listener: DialogListener?) {
            showDialog(myActivity, myTitle, myBody, null, null, drawableIcon, listener)
        }

        fun showDialog(myActivity: Activity, myBody: String, drawableIcon: Int?) {
            activity = myActivity
            showDialog(activity, null, myBody, drawableIcon, null)
        }

        fun showDialog(myActivity: Activity, myTitle: String, myBody: String, drawableIcon: Int?) {
            activity = myActivity
            showDialog(activity, myTitle, myBody, drawableIcon, null)
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
    }
}