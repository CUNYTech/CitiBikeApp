package com.cunycodes.bikearound;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

//import com.google.android.gms.identity.intents.Address;

public class MapsActivity extends AppCompatActivity //FragmentActivity - changed by Jody
                          implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener,
                          NavigationView.OnNavigationItemSelectedListener, ResultCallback<LocationSettingsResult> {

    private final String TAG = "MapsActivity";
    private static final int GOOGLE_API_CLIENT = 0;
    int REQUEST_CHECK_SETTINGS = 100;
    private String GOOGLE_DIRECTIONS_KEY = "AIzaSyBuwP1BalG9FdpoU0F5LCmHvkJOlULK6to";
    final String CITI_API_URL = "https://gbfs.citibikenyc.com/gbfs/en/station_information.json";
    final String STATION_STATUS_URL = "https://gbfs.citibikenyc.com/gbfs/en/station_status.json";
    final String GOOGLE_PLACES_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";    //Added by Mike, requires additional search criteria after url
    final String GOOGLE_PLACES_KEY = "AIzaSyDVaebTQTxdveWdMCzwAC2yj55aep6-roU";                         //added by Mike. API key to google places API
    private FirebaseUser user;                                                                         // added by Jody --do not delete, comment out if you need to operate without user
    private FirebaseAuth mAuth;                                                                        // added by Jody --do not delete, comment out if you need to operate without user
    private TextView nav_name;                                                                        // added by Jody --do not delete, comment out if you need to operate without user
    private TextView nav_membership;                                                                  // added by Jody --do not delete, comment out if you need to operate without user
    private GoogleMap mMap;
    protected GoogleApiClient mGoogleApiClient;
    private final int PERMISSION_LOCATION = 111;
    private LatLng currrentLatLng;
    private Location currentLocation;
    double currentLatitudeTEST = 40.716974;
    double currentLongitudeTEST = -74.012360;
    String LOWTIME = "03:00";
    double currentLatitude;
    double currentLongitude;
    private String text;
    private EditText textAddress;
    private Button btnSearch;
    private UserDBHelper helper;
    private SQLiteDatabase database;
    private String userMembership;
    private List<CitiBikeLocations> mLocations = new ArrayList<>();
    private CountDownTimer mCountDownTimer;
    private TextView timerView;
    private Button startTimer;
    private String timeInMinutes;
    private long timeInSeconds;
    private String distanceInMiles;
    private long distanceInMeters;
    private String markerLocationName;
    private Dialog dialog;
    final private int metersPerThirtyMin = 5800;//6700 m per 30 min on bike - mike
    final private int metersPerFortyFiveMin = 8500;//10900 m per 45 min on bike -mike
    final private int bikeTime = metersPerThirtyMin;
    long durationTimeBetweenStationsInSecs;
    private LatLng nearestLocationOnSearch;
    protected LocationRequest mLocationRequest;
    private TextToSpeech mTextToSpeech;
    int result;
    private boolean poiButtonClicked = false;
    StationInformation stationInformation = new StationInformation(); //Create a new class to hold Station information.
    private String stopsToShow = "";
    protected static List<EventPlan> allEvents;
    private Uri uri;
    private String stringUri;
    private ImageView mUsers_photo;







    ///////////////////////////////////////////////////////////////////////////////////////////////


//    private LocationManager locationManager = (LocationManager)
//            getSystemService(Context.LOCATION_SERVICE);
//    Criteria criteria = new Criteria();
//    private Location location = locationManager.getLastKnownLocation(locationManager
//            .getBestProvider(criteria, false));


    ///////////////////////////////////////////////////////////////////////////////////////////////


    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_maps); //changed from activity_maps
        boolean alarm = (PendingIntent.getBroadcast(this, 0, new Intent("ALARM"), PendingIntent.FLAG_NO_CREATE) == null);

        if(alarm) {
            Intent niceAlarm = new Intent("ALARM");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, niceAlarm, 0);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.SECOND, 3);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 60000, pendingIntent);
        }

        //added the code below - Jody
        mAuth = FirebaseAuth.getInstance();  //getInstance    added by Jody --do not delete
        user = mAuth.getCurrentUser();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // edit the Navigation Bar
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header = navigationView.getHeaderView(0);
        mUsers_photo = (ImageView) header.findViewById(R.id.users_photo);
        nav_name = (TextView) header.findViewById(R.id.user_name);
        nav_membership = (TextView) header.findViewById(R.id.user_membership);
        nav_name.setText(user.getDisplayName());
        setUP();
      //  nav_email.setText(user.getEmail());

        textAddress = (EditText) findViewById(R.id.textAddress);



        btnSearch = (Button) findViewById(R.id.searchBtn);
        startTimer = (Button) findViewById(R.id.startBtn);
        timerView = (TextView) findViewById(R.id.timerView);


        //added the code above -Jody

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (isNetworkConnection()){
            mapFragment.getMapAsync(this);

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .enableAutoManage(this, this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        } else {
            Toast.makeText(getApplicationContext(), "No network Connection", Toast.LENGTH_SHORT).show();
        }



      /*  mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build(); */

        mGoogleApiClient.connect();

        //new FetchLocations().execute();

        text = getIntent().getStringExtra("address");
        if (text != null){
            String result = getAddress(text);
            textAddress.setText(result, TextView.BufferType.EDITABLE);
            Log.d(TAG, result);
           // btnSearch.performClick();
        }

        final CounterClass timer = new CounterClass(305000, 1000);

        startTimer.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                timer.start();
            }
        });

        System.out.println("OnCreate-------laaaaaaaaaaat" + currentLatitude);
        System.out.println("OnCreate-------lnnnnnnnnnnng" + currentLongitude);

        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
           public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    result = mTextToSpeech.setLanguage(Locale.US);
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not Supported in your Device", Toast.LENGTH_LONG).show();
                }
            }
        });

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority((LocationRequest.PRIORITY_HIGH_ACCURACY));
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);

    }

    public boolean isNetworkConnection(){
        boolean isConnectedWifi = false;
        boolean isConnectedMobile = false;

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] infos = connectivityManager.getAllNetworkInfo();
        for(NetworkInfo info: infos){
            if (info.getTypeName().equalsIgnoreCase("WIFI"))
                if (info.isConnected())
                    isConnectedWifi = true;
            if (info.getTypeName().equalsIgnoreCase("MOBILE"))
                if (info.isConnected())
                    isConnectedMobile = true;

        }

        return  isConnectedMobile || isConnectedWifi;
    }


    public String getAddress(String coordinates){
        List<String> items = new ArrayList<String>(Arrays.asList(coordinates.split("\\s*,\\s*")));
        Double lat = Double.valueOf(items.get(0));
        Double lon = Double.valueOf(items.get(1));
        String result = null;
        Geocoder geocoder = new Geocoder(this);
        if (geocoder.isPresent()){
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                Address address = addresses.get(0);
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i< address.getMaxAddressLineIndex(); i++){
                    builder.append(address.getAddressLine(i)).append(", ");
                }
                result = builder.toString();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Unable to Connect Geocoder", Toast.LENGTH_SHORT).show();
            }

        }

        return result;
    }


