package com.codeledger.basicxrsonic;

import android.app.Application;

import com.bose.wearable.BoseWearable;
import com.bose.wearable.Config;

public class AudioGlassesApplication extends Application {
    public void onCreate() {
        super.onCreate();
        BoseWearable.configure(this, new Config.Builder().build());
    }
}
