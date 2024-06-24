package vn.kalapa.ekyc.managers

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils
import vn.kalapa.ekyc.utils.Helpers

class KLPNFCUtils(val activity: AppCompatActivity) : NFCUtils() {
    private var isResumed = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    var forceCall = false
    override fun callOnResume() {
        Helpers.printLog("KLPNFCUtils callOnResume - $isResumed")
        if (isResumed || forceCall) {
            super.callOnResume()
        }
    }


    companion object {
        fun openNFCSetting(activity: Activity) {
            // If NFC is not enabled, open the NFC settings screen
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }

        fun checkNFCCapacity(activity: Activity, onNFCSupported: () -> Unit, onNFCNotSupported: () -> Unit, onNFCDisabled: (() -> Unit?)? = null) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            if (nfcAdapter != null) {
                onNFCSupported()
                if (!nfcAdapter.isEnabled) {
                    if (onNFCDisabled != null) {
                        onNFCDisabled()
                    }
                }
            } else {
                // NFC is not supported on this device
                onNFCNotSupported()
            }
        }
    }
}