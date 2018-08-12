package com.airxiao.videodecoder.decodeUtil;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaCodecDecoder implements IDecoder, Runnable {
    
    private final static String TAG = "MediaCodecDecoder";

    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private VDConfig mVDConfig;
    private boolean mIsStart;
    private MediaCodec mMediaCodec;

    private int mCount;
    private long startTime = 0;

    private LinkedBlockingDeque<VideoData> decoderQueue = new LinkedBlockingDeque<>();  //线程同步的阻塞队列。
//    Thread thread;
    private boolean isFinish = false;
    private Future<?> mFuture;
    private int index;
    private boolean isIncrease = true;
    private boolean isDecode = true;

    public MediaCodecDecoder() {
    	mFuture = ThreadPool.submit(this);
//        startThread();
    }

//    private void startThread() {
//        thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    while (!isFinish) {
//                        byte[] frame = decoderQueue.take();//方法阻塞，有数据时继续执行。
//                        decodeData(frame);
////                        LogHelper.d(TAG, "queue size:" + decoderQueue.size());
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        thread.start();
//    }

    private void setFinish(boolean isFinish) {
        this.isFinish = isFinish;
    }

    public void sendData(VideoData videoData) {
        decoderQueue.offer(videoData);
    }

    @Override
    public synchronized void setConfig(VDConfig conf) {
        mVDConfig = conf;
    }

    @Override
    public synchronized VDConfig getConfig() {
        return mVDConfig;
    }

    @Override
    public synchronized void start() {
        if (mIsStart) {
            Log.w(TAG, "is already start");
            return;
        }
        if (mVDConfig == null) {
            Log.e(TAG, "start without decode config");
            return;
        }
        initMediaCodec();
    }

    private synchronized void initMediaCodec() {
        if (mIsStart) {
           return; 
        }
        try {
            mMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVDConfig.getWidth(), mVDConfig.getHeight());
            mMediaCodec.configure(format, mVDConfig.getDisplay().getSurface(), null, 0);
            mMediaCodec.start();
            mIsStart = true;
        } catch (Exception e) {
            Log.e(TAG, "initMediaCodec Exception" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (!isFinish) {
                VideoData data = decoderQueue.take();//方法阻塞，有数据时继续执行。
//            	  Log.d(TAG, "queue size:" + decoderQueue.size() + " ,Frame_Type:" + data.getSubType());
                decodeData(data);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void decodeData(VideoData videodata) {
    	if (decoderQueue.size() >= 10 * mVDConfig.getFramerate()) {
			isIncrease = false;
		} else if (!isIncrease && decoderQueue.size() <= mVDConfig.getFramerate()) {
			isIncrease = true;
            isDecode = false;
		}
    	
    	if (!isIncrease && videodata.getSubType() != 1) {
			return;
		}
        // 当队列大小小于一秒钟播放的帧数时，直到 I 帧出来后才开始所有类型帧的解码
        if (!isDecode) {
            if (videodata.getSubType() == 1) {
                isDecode = true;
            } else {
                return;
            }
        }
    	
    	byte[] data = videodata.getData();
    	
        if (!mIsStart) {
            Log.w(TAG, "decodeData fail decoder is not start");
            return;
        }

        if (data == null) {
            Log.w(TAG, "decodeData data is null");
            return;
        }

        //重置开始时间
        startTime = System.currentTimeMillis();

        try {
            ByteBuffer[] buffers = mMediaCodec.getInputBuffers();
            //-1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
            int index = mMediaCodec.dequeueInputBuffer(-1);

            if (index >= 0) {
                ByteBuffer buffer = buffers[index];
                buffer.clear();
                buffer.put(data, 0, data.length);
                mMediaCodec.queueInputBuffer(index, 0, data.length, mCount * 1000000 / mVDConfig.getFramerate(), 0);
                mCount++;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 11000);
            while (outputIndex >= 0) {
                mMediaCodec.releaseOutputBuffer(outputIndex, true);
                outputIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 11000);
            }
            if (decoderQueue.size() < mVDConfig.getFramerate()) {
                //线程休眠
                sleepThread(mVDConfig.getFramerate(), startTime, System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e(TAG, "decodeData Exception");
            e.printStackTrace();
        }
    }

    private void sleepThread(int rate, long startTime, long endTime) {
        //计算需要休眠的时间
        long time = 1000 / rate - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void stop() {
        setFinish(true);

        if (!mIsStart) {
            return;
        }
        try{
            mMediaCodec.stop();
        } catch (Exception e) {
            Log.e(TAG, "stop stop Exception" + e.getMessage());
            e.printStackTrace();
        }
        try{
            mMediaCodec.release();
        } catch (Exception e) {
            Log.e(TAG, "stop release Exception" + e.getMessage());
            e.printStackTrace();
        }
//        Log.e(TAG, "stop MediaCodec success");
        startTime = 0;
        mVDConfig = null;
        mMediaCodec = null;
        mIsStart = false;
    }
}
