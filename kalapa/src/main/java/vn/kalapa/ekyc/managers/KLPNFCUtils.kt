package vn.kalapa.ekyc.managers

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
}