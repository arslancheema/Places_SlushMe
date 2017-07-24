package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aarshad on 7/24/17.
 */

public class Geofencing implements ResultCallback {

    private static final long GEOFENCE_TIMEOUT = 86400000;
    private static final float GEOFENCE_RADIUS = 100000 ;
    private static final String TAG = Geofencing.class.getSimpleName() ;

    GoogleApiClient mGoogleApiClient;
    Context mContext;
    PendingIntent mGeofencePendingIntent;
    List<Geofence> mGeofenceList ;

    public Geofencing (Context context, GoogleApiClient googleApiClient){
        this.mContext = context;
        this.mGoogleApiClient = googleApiClient;
        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<>();

    }

    /***
     * Registers the list of geofences specified in mGeofenceList
     * Uses mGoogleApiClient to connect to the Google API Client
     * Uses getGeofencingRequest to get the list of geofences to be registered
     * Uses getGeofencePendingIntent to get the pending Intent to launch the PendingIntent
     */
    public void registerAllGeofences (){
        if (mGoogleApiClient==null || !mGoogleApiClient.isConnected()
            || mGeofenceList == null || mGeofenceList.size()==0){

        }
        try {
            LocationServices.GeofencingApi.addGeofences(
              mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException){
            Log.e(TAG,"Exception : " + securityException.getMessage());
        }

    }

    /***
     * Registers the list of geofences specified in mGeofenceList
     * Uses mGoogleApiClient to connect to the Google API Client
     * Uses getGeofencePendingIntent to get the pending Intent to launch the PendingIntent
     */
    public void unRegisterAllGeofences(){
        if (mGoogleApiClient==null || !mGoogleApiClient.isConnected()){
        return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException){
            Log.e(TAG,"Exception : " + securityException.getMessage());
        }

    }


    /***
     * Updates the local ArrayList of Geofences with data passed in the list.
     *
     * @param places is the PlaceBuffer result of getPlaceById method.
     */
    public void updateGeofenceList(PlaceBuffer places){
        mGeofenceList = new ArrayList<>();
        if (places == null || places.getCount()==0) return;
        for(Place place : places){
            String placeUID = place.getId();
            double placeLatitude = place.getLatLng().latitude;
            double placeLongitude = place.getLatLng().longitude;
            // build a geofence object
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLatitude,placeLongitude,GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            mGeofenceList.add(geofence);
        }
    }

    /***
     * Creates a GeofencingRequest object using mGeofenceList ArrayList
     *
     * @return
     */
    private GeofencingRequest getGeofencingRequest(){
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // setInitialTrigger tells what will happen if the device is already in Geofence we are about to register
        // INITIAL_TRIGGER_ENTER triger Entry Transition Event immediately
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    /***
     * Creates a PendingIntent Object using GeofenceTransitionsIntentService class
     *
     * @return PendingIntent Object
     */
    private PendingIntent getGeofencePendingIntent(){
        // Reuse the pending Intent if we already have
        if (mGeofencePendingIntent!=null){
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG, "Error adding/removing Geofences : %s " + result.getStatus().toString());
    }
}
