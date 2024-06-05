package vn.kalapa.demo.utils

import android.util.Log

internal class LogUtils {
    companion object {
        private const val TAG = "ExampleApp"
        fun printLog(vararg messages: Any) { Log.d(TAG, messages.joinToString(" ")) }
    }
}