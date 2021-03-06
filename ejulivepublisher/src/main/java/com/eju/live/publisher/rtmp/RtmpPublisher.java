package com.eju.live.publisher.rtmp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple RTMP publisher, using vanilla Java networking (no NIO)
 * This was created primarily to address a NIO bug in Android 2.2 when
 * used with Apache Mina, but also to provide an easy-to-use way to access
 * RTMP streams
 * 
 * @author francois, leo
 */
public interface RtmpPublisher {
    
    void connect(String url) throws IOException;
    
    /**
     * Issues an RTMP "publish" command and write the media content stream packets (audio and video). 
     * 
     * @param publishType specify the way to publish raw RTMP packets among "live", "record" and "append"
     * @return An outputStream allowing you to write the incoming media content data
     * @throws IllegalStateException if the client is not connected to a RTMP server
     * @throws IOException if a network/IO error occurs
     */
    void publish(String publishType) throws IllegalStateException, IOException;
     
    /**
     * Stops and closes the current RTMP stream
     */
    void closeStream() throws IllegalStateException;
    
    /**
     * Shuts down the RTMP client and stops all threads associated with it
     */
    void shutdown();

    /**
     * publish a video content packet to server
     *
     * @param data video stream byte array
     */
    void publishVideoData(byte[] data) throws IllegalStateException;

    /**
     * publish an audio content packet to server
     *
     * @param data audio stream byte array
     */
    void publishAudioData(byte[] data) throws IllegalStateException;

    /**
     * obtain event handler in publisher
     */
    EventHandler getEventHandler();

    /**
     * obtain video frame number cached in publisher
     */
    AtomicInteger getVideoFrameCacheNumber();

    /**
     * obtain the IP address of the peer if any
     */
    String getServerIpAddr();

    /**
     * obtain the PID of the peer if any
     */
    int getServerPid();

    /**
     * obtain the ID of the peer if any
     */
    int getServerId();

    /**
     * set video resolution
     *
     * @param width
     * @param height
     */
    void setVideoResolution(int width, int height);

    /**
     * RTMP event handler.
     */
    interface EventHandler {

        void onRtmpConnecting(String msg);

        void onRtmpConnected(String msg);

        void onRtmpVideoStreaming(String msg);

        void onRtmpAudioStreaming(String msg);

        void onRtmpStopped(String msg);

        void onRtmpDisconnected(String msg);

        void onRtmpOutputFps(double fps);
    }
}
