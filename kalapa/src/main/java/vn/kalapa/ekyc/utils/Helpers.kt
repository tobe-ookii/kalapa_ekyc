package vn.kalapa.ekyc.utils


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.Directory.PACKAGE_NAME
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
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.managers.KLPLanguageManager
import kotlin.reflect.KClass


internal class Helpers {

    companion object {
        private val MY_PREFERENCES = PACKAGE_NAME
        lateinit var location: Location
        private var isDebug = true
        private const val TAG = "KLP"
        var count = 0
        lateinit var advId: String
        lateinit var AppVersion: String
        private lateinit var dialog: Dialog
        private var prefs: SharedPreferences? = null

        private fun getValuePreferences(activity: Activity, key: String): String? {
            if (prefs == null) initPrefs(activity)
            return prefs?.getString(key, null)
        }

        private fun initPrefs(activity: Activity) {
            this.prefs = activity.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE)
        }

        fun getBooleanPreferences(activity: Activity, key: String, defaultValue: Boolean): Boolean {
            if (getValuePreferences(activity, key) != null)
                return getValuePreferences(activity, key) == "true"
            return defaultValue
        }

        fun getIntPreferences(activity: Activity, key: String, defaultValue: Int): Int {
            val intPrefs = getValuePreferences(activity, key)
            if (intPrefs != null)
                return Integer.parseInt(intPrefs.replace(".0", ""))
            return defaultValue
        }

        fun getStringPreferences(activity: Activity, key: String): String {
            return getValuePreferences(activity, key) ?: key
        }

        fun getPackageName(
            activity: Activity
        ): String {

            val appInfoPackageName = activity.applicationInfo.packageName
            val actPackageName =
                activity.javaClass.`package`?.name ?: activity.applicationInfo.packageName

            if (appInfoPackageName == actPackageName) return appInfoPackageName

            var i = 0
            var count = 0
            while (i < appInfoPackageName.length && i < actPackageName.length) {
                if (appInfoPackageName[i] === actPackageName[i]) {
                    count++
                }
                i++
            }
            return appInfoPackageName.substring(
                0, count
            )
        }


        fun loadJsonString(key: String): String {
            return prefs?.getString(key, "{}") as String
        }

        fun savePrefs(key: String, value: Any) {
            printLog("Save Prefs String: $key $value")
            val editor = prefs?.edit()
            editor?.putString(key, value.toString())
            editor?.apply()
        }