private void showDialog() {
    dialog = new Dialog(this);

    dialog.setTitle("             Set Destination");
    dialog.setContentView(R.layout.location_dialog);

    TextView locationName = (TextView) dialog.findViewById(R.id.locationName);
    locationName.setText(markerLocationName);


    Button cancelBt = (Button) dialog.findViewById(R.id.cancelBtn);
    cancelBt.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dialog.cancel();
        }
    });

    final Button setDirectionBtn = (Button) dialog.findViewById(R.id.setDirectionBtn);
    setDirectionBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new FetchDirections().execute();
            dialog.cancel();
        }
    });

    dialog.show();
}

/*    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            result = mTextToSpeech.setLanguage(Locale.US);
        } else {
            Toast.makeText(getApplicationContext(), "Feature not Supported in your Device", Toast.LENGTH_LONG).show();
        }

    }*/




    public class CounterClass extends CountDownTimer {

        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long millis = millisUntilFinished;
            String  time = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

            System.out.println(time);
            timerView.setText(time);

            if(time.equals("05:01")) {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(time.equals("05:00")) {
                try {
                    if(result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                        Toast.makeText(getApplicationContext(), "Feature not Supported in your Device", Toast.LENGTH_LONG).show();

                    } else {
                        mTextToSpeech.speak("There are five minutes remaining.", TextToSpeech.QUEUE_FLUSH, null);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            //added by mike. If time is Low, at 3:00, show nearest station location on map.
            //LOWTIME is a global variable
            else
                if(time.equals(LOWTIME)) {
                try {
                    stationInformation.showNearestStationLocation();

                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }


        @Override
        public void onFinish() {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    public void getDistanceAndDuration() {
        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, directions, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {

                    JSONArray list = response.getJSONArray("routes");
                    JSONObject data = list.getJSONObject(0);

                    JSONArray legs = data.getJSONArray("legs");

                    JSONObject legsInfo = legs.getJSONObject(0);

                    JSONObject distance = legsInfo.getJSONObject("distance");
                    distanceInMiles = distance.getString("text");
                    distanceInMeters = distance.getLong("value");


                    JSONObject duration = legsInfo.getJSONObject("duration");
                    timeInMinutes = duration.getString("text");
                    timeInSeconds = duration.getLong("value");

                    TextView distanceText = (TextView) dialog.findViewById(R.id.distanceText);
                    distanceText.setText(distanceInMiles);

                    TextView durationText = (TextView) dialog.findViewById(R.id.durationText);
                    durationText.setText(timeInMinutes);


                    System.out.println("Distance in miles: " + distanceInMiles);
                    System.out.println("Distance in meters: " + distanceInMeters);

                    System.out.println("Time in minutes: " + timeInMinutes);
                    System.out.println("Time in seconds: " + timeInSeconds);



                } catch (JSONException e) {
                    Log.v("TEST_API_RESPONSE_DURATION_TIME", "ERR: " );
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("TEST_API_RESPONSE", "ERR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }



    public void setUP(){
        allEvents = new ArrayList<>();
        String userName = user.getDisplayName();
        helper = new UserDBHelper(getApplicationContext());
        database = helper.getReadableDatabase();
        Cursor cursor = helper.getMembership(userName, database);
        if (cursor.moveToFirst()){
            userMembership = cursor.getString(0);
            nav_membership.setText(userMembership);

        }
        cursor.close();

        Cursor cursor1 = helper.getPhotoURI(userName, database);
        if (cursor1.moveToFirst()) {
            stringUri = cursor1.getString(0);
            uri = Uri.parse(stringUri);
            if (uri != null) {
                displayPhoto();
            } else {
                mUsers_photo.setImageResource(R.mipmap.placeholder_woman);
            }
        }
        cursor1.close();

        allEvents = helper.getAllEvents();
        helper.close();
    }


    public void displayPhoto(){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Matrix matrix = new Matrix();
            int rotate = getOrientation(uri);
            if (rotate == 0 && (bitmap.getWidth()> bitmap.getHeight())){
                matrix.postRotate(90);
            } else {
                matrix.postRotate(rotate);
            }
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0,0, bitmap.getWidth(),bitmap.getHeight(),matrix, true );
            mUsers_photo.setImageBitmap(newBitmap);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Failed To Load Photo", Toast.LENGTH_SHORT).show();
            mUsers_photo.setImageResource(R.mipmap.placeholder_woman);
        }

    }

    public int getOrientation(Uri uri){
        ExifInterface exifInterface = null;
        int rotate = 0;
        try {
            exifInterface = new ExifInterface(uri.getPath());
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
                rotate = 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
                rotate = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
                rotate = 270;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Orientation not defined", Toast.LENGTH_SHORT).show();
        }

        return rotate;
    }


    private void handleNewLocation(Location location) {
        Log.d("TEST TEST TEST TEST_handleNewLocation", location.toString());

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        currrentLatLng = new LatLng(currentLatitude, currentLongitude);

        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
        MarkerOptions options = new MarkerOptions()
                .position(currrentLatLng)
                .title("I am here!")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.cityyouarehere));
        mMap.addMarker(options);
        float zoomLevel = 14.0f; //This goes up to 21
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currrentLatLng, zoomLevel));


        new FetchLocations().execute();

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


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //downloadCitiLocationsData();

        mMap.setMyLocationEnabled(true);
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                directions = "https://maps.googleapis.com/maps/api/directions/json?origin=" + currentLatitude + "," + currentLongitude + "&destination=" + marker.getPosition().latitude + "," + marker.getPosition().longitude + "&mode=bicycling&key=" + GOOGLE_DIRECTIONS_KEY;

                getDistanceAndDuration();

                markerLocationName = marker.getTitle();



                System.out.println("OnMarkerClicked------ Distance in miles: " + distanceInMiles);
                System.out.println("OnMarkerClicked------ Distance in meters: " + distanceInMeters);

                System.out.println("OnMarkerClicked------ Time in minutes: " + timeInMinutes);
                System.out.println("OnMarkerClicked------ Time in seconds: " + timeInSeconds);


                showDialog();
                return true;
            }
        });

        System.out.println("OnMapReady-------laaaaaaaaaaat" + currentLatitude);
        System.out.println("OnMapReady-------lnnnnnnnnnnng" + currentLongitude);
        //float zoomLVL = 16.0f;
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation, zoomLVL));

        //Log.v("CURRENT_LOCATON", "ERR: " + curLocation);
        //mMap.getMyLocation();
    }

    public void onSearch(View view) throws JSONException {
        if (!isNetworkConnection()){
            Toast.makeText(getApplicationContext(), "No network Connection", Toast.LENGTH_SHORT).show();
            return;
        }


        EditText address = (EditText) findViewById(R.id.textAddress);
        String location = address.getText().toString();
        List<Address> addressList = null;

      //  if (address.getText().toString().equals(null)){
        //    alert();
       // || !location.equals("")
        //} location != null

        if(!location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
                if (addressList.isEmpty()){
                    invalidAddress();
                    return;
                }

            } catch (IOException e) {
               // alert();
                e.printStackTrace();
            }

            Address address1 = addressList.get(0);
            LatLng latLng = new LatLng(address1.getLatitude(), address1.getLongitude());
            Log.d("SEARCH", String.valueOf(latLng));
            int destID = stationInformation.getNearestLocationID(latLng);
            Log.d("SEARCH", String.valueOf(destID));
            LatLng destination = stationInformation.getLatLng(destID);
            int bikeQty = stationInformation.getBikeQuantity(destID);
            //mMap.addMarker(new MarkerOptions().position(destination).icon(BitmapDescriptorFactory.defaultMarker(230)).title(stationInformation.getName(destID)));
            mMap.addMarker(new MarkerOptions().position(latLng).title(location));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

//            stationInformation.getRoute(72, 3259);
//            getDurationBetweenStationsInSecs(72, 3259); //testing by mike --delete this
            List<LatLng> stops = stationInformation.getRoute(stationInformation.getNearestLocationID(currrentLatLng), stationInformation.getNearestLocationID(latLng));

            showStops(stops);


            nearestLocationOnSearch = stationInformation.getLatLng(stationInformation.getNearestLocationID(latLng));

            directions = "https://maps.googleapis.com/maps/api/directions/json?origin=" + currentLatitude + "," + currentLongitude + "&destination=" + nearestLocationOnSearch.latitude + "," + nearestLocationOnSearch.longitude + "&mode=bicycling&key=AIzaSyBuwP1BalG9FdpoU0F5LCmHvkJOlULK6to";
            directions = "https://maps.googleapis.com/maps/api/directions/json?origin=" + currentLatitude + "," + currentLongitude + "&destination=" + nearestLocationOnSearch.latitude + "," + nearestLocationOnSearch.longitude + stopsToShow + "&mode=bicycling&key=" + GOOGLE_DIRECTIONS_KEY;



            //mMap.clear();
            new FetchDirections().execute();

            stationInformation.showNearestStationLocation(latLng);
            //getDistanceAndDuration();

//            currentLatitude = latLng.latitude;
//            currentLongitude = latLng.longitude;


        } else {
            alert();
            return;
        }


        new FetchLocations().execute();
    }

    public void showStops(List<LatLng> stops) {
        for(int i = 0; i < stops.size(); i++) {
            mMap.addMarker(new MarkerOptions().position(stops.get(i)).icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_location_racks)));

            if(stops.size() == 1) {
                stopsToShow += "&waypoints=" + stops.get(i).latitude + "," + stops.get(i).longitude;

            } else if(stops.size() > 1) {
                if(i == 0) {
                    stopsToShow += "&waypoints=" + stops.get(i).latitude + "," + stops.get(i).longitude;

                } else {
                    stopsToShow += "|" + stops.get(i).latitude + "," + stops.get(i).longitude;
                }
            }
        }

    }

    public  void invalidAddress(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);
        alert
                .setTitle("Invalid Address")
                .setMessage("Please enter a valid address.")
                .setCancelable(true)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create().show();
    }

    public  void alert(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);
        alert
                .setTitle("Destination Required")
                .setMessage("Please enter the destination you want to search.")
                .setCancelable(true)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create().show();
    }

    public void downloadCitiLocationsData() {

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, CITI_API_URL, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {

                    JSONObject data = response.getJSONObject("data");
                    JSONArray list = data.getJSONArray("stations");

                    stationInformation.setStationLocationList(list);  //Add a JSONArray to class StationInformation for easy retrieval

                } catch (JSONException e) {
                    Log.v("TEST_API_RESPONSE", "ERR: " );
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("TEST_API_RESPONSE", "ERR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

//    public void checkTimer(){
//        LatLng myLatLng = new LatLng(currentLatitude,currentLongitude);
//        int nearestId = stationInformation.getNearestLocationID(myLatLng);
//        stationInformation.getLatLng(nearestId);
//        mMap.addMarker(new MarkerOptions().position(myLatLng).title("NearestLocation"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 17));
//        }
//}



    String directions = "https://maps.googleapis.com/maps/api/directions/json?origin=" + currentLatitude + "," + currentLongitude + "&destination=41.418976,%20-81.399025&mode=bicycling&key=AIzaSyBuwP1BalG9FdpoU0F5LCmHvkJOlULK6to";

    public void downloadDestinationRoute() {

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, directions, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {

                    //JSONObject data = response.getJSONObject("data");
                    JSONArray routes = response.getJSONArray("routes");
                    JSONObject routeInfo = routes.getJSONObject(0);

                    //JSONArray legs = routeInfo.getJSONArray("legs");
                    //JSONObject legsInfo = legs.getJSONObject(0);

                    JSONObject overview_polyline = routeInfo.getJSONObject("overview_polyline");
                    String points = overview_polyline.getString("points");

                    List<LatLng> decodePath = PolyUtil.decode(points);



                    mMap.addPolyline(new PolylineOptions().addAll(decodePath).width(3).color(0x7FFF0000));
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude,currentLongitude), 12));
                    Log.v("TEST_TEST_TEST_TEST____", "ERR: " + points );


                } catch (JSONException e) {
                    Log.v("TEST_API_RESPONSE", "ERR: " );
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("TEST_API_RESPONSE", "ERR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }



//Below Method by Mike
    //Downloads the status information from CitiBikes Status JSON
    public void downloadCitiStatusData() {

        final JsonObjectRequest jsonRequestStatus = new JsonObjectRequest(Request.Method.GET, STATION_STATUS_URL, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {

                    JSONObject data = response.getJSONObject("data");
                    JSONArray list = data.getJSONArray("stations");
                    int lastUpdate = response.getInt("last_updated");

                    stationInformation.setStationUpdate(lastUpdate); //Copies the last update info from JSon into an array in StationInformation Class
                    stationInformation.setStationStatusList(list);  //Copies the Stations' info objects from JSon into an array of objects in StationInformation Class

                } catch (JSONException e) {
                    Log.v("TEST_API_RESPONSE", "ERR: " );
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("TEST_API_RESPONSE", "ERR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequestStatus);
    }
//// Below Method by Mike
    //Takes in the last update time as long epoch, returns a string that says when it was updated.
    public  String getTimeSinceUpdateString(int timeElapsedUpdate){
        String updateTimeString = "";
        if (timeElapsedUpdate == 0)
        {
            updateTimeString = "now.";

        }
        else if (timeElapsedUpdate < 60)
        {
            updateTimeString = " < 1 min ago.";
        }
        else
        {
            updateTimeString = String.valueOf((int) timeElapsedUpdate / 60) + "minutes ago.";
        }

        return updateTimeString;

    }


//Below Class by Mike
    //Holds all the station information of citibikes
    //Information is retrievable through methods.
    public class StationInformation{
        private JSONArray stationStatusList;
        private JSONArray stationLocationList;
        private long updateTime;

        public void setStationStatusList(JSONArray arr){
            stationStatusList = arr;

        }

        public void setStationUpdate(long time){
            updateTime = time;
        }

        public void setStationLocationList(JSONArray arr){
            stationLocationList = arr;
            Log.d("SETLOCATIONLIST", "LOCATIONLIST ARRAY SET");
        }

        public int getBikeQuantity(int stationID) throws JSONException {
            try {
                for (int i = 0; i < stationStatusList.length(); i++) {
                    JSONObject obj = stationStatusList.getJSONObject(i);
                    int currentID = obj.getInt("station_id");
                    int bikeQuantity = obj.getInt("num_bikes_available");

                    if (currentID == stationID){
                        return bikeQuantity;
                    }
                }

            }
            catch (JSONException e){
                Log.e("JSONEXCEPTION", "ERROR FINDING QUANTITY OF BIKES");
            }
            Log.e("GETBIKEQUANTITY", "Couldnt find information from JSON, problem with ID?");
            return -1;
        }

    public int getOpenDockQuantity(int stationID) throws JSONException {
        try {
            for (int i = 0; i < stationStatusList.length(); i++) {
                JSONObject obj = stationStatusList.getJSONObject(i);
                int currentID = obj.getInt("station_id");
                int dockQuantity = obj.getInt("num_docks_available");

                if (currentID == stationID){
                    return dockQuantity;
                }
            }

        }
        catch (JSONException e){
            Log.e("JSONEXCEPTION", "ERROR FINDING QUANTITY OF BIKES");
        }
        Log.e("GETBIKEQUANTITY", "Couldnt find information from JSON, problem with ID?");
        return -1;
    }

        public int getNearestLocationID(LatLng latLng){
            try {
                int nearestID = -1;
                double nearestDistance = 9999;
                for (int i = 0; i < stationLocationList.length(); i++) {
                    JSONObject obj = stationLocationList.getJSONObject(i);
                    double stationLat = obj.getDouble("lat");
                    double stationLng = obj.getDouble("lon");
                    int ID = obj.getInt("station_id");
                    double myLat = latLng.latitude;
                    double myLng = latLng.longitude;
                    double distance = Math.sqrt((Math.pow((stationLat - myLat), 2)) + (Math.pow((stationLng - myLng), 2)));
                    Log.d("DISTANCE", String.valueOf(distance));
                    if (distance < nearestDistance ){
                        nearestDistance = distance;
                        nearestID = ID;
                    }

                    Log.d("NEARESTDISTANCE", String.valueOf(nearestDistance));

                }

                return nearestID;
            }

            catch (JSONException e){
                Log.e("JSONEXCEPTION", "ERROR FINDING NEAREST LOCATION");
            }
            return -1;

        }

        public LatLng getLatLng(int stationID){
            double lat = -1;
            double lng = -1;

            try {
                for (int i = 0; i < stationLocationList.length(); i++) {
                    JSONObject obj = stationLocationList.getJSONObject(i);
                    int currentID = obj.getInt("station_id");

                    if (currentID == stationID){
                        lat = obj.getDouble("lat");
                        lng = obj.getDouble("lon");
                    }

                }
                LatLng coordinates = new LatLng(lat, lng);
                Log.d("LATLNG OF GIVEN ID", String.valueOf(coordinates));
                return coordinates;
            }
            catch (JSONException e){
                Log.e("JSONEXCEPTION", "ERROR FINDING LATLNG STATION FROM ID");
            }
            LatLng coordinates = new LatLng(-1,-1);
            return coordinates;

        }

        public String getName(int stationID){
            String name = "name";
            try {
                for (int i = 0; i < stationLocationList.length(); i++) {
                    JSONObject obj = stationLocationList.getJSONObject(i);
                    int currentID = obj.getInt("station_id");
                    if (currentID == stationID){
                        name = obj.getString("name");
                    }
                }
            }
            catch (JSONException e){
                Log.e("JSONEXCEPTION", "ERROR FINDING LATLNG STATION FROM ID");
            }
            return name;
        }

        public void showNearestStationLocation(){
        LatLng myLatLng = new LatLng(currentLatitude,currentLongitude);
        int nearestId = stationInformation.getNearestLocationID(myLatLng);
        LatLng nearestLatLng = stationInformation.getLatLng(nearestId);
        mMap.addMarker(new MarkerOptions().position(nearestLatLng).title("NearestLocation"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nearestLatLng, 17));
    }

    public void showNearestStationLocation(LatLng latLng){
        LatLng myLatLng = new LatLng(latLng.latitude,latLng.longitude);
        int nearestId = stationInformation.getNearestLocationID(myLatLng);
        LatLng nearestLatLng = stationInformation.getLatLng(nearestId);
        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.cityclosest)).position(nearestLatLng).title("NearestLocation"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nearestLatLng, 17));
    }

    public List getRoute(int originId, int destId) {
        boolean keepSearching = true;
        double shortestDistanceFromDest = 9999999;
        String closestStationName = "error";
        int closestStationId = -1;
        int counter = 0;
        int prevStationId = -1;
        List stationPathList = new ArrayList();
        stationPathList.add(stationInformation.getLatLng(originId));

        while (keepSearching) {
            LatLng originLatLng = stationInformation.getLatLng(originId);
            LatLng destLatLng = stationInformation.getLatLng(destId);
            Location originLoc = latLngToLocation(originLatLng);
            Location destLoc = latLngToLocation(destLatLng);
            try {
                for (int i = 0; i < stationLocationList.length(); i++) {
                    JSONObject obj = stationLocationList.getJSONObject(i);
                    double stationLat = obj.getDouble("lat");
                    double stationLng = obj.getDouble("lon");
                    String stationName = obj.getString("name");
                    int stationId = obj.getInt("station_id");
                    LatLng stationLatLng = new LatLng(stationLat, stationLng);
                    Location stationLoc = latLngToLocation(stationLatLng);

                    double distanceOriginToStation = originLoc.distanceTo(stationLoc);
                    double distanceDestToStation = destLoc.distanceTo(stationLoc);
//                      Change distance to duration in seconds for measurments
//                    double distanceOriginToStation = getDurationBetweenStationsInSecs(originId, stationId);
//                    double distanceDestToStation = getDurationBetweenStationsInSecs(destId, stationId);

                    if (distanceOriginToStation < bikeTime && distanceDestToStation < shortestDistanceFromDest)  {    //get the station closest to destination, but within bike-able radius
//                        Log.d("GETROUTE name", String.valueOf(stationName));
//                        Log.d("GETROUTE DIST_O_S", String.valueOf(distanceOriginToStation));
//                        Log.d("GETROUTE DIST_D_S", String.valueOf(distanceDestToStation));
                        getDurationBetweenStationsInSecs(originId, stationId);
//                        Log.d("GETROUTE time to next station", String.valueOf( durationTimeBetweenStationsInSecs));
                        if (stationInformation.getBikeQuantity(stationId) > 0  && stationInformation.getOpenDockQuantity(stationId) > 0){
//                            Log.d("GETROUTE STATION ID", String.valueOf(stationId));
//                            Log.d("GETROUTE BIKEQTY", String.valueOf(stationInformation.getBikeQuantity(stationId)));
                            if (getDurationBetweenStationsInSecs(originId, stationId) < (30 * 60)){  //30 min * 60 secs
                                shortestDistanceFromDest = distanceDestToStation;
                                closestStationName = stationName;
                                closestStationId = stationId;

                            }
                        }

                    }
                }
                counter++;
                if (closestStationId == destId) {    //closest station is also the destination. That is the last stop. Stop searching.
                    Log.d("GETROUTE END", "PERFECT ITENERARY");
                    keepSearching = false;
                } else if (prevStationId == closestStationId) {  //the closest station found is also the last station found. There are no more closer stations to dest. Must walk /subway
                     Log.d("GETROUTE END", "NON PERFECT ITENERARY, WILL NEED SUBWAY / WALK");
                    keepSearching = false;
                } else if (counter > 100) { //This loop is taking too long. Terminate.
                    Log.d("GETROUTE END", "Couldnt find a route.");
                    keepSearching = false;
                } else {
                    prevStationId = closestStationId;
                    originId = closestStationId;
                }
                Log.d("GETROUTE NEXT ID", String.valueOf(closestStationId));
                stationPathList.add(stationInformation.getLatLng(closestStationId));

            } catch (Exception e) {
                Log.d("GETROUTE", String.valueOf(e));
            }

        }
        Log.d("GETROUTE PATH", String.valueOf(stationPathList));

        return stationPathList;
    }


}


    public long getDurationBetweenStationsInSecs(int originId, int destId) {
        LatLng originLatLng = stationInformation.getLatLng(originId);
        LatLng destLatLng = stationInformation.getLatLng(destId);

        double originLat = originLatLng.latitude;
        double originLng = originLatLng.longitude;
        double destLat = destLatLng.latitude;
        double destLng = destLatLng.longitude;

        String directions = "https://maps.googleapis.com/maps/api/directions/json?origin=" + originLat + "," + originLng + "&destination=" +destLat + ",%20" + destLng + "&mode=bicycling&key=AIzaSyBuwP1BalG9FdpoU0F5LCmHvkJOlULK6to";

        Log.d("DURATION URL", String.valueOf("https://maps.googleapis.com/maps/api/directions/json?origin=" + originLat + "," + originLng + "&destination=" +destLat + ",%20" + destLng + "&mode=bicycling&key=AIzaSyBuwP1BalG9FdpoU0F5LCmHvkJOlULK6to"));

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, directions, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {

                    JSONArray list = response.getJSONArray("routes");
                    JSONObject data = list.getJSONObject(0);

                    JSONArray legs = data.getJSONArray("legs");

                    JSONObject legsInfo = legs.getJSONObject(0);

                    JSONObject distance = legsInfo.getJSONObject("distance");
//                    distanceInMiles = distance.getString("text");
//                    distanceInMeters = distance.getLong("value");

                    JSONObject duration = legsInfo.getJSONObject("duration");
//                    timeInMinutes = duration.getString("text");

                    durationTimeBetweenStationsInSecs = duration.getLong("value");

                    Log.d("DURATION TIME" , String.valueOf(durationTimeBetweenStationsInSecs));

                } catch (JSONException e) {
                    Log.v("TEST_API_RESPONSE_DURATION_TIME", "ERR: " );
                    return;
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("TEST_API_RESPONSE", "ERR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
        return durationTimeBetweenStationsInSecs;
    }
    //Below Method by Mike. Retrieves and displays Nearest POI as markers.
    public void onPOIClick(View view) {
        if (!isNetworkConnection()){
            Toast.makeText(getApplicationContext(), "No network Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            //LatLng location = currrentLatLng;   //Use this line for ACTUAL location
            LatLng location = new LatLng(currentLatitude, currentLongitude);
            Log.d("PLACES", "ABOUT TO DISPLAY LATLNG");
            Log.d("PLACES", String.valueOf(location));
            String PARKS = "park";
            String CAFE = "cafe";
            Log.d("PLACES", "About to getPOI");
            getPOI(location, PARKS);
            getPOI(location, CAFE);
        }

        catch (Exception e){
            Log.e("POI", String.valueOf(e));
        }

    }

////Below Method by Mike. This method might be better as private in onPoiClick(). Throws error with Volley when in a class.
    //Method displays local cafes and parks as markers.
    public void getPOI(LatLng latLng, final String placeType) {

        if (poiButtonClicked == true){
            poiButtonClicked = false;
        }

        double lat = latLng.latitude;
        double lng = latLng.longitude;
        String URL = GOOGLE_PLACES_URL + "?location=" + String.valueOf(lat) + "," + String.valueOf(lng) + "&radius=500&type=" + placeType + "&key=" + GOOGLE_PLACES_KEY;
        Log.d("PLACES", URL);
        final JsonObjectRequest jsonRequestStatus = new JsonObjectRequest(Request.Method.GET, URL, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                int counter = 0;

                try {

                    JSONArray results = response.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++){
                        JSONObject park = results.getJSONObject(i);
                        String name = park.getString("name");
                        JSONObject geometry = park.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");


                        Log.d("PLACES", "Lat: " + location.getString("lat"));
                        Log.d("PLACES", "Name: " + name);

                        LatLng position = new LatLng(lat, lng);
                        Log.d("PLACES", String.valueOf(position));
                        int color = 0;
                        if (counter < 6) {
                            if (placeType == "park") {
                                mMap.addMarker(new MarkerOptions().position(position).title(name).icon(BitmapDescriptorFactory.fromResource(R.drawable.citytree)).snippet(placeType));
                                counter++;
                            }
                            if (placeType == "cafe") {
                                mMap.addMarker(new MarkerOptions().position(position).title(name).icon(BitmapDescriptorFactory.fromResource(R.drawable.citycafe)).snippet(placeType));
                                counter++;
                            }
                        }
                        //mMap.addMarker(new MarkerOptions().position(position).title(name).icon(BitmapDescriptorFactory.defaultMarker(color)).snippet(placeType)); //.


                    }
                } catch (JSONException e) {
                    Log.v("TEST_API_RESPONSE", "ERR: ");
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("TEST_API_RESPONSE", "ERR: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequestStatus);
    }

    public Location latLngToLocation(LatLng latLng){
        double lat = latLng.latitude;
        double lng = latLng.longitude;

        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lng);

        return location;
    }
//Method in progress by Mike. Shortest Route
//    public void shortestPathStations(int originId, int destId){
//        LatLng originLatLng = stationInformation.getLatLng(originId);
//        LatLng destLatLng = stationInformation.getLatLng(destId);
//
//        Location originLocation = latLngToLocation(originLatLng);
//        Location destLocation = latLngToLocation(destLatLng);
//
//    }


    @Override
    public void onLocationChanged(Location location) {
        mMap.clear();
        handleNewLocation(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(this);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
            Log.v("DDDDDDDDDDDDDDDD", "onConnected if");

        } else {
            Log.v("DDDDDDDDDDDDDDDD", "onConnected else");
            startLocationServices();

        }



    }




    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();

        switch(status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                break;

            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                // show user dialog

                try {
                    status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);

                } catch(IntentSender.SendIntentException e) {


            }

                break;

            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                // Location setting unavailable
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CHECK_SETTINGS) {
            if(resultCode == RESULT_OK) {

//                currentLocation = new Location("");
//                currentLocation.setLongitude(currentLongitude);
//                currentLocation.setLatitude(currentLatitude);
                Toast.makeText(getApplicationContext(), "GPS enabled", Toast.LENGTH_LONG).show();
//                handleNewLocation(currentLocation);

            } else {
                Toast.makeText(getApplicationContext(), "GPS is not enabled", Toast.LENGTH_LONG).show();
            }
        }
    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v("DDDDDDDDDDDDDDDD", "Connection Failed");

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    public void startLocationServices() {
        try {
//            mLocationRequest  = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
//            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//            mLocationRequest.setInterval(5000);
//
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,this);

            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);

        } catch (SecurityException exception) {
            Log.v("DDDDDDDDDDDDDDDD", "Exception in startLocationServices()", exception);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("DDDDDDDDDDDDDDDD", "Permission granted");
                    startLocationServices();

                } else {
                    // Show a dialog saying something like, "I can't run your location - you denied permission!"
                    Log.v("DDDDDDDDDDDDDDDD", "Permission not granted");
                    Toast.makeText(this, "I can't run your location - you denied permission!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            }
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    //Method by Jody
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
       if (id == R.id.nav_settings){
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
        } else if(id == R.id.nav_explore) {
            Intent intent = new Intent(this, RecommendedFragmentExecutor.class);
            startActivity(intent);
        } else if (id == R.id.nav_recommend){
            Intent intent = new Intent(this, FoursquarePath.class);
            startActivity(intent);
        } else if(id == R.id.nav_about){
           Intent intent = new Intent(this, AboutUs.class);
           startActivity(intent);
        } else if (id == R.id.nav_plan){
           Intent intent = new Intent(this, PlanActivity.class);
           startActivity(intent);
       }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }
 // Method Above by Jody



    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private class FetchLocations extends AsyncTask<Void, Void, List<CitiBikeLocations>> {
        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarLoad);

        @Override
        protected void onPreExecute() {
            //super.onPreExecute();
            mProgressBar.setIndeterminate(true);
            mProgressBar.getIndeterminateDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<CitiBikeLocations> doInBackground(Void... params) {


            downloadCitiStatusData();
            downloadCitiLocationsData();

            return new CitiLocationFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(List<CitiBikeLocations> citiBikeLocations) {
            setLocations(citiBikeLocations);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private void setLocations(List<CitiBikeLocations> citiBikeLocations) {


        Location nearestLocation = new Location("Nearest Location");
        LatLng latLngNear = new LatLng(0,0);

        //JSONObject defaultObj = list.getJSONObject(0);
        double defaultLat = citiBikeLocations.get(0).getLat();
        double defaultLon = citiBikeLocations.get(0).getLon();
        CitiBikeLocations l3 = citiBikeLocations.get(0);

        float[] nearDist = new float[1];
        Location.distanceBetween(currentLatitude,currentLongitude,defaultLat,defaultLon,nearDist);

        List<CitiBikeLocations> nearestLocations = new ArrayList();

        for(int i = 1; i < citiBikeLocations.size(); i++) {
            //MarkerOptions markerOptions = new MarkerOptions();

            CitiBikeLocations loc = citiBikeLocations.get(i);

            Location newLocation = new Location("New Location");
            newLocation.setLatitude(loc.getLat());
            newLocation.setLongitude(loc.getLon());

            float[] dist = new float[1];



            Location.distanceBetween(currentLatitude,currentLongitude,loc.getLat(),loc.getLon(),dist);

            if(dist[0] < nearDist[0]) {
                nearDist[0] = dist[0];
                nearestLocation.setLatitude(loc.getLat());
                nearestLocation.setLongitude(loc.getLon());
                l3 = citiBikeLocations.get(i);
            }


            else if(dist[0] < 300) {
                nearestLocations.add(loc);
            }

            latLngNear = new LatLng(nearestLocation.getLatitude(), nearestLocation.getLongitude());
            nearestLocations.add(l3);
        }

        for(int i = 0; i < nearestLocations.size(); i++) {
            CitiBikeLocations l = nearestLocations.get(i);
            if((l.getLat() == nearestLocation.getLatitude()) && (l.getLon() == nearestLocation.getLongitude())) {
                mMap.addMarker(new MarkerOptions().position(latLngNear).icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_location_racks)).title(l.getName()).snippet(0 + " NEAR bikes available."));

            } else {
                LatLng ll = new LatLng(l.getLat(),l.getLon());
                //mMap.addMarker(new MarkerOptions().position(ll).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_location_racks)).title(l.getName()).snippet(0 + " NEAR bikes available."));
            }
        }


    }


    private class FetchDirections extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            downloadDestinationRoute();
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        String address = textAddress.toString();
        if (address.equals("")){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else if (!address.equals("")){
            textAddress.setText("");
        }

        super.onBackPressed();
    }


    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = android.provider.Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            } catch (android.provider.Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return  locationMode != android.provider.Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }


}
