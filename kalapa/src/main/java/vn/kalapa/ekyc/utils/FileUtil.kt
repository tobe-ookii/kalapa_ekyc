package vn.kalapa.ekyc.utils


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import okhttp3.RequestBody
import okio.Buffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * FaceTracker
 * Created by taint on 05/16/2019.
 */
 object FileUtil {
    private val TAG = FileUtil::class.java.name
    const val FILE_FRONT_CARD = "card_image.jpg"
    const val FILE_TEMP = "temp.jpg"
    const val FILE_BACK_CARD = "card_back_image.jpg"
    const val FILE_FACE = "face_image.jpg"
    const val faceFileNameCloseEyes = "face_image_close_eyes.jpg"
    const val faceFileNameRight = "face_image_right.jpg"
    const val faceFileNameLeft = "face_image_left.jpg"
    fun saveFile(context: Context, bitmap: Bitmap, fileName: String?) {
        if (fileName == null || fileName.isEmpty()) return
        val savedPhoto = File(context.cacheDir, fileName)
        Log.d(
            TAG,
            String.format(
                "Image Save File: %s - w:%s - h:%s",
                fileName,
                bitmap.width,
                bitmap.height
            )
        )
        try {
            val outputStream = FileOutputStream(savedPhoto.path)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            //outputStream.write(capturedImage);
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun getCardFile(context: Context): File {
        return File(context.cacheDir, FILE_FRONT_CARD)
    }

    fun getTempFile(context: Context): File {
        return File(context.cacheDir, FILE_TEMP)
    }

//    fun hash(input: RequestBody): String {
//        val buffer = Buffer()
//        input.writeTo(buffer)
//        val bodyString = buffer.readUtf8()
//        val bodyBytes = bodyString.toByteArray() // convert string to bytes
//
//        val md = MessageDigest.getInstance("SHA-256")
//        val digest = md.digest(bodyBytes)
//        return digest.fold("") { str, it -> str + "%02x".format(it) }
//    }

    fun hash(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun hash(input: RequestBody): String {
        val buffer = Buffer()
        input.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        val bodyBytes = bodyString.toByteArray() // convert string to bytes

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bodyBytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun getFaceFile(context: Context): File {
        return File(context.cacheDir, FILE_FACE)
    }

    fun getFaceFileCloseEyes(context: Context): File {
        return File(context.cacheDir, faceFileNameCloseEyes)
    }

    fun getFaceFileRight(context: Context): File {
        return File(context.cacheDir, faceFileNameRight)
    }

    fun getFaceFileLeft(context: Context): File {
        return File(context.cacheDir, faceFileNameLeft)
    }

    fun getCardBackFile(context: Context): File {
        return File(context.cacheDir, FILE_BACK_CARD)
    }
}
