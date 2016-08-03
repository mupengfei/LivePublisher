package com.eju.live.publisher.impl;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.eju.live.publisher.SrsEncoder;
import com.eju.live.publisher.SrsFlvMuxer;
import com.eju.live.publisher.SrsMp4Muxer;
import com.eju.live.publisher.config.PublisherConfig;
import com.eju.live.publisher.inter.IPublisher;
import com.eju.live.publisher.inter.IPublisherListerner;
import com.eju.live.publisher.rtmp.RtmpPublisher;

import java.io.IOException;
import java.util.List;

//import com.visionin.core.VSYUV420PCallback;

public class PublisherHardCodeImpl implements IPublisher {
    private String mToken;
    private PublisherConfig mConfig;
    private IPublisherListerner mListerner;

    private AudioRecord mic = null;
    private boolean aloop = false;
    private Thread aworker = null;

    private byte[] mYuvFrameBuffer = new byte[SrsEncoder.VPREV_WIDTH * SrsEncoder.VPREV_HEIGHT * 3 / 2];

    private String rtmpUrl = "rtmp://pili-publish.qdtong.net/leju-live-2/e74b76?key=0fec13c6a0c9ae08";
    //private String rtmpUrl ="rtmp://rtmppush.ejucloud.com/ehoush/abcd";
    private boolean mIsReconnect = true;

    private boolean mIsStart = false;

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
    public void init(PublisherConfig config, IPublisherListerner listerner) {
        this.mConfig = config;
        this.mListerner = listerner;
    }//相机参数的初始化设置

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
    public void startPublish() {
        try {
            flvMuxer.start(rtmpUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        flvMuxer.setVideoResolution(mEncoder.VCROP_WIDTH, mEncoder.VCROP_HEIGHT);
        startEncoder();
        mIsStart = true;
    }

    @Override
    public void stopPublish() {
        mIsStart = false;
        stopEncoder();
        flvMuxer.stop();
    }


    @Override
    public void setReconnect(boolean flag) {
        this.mIsReconnect = flag;
    }

    @Override
    public void setReconnectTimes(int times) {

    }

    public String getRtmpUrl() {
        return this.rtmpUrl;
    }

    @Override
    public void send(byte[] data) {
        if (mIsStart)
            this.mEncoder.onGetYuvFrame(data);
    }

    @Override
    public IPublisher setRtmpUrl(String url) {
        this.rtmpUrl = url;
        return this;
    }

    private void onGetPcmFrame(byte[] pcmBuffer, int size) {
        mEncoder.onGetPcmFrame(pcmBuffer, size);
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
        mEncoder.stop();
    }

    public SrsEncoder getEncoder() {
        return mEncoder;
    }
}
