package com.example.bigproject;

import android.app.Application;
import org.osmdroid.config.Configuration;


public class Myapp extends Application {
    public void onCreate() {
        super.onCreate();
        Configuration.getInstance().setUserAgentValue("com.example.bigproject");
    }
}
