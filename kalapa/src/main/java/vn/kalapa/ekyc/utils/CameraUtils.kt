package vn.kalapa.ekyc.utils

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import kotlin.math.atan

class CameraUtils {
    companion object {
        fun getCameraFov(cameraInfo: CameraInfo): Float {
            // Extract the camera ID
            val cameraCharacteristics = Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)

            val focalLengths =
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val maxFocalLength = focalLengths?.maxOrNull() ?: 0.0f
            val minFocalLength = focalLengths?.minOrNull() ?: 0.0f

            val sensorSize =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val sensorWidth = sensorSize?.width ?: 0.0f
            val sensorHeight = sensorSize?.height ?: 0.0f
            Helpers.printLog("cameraInfo maxFocalLength $maxFocalLength minFocalLength $minFocalLength sensorWidth $sensorWidth sensorHeight $sensorHeight ")
            val FOV = 2 * atan(sensorWidth / (2 * maxFocalLength))
            val FOVinDegree = FOV * 180 / 3.14f
            Helpers.printLog("cameraInfo FOV: $FOV $FOVinDegree")
            return FOVinDegree
        }
    }
}