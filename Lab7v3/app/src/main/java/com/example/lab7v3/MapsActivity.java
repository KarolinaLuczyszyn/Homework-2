package com.example.lab7v3;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    //private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<LatLng> markerList;

    static public SensorManager mSensorManager;
    static Sensor sensor;

    boolean fab = false;

    private final String TASKS_JSON_FILE = "tasks.json";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
        restoreFromJson();
        for (LatLng latLng : markerList){
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker4))
                    .alpha(0.8f)
                    .title(String.format("Position:(%.2f, %.2f)", latLng.latitude, latLng.longitude)));
        }
    }

    public void zoomInClick(View v)
    {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v)
    {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(),"MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null)
                {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //float distance = 0f;

        /*if(markerList.size() > 0) {
            Marker lastMarker = markerList.get(markerList.size() - 1);
            float [] tmpDis = new float[3];
            Location.distanceBetween(lastMarker.getPosition().latitude, lastMarker.getPosition().longitude,
                    latLng.latitude, latLng.longitude, tmpDis);

            distance = tmpDis[0];


            PolylineOptions rectOptions = new PolylineOptions()
                    .add(lastMarker.getPosition())
                    .add(latLng)
                    .width(10)
                    .color(Color.BLUE);
            mMap.addPolyline(rectOptions);
        }*/

       Marker marker = mMap.addMarker(new MarkerOptions()
               .position(new LatLng(latLng.latitude,latLng.longitude))
               .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker4))
               .alpha(0.8f)
               .title(String.format("Position: (%.2f, %.2f)",latLng.latitude,latLng.longitude)));
       markerList.add(latLng);

       saveTasksToJson();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        /*CameraPosition cameraPos = mMap.getCameraPosition();
        if(cameraPos.zoom <14f)
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));*/
        View fab = findViewById(R.id.fab);
        fab.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce));
        fab.setVisibility(View.VISIBLE);

        View fab2 = findViewById(R.id.fab2);
        fab2.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce));
        fab2.setVisibility(View.VISIBLE);

        /*TextView acceleration_text_view = findViewById(R.id.acceleration_text_view);
        acceleration_text_view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce));
        acceleration_text_view.setVisibility(View.VISIBLE);*/
        return false;
    }

    private void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates()
    {
        fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback,null);
    }

    private void createLocationCallback()
    {
        locationCallback = new LocationCallback(){

            @Override
            public void onLocationResult(LocationResult locationResult){
                // Code executed when user's location changes
                if(locationResult != null)
                {
                    //Remove the last reported position
                    if(gpsMarker != null)
                    {
                        gpsMarker.remove();

                        //Add a custom marker to the map for location from the locationResult
                        Location location = locationResult.getLastLocation();
                        gpsMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(),location.getLongitude()))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                                .alpha(0.8f)
                                .title("Current Location"));

                    }
                }
            }

        };
    }

    private void stopLocationUpdates()
    {
        if(locationCallback != null)
        {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    public void onClickHideButtons(View view) {
        View fab = findViewById(R.id.fab);
        fab.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
        fab.setVisibility(View.INVISIBLE);

        View fab2 = findViewById(R.id.fab2);
        fab2.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
        fab2.setVisibility(View.INVISIBLE);


        View acceleration_text_view = findViewById(R.id.acceleration_text_view);
        if (acceleration_text_view.getVisibility() == View.VISIBLE)
        {
            acceleration_text_view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
            acceleration_text_view.setVisibility(View.INVISIBLE);
        }

    }

    public void onClickClearMemory(View view) {
        markerList.clear();
        mMap.clear();

        View fab = findViewById(R.id.fab);
        View fab2 = findViewById(R.id.fab2);
        View acceleration_text_view = findViewById(R.id.acceleration_text_view);
        if (fab.getVisibility() == View.VISIBLE)
        {
            fab.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
            fab.setVisibility(View.INVISIBLE);
        }
        if (fab2.getVisibility() == View.VISIBLE)
        {
            fab2.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
            fab2.setVisibility(View.INVISIBLE);
        }
        if (acceleration_text_view.getVisibility() == View.VISIBLE)
        {
            acceleration_text_view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
            acceleration_text_view.setVisibility(View.INVISIBLE);
        }

    }

    public void StartAcceleration(View view) {
        TextView acceleration_text_view = findViewById(R.id.acceleration_text_view);
        if (fab == false)
        {
            fab = true;
            acceleration_text_view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.bounce));
            acceleration_text_view.setVisibility(View.VISIBLE);
        }else
        {
            fab = false;
            acceleration_text_view.startAnimation(AnimationUtils.loadAnimation(this,R.anim.fadeout));
            acceleration_text_view.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (fab == true)
        {
            TextView acceleration_text_view = findViewById(R.id.acceleration_text_view);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Acceleration:\n");
            stringBuilder.append(String.format("x: %4f\r\r\r", event.values[0]));
            stringBuilder.append(String.format("y: %4f", event.values[1]));
            acceleration_text_view.setText(stringBuilder.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume(){
        super.onResume();
        if (sensor != null) {
            MapsActivity.mSensorManager.registerListener(this, sensor,100000);
        }
        //mSensorManager.registerListener((SensorEventListener) this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopLocationUpdates();
        if (sensor != null) {
            MapsActivity.mSensorManager.unregisterListener(this, sensor);
        }
       // mSensorManager.unregisterListener(this);
    }

    private void saveTasksToJson(){
        Gson gson = new Gson();
        String listJson = gson.toJson(markerList);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(TASKS_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void restoreFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new GsonBuilder().setLenient().create();
        String readJson;

        try{
            inputStream = openFileInput(TASKS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while((n = reader.read(buf)) >= 0){
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>(){}.getType();
            List<LatLng> o = gson.fromJson(readJson, collectionType);
            if(o != null){
                markerList.clear();
                for(LatLng latLng : o){
                    markerList.add(new LatLng(latLng.latitude, latLng.longitude));
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
