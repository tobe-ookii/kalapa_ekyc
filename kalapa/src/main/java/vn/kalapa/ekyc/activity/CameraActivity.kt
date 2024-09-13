package vn.kalapa.ekyc.activity

import vn.kalapa.ekyc.fragment.CaptureableFragment
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image.Plane
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.ViewCompat
import vn.kalapa.R
import vn.kalapa.ekyc.*
import vn.kalapa.ekyc.fragment.CameraConnectionFragment
import vn.kalapa.ekyc.fragment.LegacyCameraConnectionFragment
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.ImageUtils
import vn.kalapa.ekyc.utils.Logger
import vn.kalapa.ekyc.views.CardMaskView


abstract class CameraActivity(
    private var activityLayoutId: Int = R.layout.activity_camera,
    private var lensFacing: LENS_FACING = LENS_FACING.REAR,
    private val hideAutoCapture: Boolean = true,
    private val refocusFrequency: Int = 100
) : BaseActivity(), ImageReader.OnImageAvailableListener,
    Camera.PreviewCallback, View.OnClickListener {
    val log = Logger()
    protected val isDebug = false
    lateinit var rootContainer: View
    private val PERMISSIONS_REQUEST = 1
    lateinit var fragment: CaptureableFragment
    private val PERMISSION_CAMERA = Manifest.permission.CAMERA
    private val ASSET_PATH = ""
    protected var previewWidth = 0
    protected var previewHeight = 0
    private val debug = false
    protected var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var useCamera2API = false
    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    protected var defaultModelIndex = 0
    protected var defaultDeviceIndex = 0
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    protected lateinit var tmpBitmap: Bitmap
    protected lateinit var faceBitmap: Bitmap
    private var isAutocapturing = true
    internal lateinit var cardMaskView: CardMaskView
    lateinit var ivCloseEkyc: ImageView
    lateinit var holderCapture: View
    lateinit var holderAutoCapture: View
    lateinit var tvError: TextView
    lateinit var btnCapture: ImageButton
    lateinit var btnRetry: Button
    lateinit var btnNext: Button
    lateinit var ivAutoCapture: ImageView
    lateinit var tvGuide: TextView
    lateinit var ivPreviewImage: ImageView

    lateinit var previewHolder: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        log.d("onCreate %s", this@CameraActivity.localClassName)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        findViewById()
        setupUI()
        if (hasPermission())
            setFragment(lensFacing)
        else
            requestPermission()
    }

    private fun onPreDestroy() {

    }

    private var isRefocusTouched = false

    private fun reFocus() {
//        if (isCameraMode && !isRefocusTouched) {
//            Helpers.printLog("On Refocus Touch")
//            isRefocusTouched = true
//            fragment.resumeCamera()
//            Handler().postDelayed({ isRefocusTouched = false }, 5000)
//        }
    }

    abstract fun showEndEkyc()
    private fun findViewById() {
//        setContentView(R.layout.activity_camera)
        setContentView(activityLayoutId)
        cardMaskView = findViewById(R.id.cardMaskView)
        cardMaskView.setOnClickListener {
            reFocus()
        }
//        tvTitle = findViewById(R.id.tv_title)
        ivCloseEkyc = findViewById(R.id.iv_close_ekyc)
        rootContainer = findViewById(R.id.root_container)
        rootContainer.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
        ivCloseEkyc.setColorFilter(Color.parseColor(KalapaSDK.config.mainTextColor))
        ivCloseEkyc.setOnClickListener {
            showEndEkyc()
        }
        tvError = findViewById(R.id.tv_error)
        btnCapture = findViewById(R.id.btn_capture)
        holderCapture = findViewById(R.id.holder_capture)
        holderAutoCapture = findViewById(R.id.holder_auto_capture)
        tvGuide = findViewById(R.id.tv_guide)
        btnRetry = findViewById(R.id.btn_retry)
        btnNext = findViewById(R.id.btn_next)
        btnNext.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_next_btn))
        btnRetry.text =
            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_retry_btn))
//        btnBack = findViewById(R.id.btn_back)
//        btnInfo = findViewById(R.id.btn_info)
//        tvInstruction = findViewById(R.id.tv_instruction)
        previewHolder = findViewById(R.id.container)
        ivAutoCapture = findViewById(R.id.toggle_auto_capture)
        ivPreviewImage = findViewById(R.id.iv_preview_image)
        ivPreviewImage.isDrawingCacheEnabled = false
        Helpers.setColorTintList(ivAutoCapture, KalapaSDK.config.mainColor)
        ivAutoCapture.setOnClickListener {
            this.isAutocapturing = !isAutocapturing
            if (isAutocapturing) {
                ivAutoCapture.setImageResource(R.drawable.klp_ic_toggle_on)
            } else {
                ivAutoCapture.setImageResource(R.drawable.klp_ic_toggle_off)
            }

//            KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_subtitle_front))
        }
        btnCapture.setOnClickListener(this)
        btnRetry.setOnClickListener(this)
        btnNext.setOnClickListener(this)
