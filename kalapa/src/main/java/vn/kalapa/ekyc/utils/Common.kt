package vn.kalapa.ekyc.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern


@ExperimentalUnsignedTypes
fun ByteArray.toHex2(): String = asUByteArray().joinToString("") { it.toString(radix = 16).padStart(2, '0') }

enum class LIVENESS_VERSION(val version: Int) {
    PASSIVE(1),
    SEMI_ACTIVE(2),
    ACTIVE(3)
}

enum class SCENARIO {
    REGISTER, UPGRADE, CUSTOM, NA;

    companion object {
        fun getScenarioFromName(scenario: String): SCENARIO {
            return when (scenario) {
                REGISTER.name -> REGISTER
                UPGRADE.name -> UPGRADE
                CUSTOM.name -> CUSTOM
                else -> NA
            }
        }
    }
}

enum class SCENARIO_PLAN {
    FROM_SESSION_ID, FROM_PROVIDED_DATA
}

object Common {

    fun byteToHex(bytes: ByteArray): String {
        return bytes.toHex2()
    }

    fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun getCpuInfo(): String {
        val cpuInfo = StringBuilder()
        try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { br ->
                var line: String?
                while ((br.readLine().also { line = it }) != null) {
                    cpuInfo.append(line).append("\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return cpuInfo.toString()
    }

    fun checkIfImageOrStorageIsGranted(activity: Activity, shouldRequestIfNot: Boolean): Boolean {
        val permission = if (Build.VERSION.SDK_INT > 32)
            android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldRequestIfNot) activity.requestPermissions(arrayOf(permission), 188)
            return false
        }
        return true
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    private fun isNumeric(toCheck: String): Boolean {
        return toCheck.toDoubleOrNull() != null
    }

    fun getIdCardNumberFromMRZ(input: String?): String? {
        if (!input.isNullOrEmpty()) {
            if (input.length == 12 && isNumeric(input)) {
                return input
            }
            if (input.contains("IDVNM")) {
                Helpers.printLog("getIdCardNumberFromMRZ: $input")
                val lines = if (input.contains("\\\\n")) input.split("\\\\n") else if (input.contains("\\n")) input.split("\\n") else input.split("\n")
                for (line in lines) {
                    Helpers.printLog(line)
                    if (line.length == 30 && line.contains("IDVNM") && line.contains("<<")) {
                        val pattern = Regex("IDVNM\\d+(\\d{12})<<(\\d)")
                        val match = pattern.find(line.replace("O", "0").replace("o", "0"))
                        if (match != null) {
                            try {
                                Helpers.printLog("getIdCardNumberFromMRZ ${match.groupValues}")
                                if (match.groupValues.size > 2) {
                                    val checkSum = calculateChecksum(match.groupValues[1])
                                    Helpers.printLog("getIdCardNumberFromMRZ $checkSum")
                                    if (checkSum == Integer.parseInt(match.groupValues[2]))
                                        return match.groupValues[1]
                                    else
                                        Helpers.printLog(" Checksum not OK $line")
//                                        KalapaSDK.config.loggingHandler?.log(KALAPA_LOG_LEVEL.ERROR, KALAPA_LOG_ACTION.MRZ_FAIL, "Common", mapOf("checksum" to line))
                                }
                            } catch (exception: Exception) {
                                exception.printStackTrace()
                            }
                        } else {
//                        Helpers.printLog("$TAG No match found.")
                        }
                    }
                }
            }
        }
        return if (input.isNullOrEmpty()) null else "-1"
    }


    private fun charValue(c: Char): Int {
        return when {
            c.isDigit() -> c.toString().toInt()
            c.isLetter() -> c.toInt() - 55 // A=10, B=11, ..., Z=35
            c == '<' -> 0
            else -> throw IllegalArgumentException("Invalid character in MRZ")
        }
    }

    fun calculateChecksum(s: String): Int {
        val weights = listOf(7, 3, 1)
        var total = 0
        for ((i, char) in s.withIndex()) {
            total += charValue(char) * weights[i % 3]
        }
        return total % 10
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
    fun getDynamicLanguage(baseUrl: String, language: String?): String? {
        if (language != null && language.isEmpty()) return null
        val url = if (language == null) URL("$baseUrl/api/language") else URL("$baseUrl/api/language?language=$language")
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
            } else {
                Helpers.printLog("GET request not worked")
            }
        }
        return body.ifEmpty { null }
    }
}