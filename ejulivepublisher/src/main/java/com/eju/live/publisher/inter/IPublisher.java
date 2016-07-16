package com.eju.live.publisher.inter;

import android.app.Activity;
import android.view.SurfaceView;

import com.eju.live.publisher.config.PublisherConfig;

public interface IPublisher {
    void init(Activity mActivity, String token, PublisherConfig config, IPublisherListerner listerner);

    void startPublish(int cameraId, SurfaceView cameraView);

    void stopPublish();

    void switchCamera();

    void focus();

    void addFocus();

    void subFocus();

    void setFocus(int zoomValue);

    int getFocus();

    void openFlash();

    void closeFlash();

    void setReconnect(boolean flag);

    void setReconnectTimes(int times);

    void setResolution();

    String getRtmpUrl();
}