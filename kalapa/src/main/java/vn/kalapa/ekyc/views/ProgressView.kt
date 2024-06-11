package vn.kalapa.ekyc.views

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import vn.kalapa.R
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.utils.Helpers

class ProgressView {
    enum class ProgressViewType {
        LOADING,
        SUCCESS,
        FACEOTP_SUCCESS,
        FAILED
    }

    companion object {
        private var startTime: Long = 0
        private var sDialog: Dialog? = null
        private var viewHolder: View? = null
        private var showing = false
        private var currentContext: Context? = null
        fun showProgress(context: Context, progressType: ProgressViewType? = ProgressViewType.LOADING, mainColor: String? = null, mainTextColor: String? = null, title: String? = null, message: String? = null) {
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
                showing = true
                sDialog = Dialog(context)
                sDialog?.setOnDismissListener { showing = false }
                sDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                sDialog?.setContentView(R.layout.kalapa_progress_view)
                sDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                sDialog?.window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                val gifImageView: KLPGifImageView? = sDialog?.findViewById(R.id.klp_gif_image_view)
                val progressBar =
                    sDialog?.findViewById<View>(R.id.progress_container_progressbar) as ProgressBar

                val textView = sDialog?.findViewById(R.id.progress_container_title) as TextView
                val body = sDialog?.findViewById(R.id.custom_dialog_body) as TextView

                val mainTextColor = mainTextColor ?: KalapaSDK.config.mainTextColor
                val mainColor = mainColor ?: KalapaSDK.config.mainColor
                Helpers.printLog("Title: $title - $message ")
                textView.text = title ?: KalapaSDK.config.languageUtils.getLanguageString(context.getString(R.string.klp_alert_title))
                body.text = message ?: KalapaSDK.config.languageUtils.getLanguageString(context.getString(R.string.klp_please_wait))

                textView.setTextColor(Color.parseColor(mainTextColor))
                body.setTextColor(Color.parseColor(mainTextColor))
                if (!title.isNullOrEmpty())
                    textView.text = title
                if (!message.isNullOrEmpty())
                    body.text = message
                if (progressType == ProgressViewType.LOADING || gifImageView == null) {
                    gifImageView?.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                    viewHolder = sDialog?.findViewById(R.id.container)
                    progressBar.indeterminateTintList =
                        ColorStateList.valueOf(Color.parseColor(mainColor))
                } else {
                    gifImageView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    gifImageView.setGifImageResource(
                        when (progressType) {
                            ProgressViewType.SUCCESS -> R.drawable.gif_success
                            ProgressViewType.FACEOTP_SUCCESS -> R.drawable.success_faceotp_2
                            else -> R.drawable.gif_error
                        }
                    )
                }

                sDialog?.setCancelable(false)
                Handler().postDelayed({ sDialog?.setCancelable(true) }, 5000)
                sDialog?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hideProgress(forceClose: Boolean = false) {
            try {
                showing = false
                if (sDialog != null && sDialog!!.isShowing) {
                    var distanceTime = System.currentTimeMillis() - startTime
                    if (distanceTime < 1000 && !forceClose) {
                        sDialog?.dismiss()
                        sDialog = null
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

