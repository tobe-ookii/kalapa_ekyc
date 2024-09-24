package vn.kalapa.ekyc.capturesdk.tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import vn.kalapa.ekyc.utils.Common
import vn.kalapa.ekyc.utils.Helpers
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.min

class KLPDetector(private val context: Context, private val modelPath: String, labelPath: String, var shouldCapture: Boolean = true, private val onImageListener: OnImageDetectedListener) : KLPDetectorListener {
    private var interpreter: Interpreter
    private val labels = mutableListOf<String>()
    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0
    private val CARD_CONF = 0.75f
    private val CORNER_CONF = 0.4f
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
        private const val IOU_THRESHOLD = 0.5F
    }

    init {
        val compatList = CompatibilityList()
        Helpers.printLog("getCpuInfo ${Common.getCpuInfo()}")
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
            } else {
                this.setNumThreads(4)
            }
        }
        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = try {
            Interpreter(model, options)
        } catch (exception: Throwable) {
            Interpreter(model)
        }

        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()
        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numChannel = outputShape[1]
            numElements = outputShape[2]
        }

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun restart(isGpu: Boolean) {
        close()
        detected = false
        val options = if (isGpu) {
            val compatList = CompatibilityList()
            Interpreter.Options().apply {
                if (compatList.isDelegateSupportedOnThisDevice) {
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }
        } else {
            Interpreter.Options().apply {
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)
    }

    fun close() {
        try {
            interpreter.close()
        } catch (exception: Exception) { // Already close.
        }
    }

    fun detect(frame: Bitmap, doneProcessed: () -> Unit) {
        if (tensorWidth == 0) return
        if (tensorHeight == 0) return
        if (numChannel == 0) return
        if (numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer
//        Helpers.printLog("numElements $numElements numChannel $numChannel OUTPUT_IMAGE_TYPE $OUTPUT_IMAGE_TYPE")
        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter.run(imageBuffer, output.buffer)
        val bestBoxes = bestBox(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (bestBoxes == null) {
            onEmptyDetect()
            return
        }
        onDetect(frame.width, frame.height, bestBoxes, inferenceTime) {
            doneProcessed()
        }
    }

    private fun bestBox(array: FloatArray): List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = CONFIDENCE_THRESHOLD
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    override fun onEmptyDetect() {
        onImageListener.onImageNotDetected()
    }

    private var detectCount = 0
    private var detected = false
    override fun onDetect(frameWidth: Int, frameHeight: Int, boundingBoxes: List<BoundingBox>, inferenceTime: Long, doneProcessed: () -> Unit) {
        if (detected && shouldCapture) return
        val actualHeight = frameWidth * 0.75f
        val offsetRatio = ((frameHeight - actualHeight) / 2) / frameHeight
        val offsetBottom = 1 - offsetRatio
//        Helpers.printLog("KLPDetector onDetect frameWidth $frameWidth frameHeight $frameHeight actualHeight $actualHeight offsetRatio $offsetRatio offsetBottom $offsetBottom")
        var corners = ArrayList<BoundingBox>()
        var cards = ArrayList<BoundingBox>()
        var buffer = StringBuffer()
        var top = -1f
        var bottom = -1f
        var left = -1f
        var right = -1f
        for (boundBox in boundingBoxes) {
            if (boundBox.cnf < min(CARD_CONF, CORNER_CONF))
                continue
            if (boundBox.clsName == "card") {
                cards.add(boundBox)
            } else if (boundBox.clsName == "corner") {
                corners.add(boundBox)
            }
            if (top == -1f || boundBox.y1 < top) top = boundBox.y1
            if (bottom == -1f || boundBox.y2 > bottom) bottom = boundBox.y2
            if (left == -1f || boundBox.x1 < left) left = boundBox.x1
            if (right == -1f || boundBox.x2 > right) right = boundBox.x2
            buffer.append("${boundBox.clsName} ${boundBox.cnf} \t")

        }
        if (cards.size == 1) {
            if ((bottom - top) < 0.5f && (right - left) < 0.5f) {
//                Helpers.printLog("KLPDetector too small $top $bottom $left $right")
                onImageListener.onImageTooSmall()
            } else {
                if (cards.size == 1 && corners.size in (4..5) && top > offsetRatio && bottom < offsetBottom) {
//                    Helpers.printLog("KLPDetector onImageInMask $top $bottom $offsetRatio $offsetBottom")
                    onImageListener.onImageInMask()
                    if (shouldCapture) {
                        detectCount++
                        if (detectCount >= 5) {
                            detectCount = 0
                            onImageListener.onImageDetected()
                            detected = true
                        }
                    }
                } else {
//                    Helpers.printLog("KLPDetector out of the box - cards ${cards.size} corners ${corners.size} $top $bottom $offsetRatio $offsetBottom ")
                    detectCount--
                    if (detectCount < 0) detectCount = 0
                    if (cards.size > 0)
                        onImageListener.onImageOutOfMask()
                }
            }
        }
        doneProcessed()
    }

}

interface KLPDetectorListener {
    fun onEmptyDetect()
    fun onDetect(frameWidth: Int, frameHeight: Int, boundingBoxes: List<BoundingBox>, inferenceTime: Long, doneProcessed: () -> Unit)

}