package com.drhhusain.weatherforcast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout homeCL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIV;
    private ImageView iconIV;

    // Recycler View Variables
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;


    private final int PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // To set screen as full screen and avoid status bar to be shown
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        homeCL = findViewById(R.id.home);
        loadingPB = findViewById(R.id.pbLoading);
        cityNameTV = findViewById(R.id.idTVCity);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        RecyclerView weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEditCity);
        backIV = findViewById(R.id.black_bg);
        iconIV = findViewById(R.id.idIVIcon);
        ImageView searchIV = findViewById(R.id.idIVSearch);

        // Initialising Recycler View Variables
        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);

//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        weatherRV.setLayoutManager(linearLayoutManager);

        // Setting the adapter to Recycler View
        weatherRV.setAdapter(weatherRVAdapter);

        //weatherRV.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        // Initialising Location Manager
        // Location Manager for location
        // LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Checking weather user has granted the permission or not
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }

        //Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        //cityName = getCityName(location.getLongitude(), location.getLatitude());
        //getWeatherInfo(cityName);

        getCurrentLocation();

        searchIV.setOnClickListener(view -> {
            String city = Objects.requireNonNull(cityEdt.getText()).toString();
            if (city.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please Enter City Name", Toast.LENGTH_SHORT).show();
            } else {
                cityNameTV.setText(city);
                getWeatherInfo(city);
            }
        });

    }

    // Youtube solution
    private void getCurrentLocation() {
        loadingPB.setVisibility(View.VISIBLE);
        LocationRequest locationRequest = new LocationRequest().create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .removeLocationUpdates(this);
                        if(locationResult.getLocations().size() > 0) {
                            int latestLocation = locationResult.getLocations().size() -1;
                            getWeatherInfo(getCityName(locationResult.getLocations().get(latestLocation).getLongitude(),
                                    locationResult.getLocations().get(latestLocation).getLatitude()));
                        }
                        loadingPB.setVisibility(View.GONE);
                    }
                }, Looper.getMainLooper());
    }

    // If user not granted permission close the app
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Please provide the permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Function to get city name from latitude and longitude
    private String getCityName(double longitude, double latitute) {
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());

        try {
            List<Address> addresses = gcd.getFromLocation(latitute, longitude, 10);

            for (Address adr : addresses) {
                if (adr != null) {
                    String city = adr.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                        Toast.makeText(this, "User City Found" + cityName, Toast.LENGTH_LONG).show();

                    } else {
                        Toast.makeText(this, "User City Not Found", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cityName;
    }

    private void getWeatherInfo(String cityName) {
        String url = "http://api.weatherapi.com/v1/forecast.json?key=ed5370f82910404c8f9135932211609&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        cityNameTV.setText(cityName);

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        // JSON request object
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            loadingPB.setVisibility(View.GONE);
            homeCL.setVisibility(View.VISIBLE);
            weatherRVModelArrayList.clear();

            // Extracting the data from JSON Object
            try {
                String temperature = response.getJSONObject("current").getString("temp_c");
                temperatureTV.setText(temperature + "ºC");
                int isDay = response.getJSONObject("current").getInt("is_day");
                String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
                conditionTV.setText(condition);

                // Background image according to day and night
                if (isDay == 1) {
                    // Day
                    Picasso.get().load("https://images.pexels.com/photos/2931915/pexels-photo-2931915.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500").into(backIV);
                } else {
                    // Night
                    Picasso.get().load("https://images.pexels.com/photos/2098427/pexels-photo-2098427.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500").into(backIV);
                }

                // Data for Forecasting
                JSONObject forecastObj = response.getJSONObject("forecast");
                JSONObject forecastO = response.getJSONArray("forecastday").getJSONObject(0);
                JSONArray hourArray = forecastO.getJSONArray("hour");

                // Iterating over hourArray to extract meaningful data from array
                for (int i = 0; i < hourArray.length(); i++) {
                    JSONObject hourObj = hourArray.getJSONObject(i);
                    String time = hourObj.getString("time");
                    String temper = hourObj.getString("temp_c");
                    String img = hourObj.getJSONObject("condition").getString("icon");
                    String wind = hourObj.getString("wind_kph");

                    weatherRVModelArrayList.add(new WeatherRVModel(time, temper, img, wind));
                }
                weatherRVAdapter.notifyDataSetChanged();


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Toast.makeText(MainActivity.this, "Please enter valid city name...", Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonObjectRequest);

    }
}