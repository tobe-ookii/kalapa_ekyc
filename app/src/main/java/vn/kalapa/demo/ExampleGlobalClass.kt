package vn.kalapa.demo

import android.graphics.Bitmap
import vn.kalapa.demo.models.NFCVerificationData
import vn.kalapa.ekyc.models.PreferencesConfig

class ExampleGlobalClass {
    companion object {
        lateinit var preferencesConfig: PreferencesConfig
        lateinit var nfcData: NFCVerificationData
        lateinit var faceImage: Bitmap
        fun isNFCDataInitialized(): Boolean {
            return this::nfcData.isInitialized
        }
        fun isFaceImageInitialized(): Boolean {
            return this::faceImage.isInitialized
        }
        fun isPreferencesConfigInitialized(): Boolean{
            return this::preferencesConfig.isInitialized
        }
    }
}