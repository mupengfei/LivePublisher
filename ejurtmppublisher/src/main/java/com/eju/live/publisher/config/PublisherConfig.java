package com.eju.live.publisher.config;

import android.graphics.ImageFormat;
import android.media.AudioFormat;

import com.eju.live.publisher.inter.IVideoResolution;

public class PublisherConfig {
    public static final IVideoResolution VIDEO_720P = new VideoResolution(1280, 720);
    public static final IVideoResolution VIDEO_480P = new VideoResolution(640, 480);

    private static String VCODEC = "video/avc";
    private static String ACODEC = "audio/mp4a-latm";
    private static int VBITRATE = 600 * 1000;  // 500kbps
    private static int VFPS = 24;
    private static int VGOP = 24;
    private static int VFORMAT = ImageFormat.YV12;
    private static int ASAMPLERATE = 44100;
    private static int ACHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static int AFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int ABITRATE = 64 * 1000;  // 32kbps

}
