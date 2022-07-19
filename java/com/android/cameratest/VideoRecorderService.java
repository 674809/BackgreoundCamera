package com.android.cameratest;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Date;
import java.util.List;

public class VideoRecorderService extends Service implements SurfaceHolder.Callback {
    private static final String TAG = "BackgroundVideoRecorder";
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private RecoderBoardcast  mRecoderBoardcast;
    //Channel ID 必须保证唯一
    private static final String CHANNEL_ID = "com.appname.notification.channel";
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        initView();
        initBroadcast();
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel Channel = new NotificationChannel(CHANNEL_ID,"主服务",NotificationManager.IMPORTANCE_HIGH);
/*        Channel.enableLights(true);//设置提示灯
       Channel.setLightColor(Color.RED);//设置提示灯颜色
        Channel.setShowBadge(true);//显示logo
        Channel.setDescription("ytzn");//设置描述*/
        Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); //设置锁屏可见 VISIBILITY_PUBLIC=可见
        manager.createNotificationChannel(Channel);

        Notification notification = new Notification.Builder(this)
                .setChannelId(CHANNEL_ID)
          /*      .setContentTitle("服务")//标题
                .setContentText("运行中...")//内容
                .setWhen(System.currentTimeMillis())*/
                .setSmallIcon(R.drawable.battery)//小图标一定需要设置,否则会报错(如果不设置它启动服务前台化不会报错,但是你会发现这个通知不会启动),如果是普通通知,不设置必然报错
                .build();
        startForeground(1,notification);//服务前台化只能使用startForeground()方法,不能使用 notificationManager.notify(1,notification); 这个只是启动通知使用的,使用这个方法你只需要等待几秒就会发现报错了
    }

    private void initBroadcast() {
        mRecoderBoardcast = new RecoderBoardcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.stop.recorder");
        registerReceiver(mRecoderBoardcast,intentFilter);
    }

    private void initView() {
        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        surfaceView.setBackground(getResources().getDrawable(R.drawable.bg));
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }
    public static boolean isServiceExisted(Context context, String className) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList =activityManager.getRunningServices(Integer.MAX_VALUE);
        if(!(serviceList.size() > 0)) {
            return false;
        }
        for(int i = 0; i < serviceList.size(); i++) {
            ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
            ComponentName serviceName = serviceInfo.service;
            if(serviceName.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startRecoider(surfaceHolder);
    }
    public void startRecoider(SurfaceHolder surfaceHolder){
        camera = Camera.open();
        mediaRecorder = new MediaRecorder();
        camera.unlock();

        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        String path =  Environment.getExternalStorageDirectory()+"/AAAYBF/"+
                DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime())+
                ".mp4";
        Log.i(TAG,"path ="+path);
        mediaRecorder.setOutputFile(path);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
        if(mediaRecorder !=null){
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
        }

        if(camera !=null){
            camera.lock();
            camera.release();
        }
        windowManager.removeView(surfaceView);
        unregisterReceiver(mRecoderBoardcast);
    }

    public class RecoderBoardcast extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"onReceive Recoder");
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

}
