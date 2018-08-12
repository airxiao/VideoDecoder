package com.airxiao.videodecoder.decodeUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;


public class ThreadPool {
    private volatile static ExecutorService cachedThreadPool;
    private static byte[] ThreadPoolLock = new byte[] {};

    // 提交线程
    public static Future<?> submit(Runnable mRunnable) {
        if (cachedThreadPool == null) {
            synchronized (ThreadPoolLock) {
                if (cachedThreadPool == null) {
                    ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
                        private int i = 0;
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(r);
                            thread.setName("ThreadPool_" + i);
                            return thread;
                        }
                    });
                    cachedThreadPool = pool;
                }
            }
        }
        return cachedThreadPool.submit(mRunnable);
    }

    // 提交任务
    public static Future<Integer> submit(Callable<Integer> callable) {
        if (cachedThreadPool == null) {
            synchronized (ThreadPoolLock) {
                cachedThreadPool = Executors.newCachedThreadPool();
            }
        }
        return cachedThreadPool.submit(callable);
    }

    // 关闭
    public static void shutdown() {
        if (cachedThreadPool != null && !cachedThreadPool.isShutdown())
            cachedThreadPool.shutdown();
        cachedThreadPool = null;
    }
}
