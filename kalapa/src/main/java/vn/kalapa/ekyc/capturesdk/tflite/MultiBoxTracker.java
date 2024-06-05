/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package vn.kalapa.ekyc.capturesdk.tflite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Build;
import android.util.Pair;
import android.util.TypedValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import vn.kalapa.ekyc.utils.Logger;
import vn.kalapa.kalapasdk.tflite.OnImageDetectedListener;

/**
 * A tracker that handles non-max suppression and matches existing objects to new detections.
 */
public class MultiBoxTracker {
    private static final float TEXT_SIZE_DIP = 18;
    private static final float MIN_SIZE = 16.0f;
    private static final float CORNER_CONFIDENCE = 0.4f;
    private static final float CARD_CONFIDENCE = 0.4f;
    private static final int ACCEPT_SUCCEED_COUNT = Math.max(10 - (Build.VERSION_CODES.P - Build.VERSION.SDK_INT) * 2, 5);
    private static final int sdkVersion = Build.VERSION.SDK_INT;
    private static final int[] COLORS = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };
    final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
    private final Logger logger = new Logger();
    int successTimes = 0;

    public void resetSuccessTime() {
        this.successTimes -= 5;
    }

    private final Queue<Integer> availableColors = new LinkedList<Integer>();
    private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint boxPaint = new Paint();
    private final float textSizePx;
    private final BorderedText borderedText;
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private int sensorOrientation;
    private OnImageDetectedListener callbackListener;
    private long startTime = 0;
    private RectF cardMaskViewFrame;

    private float offsetY;
    private float density;
    private boolean isBackSide = false;

    public void setIsBackSide(boolean isBackSide) {
        this.isBackSide = isBackSide;
        neededSuccessTime = Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2 ? 11 : 7;
    }

    public MultiBoxTracker(final Context context, RectF cardMaskViewFrame, float density, float offsetY, OnImageDetectedListener callbackListener) {
        for (final int color : COLORS) {
            availableColors.add(color);
        }
        this.offsetY = offsetY;
        this.density = density;
        this.cardMaskViewFrame = cardMaskViewFrame;
        this.callbackListener = callbackListener;
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
        boxPaint.setStrokeCap(Cap.ROUND);
        boxPaint.setStrokeJoin(Join.ROUND);
        boxPaint.setStrokeMiter(100);

        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
    }

    public synchronized void setFrameConfiguration(
            final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }

    public synchronized void drawDebug(final Canvas canvas) {
        final Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60.0f);

        final Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Style.STROKE);

        for (final Pair<Float, RectF> detection : screenRects) {
            final RectF rect = detection.second;
            canvas.drawRect(rect, boxPaint);
            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
            borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
        }
    }

    public synchronized void trackResults(final List<Classifier.Recognition> results, final long timestamp) {
//        logger.w("Processing %d red-corner results from %d", results.size(), timestamp);
        processResults(results);
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    float multiplier;

    public synchronized void draw(final Canvas canvas) {
        final boolean rotated = sensorOrientation % 180 == 90;
        multiplier =
                Math.min(
                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
//        frameToCanvasMatrix =
//                ImageUtils.getTransformationMatrix(
//                        frameWidth,
//                        frameHeight,
//                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
//                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
//                        sensorOrientation,
//                        false);
//        float realOffset = offsetY * multiplier;
//        for (final TrackedRecognition recognition : trackedObjects) {
//            final RectF trackedPos = new RectF(recognition.location);
//            if (rotated) {
//                trackedPos.right -= realOffset;
//                trackedPos.left -= realOffset;
//            } else {
//                trackedPos.top -= realOffset;
//                trackedPos.bottom -= realOffset;
//            }
//            // res.displayMetrics.densityDpi
//            getFrameToCanvasMatrix().mapRect(trackedPos);
//            boxPaint.setColor(recognition.color);
////            logger.d("Rotated: " + rotated + " Multiplier: " + multiplier);
//            float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 10.0f;
//            if (recognition.title.equals("card"))
//                canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
////            final String labelString =
////                    !TextUtils.isEmpty(recognition.title)
////                            ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
////                            : String.format("%.2f", (100 * recognition.detectionConfidence));
//            // borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
//            // labelString);
////            borderedText.drawText(
////                    canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);
//        }
    }

    int neededSuccessTime = Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2 ? 17 : 11;

    private void processResults(final List<Classifier.Recognition> results) {
        final List<Pair<Float, Classifier.Recognition>> rectsToTrack = new LinkedList<Pair<Float, Classifier.Recognition>>();
        final boolean rotated = sensorOrientation % 180 == 90;
        screenRects.clear();
        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        for (final Classifier.Recognition result : results) {
            if (result.getLocation() == null) {
                continue;
            }
            final RectF detectionFrameRect = new RectF(result.getLocation());
            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

            screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                logger.w("Degenerate rectangle! " + detectionFrameRect);
                continue;
            }

            rectsToTrack.add(new Pair<Float, Classifier.Recognition>(result.getConfidence(), result));
        }

        trackedObjects.clear();
        if (rectsToTrack.isEmpty()) {
            logger.v("Nothing to track, aborting.");
            return;
        }
        for (final Pair<Float, Classifier.Recognition> potential : rectsToTrack) {
            // Pair of card and corner
            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();
//      trackedRecognition.color = COLORS[trackedObjects.size() % COLORS.length];
            trackedRecognition.color = COLORS[potential.second.getDetectedClass() % COLORS.length];
            trackedObjects.add(trackedRecognition);
        }
//        if (trackedObjects.size() >= 4) {
        for (TrackedRecognition trackedRecognition : trackedObjects) {
            float x1 = rotated ? Math.min(trackedRecognition.location.top, trackedRecognition.location.bottom) : Math.min(trackedRecognition.location.left, trackedRecognition.location.right);
            float y1 = (rotated ? Math.min(trackedRecognition.location.left, trackedRecognition.location.right) : Math.min(trackedRecognition.location.top, trackedRecognition.location.bottom)) - (multiplier != 0 ? offsetY * multiplier : offsetY);
            float width = multiplier != 0 ? multiplier * (rotated ? trackedRecognition.location.height() : trackedRecognition.location.width()) : (rotated ? trackedRecognition.location.height() : trackedRecognition.location.width());
            float height = multiplier != 0 ? multiplier * (rotated ? trackedRecognition.location.width() : trackedRecognition.location.height()) : (rotated ? trackedRecognition.location.width() : trackedRecognition.location.height());
            float x2 = x1 + width;
            float y2 = y1 + height; // for margin
//            logger.d("x1 y1 x1 y2 width height" + x1 + " - " + y1 + " - " + x2 + " - " + y2 + " - " + width + " - " + height);
            RectF correctRect = new RectF(x1, y1, x2, y2);
            if (trackedRecognition.title.contains("card") && trackedRecognition.detectionConfidence > CARD_CONFIDENCE) {
//                logger.d("Card: Confidence: " + trackedRecognition.detectionConfidence + " Is rotated :" + rotated + "- Multiplier " + multiplier + "- Density: " + density + " - Location " + correctRect + " - CardMaskView " + cardMaskViewFrame + " offset Y: " + offsetY);
                boolean topInMask = y1 + 20 * density > cardMaskViewFrame.top;
                boolean botInMask = y2 - 20 * density < cardMaskViewFrame.bottom;
                boolean rightInMask = x1 + 10 * density > cardMaskViewFrame.left;
                boolean leftInMask = x2 - 10 * density < cardMaskViewFrame.right;
                boolean cardInMask = topInMask && botInMask && rightInMask && leftInMask;
                if (cardInMask) {
                    successTimes += 3;
                    callbackListener.onImageInMask();
                    logger.d("Found card in mask... " + successTimes);
                } else {
                    successTimes -= 3;
                    callbackListener.onImageOutOfMask();
                    logger.d("Card is out of mask... " + successTimes);
                }
                if (successTimes > neededSuccessTime) {
                    successTimes = 0;
                    callbackListener.onImageDetected();
                }
                if (successTimes < 0) {
                    successTimes = 0;
                    callbackListener.onImageOutOfMask();
                }

            }
        }
//        }

    }

    int cardCount = 0;
    int cornerCount = 0;

    private static class TrackedRecognition {
        RectF location;
        float detectionConfidence;
        int color;
        String title;
    }
}
