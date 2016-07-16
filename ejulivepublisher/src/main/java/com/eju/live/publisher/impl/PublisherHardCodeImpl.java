package com.eju.live.publisher.impl;

import android.app.Activity;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.eju.live.publisher.SrsEncoder;
import com.eju.live.publisher.SrsFlvMuxer;
import com.eju.live.publisher.SrsMp4Muxer;
import com.eju.live.publisher.config.PublisherConfig;
import com.eju.live.publisher.inter.IPublisher;
import com.eju.live.publisher.inter.IPublisherListerner;
import com.eju.live.publisher.rtmp.RtmpPublisher;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublisherHardCodeImpl implements IPublisher, SurfaceHolder.Callback, Camera.PreviewCallback {
    private Activity mActivity;
    private String mToken;
    private PublisherConfig mConfig;
    private IPublisherListerner mListerner;

    private SurfaceView mCameraView;

    private Camera mCamera = null;
    private AudioRecord mic = null;
    private boolean aloop = false;
    private Thread aworker = null;

    private int mCamId = Camera.getNumberOfCameras() - 1; // default camera
    private byte[] mYuvFrameBuffer = new byte[SrsEncoder.VPREV_WIDTH * SrsEncoder.VPREV_HEIGHT * 3 / 2];

    private String rtmpUrl = "rtmp://101.231.84.147/live/abcdefgd";

    private boolean mIsReconnect = true;

    private SrsFlvMuxer flvMuxer = new SrsFlvMuxer(new RtmpPublisher.EventHandler() {
        @Override
        public void onRtmpConnecting(String msg) {
            if (mListerner != null)
                mListerner.onRtmpConnecting(msg);
        }

        @Override
        public void onRtmpConnected(String msg) {
            if (mListerner != null)
                mListerner.onRtmpConnected(msg);
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {
            if (mListerner != null)
                mListerner.onRtmpVideoStreaming(msg);
        }

        @Override
        public void onRtmpAudioStreaming(String msg) {
            if (mListerner != null)
                mListerner.onRtmpAudioStreaming(msg);
        }

        @Override
        public void onRtmpStopped(String msg) {
            if (mListerner != null)
                mListerner.onRtmpStopped(msg);
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            if (mListerner != null)
                mListerner.onRtmpDisconnected(msg);
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            if (mListerner != null)
                mListerner.onRtmpOutputFps(fps);
        }
    });

    private SrsMp4Muxer mp4Muxer = new SrsMp4Muxer(new SrsMp4Muxer.EventHandler() {
        @Override
        public void onRecordPause(String msg) {
        }

        @Override
        public void onRecordResume(String msg) {
        }

        @Override
        public void onRecordStarted(String msg) {
        }

        @Override
        public void onRecordFinished(String msg) {
        }
    });
    private SrsEncoder mEncoder = new SrsEncoder(flvMuxer, mp4Muxer);

    @Override
    public void init(Activity activity, String token, PublisherConfig config, IPublisherListerner listerner) {
        this.mActivity = activity;
        this.mToken = token;
        this.mConfig = config;
        this.mListerner = listerner;
    }//相机参数的初始化设置

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
        int degrees = getDisplayRotation(mActivity);
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
            mCamera.setPreviewDisplay(mCameraView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mCamera.autoFocus(null);
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
    public void startPublish(int cameraId, SurfaceView cameraView) {
        this.mCameraView = cameraView;
        mCameraView.getHolder().addCallback(this);
//        mCamera = Camera.open(cameraId);
//        initCamera();
        try {
            flvMuxer.start(rtmpUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        flvMuxer.setVideoResolution(mEncoder.VCROP_WIDTH, mEncoder.VCROP_HEIGHT);
        startEncoder();
    }

    @Override
    public void stopPublish() {
        stopEncoder();
        flvMuxer.stop();
    }

    @Override
    public void switchCamera() {
        if (mCamera != null && mEncoder != null) {
            mCamId = (mCamId + 1) % Camera.getNumberOfCameras();
            stopCamera();
            mEncoder.swithCameraFace();
            startCamera();
        }
    }

    @Override
    public void focus() {
        mCamera.autoFocus(null);
    }

    @Override
    public void addFocus() {
        if (!mCamera.getParameters().isSmoothZoomSupported()) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                final int MAX = params.getMaxZoom();
                if (MAX == 0)
                    return;
                int zoomValue = params.getZoom();
                zoomValue += 1;
                params.setZoom(zoomValue);
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void subFocus() {
        if (!mCamera.getParameters().isSmoothZoomSupported()) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                final int MAX = params.getMaxZoom();
                if (MAX == 0)
                    return;
                int zoomValue = params.getZoom();
                zoomValue -= 1;
                params.setZoom(zoomValue);
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setFocus(int zoomValue) {
        if (!mCamera.getParameters().isSmoothZoomSupported()) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                final int MAX = params.getMaxZoom();
                if (MAX == 0 || zoomValue > MAX)
                    return;
                params.setZoom(zoomValue);
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getFocus() {
        if (!mCamera.getParameters().isSmoothZoomSupported()) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                return params.getZoom();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public void openFlash() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
    }

    @Override
    public void closeFlash() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    }

    @Override
    public void setReconnect(boolean flag) {
        this.mIsReconnect = flag;
    }

    @Override
    public void setReconnectTimes(int times) {

    }

    @Override
    public void setResolution() {

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mCameraView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public String getRtmpUrl() {
        return this.rtmpUrl;
    }

    private void onGetYuvFrame(byte[] data) {
        mEncoder.onGetYuvFrame(data);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera c) {
        onGetYuvFrame(data);
        c.addCallbackBuffer(mYuvFrameBuffer);
    }

    private void onGetPcmFrame(byte[] pcmBuffer, int size) {
        mEncoder.onGetPcmFrame(pcmBuffer, size);
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

    private void startAudio() {
        if (mic != null) {
            return;
        }
        int bufferSize = 2 * AudioRecord.getMinBufferSize(SrsEncoder.ASAMPLERATE, SrsEncoder.ACHANNEL, SrsEncoder.AFORMAT);
        mic = new AudioRecord(MediaRecorder.AudioSource.MIC, SrsEncoder.ASAMPLERATE, SrsEncoder.ACHANNEL, SrsEncoder.AFORMAT, bufferSize);
        mic.startRecording();
        while (aloop && !Thread.interrupted()) {
            byte pcmBuffer[] = new byte[4096];
            int size = mic.read(pcmBuffer, 0, pcmBuffer.length);
            if (size <= 0) {
                break;
            }
            onGetPcmFrame(pcmBuffer, size);
        }
    }

    private void stopAudio() {
        aloop = false;
        if (aworker != null) {
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                aworker.interrupt();
            }
            aworker = null;
        }
        if (mic != null) {
            mic.setRecordPositionUpdateListener(null);
            mic.stop();
            mic.release();
            mic = null;
        }
    }

    private void startEncoder() {
        int ret = mEncoder.start();
        if (ret < 0) {
            return;
        }
        startCamera();
        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                startAudio();
            }
        });
        aloop = true;
        aworker.start();
    }

    private void stopEncoder() {
        stopAudio();
        stopCamera();
        mEncoder.stop();
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

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