//        btnInfo.setOnClickListener(this)
//        btnBack.setOnClickListener(this)
//        tvInstruction.setOnClickListener(this)
//        Helpers.setTextBoldWithColor(tvInstruction, KalapaSDK.config.mainColor)
    }


    protected fun getRgbBytes(): IntArray {
        imageConverter!!.run()
        return rgbBytes!!
    }


    protected open fun setupUI() {
        cardMaskView.setBackgroundColor(resources.getColor(R.color.transparent))
//        cardMaskView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
        cardMaskView.strokeWidth = 5F
        ViewCompat.setBackgroundTintList(
            btnRetry,
            ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
        )
        btnRetry.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))
//        ViewCompat.setBackgroundTintList(
//            btnCapture,
//            ColorStateList.valueOf(Color.parseColor(Kalapa.configUI.mainColor))
//        )
        setupCustomUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::fragment.isInitialized)
            fragment.releaseCamera()
    }

    private fun allPermissionsGranted(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                    this@CameraActivity,
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_check_permission_camera)),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            requestPermissions(
                arrayOf(PERMISSION_CAMERA),
                PERMISSIONS_REQUEST
            )
        }
    }


    // Returns true if the device supports the required hardware level, or better.
    private fun isHardwareLevelSupported(
        characteristics: CameraCharacteristics,
        requiredLevel: Int
    ): Boolean {
        val deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
        return if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            requiredLevel == deviceLevel
        } else requiredLevel <= deviceLevel
        // deviceLevel is not LEGACY, can use numerical sort
    }

    enum class LENS_FACING {
        FRONT, REAR
    }

