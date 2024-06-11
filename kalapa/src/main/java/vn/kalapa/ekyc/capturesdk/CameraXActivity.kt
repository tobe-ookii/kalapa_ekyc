package vn.kalapa.ekyc.capturesdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import vn.kalapa.R
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.activity.BaseActivity
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.CameraUtils
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.Logger
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

abstract class CameraXActivity(
    private var activityLayoutId: Int = R.layout.activity_camera_x,
    private var lensFacing: LENS_FACING = LENS_FACING.REAR,
    private val hideAutoCapture: Boolean = true,
    private val refocusFrequency: Int = 100
) : BaseActivity(), OnClickListener {
    private val log = Logger()
    var tmpBitmap: Bitmap? = null
    private var isAutocapturing = true

    //    private var imageAnalyzer: ImageAnalysis? = null
    var cameraAnalyzer: ImageAnalysis? = null

    //    private lateinit var cardMaskView: CardMaskView
    lateinit var ivCloseEkyc: ImageView
    lateinit var holderCapture: View
    lateinit var holderAutoCapture: View
    lateinit var tvError: TextView
    lateinit var btnCapture: ImageButton
    lateinit var btnRetry: Button
    lateinit var btnNext: Button
    private lateinit var ivAutoCapture: ImageView
    lateinit var tvGuide: TextView
    lateinit var rootContainer: View

    // camera X
    private lateinit var viewFinder: PreviewView

    var isCameraMode = false
    var mLastClickTime: Long = 0

    // Camera X

    private var imageCapture: ImageCapture? = null
    lateinit var cameraExecutor: ExecutorService
    override fun onEmulatorDetected() {
        // Something
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        findViewById()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        setupCamera()
    }

    private fun setupCamera() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        // Set up the listeners for take photo and video capture buttons
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun reFocus() {
    }

    abstract fun showEndEkyc()
    private fun findViewById() {
//        setContentView(R.layout.activity_camera)
        setContentView(activityLayoutId)
        // camera X
        viewFinder = findViewById(R.id.viewFinder)

//        cardMaskView = findViewById(R.id.cardMaskView)
//        cardMaskView.setOnClickListener {
//            reFocus()
//        }
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
        ivAutoCapture = findViewById(R.id.toggle_auto_capture)
        if (hideAutoCapture) ivAutoCapture.visibility = View.GONE
        Helpers.setColorTintList(ivAutoCapture, KalapaSDK.config.mainColor)
        ivAutoCapture.setOnClickListener {
            this.isAutocapturing = !isAutocapturing
            if (isAutocapturing) {
                ivAutoCapture.setImageResource(R.drawable.ic_toggle_on)
            } else {
                ivAutoCapture.setImageResource(R.drawable.ic_toggle_off)
            }
        }
        btnCapture.setOnClickListener(this)
        btnRetry.setOnClickListener(this)
        btnNext.setOnClickListener(this)
    }

    protected open fun setupUI() {
//        cardMaskView.strokeWidth = 5F
        ViewCompat.setBackgroundTintList(
            btnRetry,
            ColorStateList.valueOf(resources.getColor(R.color.white))
        )
        btnRetry.setTextColor(Color.parseColor(KalapaSDK.config.mainColor))

        ViewCompat.setBackgroundTintList(
            btnNext,
            ColorStateList.valueOf(Color.parseColor(KalapaSDK.config.mainColor))
        )
        btnNext.setTextColor(Color.parseColor(KalapaSDK.config.btnTextColor))
        setupCustomUI()
    }

    enum class LENS_FACING {
        FRONT, REAR
    }

    /**
     * Camera X function
     * */

    fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    tmpBitmap = BitmapUtil.imageProxyToBitmap(image)
                    onCaptureSuccess(image.imageInfo.rotationDegrees)
                    stopCamera()
                }
            })
    }


    abstract fun onCaptureSuccess(cameraDegree: Int)

    private fun captureVideo() {}

    open fun setupAnalyzer(): ImageAnalysis? {
//        return ImageAnalysis.Builder()
//            .build()
//            .also {
//                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                    Log.d(TAG, "Average luminosity: $luma")
//                })
//            }
        return null
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        previewViewLayerMode(false)
    }

    fun unbindAllAnalyzer() {
    }

    private var cameraProvider: ProcessCameraProvider? = null

    @SuppressLint("RestrictedApi")
    fun startCamera() {
        /**Create an instance of the ProcessCameraProvider.
         * This is used to bind the lifecycle of cameras to the lifecycle owner.
         * This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
         **/
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        /**
         * Add a listener to the cameraProviderFuture.
         * Add a Runnable as one argument.
         * We will fill it in later. Add ContextCompat.getMainExecutor() as the second argument.
         * This returns an Executor that runs on the main thread.
         */
        cameraProviderFuture.addListener({
            /**
             * In the Runnable, add a ProcessCameraProvider.
             * This is used to bind the lifecycle of our camera to the LifecycleOwner within the application's process.
             */
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            /**
             * Initialize our Preview object, call build on it, get a surface provider from viewfinder, and then set it on the preview.
             */
            // Preview
            val preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector =
                if (lensFacing == LENS_FACING.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val MIN_WITH_ACCEPTED_SIZE = if (lensFacing == LENS_FACING.FRONT) 1080 else 1088
            var opticalResolution = Size(1080, 1920)
            for (s in getOutputSizes(cameraSelector.lensFacing!!)!!) {
//                Helpers.printLog("opticalResolution S: Choosing ${s.width} ${s.height}")
                if (s.width < MIN_WITH_ACCEPTED_SIZE)
                    break
                opticalResolution = s
            }
            Helpers.printLog("opticalResolution S: ${opticalResolution.width} ${opticalResolution.height}")
            imageCapture = ImageCapture.Builder()
                .setJpegQuality(85)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .setMaxResolution(opticalResolution)
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetResolution(opticalResolution)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
            cameraAnalyzer = setupAnalyzer()
            /**
             * Create a try block. Inside that block, make sure nothing is bound to the cameraProvider,
             * and then bind our cameraSelector and preview object to the cameraProvider.
             */
            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()
                var camera: androidx.camera.core.Camera?
                if (cameraAnalyzer != null)
                    camera = cameraProvider?.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        cameraAnalyzer
                    )
                else
                // Bind use cases to camera
                    camera =
                        cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                if (camera != null) {
                    var cameraFov = CameraUtils.getCameraFov(camera.cameraInfo)
                    if (cameraFov > 75) {
                        val zoomRatio = cameraFov / 75
                        camera.cameraControl.setZoomRatio(zoomRatio)
                        Helpers.printLog("onCaptureSuccess Zoom Ratio $zoomRatio")
                    }
                }
                previewViewLayerMode(true)
                /**
                 * There are a few ways this code could fail, like if the app is no longer in focus. Wrap this code in a catch block to log if there's a failure.
                 */
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
        previewViewLayerMode(true)
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }


    /**Update Active Model. Hiện tại mặt trước mặt sau đang chung 1 model. Mai sau có thể cần update. (VD: Passport, Cà Vẹt,.. */
//    protected abstract fun updateActiveModel()

    protected abstract fun setupCustomUI()

    /**Process every single image that ready*/
//    protected abstract fun processImage()

    /**Verify Image*/
    protected abstract fun verifyImage()

    private fun getOutputSizes(lensFacing: Int = CameraCharacteristics.LENS_FACING_BACK): Array<Size>? {
        val manager = this@CameraXActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val orientation = characteristics[CameraCharacteristics.LENS_FACING]!!

            if (orientation == lensFacing) {
                val configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                return configurationMap.getOutputSizes(SurfaceTexture::class.java)
            }
        }
        return null
    }

    /**Get Layout from inherited class*/
//    protected abstract fun getLayoutId(): Int

    open fun previewViewLayerMode(isCameraMode: Boolean) = if (isCameraMode) { // start camera
        this.isCameraMode = true
        tvError.text = ""
        tvError.visibility = View.INVISIBLE
        tvError.setTextColor(resources.getColor(R.color.ekyc_red))
        holderCapture.visibility = View.VISIBLE
        holderAutoCapture.visibility =
            if (lensFacing == LENS_FACING.REAR && !hideAutoCapture) View.VISIBLE else View.GONE
        tvGuide.visibility = View.VISIBLE
        btnNext.visibility = View.INVISIBLE
        btnRetry.visibility = View.INVISIBLE
//        cardMaskView.setBackgroundColor(resources.getColor(R.color.black20))
//        viewFinder.visibility = View.VISIBLE
    } else { // stop camera
        btnNext.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE
        holderCapture.visibility = View.INVISIBLE
        holderAutoCapture.visibility = if (!hideAutoCapture) View.INVISIBLE else View.GONE
        this.isCameraMode = false
//        viewFinder.visibility = View.INVISIBLE
//        cardMaskView.setBackgroundColor(Color.parseColor(KalapaSDK.config.backgroundColor))
    }


    abstract fun onBackBtnClicked()
    abstract fun onInfoBtnClicked()

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        val id: Int = v.id
        when (id) {
            R.id.btn_next -> {
                log.d("Btn btn_next Clicked")
                verifyImage()
            }

            R.id.btn_retry -> {
                log.d("Btn btn_retry Clicked")
                onRetryClicked()
                previewViewLayerMode(true)
            }

            R.id.btn_capture -> {
                takePhoto()
                log.d("Btn btn_capture Clicked")
            }

            R.id.tv_instruction -> {
                log.d("TV tv_instruction Clicked")
                onInfoBtnClicked()
            }
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        stopCamera()
//    }

    override fun onBackPressed() {
        if (!isCameraMode)
            onRetryClicked()
        else
            onBackBtnClicked()
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

    open fun onRetryClicked() {
        startCamera()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO // Needed or not
            ).apply {
//                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
//                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                }
            }.toTypedArray()
    }

    class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }


}
