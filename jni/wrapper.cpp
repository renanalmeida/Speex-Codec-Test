    #include <jni.h>  
      
    #include <string.h>  
    #include <unistd.h>  
    #include <unistd.h>
    #include <memory.h>
    #include <speex/speex.h>
    #include <speex/speex_preprocess.h>
    #include <speex/speex_echo.h>
    #include <speex/speex.h>  


    static int codec_open = 0;  
      
    static int dec_frame_size;  
    static int enc_frame_size;  
      
    static SpeexBits ebits, dbits;  
    void *enc_state;  
    void *dec_state;  

    SpeexPreprocessState *preprocess_state;
    SpeexEchoState *echo_state;

    static JavaVM *gJavaVM;
#ifdef __cplusplus
extern "C" {
#endif

    JNIEXPORT jint JNICALL Java_com_speexsocket_Speex_open  
      (JNIEnv *env, jobject obj, jint compression) {  
        int tmp;  
      
        if (codec_open++ != 0)  
            return (jint)0;  
      
        speex_bits_init(&ebits);  
        speex_bits_init(&dbits);  
      
        enc_state = speex_encoder_init(&speex_nb_mode);  
        dec_state = speex_decoder_init(&speex_nb_mode);  
        tmp = compression;  
        speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);  
        speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);  
        speex_decoder_ctl(dec_state, SPEEX_GET_FRAME_SIZE, &dec_frame_size);  
      
        preprocess_state = speex_preprocess_state_init(enc_frame_size, 8000);
	echo_state = speex_echo_state_init(enc_frame_size, 800); // the samples in 100ms

	int denoise = 1;
	int noiseSuppress = -25;
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_DENOISE,
			&denoise); 
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS,
			&noiseSuppress);

	int agc = 1;
	int q = 20000;
	//actually default is 8000(0,32768),here make it louder for voice is not loudy enough by default. 8000
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_AGC, &agc); //����
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_AGC_LEVEL, &q);

	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_ECHO_STATE,
			echo_state);

        return (jint)0;  
    }  



    JNIEXPORT jint Java_com_speexcodextest_Speex_getFrameSize
      (JNIEnv *env, jobject obj) {
      if (!codec_open)
          return 0;
       return (jint)enc_frame_size;
     }
      
    JNIEXPORT void JNICALL Java_com_speexsocket_Speex_close  
        (JNIEnv *env, jobject obj) {  
      
        if (--codec_open != 0)  
            return;  
      
        speex_bits_destroy(&ebits);  
        speex_bits_destroy(&dbits);  
        speex_decoder_destroy(dec_state);  
        speex_encoder_destroy(enc_state);  
	speex_preprocess_state_destroy(preprocess_state);
	speex_echo_state_destroy(echo_state);
    }  




    JNIEXPORT jint JNICALL Java_com_speexsocket_Speex_decode
    	(JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {
	 

	   jbyte buffer[dec_frame_size];
	    jshort output_buffer[dec_frame_size];
	    jsize encoded_length = size;

	    if (!codec_open)
		return 0;

	    speex_bits_reset(&ebits);

	    env->GetByteArrayRegion(encoded, 0, encoded_length, buffer);
	    speex_bits_read_from(&dbits, (char *)buffer, encoded_length);
	    speex_decode_int(dec_state, &dbits, output_buffer);
	    env->SetShortArrayRegion(lin, 0, dec_frame_size,
		         output_buffer);

    	return (jint)dec_frame_size;
    }


   JNIEXPORT jint Java_com_speexsocket_Speex_encode
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {

    jshort buffer[enc_frame_size];
    jbyte output_buffer[enc_frame_size];
    int nsamples = (size-1)/enc_frame_size + 1;
    int i, tot_bytes = 0;

    if (!codec_open)
        return 0;

    speex_bits_reset(&ebits);

    for (i = 0; i < nsamples; i++) {
        env->GetShortArrayRegion(lin, offset + i*enc_frame_size, enc_frame_size, buffer);
        speex_encode_int(enc_state, buffer, &ebits);
    }
    //env->GetShortArrayRegion(lin, offset, enc_frame_size, buffer);
    //speex_encode_int(enc_state, buffer, &ebits);

    tot_bytes = speex_bits_write(&ebits, (char *)output_buffer,
                     enc_frame_size);
    env->SetByteArrayRegion(encoded, 0, tot_bytes,
                output_buffer);

    return (jint)tot_bytes;
}


JNIEXPORT jboolean JNICALL Java_com_speexsocket_Speex_preprocess(
		JNIEnv * env, jobject obj, jshortArray lin, jint size) {
	jshort audio_frame[512];
	bool ret;
	int i, nsamples = (size - 1) / enc_frame_size;
	for (i = 0; i < nsamples; i++) {
		env->GetShortArrayRegion(lin, i * enc_frame_size, enc_frame_size,
				audio_frame);

		ret = speex_preprocess_run(preprocess_state, audio_frame);

		env->SetShortArrayRegion(lin, enc_frame_size * i, enc_frame_size,
				audio_frame);
	}
	return ret;
}


JNIEXPORT jint JNICALL Java_com_speexsocket_Speex_echoPlayback(
		JNIEnv * env, jobject obj, jshortArray lin, jint size) {
	jshort echo_frame[512];
	int i, nsamples = (size - 1) / enc_frame_size;
	for (i = 0; i < nsamples; i++) {
		env->GetShortArrayRegion(lin, i * enc_frame_size, enc_frame_size,
				echo_frame);
		speex_echo_playback(echo_state, echo_frame);
	}

	return nsamples * enc_frame_size;
}


JNIEXPORT jint JNICALL Java_com_speexsocket_Speex_echoCapture(
		JNIEnv * env, jobject, jshortArray lin, jint size) {
	jshort input_frame[512];
	jshort output_frame[512];
	int i, nsamples = (size - 1) / enc_frame_size;
	for (i = 0; i < nsamples; i++) {
		env->GetShortArrayRegion(lin, i * enc_frame_size, enc_frame_size,
				input_frame);
		speex_echo_capture(echo_state, input_frame, output_frame);
		env->SetShortArrayRegion(lin, dec_frame_size * i, dec_frame_size,
				output_frame);
	}

	return nsamples * enc_frame_size;
}

#ifdef __cplusplus
}
#endif
