package com.eju.live.publisher.inter;

import android.app.Activity;
import android.view.SurfaceView;

import com.eju.live.publisher.SrsEncoder;
import com.eju.live.publisher.config.PublisherConfig;

public interface IPublisher {
    void init(PublisherConfig config, IPublisherListerner listerner);

    void startPublish();

    void stopPublish();

    void setReconnect(boolean flag);

    void setReconnectTimes(int times);

    String getRtmpUrl();

    void send(byte[] data);

    IPublisher setRtmpUrl(String url);
}