//    private fun chooseCamera(cameraFacing: LENS_FACING): String? {
//        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
//        try {
//            var cameraID: String = ""
//            for (cameraId in manager.cameraIdList) {
//                val characteristics = manager.getCameraCharacteristics(cameraId)
//
//                // We don't use a front facing camera in this sample.
//                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
//                log.i("Camera API cameraId - %s - %s - %s - %s", cameraId, facing, manager.cameraIdList.size, characteristics.keys)
//
//
//                // Fallback to camera1 API for internal cameras that don't have full support.
//                // This should help with legacy situations where using the camera2 API causes
//                // distorted or otherwise broken previews.
//                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL
//                        || isHardwareLevelSupported(
//                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
//                ))
//                log.i("Camera API lv2?: %s", useCamera2API)
//
//                if (cameraFacing == LENS_FACING.FRONT) {
//                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
//                        cameraID = cameraId
//                    }
//                } else {
//                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
//                        cameraID = cameraId
//                    }
//
//                }
//
//                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                    ?: continue
//
//            }
//            log.i("Camera ID: %s", cameraID)
//            return cameraID
//
//        } catch (e: CameraAccessException) {
//            log.e(e, "Not allowed to access camera")
//        }
//        return null
//    }

    private fun chooseCamera(cameraFacing: LENS_FACING): String? {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraFacing == LENS_FACING.FRONT) {
                    if (facing == null || facing != CameraCharacteristics.LENS_FACING_FRONT) {
                        continue
                    }
                } else {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue
                    }
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL
                        || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                ))
                log.i("Camera API lv2?: %s %s", useCamera2API, cameraId)
                return cameraId
            }
        } catch (e: CameraAccessException) {
            log.e(e, "Not allowed to access camera")
        }
        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (i in permissions.indices) {
            Helpers.printLog("Request Permission: ${permissions[i]} - ${grantResults[i]}" + permissions[i])
        }
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment(lensFacing)
            } else if (allPermissionsRejected(grantResults)) {
                Helpers.showDialog(
                    this@CameraActivity,
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_check_permission_camera_title)),
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_check_permission_camera)),
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_next_btn)),
                    KalapaSDK.config.languageUtils.getLanguageString(resources.getString(R.string.klp_cancel_button)),
                    R.drawable.ic_warning,
                    object : DialogListener {
                        override fun onYes() {
                            // Link to Setting
                            val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri: Uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
//                        TODO("Not yet implemented")
                        }

                        override fun onNo() {
                            KalapaSDK.handler.onError(KalapaSDKResultCode.PERMISSION_DENIED)
                            finish()
//                        TODO("Not yet implemented")
                        }

                    })
            } else {
                requestPermission()
            }
        }
    }


    private fun allPermissionsRejected(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }


    private fun setFragment(lensFacing: LENS_FACING = LENS_FACING.REAR) {
        val cameraId = chooseCamera(lensFacing)
        if (useCamera2API) {
            Helpers.printLog("CameraActivity: Calling CameraConnectionFragment")
            val camera2Fragment: CameraConnectionFragment = CameraConnectionFragment.newInstance(
                object : CameraConnectionFragment.ConnectionCallback {
                    override fun onPreviewSizeChosen(size: Size, rotation: Int) {
                        previewHeight = size.height
                        previewWidth = size.width
                        this@CameraActivity.onPreviewSizeChosen(size, rotation)
                    }
                },
                this,
                getLayoutId(),
                getDesiredPreviewFrameSize()
            )
            camera2Fragment.setCamera(cameraId)
            fragment = camera2Fragment
        } else {
            Helpers.printLog("CameraActivity: Calling Legacy Camera Connection")
            fragment = LegacyCameraConnectionFragment(
                this,
                lensFacing,
                getLayoutId(),
                getDesiredPreviewFrameSize()
            )
        }
        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        isCameraMode = true
    }

    /**Update Active Model. Hiện tại mặt trước mặt sau đang chung 1 model. Mai sau có thể cần update. (VD: Passport, Cà Vẹt,.. */
    protected abstract fun updateActiveModel()

    protected abstract fun setupCustomUI()

    /**Process every single image that ready*/
    protected abstract fun processImage()

    /**Verify Image*/
    protected abstract fun verifyImage()

    protected abstract fun onPreviewSizeChosen(size: Size, rotation: Int)

    /**Get Layout from inherited class*/
    protected abstract fun getLayoutId(): Int

    protected abstract fun getDesiredPreviewFrameSize(): Size

    protected abstract fun setNumThreads(numThreads: Int)

    protected abstract fun setUseNNAPI(isChecked: Boolean)


    /*Callback for Camera2 API*/
    override fun onImageAvailable(reader: ImageReader?) {
        // We need wait until we have some size from onPreviewSizeChosen

        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader!!.acquireLatestImage() ?: return
            if (isProcessingFrame || !isAutocapturing) {
                image.close()
                return
            }
            isProcessingFrame = true
            Trace.beginSection("imageAvailable")
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0],
                    yuvBytes[1],
                    yuvBytes[2],
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }
            processImage()
        } catch (e: Exception) {
            log.e(e, "Exception!")
            Trace.endSection()
            return
        }
        Trace.endSection()
    }

    protected open fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                log.d("Initializing buffer %d at size %d", i, buffer.capacity())
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }

    override fun onPreviewFrame(bytes: ByteArray?, camera: Camera?) {
        if (isProcessingFrame || !isAutocapturing) {
            log.w("Dropping frame!")
            return
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                val previewSize = camera!!.parameters.previewSize
                previewHeight = previewSize.height
                previewWidth = previewSize.width
                rgbBytes = IntArray(previewWidth * previewHeight)
                onPreviewSizeChosen(Size(previewSize.width, previewSize.height), 180)
            }
        } catch (e: java.lang.Exception) {
            log.e(e, "Exception!")
            return
        }

        isProcessingFrame = true
        yuvBytes[0] = bytes
        yRowStride = previewWidth

        imageConverter = Runnable {
            ImageUtils.convertYUV420SPToARGB8888(
                bytes,
                previewWidth,
                previewHeight,
                rgbBytes
            )
        }

        postInferenceCallback = Runnable {
            camera!!.addCallbackBuffer(bytes)
            isProcessingFrame = false
        }
        processImage()
    }

    private var mLastClickTime: Long = 0

    private fun captureImage() {
        runOnUiThread(Runnable {
            val bitmap: Bitmap = fragment.captureImage()
            Helpers.printLog(
                "DPI of devices: ",
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                resources.displayMetrics.densityDpi,
                resources.displayMetrics.density,
                (cardMaskView.centerY) / resources.displayMetrics.heightPixels
            )
            tmpBitmap =
                BitmapUtil.crop(
                    bitmap,
                    CardMaskView.ID_CARD_WIDTH_SIZE,
                    if (lensFacing == LENS_FACING.FRONT) CardMaskView.ID_CARD_WIDTH_SIZE else CardMaskView.ID_CARD_HEIGHT_SIZE + 100,
//                    CardMaskView.ID_CARD_HEIGHT_SIZE + 100,
                    0.5F,
//                    (cardMaskView.centerY + cardMaskView.translationY * resources.displayMetrics.density) / resources.displayMetrics.heightPixels
                    (cardMaskView.centerY) / resources.displayMetrics.heightPixels
                )

            ivPreviewImage.visibility = View.VISIBLE
            ivPreviewImage.setImageBitmap(tmpBitmap)
            previewViewLayerMode(false)
        })
    }


    private fun controlView_CameraStart() {
//        previewView.show()
        fragment.resumeCamera()
        btnCapture.visibility = View.VISIBLE
        btnNext.visibility = View.INVISIBLE // .hide()
        btnRetry.visibility = View.INVISIBLE //hide()
        fragment.captureImage()
    }

    fun controlView_CameraStop() {
        // previewView.hide()
        fragment.pauseCamera()
        btnCapture.visibility = View.INVISIBLE
        btnNext.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE
    }

    fun previewViewLayerMode(isCameraMode: Boolean) = if (isCameraMode) { // start camera
        if (isCameraMode) {
            ivPreviewImage.visibility = View.GONE
            ivPreviewImage.setImageDrawable(resources.getDrawable(R.drawable.guide_id))
        }
        this.isCameraMode = true
        controlView_CameraStart()
        tvError.text = ""
        tvError.setTextColor(resources.getColor(R.color.ekyc_red))
        holderCapture.visibility = View.VISIBLE
        holderAutoCapture.visibility =
            if (lensFacing == LENS_FACING.REAR && !hideAutoCapture) View.VISIBLE else View.GONE

        tvGuide.visibility = View.VISIBLE

//        tvGuide.text = resources.getText(R.string.klp_liveness_initializing)
//        tvInstruction.visibility = View.VISIBLE
    } else { // stop camera
        holderCapture.visibility = View.INVISIBLE

        holderAutoCapture.visibility = if (!hideAutoCapture) View.INVISIBLE else View.GONE
//        tvGuide.visibility = View.INVISIBLE
//        tvInstruction.visibility = View.INVISIBLE

        this.isCameraMode = false
        controlView_CameraStop()
    }


    abstract fun onBackBtnClicked()

    abstract fun onInfoBtnClicked()

    var isCameraMode = false
    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        val id: Int = v.id
        when (id) {
//            R.id.btn_back -> {
//                log.d("Btn btn_back Clicked")
//                onBackBtnClicked()
//            }
            R.id.btn_next -> {
                log.d("Btn btn_next Clicked")
                verifyImage()
            }

            R.id.btn_retry -> {
                log.d("Btn btn_retry Clicked")
                onRetryClicked()
//                tvInstruction.visibility = View.VISIBLE
                previewViewLayerMode(true)
            }

            R.id.btn_capture -> {
                log.d("Btn btn_capture Clicked")
//                tvInstruction.visibility = View.INVISIBLE
                captureImage()
            }
//            R.id.btn_info -> {
//                log.d("Btn btn_info Clicked")
//                onInfoBtnClicked()
//            }
            R.id.tv_instruction -> {
                log.d("TV tv_instruction Clicked")
                onInfoBtnClicked()
            }
        }
    }

    var focusRequestTime = 0
    fun getCameraRotationDegree(): Int {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        focusRequestTime++
        if (focusRequestTime % refocusFrequency == 0 && focusRequestTime / refocusFrequency < 2) {
            runOnUiThread {
                Helpers.printLog("on reset camera")
                onPause()
                onResume()
            }
        }
        try {
            val characteristics =
                manager.getCameraCharacteristics(if (lensFacing == LENS_FACING.REAR) "0" else "1")
            val rotationDegree = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
//            Helpers.printLog("manager.getCameraCharacteristics isBackSideCapture ${lensFacing == LENS_FACING.REAR} $rotationDegree")
            return rotationDegree
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return 0
    }

    // tflite
    protected fun readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback!!.run()
        }
    }

    protected fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    open fun onRetryClicked() {

    }

    override fun onResume() {
        log.d("onResume $this")
        super.onResume()
        handlerThread = HandlerThread("inference")
        if (handlerThread != null) {
            handlerThread!!.start()
            handler = Handler(handlerThread!!.looper)
        }
        if (this::fragment.isInitialized)
            previewViewLayerMode(true)
        else {
            if (hasPermission())
                setFragment(lensFacing)
        }
    }

    override fun onPause() {
        log.d("onPause $this")
        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
            handler = null
            handlerThread = null
        } catch (e: InterruptedException) {
            log.e(e, "Exception!")
        }
        if (this::fragment.isInitialized)
            previewViewLayerMode(false)
        super.onPause()
    }

    @Synchronized
    protected open fun runInBackground(r: Runnable?) {
        if (handler != null) {
            handler!!.post(r!!)
        }
    }

}