        fun setBackgroundBitmap(view: View) {
            var sdk = Build.VERSION.SDK_INT
            val imageBytes = BitmapUtil.convert(KalapaSDK.config.backgroundColor)
            val background = BitmapDrawable(imageBytes)
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackgroundDrawable(background)
            } else {
                view.background = background
            }
        }

        fun clean(key: String) {
            val editor = prefs?.edit()
            editor?.remove(key)
            editor?.apply()
        }

        fun getDeviceModel(): String {
            return Build.MODEL
        }

        fun setBackgroundColorTintList(it: View, color: String) {
            ViewCompat.setBackgroundTintList(it, ColorStateList.valueOf(Color.parseColor(color)))
        }


        fun setBackgroundColorTintList(it: View, color: Int) {
            ViewCompat.setBackgroundTintList(it, ColorStateList.valueOf(color))
        }

        fun setColorTintList(it: ImageView, color: String) {
            var normalizedColor = color
            if (color.length > 7) {
                normalizedColor = color.replace("#FF", "#")
            }
//            printLog("Tint Color: $normalizedColor")
            it.setColorFilter(Color.parseColor(normalizedColor))
        }


        fun setColorStroke(view: View, color: String) {
            val drawable = view.background as GradientDrawable
            drawable.mutate()
            drawable.setStroke(1, Color.parseColor(color))
        }


        fun showEndKYC(activity: Activity, dialogListener: DialogListener) {
            showDialog(
                activity,
                KLPLanguageManager.get(activity.getString(R.string.klp_exit_confirm_title)),
                KLPLanguageManager.get(activity.getString(R.string.klp_exit_confirm_message)),
                KLPLanguageManager.get(activity.getString(R.string.klp_button_confirm)),
                KLPLanguageManager.get(activity.getString(R.string.klp_button_cancel)),
                R.drawable.ic_sad_face,
                object : DialogListener {
                    override fun onYes() {
                        dialogListener.onYes()
                    }

                    override fun onNo() {
                        dialogListener.onNo()
                    }
                })
        }

        private fun dismissDialogIfNeeded() {
            if (this::dialog.isInitialized && this.dialog.isShowing) {
                dialog.dismiss()
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun showDialog(
            activity: Activity,
            myTitle: String?,
            myBody: String,
            yesTxt: String?,
            noTxt: String?,
            drawableIcon: Int?,
            listener: DialogListener?,
            alertListener: (() -> Unit)? = null,
        ) {
            dismissDialogIfNeeded()
            dialog = Dialog(activity, R.style.full_screen_dialog)
//            dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(alertListener == null)
            dialog.setContentView(R.layout.custom_layout)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val body: TextView = dialog.findViewById(R.id.custom_dialog_body)
            body.text = myBody
            body.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            val yesBtn: Button = dialog.findViewById(R.id.custom_dialog_btn_yes)
            val tvTitle: TextView = dialog.findViewById(R.id.custom_dialog_title)
            dialog.findViewById<TextView>(R.id.tv_alert_title).text = KLPLanguageManager.get(activity.getString(R.string.klp_error_occurred_title))

            tvTitle.text = myTitle
            ViewCompat.setBackgroundTintList(
                yesBtn,
                ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
            )
            tvTitle.setTextColor(Color.parseColor(KalapaSDK.config.mainTextColor))
            setBackgroundColorTintList(dialog.findViewById(R.id.container_content), KalapaSDK.config.backgroundColor)

            val ivIcon = dialog.findViewById<ImageView>(R.id.iv_dialog_icon)
            if (drawableIcon != null) ivIcon.setImageDrawable(activity.getDrawable(drawableIcon))
            yesBtn.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
            if (yesTxt != null) yesBtn.text = yesTxt
            val noBtn: TextView = dialog.findViewById(R.id.custom_dialog_btn_no)
            if (noTxt != null) noBtn.text = noTxt

            if (listener != null) {
                noBtn.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
                noBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
                noBtn.setOnClickListener {
                    dialog.dismiss()
                    listener.onNo()
                }

                yesBtn.setOnClickListener {
                    dialog.dismiss()
                    listener.onYes()
                }
            } else {
                yesBtn.text = KLPLanguageManager.get(activity.getString(R.string.klp_button_confirm))
                noBtn.visibility = View.GONE
                yesBtn.setOnClickListener {
                    dialog.dismiss()
                    if (alertListener != null) alertListener()
                }
            }
            dialog.show()
        }

        fun showDialog(
            myActivity: Activity,
            myTitle: String?,
            myBody: String,
            drawableIcon: Int?,
            listener: DialogListener?
        ) {
            showDialog(myActivity, myTitle, myBody, null, null, drawableIcon, listener)
        }

        fun showDialog(myActivity: Activity, myTitle: String, myBody: String, drawableIcon: Int?, alertListener: (() -> Unit)? = null) {
            showDialog(
                myActivity, myTitle, myBody,
                KLPLanguageManager.get(myActivity.getString(R.string.klp_button_confirm)),
                KLPLanguageManager.get(myActivity.getString(R.string.klp_button_cancel)),
                drawableIcon,
                null,
                alertListener
            )
        }

        fun printLog(vararg messages: Any) {
            if (isDebug) {
                Log.d(
                    TAG, messages.joinToString(
                        " "
                    )
                )
            }
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
//            tv.setTypeface(null, Typeface.BOLD)
            val spanString = SpannableString(
                tv.text
            )
            spanString.setSpan(
                StyleSpan(
                    Typeface.BOLD
                ), 0, spanString.length, 0
            )
            tv.text = spanString
            if (color != null) tv.setTextColor(
                Color.parseColor(
                    color
                )
            )
        }

        fun setTextRegularWithColor(tv: TextView, color: String? = null) {
//            tv.setTypeface(null, Typeface.NORMAL)
            val spanString = SpannableString(
                tv.text
            )
            spanString.setSpan(
                StyleSpan(
                    Typeface.NORMAL
                ), 0, spanString.length, 0
            )
            tv.text = spanString
            if (color != null) tv.setTextColor(
                Color.parseColor(
                    color
                )
            )
        }

        fun setRadioButtonTintList(radio: RadioButton, checkedColor: Int) {
            val colorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf(android.R.attr.state_enabled)
                ), intArrayOf(
                    Color.BLACK,  // disabled
                    checkedColor // enabled
                )
            )
            radio.buttonTintList = colorStateList // set the color tint list
            radio.invalidate() // Could not be necessary
        }

        fun fadeIn(context: Context, view: View, completion: (() -> Unit)? = null) {
            val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            // Apply the animation to a view (e.g., an ImageView)
            view.startAnimation(fadeIn) // Replace with your view ID
            Handler(Looper.getMainLooper()).postDelayed({
                if (completion != null) {
                    completion()
                }
            }, 1000)
        }

        fun transitionFromColorToColor(myView: View, fromColor: String, toColor: String) {
            // Define the start and end colors
            val colorFrom = Color.parseColor(fromColor) // Replace with color X (Red)
            val colorTo = Color.parseColor(toColor) // Replace with color Y (Blue)

            // Create a ValueAnimator that will animate between the two colors
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation.duration = 1000 // Duration in milliseconds (1 second)

            // Apply the animated color to the background of a view
            colorAnimation.addUpdateListener { animator ->
//                myView.setBackgroundColor(animator.animatedValue as Int) // Replace with your view ID
                setBackgroundColorTintList(myView, animator.animatedValue as Int)
            }

            // Start the animation
            colorAnimation.start()
        }

        fun fadeOut(context: Context, view: View, completion: (() -> Unit)? = null) {
            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            // Apply the animation to a view (e.g., an ImageView)
            view.startAnimation(fadeOut) // Replace with your view ID
            Handler(Looper.getMainLooper()).postDelayed({
                if (completion != null) {
                    completion()
                }
            }, 1000)
        }
    }
}