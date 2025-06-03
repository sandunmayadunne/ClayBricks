package com.sandun.claybricks;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.sandun.claybricks.model.User;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final String PREFS_NAME = "com.sandun.claybricks.data";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int POLYLINE_WIDTH = 12;

    // Sri Lanka boundaries
    private static final double SRI_LANKA_MIN_LAT = 5.919;
    private static final double SRI_LANKA_MAX_LAT = 9.835;
    private static final double SRI_LANKA_MIN_LNG = 79.521;
    private static final double SRI_LANKA_MAX_LNG = 81.879;

    private GoogleMap mMap;
    private LatLng selectedLocation;
    private final LatLng storeLocation = new LatLng(6.298797595431366, 80.87789404692066); // Store location (Embilipitiya)
    private FirebaseFirestore db;
    private TextView tvDistance;
    private Polyline currentPolyline;
    private User currentUser;
    private boolean locationSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeComponents();
        loadUserData();
        setupMapFragment();
        setupConfirmButton();
    }

    private void initializeComponents() {
        db = FirebaseFirestore.getInstance();
        tvDistance = findViewById(R.id.tvDistance);
    }

    private void loadUserData() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);

        if (userJson != null) {
            Gson gson = new Gson();
            currentUser = gson.fromJson(userJson, User.class);
            Log.d(TAG, "User loaded: " + currentUser.getUser_name());
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = new SupportMapFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayoutMap, mapFragment);
        fragmentTransaction.commit();
        mapFragment.getMapAsync(this);
    }

    private void setupConfirmButton() {
        Button btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedLocation != null && !locationSaved) {
                saveLocationToFirebase();
            } else if (locationSaved) {
                Toast.makeText(this, "Location already saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please select a location within Sri Lanka first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        setupMap();
        checkLocationPermission();
        setupMapClickListener();
    }

    private void setupMap() {
        // Set initial view to show all of Sri Lanka
        LatLngBounds sriLankaBounds = new LatLngBounds(
                new LatLng(SRI_LANKA_MIN_LAT, SRI_LANKA_MIN_LNG),
                new LatLng(SRI_LANKA_MAX_LAT, SRI_LANKA_MAX_LNG)
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(sriLankaBounds, 100));

        // Add only the store marker (no automatic Colombo marker)
        addStoreMarker();
    }

    private void addStoreMarker() {
        mMap.addMarker(new MarkerOptions()
                .position(storeLocation)
                .title("Our Store")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupMapClickListener() {
        mMap.setOnMapClickListener(latLng -> {
            if (isWithinSriLanka(latLng)) {
                if (!locationSaved) {
                    handleLocationSelection(latLng);
                } else {
                    Toast.makeText(this, "Location already set. Confirm to proceed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please select a location within Sri Lanka", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLocationSelection(LatLng latLng) {
        selectedLocation = latLng;
        mMap.clear();
        addStoreMarker(); // Only show store marker and new selected location
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Delivery Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        calculateDistance();
        new GetDirectionsTask().execute();
    }

    private boolean isWithinSriLanka(LatLng latLng) {
        return latLng.latitude >= SRI_LANKA_MIN_LAT &&
                latLng.latitude <= SRI_LANKA_MAX_LAT &&
                latLng.longitude >= SRI_LANKA_MIN_LNG &&
                latLng.longitude <= SRI_LANKA_MAX_LNG;
    }

    private void calculateDistance() {
        if (selectedLocation != null) {
            double distance = SphericalUtil.computeDistanceBetween(storeLocation, selectedLocation);
            double distanceKm = distance / 1000;
            tvDistance.setText(String.format("Distance: %.2f km", distanceKm));
        }
    }

    private void saveLocationToFirebase() {
        if (currentUser == null || currentUser.getUser_mobile() == null) {
            Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("location", new GeoPoint(selectedLocation.latitude, selectedLocation.longitude));
        locationData.put("distance_km", Double.parseDouble(tvDistance.getText().toString().replace("Distance: ", "").replace(" km", "")));
        locationData.put("user_mobile", currentUser.getUser_mobile());
        locationData.put("country", "Sri Lanka");
        locationData.put("user_name", currentUser.getUser_name());

        db.collection("delivery_locations")
                .whereEqualTo("user_mobile", currentUser.getUser_mobile())
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            updateExistingLocation(task.getResult().getDocuments().get(0).getReference(), locationData);
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            createNewLocation(locationData);
                        }
                    } else {
                        Toast.makeText(this, "Error checking location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateExistingLocation(DocumentReference docRef, Map<String, Object> locationData) {
        docRef.update(locationData)
                .addOnSuccessListener(aVoid -> {
                    locationSaved = true;
                    Toast.makeText(this, "Delivery location updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating location", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating location", e);
                });
    }

    private void createNewLocation(Map<String, Object> locationData) {
        db.collection("delivery_locations")
                .add(locationData)
                .addOnSuccessListener(documentReference -> {
                    locationSaved = true;
                    Toast.makeText(this, "Delivery location saved!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving location", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving location", e);
                });
    }

    private class GetDirectionsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            try {
                String origin = storeLocation.latitude + "," + storeLocation.longitude;
                String destination = selectedLocation.latitude + "," + selectedLocation.longitude;

                String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin +
                        "&destination=" + destination +
                        "&mode=driving" +
                        "&key=" + "AIzaSyA_CgriS6HjAQIot3hYbuMZ8DwDwWjIrC4";

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error: " + connection.getResponseCode());
                    return null;
                }

                return readResponse(connection.getInputStream());

            } catch (Exception e) {
                Log.e(TAG, "Error getting directions", e);
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        private String readResponse(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(MapActivity.this, "Failed to get directions", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject json = new JSONObject(result);
                String status = json.getString("status");

                if (!status.equals("OK")) {
                    String error = json.optString("error_message", "Directions service failed");
                    Toast.makeText(MapActivity.this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Directions API Error: " + error);
                    return;
                }

                processRoute(json);

            } catch (Exception e) {
                Log.e(TAG, "Error processing directions", e);
                Toast.makeText(MapActivity.this, "Error processing route", Toast.LENGTH_SHORT).show();
            }
        }

        private void processRoute(JSONObject json) throws Exception {
            JSONObject route = json.getJSONArray("routes").getJSONObject(0);
            String polyline = route.getJSONObject("overview_polyline").getString("points");
            List<LatLng> path = PolyUtil.decode(polyline);

            if (path.isEmpty()) {
                Toast.makeText(MapActivity.this, "No route path found", Toast.LENGTH_SHORT).show();
                return;
            }

            updateRouteOnMap(path);
        }
    }

    private void updateRouteOnMap(List<LatLng> path) {
        runOnUiThread(() -> {
            try {
                if (currentPolyline != null) {
                    currentPolyline.remove();
                }

                currentPolyline = mMap.addPolyline(new PolylineOptions()
                        .addAll(path)
                        .width(POLYLINE_WIDTH)
                        .color(Color.BLUE)
                        .geodesic(true));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(storeLocation);
                builder.include(selectedLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

            } catch (Exception e) {
                Log.e(TAG, "Error updating map route", e);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }
}