package com.radioyps.messengerservice;

/**
 * Created by developer on 17/10/16.
 */
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



        import android.app.KeyguardManager;
        import android.app.Notification;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.app.Service;
        import android.app.admin.DevicePolicyManager;
        import android.content.ComponentName;
        import android.content.Context;
        import android.content.Intent;
        import android.os.Binder;
        import android.os.Handler;
        import android.os.IBinder;
        import android.os.Message;
        import android.os.Messenger;
        import android.os.PowerManager;
        import android.os.RemoteException;
        import android.util.Log;
        import android.widget.Toast;

        import java.util.ArrayList;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
//        import com.radioyps.apidemos.R;
//        import com.radioyps.apidemos.app.RemoteService.Controller;

/**
 * This is an example of implementing an application service that uses the
 * {@link Messenger} class for communicating with clients.  This allows for
 * remote interaction with a service, without needing to define an AIDL
 * interface.
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
//BEGIN_INCLUDE(service)
public class MessengerService extends Service {
    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;
    int mCount = 0;
    private static final String TAG = "MessengerService";

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;

    private  boolean mStopThreadUpdate = false;
    /**
     * Handler of incoming messages from clients.
     */

    private  PowerManager.WakeLock wakeLock = null;
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    startUpdateMessageThread();
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    mStopThreadUpdate = true;
                    break;
                case MSG_SET_VALUE:
                    mValue = msg.arg1;

                    Log.i(TAG, " Message got new Value: " + mValue );
                    for (int i=mClients.size()-1; i>=0; i--) {
                        Log.i(TAG, " sendMessage to everyone with the value got: " + mValue );
                        try {
                            mClients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, mValue, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            Log.i(TAG, " client is dead. remove client id: " + i );
                            mClients.remove(i);
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Handler mHandler = new IncomingHandler();
    final Messenger mMessenger = new Messenger(mHandler);

    @Override
    public void onCreate() {
        Log.i(TAG, "MessengerService:  onCreate()>>    ");
        sendMessage(CommonConstants.MSG_UPDATE_STATUS,"MessengerService:  onCreate()>>");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);


        // Display a notification about us starting.
        showNotification();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "MessengerService:  onDestroy()>>    ");

        // Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
        try {
            wakeLock.release();
        }catch(Exception e){
            Log.e(TAG, "tring to release unlocked wakelock" );
           // Log.e(TAG, e.getMessage() );
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, " onBind()>> MessengerService: getBinding  ");
        sendMessage(CommonConstants.MSG_UPDATE_STATUS,"MessengerService: getBinding ");
        return mMessenger.getBinder();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }

    /* you cannot access the mHandler in a other process,
    * it always give null pointer
    *
    * FIXME this is a dummy code, it show here just to say this is not a good way
    * */
    private void sendMessage(int messageFlag, String mesg){
        Log.i(TAG, "sendMessage()>>" + mesg );
        if(MainActivity.mHandler == null){
            Log.i(TAG, "sendMessage()>> mHandler == null");
        }else{
        Message.obtain(MainActivity.mHandler, messageFlag, mesg).sendToTarget();
        }
    }

    private void startUpdateMessageThread(){
        mStopThreadUpdate = false;
        Thread initThread = new Thread(new updateMessageThread());
        initThread.start();
    }

//    private void lockScreen(){
//        DevicePolicyManager deviceManger = (DevicePolicyManager)getSystemService(
//                Context.DEVICE_POLICY_SERVICE);
////        activityManager = (ActivityManager)getSystemService(
////                Context.ACTIVITY_SERVICE);
//      ComponentName  compName = new ComponentName(this, MyAdmin.class);
//        boolean active = deviceManger.isAdminActive(compName);
//        if (active) {
//            deviceManger.lockNow();
//        }
//    }

    private void unLockScreen(){
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock kl = km .newKeyguardLock("MyKeyguardLock");
        kl.disableKeyguard();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
         wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        Log.i(TAG, "unLockSceen()>> acquire the wakelock");
        wakeLock.acquire();
    }

   private  void lockScreen(){
       Log.i(TAG, "lockSceen()>> release wakelock");
       wakeLock.release();
   }

    private class updateMessageThread extends Thread{
        public void run(){
            //Message.obtain(mMessenger, MSG_SET_VALUE, mCount).sendToTarget();
            while (!mStopThreadUpdate) {
                mCount += 1;
                //Log.i(TAG, "updateMessageThread. run()>> mCount: " + mCount);
//                mValue = mCount;
                Message.obtain(mHandler, MSG_SET_VALUE, mCount, 0 ).sendToTarget();
                /* Fixme the following code is wrong, as int is not a object of java

                *  public static Message obtain(Handler h, int what, Object obj)
                *
                *  nothing can be received on the client side
                *
                * */
               // Message.obtain(mHandler, MSG_SET_VALUE, mCount).sendToTarget();

                if(mCount%40 ==0 ){
                    unLockScreen();

                }
                if(mCount%45 ==0){
                    lockScreen();

                }

                try {
                    updateMessageThread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


}
//END_INCLUDE(service)
