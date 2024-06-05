package vn.kalapa.ekyc.activity

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import vn.kalapa.ekyc.utils.Helpers

abstract class BaseActivity : AppCompatActivity() {
    companion object {
        init {
            System.loadLibrary("envi")
        }
    }

    abstract fun onEmulatorDetected()
    private fun onVirtualCameraDetected() {
        Helpers.printLog("onVirtualCameraDetected virtual camera")
    }

    private var enterScreen = false
    override fun onResume() {
        if (isRunningOnEmulator() == true) onEmulatorDetected()
        if (isPathReallyExist(filesDir.absolutePath) < 0) {
            Helpers.printLog("onVirtualCameraDetected fake camera")
            onEmulatorDetected()
        } else Helpers.printLog("onVirtualCameraDetected real camera")
        if (!enterScreen) {
            enterScreen = true
        }
        super.onResume()
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