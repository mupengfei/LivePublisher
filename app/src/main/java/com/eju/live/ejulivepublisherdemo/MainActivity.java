package com.eju.live.ejulivepublisherdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.eju.live.publisher.PublisherFactory;
import com.eju.live.publisher.SrsEncoder;
import com.eju.live.publisher.SrsFlvMuxer;
import com.eju.live.publisher.SrsMp4Muxer;
import com.eju.live.publisher.rtmp.RtmpPublisher;
import com.visionin.core.VSVideoFrame;
//import com.visionin.core.VSYUV420PCallback;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "Yasea";
    private Camera mCamera = null;
    private int mCamId = Camera.getNumberOfCameras() - 1; // default camera
    private byte[] mYuvFrameBuffer = new byte[SrsEncoder.VPREV_WIDTH * SrsEncoder.VPREV_HEIGHT * 3 / 2];

    Button btnPublish = null;
    Button btnSwitch = null;
    Button btnRecord = null;
    Button btnFocus = null;
    Button btnAddFocus = null;
    Button btnSubFocus = null;
    Button btnSwitchFilter = null;
    ZoomControls mZoomControls = null;
    private SurfaceView mCameraView = null;
    private VSVideoFrame videoFrame = null;

    private String mNotifyMsg;
    private SharedPreferences sp;
    //    private String rtmpUrl = "rtmp://ossrs.net/" + getRandomAlphaString(3) + '/' + getRandomAlphaDigitString(5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // for camera, @see https://developer.android.com/reference/android/hardware/Camera.html
        btnPublish = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.publish);
        btnSwitch = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.swCam);
        btnRecord = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.record);
        btnFocus = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.focus);
        btnAddFocus = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.addFocus);
        btnSubFocus = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.subFocus);
        mZoomControls = (ZoomControls) findViewById(com.eju.live.ejulivepublisherdemo.R.id.zoomControls);
        mCameraView = (SurfaceView) findViewById(com.eju.live.ejulivepublisherdemo.R.id.preview);
        btnSwitchFilter = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.switchFilter);
        btnSwitchFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PublisherFactory.getInstance().switchCameraFilter();
            }
        });
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPublish.getText().toString().contentEquals("publish")) {
                    PublisherFactory.init(MainActivity.this, mCameraView, "");
                    PublisherFactory.getInstance().startPublish(0);
                    // initialize url.
                    final EditText efu = (EditText) findViewById(com.eju.live.ejulivepublisherdemo.R.id.url);
                    efu.setText(PublisherFactory.getInstance().getRtmpUrl());
                    btnPublish.setText("stop");
                } else if (btnPublish.getText().toString().contentEquals("stop")) {
                    PublisherFactory.getInstance().stopPublish();
                    btnPublish.setText("publish");
                    btnRecord.setText("record");
                }
            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().switchCamera();
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        btnFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().focus();
            }
        });

        btnAddFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().addFocus();
            }
        });

        btnSubFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().subFocus();
            }
        });

        mZoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().addFocus();
            }
        });

        mZoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().subFocus();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.eju.live.ejulivepublisherdemo.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == com.eju.live.ejulivepublisherdemo.R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Button btn = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.publish);
        btn.setEnabled(true);
//        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.e("surfaceCreated", "surfaceCreated" + mCameraView);
//        try {
//            videoFrame = new VSVideoFrame(surfaceHolder.getSurface());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }
//
//        videoFrame.setCameraPosition(VSVideoFrame.CAMERA_FACING_FRONT);
//        videoFrame.setOutputImageOritation(Configuration.ORIENTATION_PORTRAIT);
//
//        videoFrame.setVideoSize(640, 480);
//
//        videoFrame.setMirrorFrontVideo(true);
//        videoFrame.setMirrorFrontPreview(true);
//
//        videoFrame.setOutputSize(640, 360);
//        videoFrame.setYuv420PCallback(new VSYUV420PCallback() {
//            @Override
//            public void outputBytes(byte[] bytes) {
//                //Log.e("Visionin", ""+bytes.length);
//            }
//        });
//
//        videoFrame.setSmoothLevel(0.5f);
//        videoFrame.setBrightenLevel(0.5f);
//        videoFrame.setToningLevel(0.5f);
//
//        videoFrame.start();
//
//        try {
//            mCamera.setPreviewTexture(videoFrame.surfaceTexture());
//            mCamera.startPreview();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            mCamera.setPreviewDisplay(mCameraView.getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.e("surfaceChanged", "surfaceChanged");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void startCamera() {
        if (mCamera != null) {
            return;
        }
        if (mCamId > (Camera.getNumberOfCameras() - 1) || mCamId < 0) {
            return;
        }
        mCamera = Camera.open(mCamId);
        initCamera();
    }

    private void initCamera() {
        Camera.Parameters params = mCamera.getParameters();
        /* preview size  */
        Camera.Size size = mCamera.new Size(SrsEncoder.VPREV_WIDTH, SrsEncoder.VPREV_HEIGHT);
        if (!params.getSupportedPreviewSizes().contains(size)) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(),
                    new IllegalArgumentException(String.format("Unsupported preview size %dx%d", size.width, size.height)));
        }
        /* picture size  */
        if (!params.getSupportedPictureSizes().contains(size)) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(),
                    new IllegalArgumentException(String.format("Unsupported picture size %dx%d", size.width, size.height)));
        }
        params.setPictureSize(SrsEncoder.VPREV_WIDTH, SrsEncoder.VPREV_HEIGHT);
        params.setPreviewSize(SrsEncoder.VPREV_WIDTH, SrsEncoder.VPREV_HEIGHT);
        int[] range = findClosestFpsRange(SrsEncoder.VFPS, params.getSupportedPreviewFpsRange());
        params.setPreviewFpsRange(range[0], range[1]);
        params.setPreviewFormat(SrsEncoder.VFORMAT);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        mCamera.setParameters(params);
        //设置摄像方向
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCamId, info);
        int degrees = getDisplayRotation(this);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

        mCamera.addCallbackBuffer(mYuvFrameBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);

        try {
            videoFrame = new VSVideoFrame(mCameraView.getHolder().getSurface());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        videoFrame.setCameraPosition(VSVideoFrame.CAMERA_FACING_FRONT);
        videoFrame.setOutputImageOritation(Configuration.ORIENTATION_PORTRAIT);

        videoFrame.setVideoSize(640, 480);

        videoFrame.setMirrorFrontVideo(true);
        videoFrame.setMirrorFrontPreview(true);

        videoFrame.setOutputSize(640, 360);
//        videoFrame.setYuv420PCallback(new VSYUV420PCallback() {
//            @Override
//            public void outputBytes(byte[] bytes) {
//                //Log.e("Visionin", ""+bytes.length);
//            }
//        });

        videoFrame.setSmoothLevel(0.5f);
        videoFrame.setBrightenLevel(0.5f);
        videoFrame.setToningLevel(0.5f);

        videoFrame.start();

        try {
            Log.e("setPreviewTexture","setPreviewTexture");
            mCamera.setPreviewTexture(videoFrame.surfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e("IOException", e.getLocalizedMessage());
        }
        mCamera.autoFocus(null);
//        try {
//            mCamera.setPreviewDisplay(mCameraView.getHolder());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mCamera.startPreview();
//        mCamera.autoFocus(null);
//        mCameraView.getHolder().addCallback((SurfaceHolder.Callback) this);
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private static int[] findClosestFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (PublisherFactory.getInstance() != null)
            PublisherFactory.getInstance().getEncoder().onGetYuvFrame(data);
        ;
        camera.addCallbackBuffer(mYuvFrameBuffer);
    }
}
