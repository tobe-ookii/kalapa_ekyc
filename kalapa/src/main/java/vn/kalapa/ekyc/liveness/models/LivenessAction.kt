package vn.kalapa.ekyc.liveness.models

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import vn.kalapa.ekyc.liveness.InputFace
import vn.kalapa.ekyc.liveness.LivenessSession
import vn.kalapa.ekyc.utils.Helpers
import kotlin.math.max
import kotlin.math.min


enum class LivenessActionStatus {
    FAILED, SUCCESS, TIMEOUT
}

enum class LivenessFacePosition {
    LEFT, RIGHT, STRAIGHT, UP, DOWN, TILT_LEFT, TILT_RIGHT, NOT_DETERMINE
}


abstract class LivenessAction {
    lateinit var TAG: String
    val MAX_NO_FACE_FRAME = 10
    var startTime: Long = System.currentTimeMillis()
    lateinit var frame: Bitmap
    lateinit var cropFrame: Bitmap
    var isBreakAction = false
    var nSeconds = 2 // Time to success this challenge
    var currentActionStatus: LivenessActionStatus = LivenessActionStatus.FAILED
    var timeout = 30

    companion object x {
        fun isFaceTooSmall(face: InputFace): Boolean { // Khuôn mặt nằm trọn trong khung hình
            val trueWidth = min(face.frameWidth, face.frameHeight)
            val trueHeight = max(face.frameWidth, face.frameHeight)
            val bounds = face.face.boundingBox
            val isFaceTooSmall =
                bounds.width() < trueWidth * 0.3f || bounds.height() < trueHeight * 0.3f
//            if (!isFaceTooSmall) Helpers.printLog("isFaceTooSmall ${bounds.width()} < $trueWidth ${bounds.height()} < $trueHeight")
            return isFaceTooSmall
        }

        fun isFaceSmallEnough(face: InputFace): Boolean {
            val trueWidth = min(face.frameWidth, face.frameHeight)
            val trueHeight = max(face.frameWidth, face.frameHeight)
            val bounds = face.face.boundingBox
            val isFaceSmallEnough = bounds.width() < trueWidth * 0.4f || bounds.height() < trueHeight * 0.4f
//            if(!isFaceSmallEnough) Helpers.printLog("isFaceSmallEnough ${bounds.width()} < $trueWidth ${bounds.height()} < $trueHeight")
            return isFaceSmallEnough
        }

        fun isFaceBigEnough(face: InputFace): Boolean {
            val trueWidth = min(face.frameWidth, face.frameHeight)
            val trueHeight = max(face.frameWidth, face.frameHeight)
            val bounds = face.face.boundingBox
            val isFaceSmallEnough = bounds.width() > trueWidth * 0.5f || bounds.height() > trueHeight * 0.5f
//            if(!isFaceSmallEnough) Helpers.printLog("isFaceBigEnough ${bounds.width()} < $trueWidth ${bounds.height()} < $trueHeight")
            return isFaceSmallEnough
        }


        fun isFaceMarginRight(
            face: Face,
            frameWidth: Int,
            frameHeight: Int,
            offset: Float,
            translationY: Float
        ): Boolean {
            val bounds = face.boundingBox
            val isTooCloseToVertical = bounds.top <= 2 *  offset || bounds.bottom >= frameHeight - translationY - offset
            val isTooCloseToHorizontal = bounds.left <= offset || bounds.right >= frameWidth - offset
            val isFaceMarginRight = !isTooCloseToHorizontal && !isTooCloseToVertical
//            if (!isFaceMarginRight)
//                Helpers.printLog("isFaceMarginRight False Face: isTooCloseToHorizontal $isTooCloseToHorizontal isTooCloseToVertical $isTooCloseToVertical " +
//                        "\n offset: $offset $translationY Bounds: Top ${bounds.top} Bottom ${bounds.bottom} Left ${bounds.left} Right  ${bounds.right} \n Width ${bounds.width()} Height ${bounds.height()} \n Image w $trueWidth h $trueHeight")
//            else Helpers.printLog("isFaceMarginRight True Face: Bounds: Top ${bounds.top} Bottom ${bounds.bottom} Left ${bounds.left} Right  ${bounds.right} \n Width ${bounds.width()} Height ${bounds.height()} \n Image w $trueWidth h $trueHeight")
            return isFaceMarginRight
        }

    }

