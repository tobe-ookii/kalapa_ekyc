package vn.kalapa.ekyc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.fis.ekyc.nfc.build_in.model.ResultCode
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils
import com.fis.nfc.sdk.nfc.stepNfc.NFCUtils.NFCListener
import org.json.JSONObject
import vn.kalapa.R
import vn.kalapa.ekyc.capturesdk.CameraXPassportActivity
import vn.kalapa.ekyc.activity.CameraXSelfieActivity
import vn.kalapa.ekyc.activity.ConfirmActivity
import vn.kalapa.ekyc.capturesdk.CameraXCaptureActivity
import vn.kalapa.ekyc.capturesdk.CameraXCaptureBackActivity
import vn.kalapa.ekyc.handlers.GetDynamicLanguageHandler
import vn.kalapa.ekyc.models.BackResult
import vn.kalapa.ekyc.models.ConfirmResult
import vn.kalapa.ekyc.models.FrontResult
import vn.kalapa.ekyc.models.KalapaError
import vn.kalapa.ekyc.models.KalapaLanguageModel
import vn.kalapa.ekyc.models.KalapaResult
import vn.kalapa.ekyc.models.NFCRawData
import vn.kalapa.ekyc.models.PassportResult
import vn.kalapa.ekyc.networks.Client
import vn.kalapa.ekyc.networks.KalapaAPI
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.LanguageUtils
import vn.kalapa.ekyc.nfcsdk.activities.NFCActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.views.ProgressView
import java.io.ByteArrayOutputStream

class KalapaSDK {
    companion object {
        lateinit var session: String
        lateinit var config: KalapaSDKConfig
        lateinit var handler: KalapaHandler
        lateinit var kalapaResult: KalapaResult
        lateinit var frontResult: FrontResult
        private lateinit var passportResult: PassportResult
        private lateinit var backResult: BackResult
        lateinit var faceBitmap: Bitmap
        lateinit var frontBitmap: Bitmap
        lateinit var backBitmap: Bitmap

        fun isFrontAndBackResultInitialized(): Boolean {
            return this::frontResult.isInitialized
        }

        fun isFaceBitmapInitialized(): Boolean {
            return this::faceBitmap.isInitialized
        }

        fun isFrontBitmapInitialized(): Boolean {
            return this::frontBitmap.isInitialized
        }

        fun isBackBitmapInitialized(): Boolean {
            return this::backBitmap.isInitialized
        }

        var flowType: FaceOTPFlowType = FaceOTPFlowType.ONBOARD
        fun startLivenessForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            handler: KalapaCaptureHandler
        ) {
            this.config = config
            isFoldOpen(activity)
            this.handler = handler
//            val intent = Intent(activity, LivenessActivityForResult::class.java)
            val intent = Intent(activity, CameraXSelfieActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }

        fun isFoldOpen(activity: Context): Boolean {
            val metrics = activity.resources.displayMetrics
            var isFoldOpen = metrics.heightPixels * 1f / metrics.widthPixels < 1.2f
            Helpers.printLog("isFoldOpen: $isFoldOpen isFoldDevice ${metrics.heightPixels} ${metrics.widthPixels}")
            return isFoldOpen
        }

        fun isFoldDevice(activity: Context): Boolean {
            val metrics = activity.resources.displayMetrics
            var isFoldDevice =
                metrics.heightPixels * 1f / metrics.widthPixels > 2.4f || metrics.heightPixels * 1f / metrics.widthPixels < 1.2f
            Helpers.printLog("isFoldDevice: isFoldDevice $isFoldDevice ${metrics.heightPixels} ${metrics.widthPixels}")
            return isFoldDevice
        }

        fun isTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        }


