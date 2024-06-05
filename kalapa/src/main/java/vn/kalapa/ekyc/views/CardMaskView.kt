package vn.kalapa.ekyc.views

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import vn.kalapa.R
import vn.kalapa.ekyc.KalapaSDK
import vn.kalapa.ekyc.utils.BitmapUtil
import vn.kalapa.ekyc.utils.Helpers
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class CardMaskView : MaskView {
    private var roundRadius: Float = 0F
    private var ovalTop: Float = 0F
    private var ovalFrameTop: Float = 0F
    private val foldOpenConst = 1.5f
    private val foldClosedConst = 0.8f

    companion object {
        val ID_CARD_WIDTH_SIZE = 860
        val ID_CARD_HEIGHT_SIZE = 540
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    )
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    )
            : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    var normalWidth = 0f
    var normalHeight = 0f
    var smallWidth = 0f
    var smallHeight = 0f
    var isLivenessMode = false

    @SuppressLint("Recycle")
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)

        attrs?.let {
            metrics = resources.displayMetrics
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.CardMaskView,
                defStyleAttr,
                defStyleRes
            )

            isLivenessMode =
                typedArray.getBoolean(R.styleable.CardMaskView_liveness_mode, false)

            maskColor = typedArray.getInt(R.styleable.CardMaskView_maskColor, R.color.maskColor)
            dashColor = typedArray.getInt(R.styleable.CardMaskView_dashColor, R.color.colorPrimary)


            centerX =
                typedArray.getDimension(R.styleable.CardMaskView_centerX, metrics.widthPixels / 2F)
            centerY =
                typedArray.getDimension(R.styleable.CardMaskView_centerY, metrics.heightPixels / 2F)

            transparentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

            dashPaint.style = Paint.Style.STROKE
            dashPaint.color =
                typedArray.getInt(R.styleable.CardMaskView_dashColor, R.color.colorPrimary)
            dashPaint.strokeWidth = typedArray.getDimension(
                R.styleable.FaceDetectionView_strokeWidth,
                2 * metrics.density
            )
            dashPaint.pathEffect = DashPathEffect(floatArrayOf(20F, 20F), 0F)

            val padding = 18F * metrics.density
            var w: Float =
                if (isLivenessMode) metrics.widthPixels / 5 * 4F else metrics.widthPixels * 1F - 2 * padding
            if (w > metrics.heightPixels / 2 || KalapaSDK.isFoldOpen(context)) { // For Fold
                w = metrics.widthPixels / foldOpenConst
            }
//            else if (KalapaSDK.isFoldDevice) { // Fold when folded
//                w = metrics.widthPixels * foldClosedConst
//            }
            Helpers.printLog("CardMaskView ${metrics.widthPixels} - Height: ${metrics.heightPixels}")
            var h: Float = if (isLivenessMode) w else w / ID_CARD_WIDTH_SIZE * ID_CARD_HEIGHT_SIZE

            roundRadius = if (isLivenessMode) w / 2
            else typedArray.getDimension(R.styleable.CardMaskView_radius, 0F) * metrics.density
            normalWidth = typedArray.getDimension(R.styleable.CardMaskView_frameWidth, w)
            normalHeight = typedArray.getDimension(R.styleable.CardMaskView_frameHeight, h)
            smallWidth = normalWidth * 0.8f
            smallHeight = normalHeight * 0.8f
            frameWidth = normalWidth
            frameHeight = normalHeight

            transOff =
                typedArray.getDimension(R.styleable.CardMaskView_transOff, 0F * metrics.density)

            transOffY =
                typedArray.getDimension(R.styleable.CardMaskView_transOffY, 0F * metrics.density)

            cornerColor = typedArray.getColor(R.styleable.CardMaskView_cornerColor, -1)

            var filter: ColorFilter = PorterDuffColorFilter(cornerColor, PorterDuff.Mode.DST_IN)
            if (cornerColor != -1)
                filter = PorterDuffColorFilter(cornerColor, PorterDuff.Mode.SRC_IN)
            cornerPaint.colorFilter = filter

            setFrame()

            conner = null
        }
    }


    override fun setFrame() {
        Helpers.printLog("setFrame transOffY $transOffY")
        frame = RectF(
            centerX - frameWidth / 2,
            (centerY + transOffY) - frameHeight / 2,
            centerX + frameWidth / 2,
            (centerY + transOffY) + frameHeight / 2
        )
        transparentFrame = RectF(
            frame.left + transOff,
            frame.top + transOff,
            frame.right - transOff,
            frame.bottom - transOff
        )
    }

    fun switchIntoFarModeLiveness() {
        frameWidth = smallWidth
        frameHeight = smallWidth
        setFrame()
    }

    fun resetToDefault() {
        frameWidth = normalWidth
        frameHeight = normalWidth
        setFrame()
    }

    private fun draw4Corners(canvas: Canvas, rect: RectF, paint: Paint) {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.frame_exclude_png)
        canvas.drawBitmap(bitmap, null, rect, paint)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

//        canvas.drawColor(maskColor)
        if (!isLivenessMode) {
            canvas.drawRoundRect(transparentFrame, roundRadius, roundRadius, transparentPaint)
//            canvas.drawRoundRect(frame, roundRadius, roundRadius, dashPaint)
            draw4Corners(canvas, frame, dashPaint)
        } else {
            if (ovalTop == 0F) ovalTop = transparentFrame.top - transparentFrame.bottom * 0.0618f
            if (ovalFrameTop == 0F) ovalFrameTop = frame.top - frame.bottom * 0.0618f

            canvas.drawOval(
                transparentFrame.left,
                ovalTop,
                transparentFrame.right,
                transparentFrame.bottom,
                transparentPaint
            )
            canvas.drawOval(frame.left, ovalFrameTop, frame.right, frame.bottom, dashPaint)
        }
        // draw center in geometry
