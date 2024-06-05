package vn.kalapa.ekyc.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.net.Uri
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.io.FileNotFoundException
import java.io.InputStream

class KLPGifImageView : View {
    private var mInputStream: InputStream? = null
    private lateinit var mMovie: Movie
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mStart: Long = 0
    private var mContext: Context
    private var leftAdjustment = 0
    private var topAdjustment = 0

    constructor(context: Context) : super(context) {
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mContext = context
        if (attrs.getAttributeName(1).equals("background")) {
            val id: Int = attrs.getAttributeValue(1).substring(1).toInt()
            setGifImageResource(id)
        }
    }

    private fun init() {
        isFocusable = true
        mMovie = Movie.decodeStream(mInputStream)
        mWidth = mMovie.width()
        mHeight = mMovie.height()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(mWidth, mHeight)
//        val videoWidth = measuredWidth
//        val videoHeight = measuredHeight
//
//        val viewWidth = getDefaultSize(0, widthMeasureSpec)
//        val viewHeight = getDefaultSize(0, heightMeasureSpec)
//
//
//        if (videoWidth == viewWidth) {
//            val newWidth = (videoWidth.toFloat() / videoHeight * viewHeight).toInt()
//            setMeasuredDimension(newWidth, viewHeight)
//            leftAdjustment = -(newWidth - viewWidth) / 2
//        } else {
//            val newHeight = (videoHeight.toFloat() / videoWidth * viewWidth).toInt()
//            setMeasuredDimension(viewWidth, newHeight)
//            topAdjustment = -(newHeight - viewHeight) / 2
//        }
    }

//    override fun layout(l: Int, t: Int, r: Int, b: Int) {
//        super.layout(l + leftAdjustment, t + topAdjustment, r + leftAdjustment, b + topAdjustment)
//    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        if (mStart == 0L) {
            mStart = now
        }
        if (this::mMovie.isInitialized) {
            var duration = mMovie.duration()
            if (duration == 0) {
                duration = 1000
            }
            val relTime = ((now - mStart) % duration).toInt()
            mMovie.setTime(relTime)
            mMovie.draw(canvas, 0f, 0f)
            invalidate()
        }
    }

    fun setGifImageResource(id: Int) {
        mInputStream = mContext.resources.openRawResource(id)
        this.visibility = VISIBLE
        init()
    }

    fun setGifImageUri(uri: Uri) {
        try {
            mInputStream = mContext.contentResolver.openInputStream(uri)
            init()
        } catch (e: FileNotFoundException) {
            Log.e("GIfImageView", "File not found")
        }
    }
}