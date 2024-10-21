package vn.kalapa.ekyc.liveness

import android.content.Context
import android.graphics.Bitmap
import vn.kalapa.ekyc.managers.KLPFaceDetectorListener
import vn.kalapa.ekyc.utils.Helpers
import vn.kalapa.ekyc.utils.LIVENESS_VERSION

class LivenessHandler(val context: Context, private val livenessSessionType: LIVENESS_VERSION, private val faceDetectorListener: KLPFaceDetectorListener, private val rotationAngle: Int = 0) {

    private val LIVENESS_MAX_TIME = 1 * 60 * 1000 // 1p
    var livenessSession = LivenessSession(livenessSessionType)
    var isStop = false
    private var startTime = System.currentTimeMillis()
    private var sessionStatus: LivenessSessionStatus = LivenessSessionStatus.UNVERIFIED
    var sessionAction: String = ""
    fun stop() {
        isStop = true
    }

    fun renewSession() {
        Helpers.printLog("on renewSession")
        livenessSession.renewSession(livenessSessionType)
        sessionStatus = livenessSession.sessionStatus
        startTime = System.currentTimeMillis()
    }

    fun processSession(frame: Bitmap, image: Bitmap) {
//        Helpers.printLog("isStop? : $isStop - livenessSession.isFinished() ${livenessSession.isFinished()} - Valid time ${System.currentTimeMillis() - startTime < LIVENESS_MAX_TIME} ")
        val isExpired = System.currentTimeMillis() - startTime > LIVENESS_MAX_TIME
        if (!isStop && !livenessSession.isFinished() && !isExpired) {
            livenessSession.process(frame, image, rotationAngle,faceDetectorListener)
            sessionStatus = livenessSession.sessionStatus
            sessionAction = livenessSession.getCurrentAction()
//            faceDetectorListener.onMessage(sessionStatus, sessionAction)
        } else {
            sessionStatus = livenessSession.sessionStatus
            sessionAction = livenessSession.getCurrentAction()
//            Helpers.printLog("isStop $isStop isFinished ${livenessSession.isFinished()} - Liveness Status $sessionStatus Or expired  ${System.currentTimeMillis() - startTime < LIVENESS_MAX_TIME}")
            // Stop / Finished
            // Return typical face and message (status)
            if (livenessSession.isFinished()) {
                if (sessionStatus == LivenessSessionStatus.VERIFIED) {
                    if (livenessSession.gotTypicalFace)
                        faceDetectorListener.onFaceDetected(livenessSession.typicalFrame, livenessSession.typicalFace)
                } else
                    faceDetectorListener.onFaceDetected(frame)
            } else if (sessionStatus == LivenessSessionStatus.EXPIRED || isExpired) {
                faceDetectorListener.onExpired()
            }
        }
    }
}
