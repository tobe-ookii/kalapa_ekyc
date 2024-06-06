package vn.kalapa.ekyc.utils

import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*


class BitmapUtil {
    companion object {

        fun getRealPathFromUri(uri: Uri, contentResolver: ContentResolver): String? {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    return it.getString(columnIndex)
                }
            }
            return null
        }

        fun convertBitmapToFile(context: Context, srcBmp: Bitmap): File {
            val wrapper = ContextWrapper(context)
            var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
            file = File(file, "${UUID.randomUUID()}.jpg")
            val stream: OutputStream = FileOutputStream(file)
            srcBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            return file
        }


        fun resizeBitmapToBitmap(srcBmp: Bitmap): Bitmap {
            val image = convertBitmapToBytes(srcBmp)//srcBmp.convertToByteArray()
            return BitmapFactory.decodeByteArray(image, 0, image.size)
        }

        fun convertBitmapToBytes(srcBmp: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            srcBmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            return stream.toByteArray()
        }

        fun compressBitmapToJpeg(bitmap: Bitmap, quality: Int): Bitmap {
            Helpers.printLog("Original Bitmap: ${bitmap.byteCount / 1024 / 1024f}")
            val byteArrayOutputStream = ByteArrayOutputStream()
            // Compress the bitmap as a JPEG with the specified quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val image = byteArrayOutputStream.toByteArray()
            return BitmapFactory.decodeByteArray(image, 0, image.size)
        }


        fun convertBase64ToBytes(base64: String): ByteArray {
            return Base64.decode(base64, Base64.DEFAULT)
        }

        fun Bitmap.convertToByteArray(): ByteArray {
            val startTime = System.currentTimeMillis()
            val stream = ByteArrayOutputStream()
            this.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()
            Helpers.printLog("Exceed time convertToByteArray ${System.currentTimeMillis() - startTime}")
            return bytes
        }

        //            val width = srcBmp.width
//            val height = srcBmp.height
//            val size: Int = srcBmp.rowBytes * srcBmp.height
//            val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
//            srcBmp.copyPixelsToBuffer(byteBuffer)
//            return byteBuffer.array()
        fun convertBitmapToBase64(srcBmp: Bitmap): String {
            val byteArray = srcBmp.convertToByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }

        @Throws(IllegalArgumentException::class)
        fun convert(base64Str: String): Bitmap? {
            val decodedBytes: ByteArray = Base64.decode(
                base64Str.substring(base64Str.indexOf(",") + 1),
                Base64.DEFAULT
            )
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }

        fun convert(bitmap: Bitmap): String? {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }

        fun crop(
            src: Bitmap, w: Int, h: Int,
            horizontalCenterPercent: Float, verticalCenterPercent: Float
        ): Bitmap {
            require(!(horizontalCenterPercent < 0 || horizontalCenterPercent > 1 || verticalCenterPercent < 0 || verticalCenterPercent > 1)) {
                ("horizontalCenterPercent and verticalCenterPercent must be between 0.0f and "
                        + "1.0f, inclusive.")
            }
            val srcWidth = src.width
            val srcHeight = src.height
            // exit early if no resize/crop needed
            if (w == srcWidth && h == srcHeight) {
                return src
            }
            val m = Matrix()
            val scale = Math.max(
                w.toFloat() / srcWidth,
                h.toFloat() / srcHeight
            )
            m.setScale(scale, scale)
            val srcCroppedW: Int
            val srcCroppedH: Int
            var srcX: Int
            var srcY: Int
            srcCroppedW = Math.round(w / scale)
            srcCroppedH = Math.round(h / scale)
            srcX = (srcWidth * horizontalCenterPercent - srcCroppedW / 2).toInt()
            srcY = (srcHeight * verticalCenterPercent - srcCroppedH / 2).toInt()
            // Nudge srcX and srcY to be within the bounds of src
            srcX = Math.max(Math.min(srcX, srcWidth - srcCroppedW), 0)
            srcY = Math.max(Math.min(srcY, srcHeight - srcCroppedH), 0)
            val cropped =
                Bitmap.createBitmap(src, srcX, srcY, srcCroppedW, srcCroppedH, m, true /* filter */)
            return cropped
        }

        fun base64ToBitmap(base64: String): Bitmap {
            val decodedBytes = Base64.decode(base64.replace("data:image/jpeg;base64,", ""), Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }

        fun dpToPx(dpi: Int, dp: Int): Float {
            return dp * dpi / 160f
        }

        fun pxToDp(px: Int, dpi: Int): Float {
            return px * 160f / dpi
        }

        fun rotateBitmapToStraight(srcBmp: Bitmap, cameraDegree: Int, mirrorFlipped: Boolean = false): Bitmap {
            Helpers.printLog("rotateBitmapToStraight $cameraDegree mirrorFlipped $mirrorFlipped")
            val matrix = Matrix()
            // Mirror is basically a rotation
            matrix.setScale(1f, if (mirrorFlipped) -1f else 1f)
            matrix.postRotate(cameraDegree.toFloat())
            return Bitmap.createBitmap(
                srcBmp,
                0,
                0,
                srcBmp.width,
                srcBmp.height,
                matrix,
                false
            )
        }
//        fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap? {
//            return try {
//                if (source.height >= source.width) {
//                    if (source.height <= maxLength) {
//                        return source
//                    }
//                    val aspectRatio = source.width.toDouble() / source.height.toDouble()
//                    val targetWidth = (maxLength * aspectRatio).toInt()
//                    Bitmap.createScaledBitmap(source, targetWidth, maxLength, true)
//                } else {
//                    if (source.width <= maxLength) {
//                        return source
//                    }
//                    val aspectRatio = source.height.toDouble() / source.width.toDouble()
//                    val targetHeight = (maxLength * aspectRatio).toInt()
//                    Bitmap.createScaledBitmap(source, maxLength, targetHeight, true)
//                }
//            } catch (e: Exception) {
//                source
//            }
//        }

        fun imageProxyToBitmap(image: ImageProxy): Bitmap {
            val matrix = Matrix()
            val planeProxy = image.planes[0]
            val buffer: ByteBuffer = planeProxy.buffer
            val bytes = ByteArray(buffer.remaining())
            val exifInterface = ExifInterface(ByteArrayInputStream(bytes))
            val orientation: Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            buffer.get(bytes)
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    matrix.postRotate(90f)
                    bitmap = Bitmap.createBitmap(
                        bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true
                    )
                }

                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    matrix.postRotate(180f)
                    bitmap = Bitmap.createBitmap(
                        bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true
                    )
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    matrix.postRotate(270f)
                    bitmap = Bitmap.createBitmap(
                        bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true
                    )
                }

                else -> {}
            }
            return bitmap
        }

    }
}