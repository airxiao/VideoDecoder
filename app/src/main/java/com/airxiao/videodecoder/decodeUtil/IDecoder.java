package com.airxiao.videodecoder.decodeUtil;


public interface IDecoder {

    public void setConfig(VDConfig conf);

    public VDConfig getConfig();

    public void start();

    public void stop();

}
