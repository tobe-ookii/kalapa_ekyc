package vn.kalapa.ekyc.capturesdk.tflite;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;

public class DetectorFactory {
    public static CardClassifier getDetector(
            final AssetManager assetManager,
            final String modelFilename)
            throws IOException {
        String labelFilename = null;
        boolean isQuantized = false;
        int inputSize = 0;
        int[] output_width = new int[]{0};
        int[][] masks = new int[][]{{0}};
        int[] anchors = new int[]{0};
        Log.i("Model File Name", modelFilename);
        if (modelFilename.equals("klp_model_2.tflite") || modelFilename.equals("klp_model.tflite")) {
            labelFilename = "file:///android_asset/klp_label.txt";
            isQuantized = false;
            inputSize = 320;
            output_width = new int[]{36, 18, 9};
            masks = new int[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}};
            anchors = new int[]{
                    10, 13, 16, 30, 33, 23, 30, 61, 62, 45, 59, 119, 116, 90, 156, 198, 373, 326
            };
        }
        return CardClassifier.create(assetManager, modelFilename, labelFilename, isQuantized,
                inputSize);
    }

}
