package com.example.android.shushme;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    CheckBox locationsPermissions;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    public static final int PLACE_PICKER_REQUEST = 1;
    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private GoogleApiClient mClient;

    Geofencing mGeoFencing;
    private boolean mIsEnabled;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationsPermissions = (CheckBox) findViewById(R.id.location_permission_checkbox);
        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this,null);
        mRecyclerView.setAdapter(mAdapter);

        // Initialize the switch state and handle enable / disable switch change
        Switch offSwitch = (Switch) findViewById(R.id.enable_switch);
        mIsEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled),false);
        offSwitch.setChecked(mIsEnabled);
        offSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled), isChecked);
                mIsEnabled = isChecked;
                editor.commit();
                if (isChecked) {
                    mGeoFencing.registerAllGeofences();
                } else {
                    mGeoFencing.unRegisterAllGeofences();
                }
            }
        });

        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)
                .build();

        mGeoFencing = new Geofencing(this,mClient);

    }
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            locationsPermissions.setChecked(false);
        } else {
            locationsPermissions.setEnabled(true);
            locationsPermissions.setChecked(true);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        refreshPlacesData();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    public void onLocationPermissionClicked(View view) {
            ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    public void refreshPlacesData(){
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor data = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);
        if (data == null || data.getCount()==0) return;
        List<String> guids = new ArrayList<>();
        while (data.moveToNext()){
            guids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }
        // PlaceBuffer is Places API object which access the list of places
        PendingResult<PlaceBuffer> placesResult = Places.GeoDataApi.getPlaceById(mClient,
                guids.toArray(new String[guids.size()]));
        placesResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapPlaces(places);
                mGeoFencing.updateGeofenceList(places);
                if (mIsEnabled){
                    mGeoFencing.registerAllGeofences();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK){

            Place place = PlacePicker.getPlace(this,data);

            if (place == null) {
                Log.i(TAG, "No place selected");
                return;
            }
            // Extract the place information from the API
            String placeName = place.getName().toString();
            String placeAddress = place.getAddress().toString();
            String placeID = place.getId();

            // Insert a new place into DB
            ContentValues contentValues = new ContentValues();
            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);
            refreshPlacesData();
            Toast.makeText(this,  placeName + " was selected and inserted" , Toast.LENGTH_LONG).show();
        }
    }

    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.location_permissions_granted_message), Toast.LENGTH_LONG).show();

        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            Intent i = builder.build(this);
            startActivityForResult(i,PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
        }

    }
}
