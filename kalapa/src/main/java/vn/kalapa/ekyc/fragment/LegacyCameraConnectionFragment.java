package vn.kalapa.ekyc.fragment;

/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;


import java.io.IOException;
import java.util.List;

import vn.kalapa.R;
import vn.kalapa.ekyc.activity.CameraActivity;
import vn.kalapa.ekyc.utils.ImageUtils;
import vn.kalapa.ekyc.views.AutoFitTextureView;

@SuppressLint("ValidFragment")
public class LegacyCameraConnectionFragment extends CaptureableFragment {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Camera camera;
    private Camera.PreviewCallback imageListener;
    private CameraActivity.LENS_FACING lensFacing;
    private Size desiredSize;
    /**
     * The layout identifier to inflate for this Fragment.
     */
    private int layout;
    private AutoFitTextureView textureView;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
     * TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {

                    int index = getCameraId();
                    camera = Camera.open(index);

                    try {
                        Camera.Parameters parameters = camera.getParameters();
                        List<String> focusModes = parameters.getSupportedFocusModes();
                        if (focusModes != null
                                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
                        Size[] sizes = new Size[cameraSizes.size()];
                        int i = 0;
                        for (Camera.Size size : cameraSizes) {
                            sizes[i++] = new Size(size.width, size.height);
                        }
                        Size previewSize = CameraConnectionFragment.chooseOptimalSize(sizes, desiredSize.getWidth(), desiredSize.getHeight());
                        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
                        camera.setDisplayOrientation(90);
                        camera.setParameters(parameters);
                        camera.setPreviewTexture(texture);
                    } catch (IOException exception) {
                        camera.release();
                    }
                    setPreviewCallback();

                    camera.startPreview();
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
                }
            };
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    private void setPreviewCallback() {
        camera.setPreviewCallbackWithBuffer(imageListener);
        Camera.Size s = camera.getParameters().getPreviewSize();
        camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);
        textureView.setAspectRatio(s.height, s.width);

    }

    public LegacyCameraConnectionFragment(
            final Camera.PreviewCallback imageListener, final CameraActivity.LENS_FACING lensFacing, final int layout, final Size desiredSize) {
        this.imageListener = imageListener;
        this.layout = layout;
        this.desiredSize = desiredSize;
        this.lensFacing = lensFacing;
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    @Override
    public Bitmap captureImage() {
        return textureView != null && textureView.isAvailable() ? textureView.getBitmap() : null;
    }

    @Override
    public void pauseCamera() {
        stopCamera();
        stopBackgroundThread();
    }

    @Override
    public void resumeCamera() {
//        Helpers.Companion.printLog("LegacyCameraConnectionFragment try to resumeCamera");
        startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        if (textureView.isAvailable()) {
            try {
                camera.startPreview();
                setPreviewCallback();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeCamera();
    }

    @Override
    public void onPause() {
        pauseCamera();
        super.onPause();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            if (backgroundThread != null) {
                backgroundThread.quitSafely();
                backgroundThread.join();
                backgroundThread = null;
            }
        } catch (final InterruptedException e) {
            Log.e(LegacyCameraConnectionFragment.class.getSimpleName(), "Exception! $e");
        }
    }

    protected void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
        }
    }


    private int getCameraId() {
        CameraInfo ci = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No camera found
    }
}
