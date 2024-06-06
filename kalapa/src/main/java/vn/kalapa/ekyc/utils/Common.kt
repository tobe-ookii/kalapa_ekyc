package vn.kalapa.ekyc.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcManager
import android.util.Log
import vn.kalapa.ekyc.KalapaSDK
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

@ExperimentalUnsignedTypes
fun ByteArray.toHex2(): String = asUByteArray().joinToString("") { it.toString(radix = 16).padStart(2, '0') }

class Common {
    enum class LIVENESS_VERSION(val version: Int) {
        PASSIVE(1),
        SEMI_ACTIVE(2),
        ACTIVE(3)
    }

//    enum class COMPARE_TYPE(val compareType: Int) {
//        EKYC_FACE(1),
//        NFC_FACE(2)
//    }


    companion object {
        val MY_PREFERENCES = "KLP_FACE_OTP_PREF"
        val MY_KEY_TOKEN = "KLP_EKYC_KEY_TOKEN"
        val MY_KEY_LANGUAGE = "KLP_EKYC_KEY_LANGUAGE"
        val MY_KEY_LIVENESS_VERSION = "KLP_EKYC_KEY_LIVENESS_VERSION"
        val MY_KEY_MAIN_COLOR = "KLP_EKYC_KEY_MAIN_COLOR"
        val MY_KEY_BTN_TEXT_COLOR = "KLP_EKYC_KEY_BTN_TEXT_COLOR"
        val MY_KEY_MAIN_TEXT_COLOR = "KLP_EKYC_KEY_MAIN_TEXT_COLOR"
        val MY_KEY_BACKGROUND_COLOR = "KLP_EKYC_KEY_BACKGROUND_COLOR"
        val MY_KEY_SCENARIO = "KLP_EKYC_KEY_SCENARIO"
        val MY_KEY_ENV = "KLP_EKYC_KEY_ENVIRONMENT"

        fun byteToHex(bytes: ByteArray): String {
            return bytes.toHex2()
        }

        fun isOnline(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager != null) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }
            return false
        }

        fun parseDate(dateStr: String): Date? {
            Helpers.printLog("DateStr $dateStr")
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy")
                val date = formatter.parse(dateStr)
                return date
            } catch (e: Exception) {
                return null
            }
        }

        fun nfcAvailable(context: Context): Boolean {
            val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
            val adapter = manager.defaultAdapter
            if (adapter != null && adapter.isEnabled) {
                // adapter exists and is enabled.
                return true
            }
            return false
        }

        fun isColorMatchedFormat(hexStr: String): Boolean {
            try {
                val PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"
                val pattern: Pattern = Pattern.compile(PATTERN)
                val matcher: Matcher = pattern.matcher(hexStr)
                return matcher.matches()
            } catch (e: Exception) {
                return false
            }
        }

        fun isCharacterHexa(c: Char): Boolean {
            if (c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f')
                return true
            return false
        }


        fun getRandomItemsFromList(list: List<Any>, size: Int): List<Any> {
            return if (size > list.size)
                emptyList()
            else {
                val shuffleList = list.shuffled()
                var randomList = ArrayList<Any>()
                for (i in (0..size)) {
                    randomList.add(shuffleList[i])
                }
                list
            }
        }

        fun getIdCardNumberFromMRZ(input: String?): String? {
            if (!input.isNullOrEmpty()) {
                if (input.length == 12) return input
                if (input.contains("IDVNM")) {
                    val lines = input.split("\n")
                    for (line in lines) {
                        if (line.length == 30 && line.contains("IDVNM") && line.contains("<<")) {
                            val pattern = Regex("IDVNM\\d+(\\d{12})<<\\d")
                            val match = pattern.find(line.replace("O", "0").replace("o", "0"))
                            if (match != null) {
                                return match.groupValues[1]
                            } else {
//                        Helpers.printLog("$TAG No match found.")
                            }
                        }
                    }
                }
            }
            return null
        }

        private fun String.md5(): String {
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
        }

        fun getModelPosition(model: String): String? {
            val url = URL("https://api-ekyc.kalapa.vn/face-otp/get-nfc-location")
            val signature = "${model}Kalapa@2024".md5()
            val params = "model=$model&signature=$signature"
            Helpers.printLog("Model $model: Signature $signature")
            var body = ""
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                doOutput = true
                val writer = OutputStreamWriter(outputStream)
                writer.write(params)
                writer.flush()

                println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        body += line
                    }
                }
            }
            return body.ifEmpty { null }
        }

        fun getDynamicLanguage(baseUrl: String, language: String): String? {
            if (language.isEmpty()) return null
            //prod:  api-ekyc.kalapa.vn/face-otp
            //dev: faceotp-dev.kalapa.vn/api
            val url = URL("$baseUrl/lang/get?language=$language")
            var body = ""
            Helpers.printLog("\nSent 'GET' request to URL : $url")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connect()
                Helpers.printLog("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    body = reader.readText()
                    reader.close()

//                    Helpers.printLog("Response Code: $responseCode")
//                    Helpers.printLog("Response Message: $responseMessage")
//                    Helpers.printLog("Response Body: $body")
                } else {
                    Helpers.printLog("GET request not worked")
                }
            }
            return body.ifEmpty { null }
        }

    }
}