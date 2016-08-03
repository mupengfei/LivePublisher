package com.eju.live.ejulivepublisherdemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.visionin.Visionin;
import com.visionin.core.VSVideoFrame;
//import com.visionin.core.VSYUV420PCallback;

import java.io.IOException;

public class ByteDemoActivity extends Activity implements SurfaceHolder.Callback{
    private static final String TAG = "Activity";
    private boolean VERBOSE = false;
    private SurfaceView surfaceView;
    protected SurfaceView displayView;
    private Camera mCamera;
    private VSVideoFrame videoFrame;
    SurfaceHolder surfaceHolder;
    SurfaceHolder displayHolder;
    Camera.Size     videoSize;
    protected ImageView imageView;
    protected int[] rgbBuffer = new int[360*640];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        // 1. 初始化
        Visionin.initialize(this, "293cd8f2fd5cdf0e403f535f2563b5b4", "44ce96297a8bcc10eaf095d216d045ec");

        surfaceView = (SurfaceView) findViewById(R.id.camera_surfaceView);
        displayView = (SurfaceView) findViewById(R.id.displayView);
        imageView = (ImageView) findViewById(R.id.imageView);

        surfaceHolder = surfaceView.getHolder();

        displayHolder = displayView.getHolder();
        displayHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        Log.d(TAG, "onCreate complete: " + this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume -- acquiring camera");
        super.onResume();
        surfaceHolder.addCallback(this);
        Log.d(TAG, "onResume complete: " + this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause -- releasing camera");
        super.onPause();
        videoFrame.stop();
        videoFrame.destroy();
        videoFrame = null;
        releaseCamera();
        Log.d(TAG, "onPause complete");
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    private Camera.Size openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }

        Camera.Parameters parms = mCamera.getParameters();
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.e(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == desiredWidth && size.height == desiredHeight) {
                parms.setPreviewSize(desiredWidth, desiredHeight);
                break;
            }
        }

        parms.setRecordingHint(true);
        mCamera.setParameters(parms);
//        Log.d(TAG, "Setting orientation to 90");
//        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();

        int[] fpsRange = new int[2];
        parms.getPreviewFpsRange(fpsRange);
        return parms.getPreviewSize();
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        videoSize = openCamera(1280, 720);      // updates mCameraPreviewWidth/Height

        try {
            videoFrame = new VSVideoFrame(surfaceHolder.getSurface());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        videoFrame.setCameraPosition(VSVideoFrame.CAMERA_FACING_FRONT);
        videoFrame.setVideoSize(videoSize.width, videoSize.height);
        videoFrame.setMirrorFrontVideo(true);
        videoFrame.setMirrorFrontPreview(true);
        videoFrame.setOutputSize(360, 640);
        videoFrame.setSmoothLevel(0.5f);
        videoFrame.setBrightenLevel(0.5f);
        videoFrame.setToningLevel(0.5f);
//        videoFrame.setYuv420PCallback(new VSYUV420PCallback() {
//            @Override
//            public void outputBytes(byte[] bytes) {
//                imageView.setImageBitmap(rawByteArray2RGBABitmap2(bytes, 360, 640));
//            }
//        });

        videoFrame.start();
        displayHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Camera.Parameters p = mCamera.getParameters();
                p.setPreviewFormat(ImageFormat.NV21);
                mCamera.setParameters(p);
                mCamera.startPreview();
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        if (videoFrame!=null) {
                            videoFrame.makeCurrent();
                            videoFrame.processBytes(bytes, videoSize.width, videoSize.height, VSVideoFrame.GPU_NV21);
                        }
                    }
                });
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public static void decodeYUV420SPrgb565(int[] rgb, byte[] yuv420sp, int width,
                                            int height) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width/2 + (j>>1)]));
                int v = (0xff & ((int) data[frameSize + width*height/4 + (i >> 1) * width/2 + (j>>1)]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b<<16) + (g << 8) + r;
            }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0 , width, 0, 0, width, height);
        return bmp;
    }
}
