package vn.kalapa.ekyc.fragment;

import android.app.Fragment;
import android.graphics.Bitmap;

public abstract class CaptureableFragment extends Fragment {
    public abstract Bitmap captureImage();
    public abstract void pauseCamera();
    public abstract void resumeCamera();
    public abstract void releaseCamera();

}
