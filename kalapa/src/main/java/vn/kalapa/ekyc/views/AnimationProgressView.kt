package vn.kalapa.ekyc.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import vn.kalapa.R
import vn.kalapa.ekyc.utils.Helpers

class AnimationProgressView {

    companion object {
        private var startTime: Long = 0
        private var sDialog: Dialog? = null
        private var viewHolder: View? = null
        private var showing = false
        private var currentContext: Context? = null
        private var gifAnimation: KLPGifImageView? = null
        fun showProgress(context: Context, animDrawable: Int) {
            try {
                if (currentContext != context) {
                    sDialog?.dismiss()
                    sDialog = null
                    currentContext = context
                } else {
                    if (showing) {
                        Log.d("ProgressView", "One instance is showing...")
                        hideProgress()
                    }
                }
                startTime = System.currentTimeMillis()
//                val mainTextColor = Kalapa.SDKConfig.mainTextColor
//                val mainColor = Kalapa.SDKConfig.mainColor
                showing = true
                sDialog = Dialog(context)
                sDialog?.setOnDismissListener { showing = false }
                sDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                sDialog?.setContentView(R.layout.animation_progress)
                gifAnimation = sDialog?.findViewById(R.id.gif_animation)
                if (gifAnimation != null) {
                    gifAnimation?.setGifImageResource(animDrawable)
                }
                sDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                sDialog?.setCancelable(true)
                sDialog?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun transformAnimation(animDrawable: Int) {
            if (sDialog != null && sDialog!!.isShowing) {
                gifAnimation?.setGifImageResource(animDrawable)
            }
        }

        fun hideProgress(forceClose: Boolean = false) {
            try {
                showing = false
                if (sDialog != null && sDialog!!.isShowing) {
                    var distanceTime = System.currentTimeMillis() - startTime
                    if (distanceTime < 1000 && !forceClose) {
                        Handler().postDelayed({
                            sDialog?.dismiss()
                            sDialog = null
                        }, 1000 - distanceTime)
                    } else {
                        sDialog?.dismiss()
                        sDialog = null
                    }
                }
            } catch (e: Exception) {
                Helpers.printLog("Ignore hide progress exception")
            }

        }
    }
}

