package com.android.cameratest;

import android.app.Activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.cameratest.permissioin.Permissions;

import java.util.List;

public class MainActivity extends Activity {
    private String TAG ="BackgroundVideoRecorder";

    public Context mContext;
    public static String BASE_PATH = "/sdcard/AAAAAA";
    private AutoFitTextureView mTextureview;
    private boolean mIsRecordingVideo; //开始停止录像
    //public static String BASE_PATH = Environment.getExternalStorageDirectory() + "/AAAYBF";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        Permissions.requestPermissionAll(this);
       boolean isSurvival = isServiceExisted(mContext,"com.android.cameratest.VideoRecorderService");
        Log.i(TAG,"isSurvival :"+isSurvival);
       if(isSurvival){
           stop();
        }else {
            start();
        }
         finish();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void stop() {
        Intent intent = new Intent();
        intent.setAction("com.stop.recorder");
        sendBroadcast(intent);
    }
    private void start() {
        Intent intent = new Intent(this, VideoRecorderService.class);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Permissions.changePermissionState(this,permissions[0],true);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
      /*  if(mCameraController !=null){
            mCameraController.stopRecordingVideo();
        }*/

    }
}