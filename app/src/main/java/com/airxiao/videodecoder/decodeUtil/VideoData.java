package com.airxiao.videodecoder.decodeUtil;

public class VideoData {
	
	// I帧 = 1， P帧 = 2， B帧 = 3， 数据无效 = 0
	private int subType; 
	private byte[] data;
	
	public VideoData(int subType, byte[] data) {
		super();
		this.subType = subType;
		this.data = data;
	}
	
	public int getSubType() {
		return subType;
	}
	public void setSubType(int subType) {
		this.subType = subType;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}
