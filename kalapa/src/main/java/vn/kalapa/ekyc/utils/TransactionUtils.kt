package vn.kalapa.ekyc.utils

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

class TransactionUtils {
    companion object {
        fun compressBase64ForTransaction(base64: String): String {
            val compressedData = compressUntilLimit(base64)
            Helpers.printLog("compressedData")
            return compressedData ?: base64
        }

        // Check if the data exceeds the transaction limit
        private fun exceedsTransactionLimit(data: String, limitInBytes: Int = 512 * 1024): Boolean {
            val byteArray = data.toByteArray(Charsets.UTF_8)
            return byteArray.size > limitInBytes
        }

        // Compress the string using GZIP
        @Throws(IOException::class)
        private fun compress(data: String): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
            gzipOutputStream.write(data.toByteArray(Charsets.UTF_8))
            gzipOutputStream.close()
            return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
        }

        // Function to compress by 90% until it satisfies the limit
        private fun compressUntilLimit(base64String: String, limitInBytes: Int = 512 * 1024): String? {
            var compressedString = base64String

            // Keep compressing while the data exceeds the transaction limit
            while (exceedsTransactionLimit(compressedString, limitInBytes)) {
                try {
                    compressedString = compress(compressedString)

                    // Check if it's compressed enough by 90%
                    val byteArray = compressedString.toByteArray(Charsets.UTF_8)
                    if (byteArray.size <= limitInBytes) {
                        return compressedString // Return compressed result if within limit
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }
            }
            return compressedString // Return final string after compression
        }

    }
}