package com.example.android.shushme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by aarshad on 7/24/17.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /***
     * Handles the Broadcast message sent when Geofence transition is triggered
     * This is run on the main thread so heavy tasks should be done at AsyncTask
     * @param context
     * @param intent
     */

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
