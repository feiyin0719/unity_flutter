package com.example.unitylibrary;

import android.util.Log;

import com.unity3d.player.UnityPlayer;

public class FlutterApp {


    public FlutterApp() {


    }

    public void startFlutter() {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("myyf", "startFlutter");
                FlutterSurfaceFragment flutterSurfaceFragment = FlutterSurfaceFragment.createDefault();
                UnityPlayer.currentActivity.getFragmentManager().
                        beginTransaction().
                        add(flutterSurfaceFragment,"flutter")
                        .commitAllowingStateLoss();
            }
        });

    }


}
