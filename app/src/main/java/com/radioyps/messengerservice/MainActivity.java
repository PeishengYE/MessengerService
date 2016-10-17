package com.radioyps.messengerservice;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

//    MessengerService messengerService;
    TextView statusTextView;
    public static   Handler mHandler = null;
    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        infoip = (TextView) findViewById(R.id.infoip);
        statusTextView = (TextView) findViewById(R.id.status_id);

        //socketConnectServer = new SocketConnectServer(this);
        //infoip.setText(socketConnectServer.getIpAddress()+":"+ socketConnectServer.getPort());

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CommonConstants.MSG_UPDATE_STATUS:
                        statusTextView.setText((String) msg.obj);
                        Log.i(TAG, "handleMessage()>>  String " +(String) msg.obj);
                        break;

                }
            }
        };



      if(mHandler != null) {
//            Intent intent = new Intent(this, MessengerService.class);
//            startService(intent);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        socketConnectServer.onDestroy();
    }


}
