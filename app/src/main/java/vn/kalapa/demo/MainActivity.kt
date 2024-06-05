//package vn.kalapa.demo
//
//import android.annotation.SuppressLint
//import android.content.*
//import android.content.pm.PackageInfo
//import android.content.pm.PackageManager
//import android.content.res.ColorStateList
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AlertDialog
//import androidx.core.view.ViewCompat
//import androidx.lifecycle.LifecycleObserver
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.RequestBody
//import org.json.JSONObject
//import vn.kalapa.demo.activities.BaseActivity
//import vn.kalapa.demo.network.Client
//import vn.kalapa.demo.utils.Helpers
//import vn.kalapa.demo.utils.LogUtils
//import vn.kalapa.faceotp.*
//import vn.kalapa.faceotp.models.*
//import vn.kalapa.faceotp.utils.BitmapUtil.Companion.convertToByteArray
//import vn.kalapa.faceotp.utils.Common
//import vn.kalapa.faceotp.utils.LocaleHelper
//import java.util.*
//
//
//class MainActivity : BaseActivity() {
//    val TAG = "MainActivity"
//    private lateinit var ekycButton: Button
//    var tvWelcome: TextView? = null
//    var tvWelcomeSubtitle: TextView? = null
//    var container: View? = null
//    lateinit var tvVersion: TextView
//
//    private var preferencesConfig: PreferencesConfig? = null
//
//    var livenessVersion: Int = Common.LIVENESS_VERSION.PASSIVE.version
//    var scenario: String = FaceOTPFlowType.VERIFY.name
//    private lateinit var sdkConfig: KalapaSDKConfig
//
//    private val BASE_URL = "https://ekyc-api.kalapa.vn"
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        Helpers.init(this@MainActivity)
//        setupBinding()
//        getPreferencesValuesAndApply()
//        ekycButton.setOnClickListener {
//            if (this::sdkConfig.isInitialized)
//                startFaceOTP()
////                startNFC()
//            else
//                openSettingUI()
//        }
//    }
//
//
//    private fun startFaceOTP() {
//        KalapaSDK.startLivenessForResult(
//            this@MainActivity,
//            sdkConfig,
//            object : KalapaCaptureHandler() {
//                override fun process(
//                    bitmap: Bitmap,
//                    mediaType: KalapaSDKMediaType,
//                    callback: KalapaSDKCallback
//                ) {
//                    val SELFIE_PATH = "/bank-svc/ekyc/check/face"
//                    val requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), bitmap.convertToByteArray())
//                    Client(BASE_URL).postFormData(SELFIE_PATH, mapOf(), requestFile, "face_image", object : Client.RequestListener {
//                        override fun success(jsonObject: JSONObject) {
//                            // Your liveness / check selfie response here. Save it to use
////                            callback.sendError("Something")
//                            callback.sendDone {
//                                if (scenario == FaceOTPFlowType.VERIFY.name)
//                                    Helpers.showDialog(this@MainActivity, getString(R.string.klp_alert_title), getString(R.string.klp_faceOTP_register_successful), R.drawable.image_checkmark)
//                                else
////                                    Helpers.showDialog(this@MainActivity, getString(R.string.klp_title_congratulation), getString(R.string.klp_message_device_activated_successfully), R.drawable.image_checkmark)
//                                    startNFC()
//                            }
//                        }
//
//                        override fun fail(error: String) {
//                            callback.sendError(error)
//                        }
//
//                        override fun timeout() {
//                            callback.sendError(getString(R.string.klp_error_happen))
//                        }
//
//                    })
//                    LogUtils.printLog("callback.onDone startLivenessForResult")
//                }
//
//                override fun onError(resultCode: KalapaCaptureResultCode) {
//                    // Capture not finished return an occurred
//                    Helpers.showDialog(this@MainActivity, "onError", "startLivenessForResult ResultCode ${resultCode.name}", R.drawable.frowning_face)
//                }
//
//
//            })
//    }
//
//    private fun startNFC() {
//        KalapaSDK.startNFCForResult(this@MainActivity, sdkConfig, object : KalapaNFCHandler("") {
//            override fun process(nfcData: String, callback: KalapaSDKCallback) {
//                // MRZ done successfully. Return Raw NFC Data
//                // Do something with NFC Data
//                // Trigger if error
//                val NFC_PATH = "/bank-svc/ekyc/check/nfc"
////                LogUtils.printLog("nfcData: $nfcData")
////                callback.sendDone {
////                    startFaceOTP()
////                }
//                Client(BASE_URL).postXWWWFormData(NFC_PATH, mapOf(), mapOf("nfc_data" to nfcData), object : Client.RequestListener {
//                    override fun success(jsonObject: JSONObject) {
//                        // Your nfc response here. Save it for later
//                        callback.sendDone {
//                            Helpers.showDialog(this@MainActivity, getString(R.string.klp_alert_title), getString(R.string.klp_faceOTP_register_successful), R.drawable.image_checkmark)
//                        }
//                    }
//
//                    override fun fail(error: String) {
//                        callback.sendError(error)
//                    }
//
//                    override fun timeout() {
//                        callback.sendError(getString(R.string.klp_error_happen))
//                    }
//                })
//                LogUtils.printLog("callback.onDone startNFCForResult")
//
//            }
//
//            override fun onError(resultCode: KalapaNFCResultCode) {
//                // MRZ not finished return an occurred
//                Helpers.showDialog(this@MainActivity, "onError", "startNFCForResult ResultCode ${resultCode.name}", R.drawable.frowning_face)
//            }
//
//        })
//    }
//
//
//    private fun showAlert(
//        title: String,
//        message: String,
//        cancelable: Boolean,
//        onConfirmPressed: () -> Unit
//    ) {
//        val alertDialogBuilder = AlertDialog.Builder(this)
//        alertDialogBuilder.setTitle(title)
//            .setMessage(message)
//            .setCancelable(cancelable)
//            .setPositiveButton(resources.getString(R.string.klp_start)) { p0, p1 ->
//                run {
//                    onConfirmPressed()
//                }
//            }
//            .create()
//            .show()
//    }
//
//
//    private fun initConfig() {
//        if (preferencesConfig != null) {
//
//            LogUtils.printLog("Setting Config $preferencesConfig")
//            KalapaSDK.config = KalapaSDKConfig(
//                this@MainActivity,
//                preferencesConfig!!.backgroundColor,
//                preferencesConfig!!.mainColor,
//                preferencesConfig!!.mainTextColor,
//                preferencesConfig!!.btnTextColor,
//                preferencesConfig!!.livenessVersion,
//                preferencesConfig!!.language
//            )
//        } else {
//            LogUtils.printLog("Setting preferencesConfig is null")
//        }
//    }
//
//
//    private fun getPreferencesValuesAndApply() {
//        preferencesConfig = Helpers.getSharedPreferencesConfig(this@MainActivity)
//        if (preferencesConfig != null) {
//            val backgroundColor = preferencesConfig!!.backgroundColor
//            val txtColor = preferencesConfig!!.mainTextColor
//            val btnColor = preferencesConfig!!.mainColor
//            val btnTxtColor = preferencesConfig!!.btnTextColor
//            val lang = preferencesConfig!!.language
//            livenessVersion = preferencesConfig!!.livenessVersion
//            scenario = preferencesConfig!!.scenario
//            refreshText(lang)
//            refreshColor(backgroundColor, btnColor, txtColor, btnTxtColor, lang)
//        }
//
//    }
//
//    private fun refreshText(lang: String) {
//        val locale = Locale(if (lang == "vi") LocaleHelper.VIETNAMESE else if (lang == "ko") LocaleHelper.KOREAN else LocaleHelper.ENGLISH)
//        Locale.setDefault(locale)
//        val configuration = this.resources.configuration
//        configuration.setLocale(locale)
//        configuration.setLayoutDirection(locale)
//        this.resources.updateConfiguration(configuration, this.resources.displayMetrics)
//        if (preferencesConfig != null) {
//            if (preferencesConfig!!.language.contains("en")) {
//                ekycButton.text = "Start"
//                ekycButton.invalidate()
//                tvWelcome?.text = "Welcome to"
//                tvWelcome?.invalidate()
//                tvWelcomeSubtitle?.text = "Face OTP Demo"
//                tvWelcomeSubtitle?.invalidate()
//            } else {
//                ekycButton.text = "Bắt đầu"
//                ekycButton.invalidate()
//                tvWelcome?.text = "Mời trải nghiệm"
//                tvWelcome?.invalidate()
//                tvWelcomeSubtitle?.text = "Xác thực khuôn mặt"
//                tvWelcomeSubtitle?.invalidate()
//            }
//
//
//        } else {
//            ekycButton.text = resources.getString(R.string.klp_start)
//            tvWelcome?.text = resources.getString(R.string.klp_name)
//            tvWelcomeSubtitle?.text = resources.getString(R.string.klp_face_otp_demo)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        LogUtils.printLog("onResume")
//        getPreferencesValuesAndApply()
//    }
//
//
//    private fun refreshColor(
//        backgroundColor: String,
//        btnColor: String,
//        txtColor: String,
//        btnTxtColor: String,
//        lang: String
//    ) {
//        sdkConfig = KalapaSDKConfig(
//            this@MainActivity,
//            backgroundColor,
//            btnColor,
//            txtColor,
//            btnTxtColor,
//            livenessVersion,
//            lang
//        )
//        ekycButton.setTextColor(Color.parseColor(btnTxtColor))
//        ViewCompat.setBackgroundTintList(
//            ekycButton,
//            ColorStateList.valueOf(Color.parseColor(btnColor))
//        )
//    }
//
//    @SuppressLint("ResourceType")
//    fun setupBinding() {
//        ekycButton = findViewById(R.id.btn_eykc)
//        tvWelcome = findViewById(R.id.tv_welcome)
//        tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle)
//        tvVersion = findViewById(R.id.tv_version_code)
//
//        try {
//            val pInfo: PackageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
//            val version = pInfo.versionName
//            tvVersion.text = "version $version"
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//        }
//        container = findViewById(R.id.container)
//        findViewById<ImageView>(R.id.iv_setting).setOnClickListener {
//            openSettingUI()
//        }
//        ekycButton.text =  KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_start))
//    }
//
//}
