# 视频硬解码
[原文链接](https://airxiao.github.io/2018/08/12/%E8%A7%86%E9%A2%91%E7%A1%AC%E8%A7%A3%E7%A0%81/)

### 背景

实时视频在播放的时候出现卡顿和延迟现象，经分析是因为视频在 C 层采用的是软解码的方式进行播放，因此决定在 Java 层改用硬解码方式。

### 软解码和硬解码区别

- 软解码：使用 CPU 进行编码，实现直接、简单，参数调整方便，升级易，但 CPU 负载重，性能较硬编码低。
- 硬解码：使用显卡 GPU、专用的 DSP、FPGA、ASIC 芯片等方式进行编码，性能高。

可见使用硬解码的方式能降低 CPU 的负载，使得视频能播放得更加流畅。

### 硬解码实现

#### 1.初始化解码器

视频的硬解码主要用到了 MediaCodec 类，对于开发者来说只要把 H.264 标准的码流通过这个类进行解码就可以得到 YUV 格式的视频了，而解码过程也是 Android 都已经封装好了，所以使用起来十分方便。本人也对硬解部分做了封装，有需要的小伙伴也可以直接拿去使用。以下的讲解也是基于我封装的基础上进行讲解的。

对于使用视频硬解，可以在 SurfaceView 的 surfaceCreated 回调里面对 MediaCodec 进行初始化。

```java
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
```

初始化的过程主要是创建一个新的 MediaCodec，而在我封装的 MediaCodecDecoder 里面主要是新建了一个进行解码的线程和一个存储视频帧的队列。通过调用 mediaCodecDecoder.start() 就完成了初始化的过程。

```java
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
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE,        mVDConfig.getWidth(), mVDConfig.getHeight());
        mMediaCodec.configure(format, mVDConfig.getDisplay().getSurface(), null, 0);
        mMediaCodec.start();
        mIsStart = true;
    } catch (Exception e) {
        Log.e(TAG, "initMediaCodec Exception" + e.getMessage());
        e.printStackTrace();
    }
}
```

可以看到在初始化主要是设置了一些基本信息，比如要解码的码流类型，视频帧的宽高以及设置解码完后要进行播放的 SurfaveView 控件。

#### 2.解码

初始化完成后就可以进行解码操作了。

```java
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
```

假设 onVideoDataCallback 是一个码流回调接口，那就可以直接调用解码器的 sendData() 进行解码操作了。

```java
public void sendData(VideoData videoData) {
    decoderQueue.offer(videoData);
}
```

可以看到主要是给解码器中的队列塞进待解码数据。

```
    public MediaCodecDecoder() {
       mFuture = ThreadPool.submit(this);
    }
```

上面我们说到解码器里面除了解码队列还有一个解码线程，解码线程是在初始化解码器的时候就创建的，而且还是从线程池中获取的。

```java
    @Override
    public void run() {
        try {
            while (!isFinish) {
                VideoData data = decoderQueue.take();//方法阻塞，有数据时继续执行。
//               Log.d(TAG, "queue size:" + decoderQueue.size() + " ,Frame_Type:" + data.getSubType());
                decodeData(data);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

通过线程的 run() 方法可以看到线程内部是一个 while 循环，循环内则是通过队列的 take() 方法获取到队列出口的第一个数据然后进行 decodeData() 解码处理。线程中的队列是 LinkedBlockingDeque，它是一个线程同步的阻塞队列，这样能避免在没有码流上来的情况下子线程一直运行造成的资源损耗。

```java
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
                mMediaCodec.queueInputBuffer(index, 0, data.length, mCount * 1000000 /    mVDConfig.getFramerate(), 0);
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
```

视频数据 dequeueOutputBuffer() 方法塞给 MediaCodec 进行硬解处理（要注意第三个参数是时间戳，既需要播放的时间，因此要线性增加，注意单位是微秒），然后再通过 dequeueOutputBuffer() 函数取出解码后的视频数据。

视频在播放的时候是以一帧一帧的形式播放的，类似于一张张图片播放。如果在单通道播放的情况下注释掉 sleepThread() 函数会发现视频播放很快，原因在于每一帧数据上来解码器都能很快进行操作并播放，比如这一帧的数据是在第5秒钟播放的，现在在第2秒钟就播了，而且前面的视频帧全部都提前播放了。所以对于每一帧视频做了相应的处理，使他们能以相对正常的进度播放。

```java
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
```

具体处理的策略就是每一帧视频播放的间隔减去代码解码需要的时间就是线程需要睡眠的时间。但是在实际的使用过程中，我们发现如果同时打开多路视频的情况，这时候队列内的数据会不断增加，这会导致越播越慢的情况。比如一个视频要一分钟播放，但是却播了两分钟，原因是由于在多路解码的情况下如果超出硬件设备所能支持的最大硬解限制就会导致解码的时间比帧间隔的时间还长，造成延迟的现象。这时候为了保证实时性，当队列内的视频帧数据超过一秒钟（可根据实际需求更改）的大小时，我们不会对视频帧进行睡眠处理。

```java
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
```

在decodeData() 函数的前面部分存在了这样的一段代码，它的策略是为了保证存在播放多路视频的需求，但是又要尽可能的保证实时性。当多路视频的情况下，设备硬解码的时间大于帧间隔，这时候队列内的数据越来越多，当数据的大小达到可以播放 10 秒（可根据实际需求更改）视频的时候，这时候只对队列内的 I 帧（关键帧）进行解码播放，而 P帧 和 B 帧 统统扔掉，当队列大小小于一秒钟（可根据实际需求更改）播放的帧数时，直到 I 帧出来后又让解码器可以解析所有类型的视频帧，这样即可保证视频的实时性（当然也有其他的策略）。

#### 3.释放解码器

当视频解码完成后，要对解码器进行释放处理。

```java
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
```

设置 setFinish(true) 释放掉线程的循环，再依次调用 mMediaCodec.stop() 和 mMediaCodec.release() 停止解码并释放。

### 总结

以上是本人基于自己对视频硬解的理解封装的解码器，其中所涉及的播放策略可根据自己的实际需求进更改并改进。