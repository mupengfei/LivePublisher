package com.eju.live.publisher.config;

import com.eju.live.publisher.inter.IVideoResolution;

public class VideoResolution implements IVideoResolution {
    private int width;
    private int height;

    public VideoResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
