package com.eju.live.publisher;

import android.app.Activity;
import android.view.SurfaceView;

import com.eju.live.publisher.config.PublisherConfig;
import com.eju.live.publisher.impl.PublisherHardCodeImpl;
import com.eju.live.publisher.inter.IPublisher;
import com.eju.live.publisher.inter.IPublisherListerner;

public class PublisherFactory {
    private static IPublisher mPublisher;

    public static IPublisher init() {
        if (mPublisher == null)
            mPublisher = new PublisherHardCodeImpl();
        mPublisher.init( null, null);
        return mPublisher;
    }

    public static IPublisher init(PublisherConfig config) {
        if (mPublisher == null)
            mPublisher = new PublisherHardCodeImpl();
        mPublisher.init( config, null);
        return mPublisher;
    }

    public static IPublisher init(PublisherConfig config, IPublisherListerner listerner) {
        if (mPublisher == null)
            mPublisher = new PublisherHardCodeImpl();
        mPublisher.init(config, listerner);
        return mPublisher;
    }

    public static IPublisher getInstance() {
        return mPublisher;
    }
}
