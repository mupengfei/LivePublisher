package com.eju.live.ejulivepublisherdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.eju.live.publisher.PublisherFactory;
import com.eju.live.publisher.SrsEncoder;
import com.eju.live.publisher.SrsFlvMuxer;
import com.eju.live.publisher.SrsMp4Muxer;
import com.eju.live.publisher.rtmp.RtmpPublisher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String TAG = "Yasea";

    Button btnPublish = null;
    Button btnSwitch = null;
    Button btnRecord = null;
    Button btnFocus = null;
    Button btnAddFocus = null;
    Button btnSubFocus = null;
    ZoomControls mZoomControls = null;
    private SurfaceView mCameraView = null;

    private String mNotifyMsg;
    private SharedPreferences sp;
    //    private String rtmpUrl = "rtmp://ossrs.net/" + getRandomAlphaString(3) + '/' + getRandomAlphaDigitString(5);

    private SrsFlvMuxer flvMuxer = new SrsFlvMuxer(new RtmpPublisher.EventHandler() {
        @Override
        public void onRtmpConnecting(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpConnected(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {
        }

        @Override
        public void onRtmpAudioStreaming(String msg) {
        }

        @Override
        public void onRtmpStopped(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            Log.i(TAG, String.format("Output Fps: %f", fps));
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // for camera, @see https://developer.android.com/reference/android/hardware/Camera.html
        btnPublish = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.publish);
        btnSwitch = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.swCam);
        btnRecord = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.record);
        btnFocus = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.focus);
        btnAddFocus = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.addFocus);
        btnSubFocus = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.subFocus);
        mZoomControls = (ZoomControls) findViewById(com.eju.live.ejulivepublisherdemo.R.id.zoomControls);
        mCameraView = (SurfaceView) findViewById(com.eju.live.ejulivepublisherdemo.R.id.preview);

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPublish.getText().toString().contentEquals("publish")) {
                    PublisherFactory.init(MainActivity.this, "").startPublish(0, mCameraView);
                    // initialize url.
                    final EditText efu = (EditText) findViewById(com.eju.live.ejulivepublisherdemo.R.id.url);
                    efu.setText(PublisherFactory.getInstance().getRtmpUrl());
                    btnPublish.setText("stop");
                } else if (btnPublish.getText().toString().contentEquals("stop")) {
                    PublisherFactory.getInstance().stopPublish();
                    btnPublish.setText("publish");
                    btnRecord.setText("record");
                }
            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().switchCamera();
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        btnFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().focus();
            }
        });

        btnAddFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().addFocus();
            }
        });

        btnSubFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().subFocus();
            }
        });

        mZoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().addFocus();
            }
        });

        mZoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublisherFactory.getInstance().subFocus();
            }
        });

//        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            @Override
//            public void uncaughtException(Thread thread, Throwable ex) {
//                mNotifyMsg = ex.getMessage();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.e("mNotifyMsg", mNotifyMsg);
//                        Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_LONG).show();
//                        btnPublish.setText("publish");
//                        btnRecord.setText("record");
//                    }
//                });
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.eju.live.ejulivepublisherdemo.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == com.eju.live.ejulivepublisherdemo.R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Button btn = (Button) findViewById(com.eju.live.ejulivepublisherdemo.R.id.publish);
        btn.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        PublisherFactory.getInstance().stopPublish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        btnRecord.setText("record");
//        stopEncoder();
//        mEncoder.setScreenOrientation(newConfig.orientation);
//        if (btnPublish.getText().toString().contentEquals("stop")) {
//            startEncoder();
//        }
    }
}
