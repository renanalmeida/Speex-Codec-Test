package com.speexsocket;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {


    private Socket speakSocket = null;
    private BaseAudioRecorder mRecorder;
    private EditText mTargetAddr;
    private ToggleButton mToggleButton;
    private SharedPreferences sp;
    int config = 0;
    WelThread mWelThread = new WelThread();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = this.getPreferences(MODE_PRIVATE);
        String text = sp.getString("addr", "");
        mTargetAddr = (EditText) findViewById(R.id.et_main_ip);
        mTargetAddr.setText(text);
        mRecorder = new AudioRecorder();
        init();
    }


    @Override
    protected void onResume() {
        AudioManager audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL); //�趨Ϊͨ����

        try {
            mWelThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        AudioManager audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL); //�趨Ϊ������

        if (mToggleButton.isChecked())
            mToggleButton.setChecked(false);

        mWelThread.safeStop();
        super.onPause();
    }


    private void init() {
        final CheckBox cbEnableSpeex = (CheckBox) findViewById(R.id.cb_comp_speex);
        cbEnableSpeex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                if (mRecorder != null)
                    mRecorder.enableSpeex(arg1);
            }
        });
        final CheckBox cbPreProcess = (CheckBox) findViewById(R.id.cb_preprocess);
        cbPreProcess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                if (arg1) {

                    config |= BaseAudioRecorder.SPEEX_PREPROCESS;
                    if (mRecorder != null)
                        mRecorder.speexConfig(config);
                }
            }
        });
        final CheckBox cbSpeexEcho = (CheckBox) findViewById(R.id.cb_speex_echo);
        cbPreProcess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                if (arg1) {
                    config |= BaseAudioRecorder.SPEEX_ECHO;
                    if (mRecorder != null)
                        mRecorder.speexConfig(config);
                }
            }
        });

        mToggleButton = (ToggleButton) findViewById(R.id.tb_openSocket_main);
        mToggleButton.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton bt, boolean isChecked) {


                // TODO Auto-generated method stub
                if (isChecked) {
                    new Thread(new Runnable()

                    {
                        public void run() {
                            try {

                                if (speakSocket == null) {

                                    speakSocket = new Socket(mTargetAddr.getText()
                                            .toString(), 60000);
                                    mRecorder = new AudioRecorder();

                                    SharedPreferences.Editor ed = sp.edit();
                                    ed.putString("addr", mTargetAddr.getText()
                                            .toString());
                                    ed.commit();
                                }
                                mRecorder.setHandler(mHandler);
                                mRecorder.enableSpeex(cbEnableSpeex.isChecked());
                                mRecorder.speexConfig(config);
                                mRecorder.enableMicVolumn(true);
                                mRecorder.startRecord(speakSocket);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();

                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Falha na conexão.", Toast.LENGTH_LONG).show();
                                        mToggleButton.setChecked(false);
                                    }
                                });

                            }
                        }
                    }).start();
                } else {
                    mRecorder.stopRecord();
                    try {
                        if (speakSocket != null && !speakSocket.isClosed())
                            speakSocket.close();
                        speakSocket = null;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!mToggleButton.isChecked())
                        mToggleButton.setChecked(false);
                    speakSocket = (Socket) msg.obj;
                    mToggleButton.setChecked(true);
                    break;
                case 2:
                    mToggleButton.setChecked(false);
            }
        }
    };


    class WelThread extends Thread {
        ServerSocket ss;
        boolean isLooped = true;

        @Override
        public void run() {
            try {
                ss = new ServerSocket(60000);
                isLooped = true;

                while (isLooped) {
                    Socket temp_speakSocket = ss.accept();
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = temp_speakSocket;
                    mHandler.sendMessage(msg);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void safeStop() {
            if (ss != null && !ss.isClosed())
                try {
                    ss.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            isLooped = false;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
