package com.odoo.core.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;


public class LocationTrack implements LocationListener {

    public static final String TAG = LocationTask.class.getSimpleName();
    private final Context mContext;
    private Odoo mOdoo;
    private OUser user;
    protected String serverURL;
    boolean checkGPS = false;
    boolean checkNetwork = false;
    boolean canGetLocation = false;
    Location loc;
    double latitude;
    double longitude;


    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;


    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    protected LocationManager locationManager;

    public LocationTrack(Context mContext, App app) {
        this.mContext = mContext;
        this.user = OUser.current(mContext);

        locationManager=(LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(mContext, "Check GPS permissions", Toast.LENGTH_SHORT);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }
    }


    public double getLongitude() {
        if (loc != null) {
            longitude = loc.getLongitude();
        }
        return longitude;
    }

    public double getLatitude() {
        if (loc != null) {
            latitude = loc.getLatitude();
        }
        return latitude;
    }



    public void stopListener() {
        if (locationManager != null) {

/*            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }*/
            locationManager.removeUpdates(LocationTrack.this);
        }
    }

    public void onLocationChanged(android.location.Location location) {
//        String urlParams = String.format("/gps?lat=%s&long=%s",location.getLatitude(), location.getLongitude());
        ORecordValues locData = new ORecordValues();
        locData.put("latitude", location.getLatitude());
        locData.put("longitude", location.getLongitude());
        new LocationTask().execute(locData);
    }


    private class LocationTask extends AsyncTask<ORecordValues, Integer, Integer> {
        @Override
        protected Integer doInBackground(ORecordValues... values) {
            try {
                if (mOdoo == null) {
                    mOdoo = OSyncAdapter.createOdooInstance(mContext, user);
                }
                mOdoo.createRecord("location.history", values[0]);
            } catch (Exception e) {
                Log.e(TAG, "Location sender AsyncTask error: " + e.toString());
            }
            return null;
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}

