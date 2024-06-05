package vn.kalapa.ekyc.views

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View


internal abstract class MaskView : View {

    protected var transparentPaint: Paint = Paint()
    protected var dashPaint: Paint = Paint()
    protected var cornerPaint: Paint = Paint()
    protected lateinit var metrics: DisplayMetrics

    var frame: RectF = RectF()
    protected var transparentFrame: RectF = RectF()
    protected var conner: Bitmap? = null
    protected var connerSize: Float = 0F

    var transOff: Float = 0F
        set(value) {
            field = value
            setFrame()
            invalidate()
        }
    var transOffY: Float = 0F
        set(value) {
            field = value
            setFrame()
            invalidate()
        }

    var frameWidth: Float = 0F
        set(value) {
            field = value
            invalidate()
        }

    var frameHeight: Float = 0F
        set(value) {
            field = value
            invalidate()
        }

    var maskColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    var dashColor: Int = 0
        set(value) {
            field = value
            dashPaint.color = value
            invalidate()
        }

    var strokeWidth: Float = 0F
        set(value) {
            field = value
            dashPaint.strokeWidth = value
            invalidate()
        }

    var cornerColor: Int = 0
        set(value) {
            field = value
            var filter: ColorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.DST_IN)
            if (cornerColor != -1) {
                filter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            }
            cornerPaint.colorFilter = filter
            invalidate()
        }

    var centerX: Float = 0F
        set(value) {
            field = value
            setFrame()
            invalidate()
        }

    var centerY: Float = 0F
        set(value) {
            field = value
            setFrame()
            invalidate()
        }


    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    )
            : super(context, attrs, defStyleAttr) {
        attrs.let {
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    )
            : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    abstract fun crop(image: Bitmap, cameraDegree: Int): Bitmap
    abstract fun setFrame()
}