    fun getFacePosition(face1: Face, face2: Face): LivenessFacePosition {
        val rotY = face1.headEulerAngleY // Trái phải. Trái: 40. Phải -40
        val rotZ = face1.headEulerAngleZ // Nghiêng trái phải. Nghiêng trái: -30 Nghiêng phải 30
        val rotX = face1.headEulerAngleX // Trên dưới Trên >25. Dưới: -20
        val face1NotStraight =
            rotZ < -15 || rotZ > 15 || rotX < -15 || rotX > 15 || rotY < -15 || rotY > 15
        val rotY1 = face2.headEulerAngleY // Trái phải. Trái: 40. Phải -40
        val rotZ1 = face2.headEulerAngleZ // Nghiêng trái phải. Nghiêng trái: -30 Nghiêng phải 30
        val rotX1 = face2.headEulerAngleX // Trên dưới Trên >25. Dưới: -20
        val face2NotStraight =
            rotZ1 < -15 || rotZ1 > 15 || rotX1 < -15 || rotX1 > 15 || rotY1 < -15 || rotY1 > 15
//        Helpers.printLog("getFacePosition Face1: X $rotX Y $rotY Z $rotZ - Face2: X $rotX1 Y $rotY1 Z $rotZ1")
        return if (!face1NotStraight && !face2NotStraight) LivenessFacePosition.STRAIGHT
        else if (min(rotY, rotY1) > 15) LivenessFacePosition.LEFT
        else if (max(rotY, rotY1) < -15) LivenessFacePosition.RIGHT
        else if (min(rotX, rotX1) > 15) LivenessFacePosition.UP
        else if (max(rotX, rotX1) < -15) LivenessFacePosition.DOWN
        else if (min(rotZ, rotZ1) > 25) LivenessFacePosition.TILT_RIGHT
        else if (max(rotZ, rotZ1) < -25) LivenessFacePosition.TILT_LEFT
        else return LivenessFacePosition.NOT_DETERMINE
    }


    fun getFacePosition(face1: Face): LivenessFacePosition {
        val rotY = face1.headEulerAngleY // Trái phải. Trái: 40. Phải -40
        val rotZ = face1.headEulerAngleZ // Nghiêng trái phải. Nghiêng trái: -30 Nghiêng phải 30
        val rotX = face1.headEulerAngleX // Trên dưới Trên >25. Dưới: -20
        val face1NotStraight =
            rotZ < -15 || rotZ > 15 || rotX < -15 || rotX > 15 || rotY < -15 || rotY > 15
//        Helpers.printLog("getFacePosition Face1: X $rotX Y $rotY Z $rotZ")
        val facePosition = if (!face1NotStraight) LivenessFacePosition.STRAIGHT
        else if (rotY > 25) LivenessFacePosition.LEFT
        else if (rotY < -25) LivenessFacePosition.RIGHT
        else if (rotX > 15) LivenessFacePosition.UP
        else if (rotX < -15) LivenessFacePosition.DOWN
        else if (rotZ > 25) LivenessFacePosition.TILT_RIGHT
        else if (rotZ < -25) LivenessFacePosition.TILT_LEFT
        else LivenessFacePosition.NOT_DETERMINE
//        Helpers.printLog("Face Position: $facePosition")
        return facePosition
    }

    fun isFaceLookStraight(face: Face): Boolean {
        val rotY = face.headEulerAngleY // Trái phải
        val rotZ = face.headEulerAngleZ // Xa Gần
        val rotX = face.headEulerAngleX // Trên dưới
        val faceNotStraight =
            rotZ < -15 || rotZ > 15 || rotX < -15 || rotX > 15 || rotY < -15 || rotY > 15
        return !faceNotStraight
    }

    private fun isEyesClosed(face: Face): Boolean {
        return face.rightEyeOpenProbability == null || face.rightEyeOpenProbability!! < 0.3f || face.leftEyeOpenProbability == null || face.leftEyeOpenProbability!! < 0.3f
    }


    abstract fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus

