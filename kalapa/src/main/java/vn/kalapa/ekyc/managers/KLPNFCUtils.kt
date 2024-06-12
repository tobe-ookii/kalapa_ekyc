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

        fun checkNFCCapacity(activity: Activity, onNFCEnabled: () -> Unit, onNFCDisabled: () -> Unit, onNFCNotSupported: () -> Unit) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            if (nfcAdapter != null) {
                if (!nfcAdapter.isEnabled) {
                    onNFCDisabled()
                } else {
                    // NFC is enabled
                    onNFCEnabled()
                }
            } else {
                // NFC is not supported on this device
                onNFCNotSupported()
            }
        }
    }
}