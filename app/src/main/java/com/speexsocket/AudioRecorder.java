package com.speexsocket;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * ��Ƶ����ģ��
 * 
 * @author liling
 * @version 1.0.0
 */
public class AudioRecorder extends BaseAudioRecorder {
	private boolean isRecording = false;// �Ƿ�¼�ŵı��
	private static final int frequency = 8000; // ��ͬ���ֻ�Ӳ�����ò�һ��������Ҫ���²���
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int recBufSize;
	private int playBufSize;
	private AudioRecord audioRecord;
	private AudioTrack audioTrack;
	private DataOutputStream out;
	private BufferedInputStream bufferIns;
	private Socket client;

	private Speex speex; 
	public AudioRecorder() {
	}

	/**
	 * ��ʼ¼��
	 */
	public void startRecord(Socket s) {
		try {
			speex = Speex.getInstance();
			
			client = s;
			recBufSize = AudioRecord.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding);
			
			
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					frequency, channelConfiguration, audioEncoding, recBufSize);

			playBufSize = AudioTrack.getMinBufferSize(frequency,
					channelConfiguration, audioEncoding);
			
			//recBufSize = speex.fixToSpeexSize(recBufSize, speex.getFrameSize());
			// = speex.fixToSpeexSize(playBufSize, speex.getEncodedFrameSize());
			
			
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
					channelConfiguration, audioEncoding, playBufSize,
					AudioTrack.MODE_STREAM);
			audioTrack.setStereoVolume(AudioTrack.getMaxVolume(),
					AudioTrack.getMaxVolume());// ���õ�ǰ������СΪ���ֵ
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		isRecording = true;
		new RecordPlayThread().start();
		new TrackPlayThread().start();
	}

	/**
	 * ֹͣ¼��
	 */
	public void stopRecord() {
		if (!isRecording)
			return;
		isRecording = false;
		try {
			out.flush();
			out.close();
			bufferIns.close();
			client.close();
			Speex.release();
			release();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void release() {
		audioRecord.release(); // �ͷ���Դ���ɶ�ν�����Ƶ����
		audioTrack.release();
	}

	class RecordPlayThread extends Thread {
		public void run() {
			try { 
				byte[] buffer = new byte[recBufSize]; //��ʵ������ô��Speexѹ��Ч�ʺ�����
				short[] bufferS = new short[recBufSize];
				
				audioRecord.startRecording(); // ��ʼ¼��
				out = new DataOutputStream(client.getOutputStream());
				while (isRecording) {
					// ��MIC������ݵ�������
					int bufferReadResult = audioRecord.read(bufferS, 0,
							recBufSize);
					Log.d("tag1", "��С:" + bufferReadResult);


					if (!enableSp)
						out.write(toByte(bufferS), 0, bufferReadResult*2);
					
					else {
						int encodeSize = bufferReadResult;
						int decodeSize = bufferReadResult;

						if (speexEcho) {
							speex.echoCapture(bufferS, bufferReadResult);
						}

						if (speexPreprocess)
							speex.preprocess(bufferS, bufferReadResult);

						if (speexCompress) {
							Log.d("Recorder","speexCompress!!!");
							encodeSize = speex.encode(bufferS, 0, buffer,
									bufferReadResult);
							out.write(buffer, 0, encodeSize); 
						}

						else
							out.write(toByte(bufferS), 0, bufferReadResult*2); 
					}
					
				}
				audioRecord.stop();
			} catch (SocketException e) {
				e.printStackTrace(); 
			} catch (Exception e) {
				e.printStackTrace();
				try {
					audioRecord.stop();
				} catch (Exception e1) {
				}
			}
		}
	};

	class TrackPlayThread extends Thread {
		public void run() {
			try {
				bufferIns = new BufferedInputStream(client.getInputStream());
				byte[] buffer = new byte[playBufSize];
				short[] bufferS;
				audioTrack.play();// ��ʼ����
				int bufferReadLen = 0;
				while ((bufferReadLen = bufferIns.read(buffer)) >= 0) {  
					int decodeSize = bufferReadLen/2;
					bufferS = toShort(buffer);
					
					if(speexCompress)
					{
						bufferS = new short[bufferReadLen*160/(24)];
						decodeSize = speex.decode(buffer, bufferS,
								bufferReadLen); //ѹ�����ȵ���socket���յ��ĳ���
					}

					if (speexEcho) {
						speex.echoPlayback(bufferS, decodeSize);
					}
					audioTrack.write(toByte(bufferS), 0, decodeSize*2);
				}
				audioTrack.stop();
			} 
			catch (SocketException e) {
				e.printStackTrace();
				activityHandler.sendEmptyMessage(2);
				try {
					audioTrack.stop();
				} catch (Exception e1) {
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
				try {
					audioTrack.stop();
				} catch (Exception e1) {
				}
			}
		}
	}

	protected byte[] toByte(short[] inputData) {
		int len = inputData.length * 2;
		byte[] ret = new byte[len];

		for (int i = 0; i < len; i += 2) {
			ret[i] = (byte) (inputData[i / 2] & 0xff);
			ret[i + 1] = (byte) ((inputData[i / 2] >> 8) & 0xff);
		}
		return ret;
	}
	
	protected short[] toShort(byte[] inputData) {
		int len = inputData.length / 2;
		short[] ret = new short[len];

		for (int i = 0; i < len; i++) {
			ret[i] = (short) ((inputData[i * 2 + 1] << 8) & 0xffff | (inputData[i * 2] & 0x00ff));
		}
		return ret;
	}
}