//        canvas.drawOval(frame.centerX(), frame.centerY(), frame.centerX() + 1, frame.centerY() + 1, dashPaint)

        // draw 4 conner
        conner?.let {
            val haftSize = connerSize / 2F
            var bitmap = it

            // left top
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    frame.left - haftSize,
                    frame.top - haftSize,
                    frame.left - haftSize + connerSize,
                    frame.top - haftSize + connerSize
                ),
                cornerPaint
            )

            // right top
            var matrix = Matrix()
            matrix.setRotate(90F)
            bitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    frame.right - haftSize,
                    frame.top - haftSize,
                    frame.right - haftSize + connerSize,
                    frame.top - haftSize + connerSize
                ),
                cornerPaint
            )


            // left bottom
            matrix = Matrix()
            matrix.setRotate(-90F)
            bitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    frame.left - haftSize,
                    frame.bottom - haftSize,
                    frame.left - haftSize + connerSize,
                    frame.bottom - haftSize + connerSize
                ),
                cornerPaint
            )

            // right bottom
            matrix = Matrix()
            matrix.setRotate(180F)
            bitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    frame.right - haftSize,
                    frame.bottom - haftSize,
                    frame.right - haftSize + connerSize,
                    frame.bottom - haftSize + connerSize
                ),
                cornerPaint
            )

        }
    }

    override fun crop(inputImage: Bitmap, cameraDegree: Int): Bitmap {
        // Solution 1:
        val image = BitmapUtil.rotateBitmapToStraight(inputImage, cameraDegree)
        val metrics = resources.displayMetrics
        val scaleW = image.width.toFloat() / width // >= 1
        val scaleH = image.height.toFloat() / height // >= 1
        val actuallyScale = min(scaleW, scaleH)

        var xCrop = (transparentFrame.left - transOffY + transOff * 2) * scaleW
        var yCrop = transparentFrame.top * scaleH
        var wCrop = transparentFrame.width() * actuallyScale
        var hCrop = transparentFrame.height() * actuallyScale
        Helpers.printLog("Camera Degree: cameraDegree $cameraDegree ${metrics.densityDpi} ${metrics.density} scaleW $scaleW scaleH $scaleH transOffY $transOffY transOff $transOff")
        Helpers.printLog(
            "frameWidth $frameWidth frameHeight $frameHeight frame centerX ${frame.centerX()} frame center Y ${frame.centerY()}" +
                    " \n image.width ${image.width} image.height ${image.height}" +
                    " \n width $width height $height centerX $centerX centerY $centerY" +
                    "\n frame ${frame.width()} height() ${frame.height()} top() ${frame.top}  left() ${frame.left} center X ${frame.centerX()} center Y ${frame.centerY()}"
        )
        if (KalapaSDK.isFoldOpen(context)) {
            Helpers.printLog("Crop on Foldable is open")
            if (yCrop + hCrop + abs(transOffY) <= image.height) hCrop += abs(transOffY)
            if (yCrop + hCrop > image.height) hCrop = image.height - yCrop

            return Bitmap.createBitmap(
                image,
                0,
                yCrop.toInt(),
                image.width,
                hCrop.toInt()
            )

        } else if (KalapaSDK.isTablet(context)) {
            Helpers.printLog("Crop on Tablet")
            if (xCrop - 40 * scaleW > 0) {
                xCrop -= 40 * scaleW
                wCrop += 40 * scaleW
            }
            if (yCrop - 40 * scaleH > 0) {
                yCrop -= 40 * scaleH
                hCrop += 40 + scaleH
            }
            wCrop = transparentFrame.width() * max(scaleW, scaleH)
            hCrop = transparentFrame.height() * max(scaleW, scaleH)
            if (yCrop + hCrop > image.height) hCrop = image.height - yCrop
            if (xCrop + wCrop > image.width) wCrop = image.width - xCrop
            return Bitmap.createBitmap(
                image,
                xCrop.toInt(),
                yCrop.toInt(),
                wCrop.toInt(),
                hCrop.toInt()
            )
        }


        val widthOverfitting = xCrop < 0 || xCrop + wCrop > image.width
        val heightOverfitting = yCrop < 0 || yCrop + hCrop > image.height
        if (widthOverfitting || heightOverfitting) {
            if (widthOverfitting) {
                if (yCrop < 0) yCrop = 0f
                var newHeight =
                    (image.width * 1.0f * ID_CARD_HEIGHT_SIZE / ID_CARD_WIDTH_SIZE).toInt()
                if (yCrop + newHeight > image.height) newHeight = image.height
                return Bitmap.createBitmap(image, 0, yCrop.toInt(), image.width, newHeight)
            } else {
                if (xCrop < 0) xCrop = 0f
                var newWidth =
                    (image.height * 1.0f * ID_CARD_WIDTH_SIZE / ID_CARD_HEIGHT_SIZE).toInt()
                if (yCrop + newWidth > image.width) newWidth = image.width
                return Bitmap.createBitmap(image, xCrop.toInt(), 0, newWidth, image.height)
            }
        } else
            return Bitmap.createBitmap(
                image,
                xCrop.toInt(),
                yCrop.toInt(),
                wCrop.toInt(),
                hCrop.toInt()
            )
    }

}