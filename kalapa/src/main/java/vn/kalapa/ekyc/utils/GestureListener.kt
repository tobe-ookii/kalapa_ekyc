package vn.kalapa.ekyc.utils

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

 class GestureListener : GestureDetector.SimpleOnGestureListener() {
    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    // event when double tap occurs
    override fun onDoubleTap(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        Log.d("Double Tap", "Tapped at: ($x,$y)")
        return true
    }
}