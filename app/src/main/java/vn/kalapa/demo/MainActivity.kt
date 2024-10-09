package vn.kalapa.demo


import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import vn.kalapa.demo.activities.BaseActivity
import vn.kalapa.demo.utils.Helpers
import vn.kalapa.ekyc.IKalapaRawDataProcessor
import vn.kalapa.ekyc.KalapaHandler
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.KalapaSDKConfig
import vn.kalapa.ekyc.KalapaSDKMediaType
import vn.kalapa.ekyc.KalapaSDKResultCode
import vn.kalapa.ekyc.KalapaScanNFCCallback
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.networks.Client


class MainActivity : BaseActivity() {
    val TAG = "MainActivity"
    private lateinit var ekycButton: Button

    private val BASE_URL = "https://ekyc-api.kalapa.vn"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Helpers.init(this@MainActivity)
        ekycButton.setOnClickListener {
            val klpConfig = KalapaSDKConfig.KalapaSDKConfigBuilder(this@MainActivity)
                .build()
            val rawDataProcessor = object :IKalapaRawDataProcessor{
                override fun processLivenessData(portraitBase64: String, completion: Client.RequestListener) {
                    TODO("Process portrait image in base64 after liveness process")
                }

                override fun processCaptureData(documentBase64: String, documentType: KalapaSDKMediaType, completion: Client.RequestListener) {
                    TODO("Process document image in base64 after capture process. documentType can be FRONT/BACK")
                }

                override fun processNFCData(idCardNumber: String, nfcRawData: String, completion: Client.RequestListener) {
                    TODO("Process NFC data after NFC process.")
                }

            }
            val klpHandler = object : KalapaHandler() {
                override fun onNFCTimeoutHandle(activity: Activity, sdkCallback: KalapaScanNFCCallback) {
                    // This handler is called when user stuck in NFC screen for long time. You can implement your UI code and use sdkCallback to tell SDK should retry / stay in this screen or close the SDK
                    super.onNFCTimeoutHandle(activity, sdkCallback)
                }

                override fun onComplete(kalapaResult: KalapaResult) {
                    // This handler is called when the eKYC process success with results
                    super.onComplete(kalapaResult)
                }

                override fun onError(resultCode: KalapaSDKResultCode) {
                    // This handler is called when the eKYC process ends with an error
                    super.onError(resultCode)
                }

                override fun onExpired() {
                    // This handler is called when current session goes expired and user clicks the Retry button in the popup.
                }

            }

            KalapaSDK.KalapaSDKBuilder(this@MainActivity, klpConfig)
                .withMrz("<<YOUR_MRZ>>")
                .withFaceData("<<YOUR_FACE_DATA>>")
                .build()
                .start("<<YOUR_SESSION_ID>>", "<<YOUR_FLOW>>", klpHandler)

            KalapaSDK.KalapaSDKBuilder(this@MainActivity, klpConfig).build()
                .startCustomFlow(true, true, true, klpHandler)
        }
    }

    private fun showAlert(
        title: String,
        message: String,
        cancelable: Boolean,
        onConfirmPressed: () -> Unit
    ) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
            .setPositiveButton(resources.getString(R.string.klp_nfc_button_start)) { p0, p1 ->
                run {
                    onConfirmPressed()
                }
            }
            .create()
            .show()
    }

}
