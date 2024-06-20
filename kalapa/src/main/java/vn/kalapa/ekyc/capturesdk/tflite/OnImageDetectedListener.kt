package vn.kalapa.ekyc.capturesdk.tflite

interface OnImageDetectedListener {
    fun onImageDetected()
    fun onImageOutOfMask()
    fun onImageInMask()
}