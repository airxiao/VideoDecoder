package com.airxiao.videodecoder;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.airxiao.videodecoder.decodeUtil.MediaCodecDecoder;
import com.airxiao.videodecoder.decodeUtil.VDConfig;
import com.airxiao.videodecoder.decodeUtil.VideoData;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;

    private MediaCodecDecoder mediaCodecDecoder;

    private static final String TAG = "MainActivity";
    private static final int DEFAULT_RATE = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        surfaceView.getHolder().addCallback(this);
    }

    /**
     * H.264码流回调
     * @param index 窗口序号
     * @param data 码流
     * @param len 码流长度
     * @param rate 帧率
     * @param timestamp 时间戳
     * @param width 视频宽度
     * @param height 视频高度
     * @param subType 视频帧类型(I P B)
     */
    public void onVideoDataCallback(int index, byte[] data, int len, int rate, long timestamp, int width, int height, int subType) {
        Log.d("EventManager", "index:" + index + ",dataLen:" + data.length + ",len:" + len + ",rate:" + rate + ",timestamp:" + timestamp + ",width:" + width + ",height:" + height);
        mediaCodecDecoder.sendData(new VideoData(subType, data));
    }

    private MediaCodecDecoder createNewDecoder(SurfaceHolder holder) {
        VDConfig vdConfig = new VDConfig();
        vdConfig.setFramerate(DEFAULT_RATE);
        vdConfig.setHeight(holder.getSurfaceFrame().height());
        vdConfig.setWidth(holder.getSurfaceFrame().width());
        vdConfig.setDisplay(holder);
        MediaCodecDecoder mediaCodecDecoder = new MediaCodecDecoder();
        mediaCodecDecoder.setConfig(vdConfig);
        mediaCodecDecoder.start();
        return mediaCodecDecoder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mediaCodecDecoder = createNewDecoder(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
//        mediaCodecDecoder.setFinish(true);
        mediaCodecDecoder.stop();
    }
}