    fun process(session: LivenessSession): LivenessActionStatus {
        if (isBreakAction) {
//            Helpers.printLog("${this.javaClass.kotlin.simpleName} is Break Action. Returning Success")
            return LivenessActionStatus.SUCCESS
        }
        if (isNotEnoughFrame()) {
//            Helpers.printLog("${this.javaClass.kotlin.simpleName} is not enough frame...")
            currentActionStatus = LivenessActionStatus.FAILED
            return currentActionStatus
        }
        if (System.currentTimeMillis() - startTime > timeout * 1000) {
            currentActionStatus = LivenessActionStatus.TIMEOUT
            return currentActionStatus
        }
        val faces: List<InputFace> = getFacesByTime(session, nSeconds)
        if (faces.size < 2) {
            return if (nSeconds == 0 && faces.size == 1) {
                currentActionStatus = individualProcess(session, faces)
                currentActionStatus
            } else {
//                Helpers.printLog("${this.javaClass.kotlin.simpleName} is not enough frame to process... nSeconds $nSeconds face.size ${faces.size}")
                currentActionStatus = LivenessActionStatus.FAILED
                currentActionStatus
            }
        } else {
            if (this.TAG == "HoldSteady2Seconds" && (isEyesClosed(faces[faces.size - 1].face) || isEyesClosed(faces[0].face))) { // Nếu đang ở HoldSteady và 1 trong 2 mắt nhắm th return false.
                currentActionStatus = LivenessActionStatus.FAILED
                return currentActionStatus
            } else {
                Helpers.printLog("${this.javaClass.kotlin.simpleName} is processing... nSeconds $nSeconds face.size ${faces.size}")
                currentActionStatus = individualProcess(session, faces)
                return currentActionStatus
            }

        }

    }

    private fun isNotEnoughFrame(): Boolean {
//        Helpers.printLog("LivenessSession ${this.javaClass.simpleName} $startTime - $nSeconds isNotEnoughFrame $isNotEnoughFrame")
        return nSeconds != 0 && (System.currentTimeMillis() - startTime < nSeconds * 1000)
    }

    private fun getFacesByTime(session: LivenessSession, seconds: Int): List<InputFace> {
        val timeInMillis = seconds * 1000
        var from = 0
        if (seconds == 0)
            return listOf(session.faceList[session.faceList.size - 1])
        else {
            if (session.faceList[session.faceList.size - 1].inputTime - session.faceList[0].inputTime > timeInMillis) {
                session.faceList.removeAll { session.faceList[session.faceList.size - 1].inputTime - it.inputTime >= timeInMillis }
                return session.faceList
            } else return listOf()
        }
    }
}

class HoldSteady2Seconds(seconds: Int?) : LivenessAction() {

    init {
        TAG = "HoldSteady2Seconds"
        if (seconds != null) nSeconds = seconds
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession HoldSteady2Seconds is processing nSeconds $nSeconds ${faces.size}")
        val face1 = faces[0]
        val face2 = faces[faces.size - 1]
        if (isFaceLookStraight(face1.face) && isFaceLookStraight(face2.face)) {
            return LivenessActionStatus.SUCCESS
        }
//        else Helpers.printLog("LivenessSession HoldSteady2Seconds not straight!")
        return LivenessActionStatus.FAILED
    }
}

class ShakeHead : LivenessAction() {
    init {
        TAG = "ShakeHead"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession ${this.javaClass.simpleName} is processing")
        return LivenessActionStatus.FAILED
    }
}

class TurnLeft : LivenessAction() {

    init {
        nSeconds = 0
        TAG = "TurnLeft"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession ${this.javaClass.simpleName} is processing ${faces.size}")
        val face1 = faces[0]
        if (getFacePosition(face1.face) == LivenessFacePosition.LEFT) {
            return LivenessActionStatus.SUCCESS
        }
        return LivenessActionStatus.FAILED
    }
}

class TurnRight : LivenessAction() {

    init {
        nSeconds = 0
        TAG = "TurnRight"

    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
        val face1 = faces[faces.size - 1]
//        val face2 = faces[faces.size - 1]
        val facePosition = getFacePosition(face1.face)
        if (facePosition == LivenessFacePosition.RIGHT) {
            return LivenessActionStatus.SUCCESS
        }
        return LivenessActionStatus.FAILED
    }
}

class TurnDown : LivenessAction() {

