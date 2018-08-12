package com.airxiao.videodecoder.decodeUtil;

import android.view.SurfaceHolder;


public class VideoDecoderInfo {

    private SurfaceHolder surfaceHolder;
    private MediaCodecDecoder mediaCodecDecoder;

    public VideoDecoderInfo(SurfaceHolder surfaceHolder, MediaCodecDecoder mediaCodecDecoder) {
        this.surfaceHolder = surfaceHolder;
        this.mediaCodecDecoder = mediaCodecDecoder;
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public MediaCodecDecoder getMediaCodecDecoder() {
        return mediaCodecDecoder;
    }

    public void setMediaCodecDecoder(MediaCodecDecoder mediaCodecDecoder) {
        this.mediaCodecDecoder = mediaCodecDecoder;
    }
}
