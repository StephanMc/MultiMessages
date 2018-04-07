package com.stephanmc.multimessages.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Device info for bug report
 */
public class DeviceInfo {
    private static final String TAG = DeviceInfo.class.getSimpleName();

    public static boolean isConnectedToMobileInternet(Context context) {
        // mobile
        State mobile = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(
                ConnectivityManager.TYPE_MOBILE).getState();

        return mobile == NetworkInfo.State.CONNECTED || mobile == NetworkInfo.State.CONNECTING;
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public static String getInfosAboutDevice(Activity activity) {
        StringBuilder result = new StringBuilder();
        try {
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(),
                    PackageManager.GET_META_DATA);
            result.append("\n Package Name: ").append(activity.getPackageName());
            result.append("\n Version Name: ").append(packageInfo.versionName);
            result.append("\n Version Code: ").append(packageInfo.versionCode);
            result.append("\n");
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        result.append("\n OS Version: ").append(System.getProperty("os.version")).append(" (").append(
                Build.VERSION.INCREMENTAL).append(")");
        result.append("\n OS API Level: ").append(Build.VERSION.SDK);
        result.append("\n Device: ").append(Build.DEVICE);
        result.append("\n Model (and Product): ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(")");

        // more from
        // http://developer.android.com/reference/android/os/Build.html :
        result.append("\n Manufacturer: ").append(Build.MANUFACTURER);
        result.append("\n Other TAGS: ").append(Build.TAGS);

        result.append("\n screenWidth: ").append(
                activity.getWindow().getWindowManager().getDefaultDisplay().getWidth());
        result.append("\n screenHeigth: ").append(
                activity.getWindow().getWindowManager().getDefaultDisplay().getHeight());
        result.append("\n Keyboard available: ").append(
                activity.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS);

        result.append("\n Trackball available: ").append(
                activity.getResources().getConfiguration().navigation == Configuration.NAVIGATION_TRACKBALL);
        result.append("\n SD Card state: ").append(Environment.getExternalStorageState());
        Properties systemProperties = System.getProperties();
        Enumeration keys = systemProperties.keys();
        String key;
        while (keys.hasMoreElements()) {
            key = (String) keys.nextElement();
            result.append("\n > ").append(key).append(" = ").append(systemProperties.get(key));
        }
        return result.toString();
    }

    public static boolean isScreenOn(Context context) {
        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
    }

    public static boolean isConnectedToWifi(Context context) {
        // via vwifi
        State wifi = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(
                ConnectivityManager.TYPE_WIFI).getState();
        return wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING;
    }

    /**
     * @return true if the current thread is the UI thread
     */
    public static boolean isUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    /**
     * @return the size with size.x=width and size.y=height
     */
    public static Point getScreenSize(Activity a) {
        return getScreenSize(a.getWindowManager().getDefaultDisplay());
    }

    @SuppressLint("NewApi")
    private static Point getScreenSize(Display display) {
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            display.getSize(size);
        } else {
            size.x = display.getWidth();
            size.y = display.getHeight();
        }
        return size;
    }

}