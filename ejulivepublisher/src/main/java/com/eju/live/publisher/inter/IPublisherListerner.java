package com.eju.live.publisher.inter;

import com.eju.live.publisher.rtmp.RtmpPublisher;

public interface IPublisherListerner extends RtmpPublisher.EventHandler {
    void onAuthorizeSuc(String msg);

    void onError(String msg);
}
