package com.hitachi_tstv.mist.it.gps_pxd;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AutoStartup extends Service {

    private LocationManager locationManager;
    private String latString, longString, urlString;
    final String link = "http://203.154.103.43/";
    final String project = "TmsPxd";


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Location requestLocation(String strProvider, String strError) {

        Location location = null;

        if (locationManager.isProviderEnabled(strProvider)) {


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            locationManager.requestLocationUpdates(strProvider, 1000, 10, locationListener);
            location = locationManager.getLastKnownLocation(strProvider);

        } else {
            Log.d("GPS", strError);
        }


        return location;
    }

    public final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
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
    };

    private void setupLocation() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);


    }   // setupLocation

    public boolean setLatLong(int rev) {
        boolean b = true;
        boolean result = false;

        do {
            Log.d("ServiceTag", "Do");
            String strLat = "Unknown";
            String strLng = "Unknown";
            setupLocation();
            Location networkLocation = requestLocation(LocationManager.NETWORK_PROVIDER, "No Internet");
            if (networkLocation != null) {
                strLat = String.format(new Locale("th"), "%.7f", networkLocation.getLatitude());
                strLng = String.format(new Locale("th"), "%.7f", networkLocation.getLongitude());
            }

            Location gpsLocation = requestLocation(LocationManager.GPS_PROVIDER, "No GPS card");
            if (gpsLocation != null) {
                strLat = String.format(new Locale("th"), "%.7f", gpsLocation.getLatitude());
                strLng = String.format(new Locale("th"), "%.7f", gpsLocation.getLongitude());
            }

            if (strLat.equals("Unknown") && strLng.equals("Unknown") && rev < 10) {

                rev++;
                Log.d("ServiceTag", "Repeat");
            } else if (strLat.equals("Unknown") && strLng.equals("Unknown") && rev >= 10) {
                //Can't get lat/long
                Log.d("ServiceTag", "Can't get lat/long");
                rev++;
                b = false;
            } else {
                latString = strLat;
                longString = strLng;
                b = false;
                result = true;
            }
        } while (b);


        return result;

    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }


    private class SynSendLatLng extends AsyncTask<Void, Void, Void> {
        private String dateString, latString, lngString, deviceNameString;

        SynSendLatLng(String dateString, String latString, String lngString, String deviceNameString) {
            this.dateString = dateString;
            this.latString = latString;
            this.lngString = lngString;
            this.deviceNameString = deviceNameString;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if (latString == null) {
                    latString = "";
                }
                if (lngString == null) {
                    lngString = "";
                }
                OkHttpClient okHttpClient = new OkHttpClient();
                RequestBody requestBody = new FormEncodingBuilder()
                        .add("isAdd", "true")
                        .add("gps_timeStamp", dateString)
                        .add("gps_lat", latString)
                        .add("gps_lon", lngString)
                        .add("device_name", deviceNameString).build();
                Request.Builder builder = new Request.Builder();
                Request request = builder.post(requestBody).url(urlString).build();
                okHttpClient.newCall(request).execute();

            } catch (IOException e) {
                Log.d("ServiceTag", "Do In Back ==> " + e.toString());
                e.printStackTrace();
            }
            return null;
        }
    }

    private void repeat() {
        Log.d("ServiceTag", "PXD On Loop 5 minutes");
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceString = myDevice.getName();
        Log.d("ServiceTag", "DEVICE ==> " + deviceString);
        Log.d("ServiceTag", "Lat ==> " + latString + " Long ==> " + longString);
        Log.d("ServiceTag", "Date Time ==> " + getDateTime());

        if (setLatLong(0)) {
            SynSendLatLng synSendLatLng = new SynSendLatLng(getDateTime(), latString, longString, deviceString);
            synSendLatLng.execute();
        } else {
            SynSendLatLng synSendLatLng = new SynSendLatLng(getDateTime(), latString, longString, deviceString);
            synSendLatLng.execute();
        }


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                repeat();
            }
        }, 300000);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        urlString = link + project + "/app/CenterService/updateRecNow.php";
        Log.d("ServiceTag", "Open Device");
        repeat();
    }
}
