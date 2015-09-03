package com.speexsocket;

import java.net.Socket;

import android.os.Handler;

public abstract class BaseAudioRecorder {
	public void startRecord(Socket s){}
	void stopRecord(){}


	protected float volumn = 80;
	public void setMicVolumn(float vol) {
		// TODO Auto-generated method stub
		volumn = vol;
	};
	
	protected boolean enableMic  = false;
	public void enableMicVolumn(boolean flat){
		enableMic = flat;
	} 

	protected boolean enableSp = false;

	public void enableSpeex(boolean flag) {
		// TODO Auto-generated method stub
		enableSp = flag;
	}
	
	protected boolean speexCompress = false;
	protected boolean speexEcho = false;
	protected boolean speexPreprocess = false;

	public static final int SPEEX_COMPRESS = 0x01;
	public static final int SPEEX_ECHO = 0x02;
	public static final int SPEEX_PREPROCESS = 0x04; 
	
	public void speexConfig(int config){
		speexCompress = (config & SPEEX_COMPRESS) == SPEEX_COMPRESS;
		speexEcho = (config & SPEEX_ECHO) == SPEEX_ECHO;
		speexPreprocess = (config & SPEEX_PREPROCESS) == SPEEX_PREPROCESS;
	}
	
	Handler activityHandler;
	public void setHandler(Handler h)
	{
		activityHandler = h;
	}
}