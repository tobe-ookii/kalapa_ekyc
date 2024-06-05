package vn.kalapa.ekyc.managers
//
import android.graphics.Bitmap
import com.google.mlkit.vision.face.*
import vn.kalapa.ekyc.liveness.LivenessSessionStatus

//
interface KLPFaceDetectorListener {
    fun onSuccessListener(faces: List<Face>)
    fun onMessage(status: LivenessSessionStatus?, message: String?)
    fun onFaceDetected(frame: Bitmap, face: Bitmap? = null)

    fun onExpired()

}


//enum class KLPLivenessAction {
//    TURN_LEFT, TURN_RIGHT,
//    TILT_LEFT, TILT_RIGHT,
//    TURN_UP, TURN_DOWN,
//    EYE_BLINK, SHAKE_HEAD, NOD_HEAD, SMILE,
//    MOVE_FAR_AWAY, MOVE_CLOSER,
//    HOLD_STEADY_2SECONDS
//}
//enum class KLPLivenessVersion(val version: Int) {
//    ACTIVE(1),
//    SEMI_ACTIVE(2),
//    PASSIVE(3)
//}

//class KLPFaceDetector(
//    val context: Context,
//    faceDetectorOptions: FaceDetectorOptions = FaceDetectorOptions.Builder()
//        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
////        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//        .setMinFaceSize(0.9f)
//        .build(),
//) {
//    private var faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)
//    private var faceDetectConfidence = 0
//    private var faceDetectorMessage = ""
//    fun process(image: Bitmap, cameraOrientation: Int, livenessVersion: KLPLivenessVersion = KLPLivenessVersion.PASSIVE, klpFaceDetectorListener: KLPFaceDetectorListener) {
//        faceDetector.process(InputImage.fromBitmap(image, cameraOrientation))
//            .addOnSuccessListener {
//                klpFaceDetectorListener.onSuccessListener(it)
//                if (it.size > 0) {
//                    if (it.size > 1) {
//                        // More than one face.
//                        Helpers.printLog("More than once face detected")
//                        faceDetectConfidence = 0
//                        faceDetectorMessage = context.getString(R.string.klp_liveness_too_many_faces)
//                        faceDetectConfidence--
//                        klpFaceDetectorListener.onMessage(null, faceDetectorMessage)
//                    } else {
//                        when (livenessVersion) {
//                            KLPLivenessVersion.PASSIVE -> processPassiveLivenessVersion(image, it[0], klpFaceDetectorListener)
//                            KLPLivenessVersion.SEMI_ACTIVE -> processSemiActiveLivenessVersion(image, it[0], klpFaceDetectorListener)
//                            KLPLivenessVersion.ACTIVE -> processActiveLivenessVersion(image, it[0], klpFaceDetectorListener)
//                        }
//                    }
//                } else {
//                    faceDetectConfidence--
//                    if (faceDetectConfidence < 0) faceDetectConfidence = 0
//                    klpFaceDetectorListener.onMessage(null, context.getString(R.string.klp_guild_liveness_not_face))
//                    Helpers.printLog("No face detected $cameraOrientation")
//                }
//            }
//            .addOnFailureListener {
//                // Exception
//                Helpers.printLog("Exception $it")
//                it.localizedMessage?.let { it1 -> klpFaceDetectorListener.onMessage(null, it1) }
//            }
//
//    }
//
//    private val actionList = listOf(KLPLivenessAction.TURN_RIGHT, KLPLivenessAction.TURN_LEFT, KLPLivenessAction.TURN_DOWN, KLPLivenessAction.TURN_UP, KLPLivenessAction.TILT_LEFT, KLPLivenessAction.TILT_RIGHT, KLPLivenessAction.SMILE)
//    var randomList = getRandomItemsFromList(actionList, 3)
//    var activeFaceBitmap: Bitmap? = null
//    var is1stActionValid = false
//    var is2ndActionValid = false
//    var is3rdActionValid = false
//
//    private fun processActiveLivenessVersion(image: Bitmap, face: Face, klpFaceDetectorListener: KLPFaceDetectorListener) {
//        val metrics = context.resources.displayMetrics
//        Helpers.printLog("Density ${metrics.density} ${metrics.densityDpi}")
//
//        // Nếu thành công 3 thì thành công ,Thất bại 1 thì làm lại từ đầu.
//        // Ở phần này cần mặt đủ to.
//        val bounds = face.boundingBox
//        val rotY = face.headEulerAngleY // Trái phải
//        val rotZ = face.headEulerAngleZ // Xa Gần
//        val rotX = face.headEulerAngleX // Trên dưới
//        val rightEyeLandmark: Float = if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) face.getLandmark(FaceLandmark.RIGHT_EYE)!!.position.x else 0f
//        val leftEyeLandmark: Float = if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) face.getLandmark(FaceLandmark.LEFT_EYE)!!.position.x else 0f
//        val eyeDistance = abs(rightEyeLandmark - leftEyeLandmark)
//
//        val headNotStraight = rotZ < -15 || rotZ > 15 || rotX < -15 || rotX > 15 || rotY < -15 || rotY > 15
//        val eyeClosed = face.rightEyeOpenProbability == null || face.rightEyeOpenProbability!! < 0.9f || face.leftEyeOpenProbability == null || face.leftEyeOpenProbability!! < 0.9f
//        val faceTooSmall = bounds.width() < 350 * metrics.density || bounds.height() < 350 * metrics.density
//        val faceTooBig = bounds.width() < 350 || bounds.height() < 350
//
//        if (faceDetectConfidence < 0) {
//            if (!headNotStraight && !eyeClosed && !faceTooSmall && !faceTooBig) {
//                faceDetectConfidence = 0
//                getRandomItemsFromList(actionList, 3)
//                activeFaceBitmap = image
//            } else {
//                activeFaceBitmap = null
//            }
//        } else if (randomList.isNotEmpty()) {
//            var currentAction = randomList[0] as KLPLivenessAction
//
//            // If detected
//            randomList.drop(0)
//        }
//
//        // After processing
//        if (faceDetectConfidence < 0) {
//
//        } else if (faceDetectConfidence == 0) {
//            // Start.
//        } else if (faceDetectConfidence > 0 && randomList.isEmpty()) {
//            klpFaceDetectorListener.onFaceDetected(image)
//        }
//    }
//
//
//    private fun processSemiActiveLivenessVersion(image: Bitmap, face: Face, klpFaceDetectorListener: KLPFaceDetectorListener) {
//        // Xa gần.
//        val metrics = context.resources.displayMetrics
//        val bounds = face.boundingBox
//        val rotY = face.headEulerAngleY // Trái phải
//        val rotZ = face.headEulerAngleZ // Xa Gần
//        val rotX = face.headEulerAngleX // Trên dưới
//        val rightEyeLandmark: Float = if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) face.getLandmark(FaceLandmark.RIGHT_EYE)!!.position.x else 0f
//        val leftEyeLandmark: Float = if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) face.getLandmark(FaceLandmark.LEFT_EYE)!!.position.x else 0f
//        val eyeDistance = abs(rightEyeLandmark - leftEyeLandmark)
//
//        val faceNotStraight = rotZ < -15 || rotZ > 15 || rotX < -15 || rotX > 15 || rotY < -15 || rotY > 15
//        val eyeIsClosed = face.rightEyeOpenProbability == null || face.rightEyeOpenProbability!! < 0.9f || face.leftEyeOpenProbability == null || face.leftEyeOpenProbability!! < 0.9f
//
//        // Không chấp nhận nhắm mắt, nghiêng đầu, ...
//        if (faceNotStraight) {
//            faceDetectorMessage = context.getString(R.string.klp_angle_not_correct)
//            faceDetectConfidence--
//        } else if (eyeIsClosed) {
//            Helpers.printLog("Eyes are closed ${face.rightEyeOpenProbability} left ${face.rightEyeOpenProbability}")
//            faceDetectorMessage = context.getString(R.string.klp_liveness_look_straight)
//            faceDetectConfidence--
//        } else {
//            val faceTooSmall = image.width * 1.0f / bounds.width() > 1.7f || image.height * 1.0f / bounds.height() > 1.7f
//            if (faceTooSmall) {
//                faceDetectorMessage = context.getString(R.string.klp_liveness_too_far)
//            } else {
//                faceDetectorMessage = ""
//
//            }
//        }
//
//        if (faceDetectorMessage == "") {
//            if (faceDetectConfidence < 0) faceDetectConfidence = 0
//            faceDetectConfidence++
//            if (faceDetectConfidence > 5) {
//                faceDetectConfidence = 0
//                klpFaceDetectorListener.onFaceDetected(image)
//            }
//        }
//        klpFaceDetectorListener.onMessage(null, faceDetectorMessage)
//
//    }
//
//    private fun processPassiveLivenessVersion(image: Bitmap, face: Face, klpFaceDetectorListener: KLPFaceDetectorListener) {
//        val metrics = context.resources.displayMetrics
//        Helpers.printLog("Density ${metrics.density} ${metrics.densityDpi}")
//        val bounds = face.boundingBox
//        val rotY = face.headEulerAngleY // Trái phải
//        val rotZ = face.headEulerAngleZ // Xa Gần
//        val rotX = face.headEulerAngleX // Trên dưới
//
//        val mouthLeftLandmark = face.getLandmark(FaceLandmark.MOUTH_LEFT)
//        val mouthRightLandmark = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
//        val rightEyeLandmark: Float = if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) face.getLandmark(FaceLandmark.RIGHT_EYE)!!.position.x else 0f
//        val leftEyeLandmark: Float = if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) face.getLandmark(FaceLandmark.LEFT_EYE)!!.position.x else 0f
//        val eyeDistance = abs(rightEyeLandmark - leftEyeLandmark)
//        Helpers.printLog("rotZ $rotZ rotY $rotY rotX $rotX Bounds: ${bounds.width()} ${bounds.height()} eyeDistance: $rightEyeLandmark $leftEyeLandmark $eyeDistance Mouth Landmark $mouthLeftLandmark $mouthRightLandmark")
//        Helpers.printLog("Face: Bounds: Width : ${bounds.width()} - ${image.width} Height: ${bounds.height()} - ${image.height} DpToPixel ${BitmapUtil.dpToPx(metrics.densityDpi, bounds.width())} PixelToDP ${BitmapUtil.pxToDp(300, metrics.densityDpi)}")
//
//        val faceNotStraight = rotZ < -15 || rotZ > 15 || rotX < -15 || rotX > 15 || rotY < -15 || rotY > 15
//        val eyeIsClosed = face.rightEyeOpenProbability == null || face.rightEyeOpenProbability!! < 0.9f || face.leftEyeOpenProbability == null || face.leftEyeOpenProbability!! < 0.9f
//        val faceTooSmall = image.width * 1.0f / bounds.width() > 2.0f //  || image.height * 1.0f / bounds.height() > 1.7f
////        val faceTooBig = bounds.height() * 1.0f / image.height > 0.95f // || image.height * 1.0f / bounds.height() < 0.85f
//
//        val width = if (image.width < image.height) image.width else image.height
//        val height = if (image.width < image.height) image.height else image.width
////        Helpers.printLog("Face: Bounds: Top ${bounds.top} Bottom ${bounds.bottom} Left ${bounds.left} Right ${bounds.right} Width ${bounds.width()} Height ${bounds.height()} \n Image w $width h $height")
//        val offsetMargin = 10 // Cách 4 cạnh 10
//        val tooNearToTheLeft = bounds.left < offsetMargin
//        val tooNearToTheRight = width - bounds.right < offsetMargin
//        val tooNearToTheTop = bounds.top < offsetMargin
//        val tooNearToTheBottom = (height - bounds.bottom) < offsetMargin
//        val isFaceMarginRight = !tooNearToTheTop && !tooNearToTheRight && !tooNearToTheBottom && !tooNearToTheLeft
//        if (faceNotStraight)
//            faceDetectorMessage = context.getString(R.string.klp_angle_not_correct)
//        else if (eyeIsClosed) {
//            Helpers.printLog("Eyes are closed ${face.rightEyeOpenProbability} left ${face.rightEyeOpenProbability}")
//            faceDetectorMessage = context.getString(R.string.klp_liveness_look_straight)
////        } else if (bounds.width() < 350 || bounds.height() < 350) {
//        } else if (faceTooSmall) {
//            faceDetectorMessage = context.getString(R.string.klp_liveness_too_far)
////        } else if (faceTooBig) {
////            faceDetectorMessage = context.getString(R.string.klp_liveness_too_large)
//        } else if (!isFaceMarginRight) {
//            Helpers.printLog("Face: Bounds: Density: ${metrics.density} DensityDPI: ${metrics.densityDpi}  isFaceMarginRight Top ${bounds.top} Bottom ${bounds.bottom} Left ${bounds.left} Right ${bounds.right} Width ${bounds.width()} Height ${bounds.height()} \n Image $width $height")
//            Helpers.printLog("Face: Bounds tooNearToTheLeft $tooNearToTheLeft tooNearToTheRight $tooNearToTheRight tooNearToTheTop $tooNearToTheTop tooNearToTheBottom $tooNearToTheBottom ")
//            faceDetectorMessage = context.getString(R.string.klp_liveness_too_close)
//        } else { // Seems good!
//            faceDetectorMessage = ""
//        }
//
//        if (faceDetectorMessage == "") {
//            if (faceDetectConfidence < 0) faceDetectConfidence = 0
//            faceDetectConfidence++
//            if (faceDetectConfidence > 5) {
//                faceDetectConfidence = 0
//                klpFaceDetectorListener.onFaceDetected(image)
//            }
//        } else faceDetectConfidence--
//        klpFaceDetectorListener.onMessage(null, faceDetectorMessage)
//    }
//
//}