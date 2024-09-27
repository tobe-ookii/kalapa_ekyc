package vn.kalapa.demo

import android.graphics.Bitmap
import vn.kalapa.demo.models.NFCVerificationData
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.models.PreferencesConfig

class ExampleGlobalClass {
    companion object {
        lateinit var preferencesConfig: PreferencesConfig
        lateinit var nfcData: NFCVerificationData
        lateinit var kalapaResult: KalapaResult
        var faceImage: Bitmap? = null
        var frontImage: Bitmap? = null
        var backImage: Bitmap? = null

        fun isNFCDataInitialized(): Boolean {
            return this::nfcData.isInitialized
        }

        fun isHaveResult(): Boolean {
            return this::kalapaResult.isInitialized
        }

        fun isPreferencesConfigInitialized(): Boolean {
            return this::preferencesConfig.isInitialized
        }

        fun isKalapaResultInitialized(): Boolean {
            return this::kalapaResult.isInitialized
        }
    }
}