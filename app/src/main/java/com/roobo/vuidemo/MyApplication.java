package com.roobo.vuidemo;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by HP on 2019/7/4.
 */

public class MyApplication extends Application {

    public static final String TAG_HEAD = "VUIDemo_";

    public static final String TAG = TAG_HEAD + MyApplication.class.getSimpleName();

    public final static int MSG_INIT_SUCCESS = 1;
    public final static int MSG_INIT_FAIL = 2;
    public final static int MSG_AI_RESULT = 0;

    /**
     * roobo分配【保密】，需要更换成对应项目的ID
     */
    public final static String APP_ID = "zMWY0MGZiMDUyYTh";
    /**
     * roobo分配【保密】，需要更换成对应项目的PUBLIC_KEY
     */
    public final static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+tRBGVsDipoExE++l97Gyn1pY89rC6WFDcTYe9gsZCGwgBuHi0dqBdckuWTXJplCGL+PnxjyYpRQOwizAXJ5On5KA6TUDCZufhQ4dcOW7/dBPjRQt2l5WF0U0M9evPi14N4B3kogwvGhfoHsv6YL+shakKZ3lgD9gmWx+KM7qdQIDAQAB";


    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void initPermission(Activity activity) {
        String permissions[] = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        };
        ArrayList<String> toApplyList = new ArrayList<String>();
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, perm)) {
                toApplyList.add(perm);
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(activity, toApplyList.toArray(tmpList), 123);
        }
    }

    /**
     * 获取设备唯一标识符，分为两种方式：
     * 1 预分配并生产线烧录方式，直接将分配的SN号赋值给deviceID；
     * 2 通过唯一标识符在线注册方式，获取设备的唯一标识符赋值给deviceID；唯一标识可以采用序列号、MAC地址、IME号
     *
     * @return
     */
    public static String getDeviceID() {
        return getDeviceSerialNO();
    }

    /**
     * 获取设备的唯一标识符，此函数做为参考，不保证返回标识符的唯一性，开发者需要根据自身设备的特性自己实现
     *
     * @return
     */
    public static String getDeviceSerialNO() {
        String serial = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception ignored) {
        }
        if (TextUtils.isEmpty(serial)) {
            serial = android.os.Build.SERIAL;
        }
        return serial;
    }

    /**
     * 返回当前程序版本名
     */
    public static String getAppVersionName(Context context) {
        Log.d(TAG, "[getAppVersionName]");
        String versionName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.d(TAG, "[getAppVersionName] Exception:" + e.getLocalizedMessage());
        }
        return versionName;
    }
}
