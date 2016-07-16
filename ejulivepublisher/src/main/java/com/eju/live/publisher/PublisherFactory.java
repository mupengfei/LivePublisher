package com.eju.live.publisher;

import android.app.Activity;

import com.eju.live.publisher.config.PublisherConfig;
import com.eju.live.publisher.impl.PublisherHardCodeImpl;
import com.eju.live.publisher.inter.IPublisher;
import com.eju.live.publisher.inter.IPublisherListerner;

public class PublisherFactory {
    private static IPublisher mPublisher;

    public static IPublisher init(Activity activity, String token) {
        if (mPublisher == null)
            mPublisher = new PublisherHardCodeImpl();
        mPublisher.init(activity, token, null, null);
        return mPublisher;
    }

    public static IPublisher init(Activity activity, String token, PublisherConfig config) {
        if (mPublisher == null)
            mPublisher = new PublisherHardCodeImpl();
        mPublisher.init(activity, token, config, null);
        return mPublisher;
    }

    public static IPublisher init(Activity activity, String token, PublisherConfig config, IPublisherListerner listerner) {
        if (mPublisher == null)
            mPublisher = new PublisherHardCodeImpl();
        mPublisher.init(activity, token, config, listerner);
        return mPublisher;
    }

    public static IPublisher getInstance() {
        return mPublisher;
    }
}
