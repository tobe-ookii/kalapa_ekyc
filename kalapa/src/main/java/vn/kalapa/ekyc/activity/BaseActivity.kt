package vn.kalapa.ekyc.activity

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import vn.kalapa.R
import vn.kalapa.ekyc.DialogListener
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.KalapaSDKCallback
import vn.kalapa.ekyc.managers.KLPLanguageManager
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.views.ProgressView

abstract class BaseActivity : AppCompatActivity(), KalapaSDKCallback {
    companion object {
        init {
            System.loadLibrary("envi")
        }
    }

    override fun sendExpired() {
        ProgressView.hideProgress()
        Helpers.showDialog(this@BaseActivity,
            KLPLanguageManager.get(resources.getString(R.string.klp_error_unknown)),
            KLPLanguageManager.get(resources.getString(R.string.klp_error_timeout)),
            KLPLanguageManager.get(resources.getString(R.string.klp_button_confirm)),
            KLPLanguageManager.get(resources.getString(R.string.klp_button_cancel)), null, object : DialogListener {
                override fun onYes() {
                    KalapaSDK.handler.onExpired()
                    finish()
                }

                override fun onNo() {
                    finish()
                }

            })

    }

    abstract fun onEmulatorDetected()
    private fun onVirtualCameraDetected() {
        Helpers.printLog("onVirtualCameraDetected virtual camera")
    }

    private var enterScreen = false
    override fun onResume() {
        if (isRunningOnEmulator() == true) {
            Helpers.printLog("isRunningOnEmulator Emulator detected!")
            if (KalapaSDK.isConfigInitialized() && KalapaSDK.config.baseURL.contains("dev") && KalapaSDK.config.baseURL.contains("kalapa")) {
                Helpers.printLog("isRunningOnEmulator still accept when dev")
            } else
                onEmulatorDetected()
        }
        if (isPathReallyExist(filesDir.absolutePath) < 0) {
            Helpers.printLog("onVirtualCameraDetected fake camera")
            onEmulatorDetected()
        } else Helpers.printLog("onVirtualCameraDetected real camera")
        if (!enterScreen) {
            enterScreen = true
        }
        super.onResume()
    }

    override fun attachBaseContext(newBase: Context?) {
        val configuration = newBase!!.resources.configuration
        configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
        applyOverrideConfiguration(configuration)
        super.attachBaseContext(newBase)
    }


    private var isRunningOnEmulator: Boolean? = null

    private fun isRunningOnEmulator(): Boolean? {
        isRunningOnEmulator?.let {
            return it
        }
        // Android SDK emulator
        isRunningOnEmulator = runningOnAndroidStudioEmulator()
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("VirtualBox") //bluestacks
                || "QC_Reference_Phone" == Build.BOARD && !"Xiaomi".equals(Build.MANUFACTURER, ignoreCase = true) //bluestacks
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST == "Build2" //MSI App Player
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.PRODUCT == "google_sdk"
                // another Android SDK emulator check
                || System.getProperties().getProperty("ro.kernel.qemu") == "1"
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
        return isRunningOnEmulator
    }

    private fun runningOnAndroidStudioEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("google/sdk_gphone")
                && Build.FINGERPRINT.endsWith(":user/release-keys")
                && Build.MANUFACTURER == "Google" && Build.PRODUCT.startsWith("sdk_gphone") && Build.BRAND == "google"
                && Build.MODEL.startsWith("sdk_gphone")
    }

    external fun isPathReallyExist(path: String?): Int

}