package com.sandun.claybricks.navigation;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.PolyUtil;
import com.sandun.claybricks.R;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DeliveryMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String deliveryId;
    private LatLng deliveryLocation;
    private LatLng lorryCurrentLocation;
    private Polyline currentPolyline;
    private ProgressDialog progressDialog;
    private final LatLng storeLocation = new LatLng(6.298797595431366, 80.87789404692066);

    public static class Delivery {
        private GeoPoint deliveryLocation;
        private GeoPoint lorryCurrentLocation;

        public GeoPoint getDeliveryLocation() {
            return deliveryLocation;
        }

        public GeoPoint getLorryCurrentLocation() {
            return lorryCurrentLocation;
        }

        public void setLorryCurrentLocation(GeoPoint lorryCurrentLocation) {
            this.lorryCurrentLocation = lorryCurrentLocation;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_map);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating route...");
        progressDialog.setCancelable(false);

        db = FirebaseFirestore.getInstance();
        deliveryId = getIntent().getStringExtra("DELIVERY_ID");

        if (deliveryId == null) {
            finish();
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadDeliveryDetails();
    }

    private void loadDeliveryDetails() {
        db.collection("clay-bricks-delivery-order").document(deliveryId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Delivery delivery = documentSnapshot.toObject(Delivery.class);
                        if (delivery != null) {
                            if (delivery.getDeliveryLocation() != null) {
                                GeoPoint geoPoint = delivery.getDeliveryLocation();
                                deliveryLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            }

                            if (delivery.getLorryCurrentLocation() != null) {
                                GeoPoint lorryGeo = delivery.getLorryCurrentLocation();
                                lorryCurrentLocation = new LatLng(lorryGeo.getLatitude(), lorryGeo.getLongitude());
                            }

                            updateMap();
                        }
                    } else {
                        finish();
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.setOnMapLongClickListener(this::updateLorryLocation);
        updateMap();
    }

    private void updateLorryLocation(LatLng newLocation) {
        GeoPoint newGeoPoint = new GeoPoint(newLocation.latitude, newLocation.longitude);

        db.collection("clay-bricks-delivery-order").document(deliveryId)
                .update("lorryCurrentLocation", newGeoPoint)
                .addOnSuccessListener(aVoid -> {
                    lorryCurrentLocation = newLocation;
                    updateMap();
                });
    }

    private void updateMap() {
        if (mMap == null) return;

        mMap.clear();

        // Store marker
        mMap.addMarker(new MarkerOptions()
                .position(storeLocation)
                .title("Store Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Delivery location
        if (deliveryLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(deliveryLocation)
                    .title("Delivery Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Lorry with custom icon
        if (lorryCurrentLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(lorryCurrentLocation)
                    .title("Lorry Position")
                    .icon(BitmapDescriptorFactory.fromBitmap(
                            Bitmap.createScaledBitmap(
                                    BitmapFactory.decodeResource(getResources(), R.drawable.on_the_way_location),
                                    60, 90, false))));
        }

        // Camera zoom
        LatLngBounds.Builder builder = new LatLngBounds.Builder().include(storeLocation);
        if (deliveryLocation != null) builder.include(deliveryLocation);
        if (lorryCurrentLocation != null) builder.include(lorryCurrentLocation);

        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        } catch (IllegalStateException ignored) {
        }

        // Draw route if possible
        if (lorryCurrentLocation != null && deliveryLocation != null) {
            new GetDirectionsTask().execute();
        }
    }

    private class GetDirectionsTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            try {
                String origin = lorryCurrentLocation.latitude + "," + lorryCurrentLocation.longitude;
                String destination = deliveryLocation.latitude + "," + deliveryLocation.longitude;

                URL url = new URL("https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=" + origin +
                        "&destination=" + destination +
                        "&mode=driving" +
                        "&key=AIzaSyA_CgriS6HjAQIot3hYbuMZ8DwDwWjIrC4");

                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(15000);
                connection.setConnectTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                return result.toString();

            } catch (Exception e) {
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                String encodedPolyline = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points");

                List<LatLng> decodedPath = PolyUtil.decode(encodedPolyline);

                if (currentPolyline != null) currentPolyline.remove();

                currentPolyline = mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(12)
                        .color(Color.argb(255, 255, 165, 0)));

            } catch (Exception ignored) {
            } finally {
                progressDialog.dismiss();
            }
        }
    }
}