        fun startCaptureForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            handler: KalapaCaptureHandler
        ) {
            val metrics = activity.resources.displayMetrics
            this.config = config
            this.handler = handler
            val intent = Intent(activity, CameraXCaptureActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }

        fun startCaptureBackForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            handler: KalapaCaptureHandler
        ) {
            val metrics = activity.resources.displayMetrics
            this.config = config
            this.handler = handler
            val intent = Intent(activity, CameraXCaptureBackActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }

        fun startConfirmForResult(
            activity: Activity,
            session: String,
            config: KalapaSDKConfig,
            handler: KalapaHandler
        ) {
            this.session = session
            this.config = config
            this.handler = handler
            val intent = Intent(activity, ConfirmActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }


        fun startCapturingPassportForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            handler: KalapaCaptureHandler
        ) {
            this.config = config
            this.handler = handler
            val intent = Intent(activity, CameraXPassportActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }

        fun startNFCForResult(
            activity: Activity,
            config: KalapaSDKConfig,
            nfcHandler: KalapaNFCHandler
        ) {
            this.config = config
            this.handler = nfcHandler
            val intent = Intent(activity, NFCActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("mrz", nfcHandler.mrz)
            activity.startActivity(intent)
        }

        fun startNFCOnly(
            activity: Activity,
            session: String,
            config: KalapaSDKConfig,
            nfcHandler: KalapaNFCHandler
        ) {
            this.session = session
            this.config = config
            this.handler = nfcHandler
            val intent = Intent(activity, NFCActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("mrz", nfcHandler.mrz)
            activity.startActivity(intent)
        }

        fun startFullEKYC(
            activity: Activity,
            session: String,
            config: KalapaSDKConfig,
            kalapaCustomHandler: KalapaHandler
        ) {
            this.session = session
            this.config = config
            this.kalapaResult = KalapaResult()
            val onGeneralError: (resultCode: KalapaSDKResultCode) -> Unit = {
                kalapaCustomHandler.onError(it)
            }

            /*****-STEP 5-*****/
            val localStartConfirmForResult = {
                startConfirmForResult(activity, session, config, object : KalapaHandler() {
                    override fun onError(resultCode: KalapaSDKResultCode) {
                        onGeneralError(resultCode)
                    }

                    override fun onComplete(kalapaResult: KalapaResult) {
                        super.onComplete(kalapaResult)
                        Helpers.printLog("startFullEKYC localStartConfirmForResult onComplete KalapaResult: $kalapaResult \n ${kalapaResult.decision} ${kalapaResult.decisionDetail} ")
                        kalapaCustomHandler.onComplete(kalapaResult)
                    }


                })
            }

            /*****-STEP 4-*****/
            val localStartLivenessForResult = {
                startLivenessForResult(activity, config, object : KalapaCaptureHandler() {
                    private val endpoint = "/api/kyc/app/check-selfie"
                    override fun process(
                        base64: String,
                        mediaType: KalapaSDKMediaType,
                        callback: KalapaSDKCallback
                    ) {
                        faceBitmap = BitmapUtil.base64ToBitmap(base64)
                        KalapaAPI.selfieCheck(
                            endpoint,
                            faceBitmap,
                            object : Client.RequestListener {
                                override fun success(jsonObject: JSONObject) {
                                    // Set Liveness. Call Confirm
                                    if (config.captureImage)
                                        callback.sendDone {
                                            localStartConfirmForResult()
                                        }
                                    else {
                                        val path = "${Companion.config.baseURL}/api/kyc/confirm"
                                        KalapaAPI.confirm(path, "", "", "", "", "", "", "", "", "", object : Client.ConfirmListener {
                                            override fun success(confirmResult: ConfirmResult) {
                                                Helpers.printLog("confirmResult")
                                                kalapaResult.decision = confirmResult.decision_detail?.decision
                                                kalapaResult.decisionDetail = confirmResult.decision_detail?.details
                                                kalapaResult.nfc_data = confirmResult.nfc_data
                                                if (confirmResult.selfie_data != null)
                                                    kalapaResult.selfie_data = confirmResult.selfie_data.data
                                                callback.sendDone {
                                                    kalapaCustomHandler.onComplete(kalapaResult)
                                                }
                                            }

                                            override fun fail(error: KalapaError) {
                                                callback.sendError(error.getMessageError())
                                            }

                                            override fun timeout() {
                                                config.languageUtils.getLanguageString(activity.getString(R.string.klp_timeout_body))
                                            }

                                        })
                                    }
                                }

                                override fun fail(error: KalapaError) {
                                    callback.sendError(error.getMessageError())
                                }

                                override fun timeout() {
                                    callback.sendError(
                                        config.languageUtils.getLanguageString(
                                            activity.getString(R.string.klp_timeout_body)
                                        )
                                    )
                                }

                            })

                    }

                    override fun onError(resultCode: KalapaSDKResultCode) {
                        onGeneralError(resultCode)
                    }

                })
            }

            /*****-STEP 3-*****/
            val localStartNFCForResult = {
                startNFCForResult(
                    activity,
                    config,
                    object : KalapaNFCHandler(
                        if (!isFrontAndBackResultInitialized()) "" else frontResult.myFields?.idNumber
                            ?: frontResult.mrzData?.data?.rawMRZ ?: ""
                    ) {
                        private val endpoint = "/api/nfc/verify"
                        override fun process(
                            idCardNumber: String,
                            nfcData: String,
                            callback: KalapaSDKCallback
                        ) {
                            // Submit NFC.
                            KalapaAPI.nfcCheck(
                                endPoint = endpoint,
                                body = NFCRawData.fromJson(nfcData)!!,
                                object : Client.RequestListener {
                                    override fun success(jsonObject: JSONObject) {
                                        // Set NFC. Call liveness
                                        callback.sendDone {
                                            localStartLivenessForResult()
                                        }
                                    }

                                    override fun fail(error: KalapaError) {
                                        callback.sendError(error.getMessageError())
                                    }

                                    override fun timeout() {
                                        callback.sendError(
                                            config.languageUtils.getLanguageString(
                                                activity.getString(R.string.klp_timeout_body)
                                            )
                                        )
                                    }

                                })
                        }

                        override fun onError(resultCode: KalapaSDKResultCode) {
                            onGeneralError(resultCode)
                        }

                    })
            }

            /*****-STEP 2-*****/
            val localStartBackForResult = {
                startCaptureBackForResult(activity, config, object : KalapaCaptureHandler() {
                    private val endpoint = "/api/kyc/app/scan-back"
                    override fun process(
                        base64: String,
                        mediaType: KalapaSDKMediaType,
                        callback: KalapaSDKCallback
                    ) {
                        backBitmap = BitmapUtil.base64ToBitmap(base64)
                        KalapaAPI.imageCheck(endpoint, backBitmap, object : Client.RequestListener {
                            override fun success(jsonObject: JSONObject) {
                                backResult = BackResult.fromJson(jsonObject.toString())!!
                                Helpers.printLog("imageCheck $endpoint $jsonObject")
                                callback.sendDone {
                                    // Call NFC if needed!
                                    if (backResult.cardType?.contains("eid") == true && KalapaSDK.config.useNFC) {
                                        localStartNFCForResult()
                                    } else {
                                        localStartLivenessForResult()
                                    }
                                }
                            }

                            override fun fail(error: KalapaError) {
                                Helpers.printLog("error: $error ${error.getMessageError()}")
                                callback.sendError(error.getMessageError())
                            }

                            override fun timeout() {
                                callback.sendError(
                                    config.languageUtils.getLanguageString(
                                        activity.getString(
                                            R.string.klp_timeout_body
                                        )
                                    )
                                )
                            }
                        })
                    }

                    override fun onError(resultCode: KalapaSDKResultCode) {
                        onGeneralError(resultCode)
                    }

                })
            }

            /*****-STEP 1-*****/
            val localStartFrontForResult = {
                startCaptureForResult(activity, this.config, object : KalapaCaptureHandler() {
                    private val endpoint = "/api/kyc/app/scan-front"
                    override fun process(
                        base64: String,
                        mediaType: KalapaSDKMediaType,
                        callback: KalapaSDKCallback
                    ) {
                        // Check Front!
                        frontBitmap = BitmapUtil.base64ToBitmap(base64)
                        KalapaAPI.imageCheck(
                            endpoint,
                            frontBitmap,
                            object : Client.RequestListener {
                                override fun success(jsonObject: JSONObject) {
                                    // Call Back!
                                    frontResult = FrontResult.fromJson(jsonObject.toString())!!
                                    Helpers.printLog("imageCheck $endpoint $jsonObject")
                                    callback.sendDone {
                                        localStartBackForResult()
                                    }
                                }

                                override fun fail(error: KalapaError) {
                                    callback.sendError(error.getMessageError())
                                }

                                override fun timeout() {
                                    callback.sendError(
                                        config.languageUtils.getLanguageString(
                                            activity.getString(
                                                R.string.klp_timeout_body
                                            )
                                        )
                                    )
                                }

                            })
                    }

                    override fun onError(resultCode: KalapaSDKResultCode) {
                        // Got Message
                        onGeneralError(resultCode)
                    }

                })
            }

            if (KalapaSDK.config.captureImage) {
                localStartFrontForResult()
            } else {
                localStartNFCForResult()
            }
        }

        fun checkNFCCapacity(activity: Activity): KalapaSDKNFCStatus {
            val nfcUtils = NFCUtils()
            var status: KalapaSDKNFCStatus? = null
            nfcUtils.init(activity).setListener(object : NFCListener {
                override fun OnSuccess(p0: String?) {
                    TODO("Not yet implemented")
                }

                override fun OnFail(p0: String?) {
                    TODO("Not yet implemented")
                }

                override fun OnError(p0: ResultCode?) {
                    TODO("Not yet implemented")
                }

                override fun OnStartProcess() {
                    TODO("Not yet implemented")
                }

                override fun OnProcessFinished() {
                    TODO("Not yet implemented")
                }

                override fun CheckNFCAvailable(p0: Int) {
                    Helpers.printLog("NFCActivity initNFC OnNFCAvailable $p0")
                    val isNFCSupport = p0 >= 0
                    val isNFCNotEnabled = p0 == 0
                    Helpers.printLog("NFCActivity initNFC isNFCSupport $isNFCSupport isNFCNotEnabled $isNFCNotEnabled")
                    status = if (!isNFCNotEnabled && isNFCSupport) {
                        KalapaSDKNFCStatus.SUPPORTED
                    } else {
                        if (isNFCNotEnabled)
                            KalapaSDKNFCStatus.NOT_ENABLED
                        else
                            KalapaSDKNFCStatus.NOT_SUPPORTED
                    }
                }
            }).callOnResume()
            while (true) {
                if (status != null)
                    return status as KalapaSDKNFCStatus
            }
        }
    }

}

class KalapaSDKConfig(
    var context: Activity,
    var backgroundColor: String = "#FFFFFF",
    var mainColor: String = "#62A583",
    var mainTextColor: String = "#65657B",
    var btnTextColor: String = "#FFFFFF",
    var livenessVersion: Int = 0,
    var language: String,
    var minNFCRetry: Int = 3,
    var baseURL: String = "api-ekyc.kalapa.vn/face-otp",
    var useNFC: Boolean = true,
    var captureImage: Boolean = true
) {
    var languageUtils: LanguageUtils = LanguageUtils(context)
    var customTitle: String = ""
    var customSubTitle: String = ""

    init {
        pullLanguage()
    }

    private fun pullLanguage() {
        val languageJsonBody: String? = null
//            GetDynamicLanguageHandler(context).execute(baseURL, language).get() // null //
        if (!languageJsonBody.isNullOrEmpty() && languageJsonBody != "-1") {
            val klpLanguageModel = KalapaLanguageModel.fromJson(languageJsonBody)
            if ((klpLanguageModel?.error != null) && (klpLanguageModel.error.code == 0) && klpLanguageModel.data != null) {
                // Thành công
                if (klpLanguageModel.data.data?.sdk?.isNotEmpty() == true) {
                    languageUtils.setLanguage(klpLanguageModel.data.data.sdk)
                }
            }
        }
    }
}


abstract class KalapaCaptureHandler : KalapaHandler() {
    abstract fun process(base64: String, mediaType: KalapaSDKMediaType, callback: KalapaSDKCallback)
}

abstract class KalapaHandler {
    abstract fun onError(resultCode: KalapaSDKResultCode)
    open fun onProcessFinished() {
        Helpers.printLog("KalapaHandler onProcessFinished")
    }

    open fun onComplete(kalapaResult: KalapaResult) {
        Helpers.printLog("KalapaHandler onComplete $kalapaResult")
    }
}

abstract class KalapaNFCHandler(val mrz: String?) : KalapaHandler() {
    abstract fun process(idCardNumber: String, nfcData: String, callback: KalapaSDKCallback)
}

enum class KalapaSDKNFCStatus(status: Int) {
    NOT_SUPPORTED(-1),
    NOT_ENABLED(0),
    SUPPORTED(1)
}

enum class KalapaSDKResultCode {
    UNKNOWN, SUCCESS, PERMISSION_DENIED, USER_CONSENT_DECLINED, SUCCESS_WITH_WARNING, CANNOT_OPEN_DEVICE, CARD_NOT_FOUND, WRONG_CCCDID, CARD_LOST_CONNECTION, USER_LEAVE, EMULATOR_DETECTED, DEVICE_NOT_SUPPORTED
}


enum class KalapaSDKMediaType {
    FRONT, BACK, PORTRAIT, PASSPORT
}

interface KalapaSDKCallback {
    fun sendError(message: String?)
    fun sendDone(nextAction: () -> Unit)

}

enum class FaceOTPFlowType(val flow: String?) {
    ONBOARD("ONBOARD"),
    PASSPORT("PASSPORT"),
    VERIFY(null)
}

fun ImageProxy.toBitmap(): Bitmap? {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    return yuvImage.toBitmap()
}

private fun YuvImage.toBitmap(): Bitmap? {
    val out = ByteArrayOutputStream()
    if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
        return null
    val imageBytes: ByteArray = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val pixelCount = image.cropRect.width() * image.cropRect.height()
    val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
    val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
    imageToByteBuffer(image, outputBuffer, pixelCount)
    return outputBuffer
}

private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
    assert(image.format == ImageFormat.YUV_420_888)

    val imageCrop = image.cropRect
    val imagePlanes = image.planes

    imagePlanes.forEachIndexed { planeIndex, plane ->
        // How many values are read in input for each output value written
        // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
        //
        // Y Plane            U Plane    V Plane
        // ===============    =======    =======
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        val outputStride: Int

        // The index in the output buffer the next value will be written at
        // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
        //
        // First chunk        Second chunk
        // ===============    ===============
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        var outputOffset: Int

        when (planeIndex) {
            0 -> {
                outputStride = 1
                outputOffset = 0
            }

            1 -> {
                outputStride = 2
                // For NV21 format, U is in odd-numbered indices
                outputOffset = pixelCount + 1
            }

            2 -> {
                outputStride = 2
                // For NV21 format, V is in even-numbered indices
                outputOffset = pixelCount
            }

            else -> {
                // Image contains more than 3 planes, something strange is going on
                return@forEachIndexed
            }
        }

        val planeBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // We have to divide the width and height by two if it's not the Y plane
        val planeCrop = if (planeIndex == 0) {
            imageCrop
        } else {
            Rect(
                imageCrop.left / 2,
                imageCrop.top / 2,
                imageCrop.right / 2,
                imageCrop.bottom / 2
            )
        }

        val planeWidth = planeCrop.width()
        val planeHeight = planeCrop.height()

        // Intermediate buffer used to store the bytes of each row
        val rowBuffer = ByteArray(plane.rowStride)

        // Size of each row in bytes
        val rowLength = if (pixelStride == 1 && outputStride == 1) {
            planeWidth
        } else {
            // Take into account that the stride may include data from pixels other than this
            // particular plane and row, and that could be between pixels and not after every
            // pixel:
            //
            // |---- Pixel stride ----|                    Row ends here --> |
            // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
            //
            // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
            (planeWidth - 1) * pixelStride + 1
        }

        for (row in 0 until planeHeight) {
            // Move buffer position to the beginning of this row
            planeBuffer.position(
                (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
            )

            if (pixelStride == 1 && outputStride == 1) {
                // When there is a single stride value for pixel and output, we can just copy
                // the entire row in a single step
                planeBuffer.get(outputBuffer, outputOffset, rowLength)
                outputOffset += rowLength
            } else {
                // When either pixel or output have a stride > 1 we must copy pixel by pixel
                planeBuffer.get(rowBuffer, 0, rowLength)
                for (col in 0 until planeWidth) {
                    outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                    outputOffset += outputStride
                }
            }
        }
    }
}