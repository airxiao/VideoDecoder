package com.airxiao.videodecoder.decodeUtil;

import android.view.SurfaceHolder;


public class VDConfig {

	private int mWidth; 
	private int mHeight;

//	private int mStreamType = 0;
	private int mFramerate;
	
	private SurfaceHolder mSurfaceHolder;
	
	public void setWidth(int nWidth){
		mWidth = nWidth;
	}
	
	public int getWidth(){
		return mWidth;
	}
	
	public void setHeight(int nHeight){
		mHeight = nHeight;
	}
	
	public int getHeight(){
		return mHeight;
	}

//    public int getStreamType() {
//        return mStreamType;
//    }
//
//    public void setStreamType(int streamType) {
//        this.mStreamType = streamType;
//    }
    
    public void setDisplay(SurfaceHolder surfaceHolder){
        mSurfaceHolder = surfaceHolder;
    }
    
    public SurfaceHolder getDisplay(){
        return mSurfaceHolder;
    }

    public int getFramerate() {
        return mFramerate;
    }

    public void setFramerate(int framerate) {
        this.mFramerate = framerate;
    }

}