    init {
        nSeconds = 0
        TAG = "TurnDown"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession $TAG is processing ${faces.size}")
        val face1 = faces[0]
//        val face2 = faces[faces.size - 1]
//        val facePosition = getFacePosition(face1.face, face2.face)
        val facePosition = getFacePosition(face1.face)
//        Helpers.printLog("getFacePosition Face1: $facePosition")
        if (facePosition == LivenessFacePosition.DOWN) {
            return LivenessActionStatus.SUCCESS
        }
        return LivenessActionStatus.FAILED
    }
}

class TurnUp : LivenessAction() {
    init {
        nSeconds = 0
        TAG = "TurnUp"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession $TAG is processing ${faces.size}")
        val face1 = faces[0]
//        val face2 = faces[faces.size - 1]
//        val facePosition = getFacePosition(face1.face, face2.face)
        val facePosition = getFacePosition(face1.face)
        Helpers.printLog("getFacePosition Face1: $facePosition")
        if (facePosition == LivenessFacePosition.UP) {
            return LivenessActionStatus.SUCCESS
        }
        return LivenessActionStatus.FAILED
    }
}

class TiltRight : LivenessAction() {

    init {
        nSeconds = 0
        TAG = "TiltRight"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession $TAG is processing ${faces.size}")
        val face1 = faces[0]
//        val face2 = faces[faces.size - 1]
//        val facePosition = getFacePosition(face1.face, face2.face)
        val facePosition = getFacePosition(face1.face)
        Helpers.printLog("getFacePosition Face1: $facePosition")
        if (facePosition == LivenessFacePosition.TILT_RIGHT) {
            return LivenessActionStatus.SUCCESS
        }
        return LivenessActionStatus.FAILED
    }
}

class TiltLeft : LivenessAction() {

    init {
        nSeconds = 0
        TAG = "TiltLeft"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession $TAG is processing ${faces.size}")
        val face1 = faces[0]
//        val face2 = faces[faces.size - 1]
//        val facePosition = getFacePosition(face1.face, face2.face)
        val facePosition = getFacePosition(face1.face)
        Helpers.printLog("getFacePosition Face1: $facePosition")
        if (facePosition == LivenessFacePosition.TILT_LEFT) {
            return LivenessActionStatus.SUCCESS
        }
        return LivenessActionStatus.FAILED
    }
}

class ComeClose : LivenessAction() {
    init {
        TAG = "ComeClose"
        nSeconds = 1
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession ComeClose is processing nSeconds $nSeconds ${faces.size}")
        val face1 = faces[0]
        val face2 = faces[faces.size - 1]
        if (isFaceBigEnough(face1) && isFaceBigEnough(face2))
            return LivenessActionStatus.SUCCESS
        return LivenessActionStatus.FAILED
    }
}

class GoFar : LivenessAction() {
    init {
        TAG = "GoFar"
        nSeconds = 1
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
//        Helpers.printLog("LivenessSession $TAG is processing ${faces.size}")
        val face1 = faces[0]
        val face2 = faces[faces.size - 1]
        val faceIsFarEnough = isFaceSmallEnough(face1) && isFaceSmallEnough(face2)
        if (faceIsFarEnough) {
            Helpers.printLog("$TAG faceIsFarEnough")
            return LivenessActionStatus.SUCCESS
        } else {
            Helpers.printLog(
                "$TAG faceIsFarEnough Face1: ${isFaceSmallEnough(face1)} Face2: ${
                    isFaceSmallEnough(face2)
                }"
            )
        }
        return LivenessActionStatus.FAILED
    }
}

open class BreakAction : LivenessAction() {
    init {
        isBreakAction = true
        TAG = "BreakAction"
    }

    override fun individualProcess(
        session: LivenessSession,
        faces: List<InputFace>
    ): LivenessActionStatus {
        Helpers.printLog("LivenessSession ${this.javaClass.simpleName} on default process SUCCESS")
        return LivenessActionStatus.SUCCESS
    }

}

class Success : BreakAction() {
    init {
        TAG = "Success"
    }
}

class NotFollow : BreakAction() {   init {
    TAG = "NotFollow"
}
}

class Processing : BreakAction() {   init {
    TAG = "Processing"
}
}

class Timeout : BreakAction() {   init {
    TAG = "Timeout"
}
}
