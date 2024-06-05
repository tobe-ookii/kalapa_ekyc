package vn.kalapa.ekyc.liveness

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import vn.kalapa.ekyc.liveness.models.*
import vn.kalapa.ekyc.managers.KLPFaceDetectorListener
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import java.util.Collections.max
import kotlin.random.Random

class InputFace(val inputTime: Long, val face: Face, val frameWidth: Int, val frameHeight: Int)
class LivenessSession(private var livenessSessionType: Common.LIVENESS_VERSION = Common.LIVENESS_VERSION.PASSIVE) {
    var sessionStatus: LivenessSessionStatus = LivenessSessionStatus.UNVERIFIED
    private val MAX_N_FRAME = 600
    var faceList = ArrayList<InputFace>()
    private var actionList = ArrayList<LivenessAction>()
    private var currActionIdx: Int = -1
    private var currAction: LivenessAction? = null
    private lateinit var index2Action: Map<Int, LivenessAction>
    private val TAG = "LivenessSession"
    lateinit var typicalFace: Bitmap
    lateinit var typicalFrame: Bitmap
    var gotTypicalFace = false
    private val faceDetectorOptions: FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.9f)
        .build()
    private var faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)

    fun renewSession(livenessSessionType: Common.LIVENESS_VERSION) {
        this.livenessSessionType = livenessSessionType
        this.sessionStatus = LivenessSessionStatus.UNVERIFIED
        gotTypicalFace = false
        refreshFaceList()
        genActionList()
    }

    private fun refreshFaceList(){
        faceList = ArrayList()
    }

    init {
        genActionList()
    }

    private fun genActionList() {
        // v1
//        actionList.add(HoldSteady2Seconds())
//        actionList.add(if (Random.nextInt(2) % 2 == 0) TurnLeft() else TurnRight())
//        actionList.add(if (Random.nextInt(2) % 2 == 0) LookUp() else LookDown())
//        currActionIdx = -1
        // v2
        Helpers.printLog("genActionList ${livenessSessionType.name} ")
        when (livenessSessionType) {
            Common.LIVENESS_VERSION.PASSIVE -> {
                index2Action = mapOf(
                    1 to HoldSteady2Seconds(2),
                    2 to Processing()
                )
            }

            Common.LIVENESS_VERSION.ACTIVE -> {
                index2Action = mapOf(
                    1 to HoldSteady2Seconds(1),
                    2 to if (Random.nextInt(2) % 2 == 0) TurnLeft() else TurnRight(),
                    3 to if (Random.nextInt(2) % 2 == 0) TurnUp() else TurnDown(),
                    4 to if (Random.nextInt(2) % 2 == 0) TiltLeft() else TiltRight(),
                    5 to HoldSteady2Seconds(2),
                    6 to Processing(),
                )
            }

            Common.LIVENESS_VERSION.SEMI_ACTIVE -> {
                index2Action = mapOf(
                    1 to HoldSteady2Seconds(1),
                    2 to GoFar(),
                    3 to ComeClose(),
                    4 to HoldSteady2Seconds(2),
                    5 to Processing(),
                )
            }
        }
        currActionIdx = 0
        currAction = null
    }

    private fun _process(face: Face, frameWidth: Int, frameHeight: Int) {
        this.faceList.add(InputFace(System.currentTimeMillis(), face, frameWidth, frameHeight))
        if (this.faceList.size > MAX_N_FRAME)
            sessionStatus = LivenessSessionStatus.EXPIRED
    }

    fun process(
        frame: Bitmap,
        cropImage: Bitmap,
        rotationAngle: Int,
        offset: Float,
        translationY: Float,
        faceDetectorListener: KLPFaceDetectorListener) {
        // Processing. Tối đa 60s 1 session.
        if (!isFinished())
            faceDetector.process(InputImage.fromBitmap(cropImage, rotationAngle))
                .addOnSuccessListener {
                    if (!isFinished()) {
                        if (it.size == 0) {  // No Face
                            sessionStatus = LivenessSessionStatus.NO_FACE
                        } else {
                            var faceIsSmallEnough = 0
                            for (face in it) {
                                val inputFace = InputFace(
                                    System.currentTimeMillis(),
                                    face,
                                    cropImage.width,
                                    cropImage.height
                                )
                                if (LivenessAction.isFaceSmallEnough(inputFace)) faceIsSmallEnough++
                            }
                            if (it.size > 1 && faceIsSmallEnough > 1) { // More than one face
                                sessionStatus = LivenessSessionStatus.TOO_MANY_FACES
                                refreshFaceList()
                            } else {
                                val face = it[0]
                                if (!LivenessAction.isFaceMarginRight(face, cropImage.width, cropImage.height, offset, translationY)) {
                                    sessionStatus = LivenessSessionStatus.OFF_CENTER
                                    refreshFaceList()
                                } else if (livenessSessionType != Common.LIVENESS_VERSION.SEMI_ACTIVE && LivenessAction.isFaceTooSmall(InputFace(System.currentTimeMillis(), face, cropImage.width, cropImage.height))) {
                                    sessionStatus = LivenessSessionStatus.TOO_SMALL
                                } else {
                                    _process(face, cropImage.width, cropImage.height)
                                    handleAction(frame, cropImage)
                                }
                            }
                        }
//                        if (currAction != null)
                        Helpers.printLog("$TAG Processing: ${currAction?.TAG} - Liveness Session Status $sessionStatus ${it.size} - faceList ${faceList.size}")
                        faceDetectorListener.onMessage(sessionStatus, currAction?.TAG)
                    }
                }
    }

    private fun addAction(nextIndex: Int, action: LivenessAction? = null) {
        currActionIdx += nextIndex
//        Helpers.printLog(
//            "$TAG addAction: nextIndex $nextIndex currActionIdx $currActionIdx " +
//                    "- max index2Action ${max(index2Action.keys)}- Action ${action?.TAG}" +
//                    " - index2Action[currActionIdx] ${index2Action[currActionIdx]}"
//        )
        if (currActionIdx > max(index2Action.keys)) { // Khi pass tất cả action
            sessionStatus = LivenessSessionStatus.VERIFIED
//            Helpers.printLog("$TAG Session verified successfully")
            return
        }
        if (nextIndex == 0) // Truyền thêm action vào.
            action?.let { actionList.add(it) }
        else {
            index2Action[currActionIdx]?.let {
                actionList.add(it)
//                Helpers.printLog("Next Action is ${it.TAG}")
            }
        }
//        Helpers.printLog("$TAG currentAction ${getCurrentAction()}")

    }

    private fun handleAction(frame: Bitmap, cropImage: Bitmap) {
        if (currActionIdx == 0)
            addAction(1)

        if (currActionIdx <= max(index2Action.keys)) {
            currAction = actionList[actionList.size - 1]
            val currActionStatus = currAction!!.process(this)
            if (currActionStatus == LivenessActionStatus.SUCCESS && (currAction?.TAG == "HoldSteady2Seconds" || currAction?.TAG == "ComeClose")) {
                if (!gotTypicalFace) {
                    Helpers.printLog("Found typical frame!")
                    typicalFrame = frame
                    typicalFace = cropImage
                }
                gotTypicalFace = true
            }
            when (currActionStatus) {
                LivenessActionStatus.SUCCESS -> if (currAction!!.isBreakAction) addAction(1) else addAction(
                    0,
                    Success()
                )

                LivenessActionStatus.TIMEOUT -> {
                    genActionList()
                }

                LivenessActionStatus.FAILED -> sessionStatus = LivenessSessionStatus.PROCESSING
            }
        }
    }


    fun getCurrentAction(): String {
//        Helpers.printLog("currActionIdx $currActionIdx - ${actionList.size}")
        return if (currActionIdx <= 0 || currActionIdx >= actionList.size) ""
        else actionList[actionList.size - 1].TAG
    }

    fun isFinished(): Boolean {
        return sessionStatus == LivenessSessionStatus.FAILED || sessionStatus == LivenessSessionStatus.VERIFIED
    }

    fun switchAction() {

    }

}

enum class LivenessSessionStatus(val status: Int) {
    UNVERIFIED(-1),
    PROCESSING(0),
    VERIFIED(1),
    EXPIRED(2),
    FAILED(3),
    TOO_SMALL(4),
    TOO_LARGE(5),
    OFF_CENTER(6),
    TOO_MANY_FACES(7),
    NO_FACE(8),
    EYE_CLOSED(9)
}
//val PROCESSING = 0
//val VERIFIED = 1
//val EXPIRED = 2
//val FAILED = 3
//val TOO_SMALL = 4
//val TOO_LARGE = 5
//val OFF_CENTER = 6
//val TOO_MANY_FACES = 7