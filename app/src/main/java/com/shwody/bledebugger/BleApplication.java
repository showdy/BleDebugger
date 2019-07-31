package com.shwody.bledebugger;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class BleApplication extends Application {

    private static final String TAG = "BleApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.d(TAG, "onActivityCreated: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.d(TAG, "onActivityStarted: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d(TAG, "onActivityResumed: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d(TAG, "onActivityPaused: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d(TAG, "onActivityStopped: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }
}
