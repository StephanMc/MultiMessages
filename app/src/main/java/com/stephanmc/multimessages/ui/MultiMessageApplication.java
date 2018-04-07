package com.stephanmc.multimessages.ui;

import android.app.Application;


public class MultiMessageApplication extends Application {

    private static MultiMessageApplication mInstance;

    public static MultiMessageApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
}
