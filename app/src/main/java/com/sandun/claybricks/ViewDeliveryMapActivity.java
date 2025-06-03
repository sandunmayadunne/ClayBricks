package com.sandun.claybricks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.sandun.claybricks.R;

public class ViewDeliveryMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String deliveryId;
    private LatLng deliveryLocation;
    private LatLng lorryCurrentLocation;
    private final LatLng storeLocation = new LatLng(6.298797595431366, 80.87789404692066);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_delivery_map);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get delivery ID from intent
        deliveryId = getIntent().getStringExtra("DELIVERY_ID");
        if (deliveryId == null || deliveryId.isEmpty()) {
            Toast.makeText(this, "Invalid delivery ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mapFragment.getMapAsync(this);

        loadDeliveryDetails();
    }

    private void loadDeliveryDetails() {
        db.collection("clay-bricks-delivery-order").document(deliveryId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "Listen failed", e);
                        Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Delivery delivery = documentSnapshot.toObject(Delivery.class);
                        if (delivery != null) {
                            // Update delivery location
                            if (delivery.getDeliveryLocation() != null) {
                                GeoPoint geoPoint = delivery.getDeliveryLocation();
                                deliveryLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            }

                            // Update lorry location
                            if (delivery.getLorryCurrentLocation() != null) {
                                GeoPoint lorryGeo = delivery.getLorryCurrentLocation();
                                lorryCurrentLocation = new LatLng(lorryGeo.getLatitude(), lorryGeo.getLongitude());
                            }

                            updateMap();
                        }
                    } else {
                        Toast.makeText(this, "Delivery not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        updateMap();
    }

    private void updateMap() {
        if (mMap == null) return;

        mMap.clear();

        try {
            // Add store marker
            mMap.addMarker(new MarkerOptions()
                    .position(storeLocation)
                    .title("Store")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            // Add delivery location marker
            if (deliveryLocation != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(deliveryLocation)
                        .title("Delivery Point")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

            // Add lorry marker
            if (lorryCurrentLocation != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(lorryCurrentLocation)
                        .title("Delivery Position")
                        .icon(BitmapDescriptorFactory.fromBitmap(
                                Bitmap.createScaledBitmap(
                                        BitmapFactory.decodeResource(getResources(), R.drawable.on_the_way_location),
                                        60, 90, false))));
            }

            // Update camera position
            LatLngBounds.Builder builder = new LatLngBounds.Builder().include(storeLocation);
            if (deliveryLocation != null) builder.include(deliveryLocation);
            if (lorryCurrentLocation != null) builder.include(lorryCurrentLocation);

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        } catch (Exception e) {
            Log.e("MapError", "Error updating map: " + e.getMessage());
            Toast.makeText(this, "Error showing map", Toast.LENGTH_SHORT).show();
        }
    }

    public static class Delivery {
        private GeoPoint deliveryLocation;
        private GeoPoint lorryCurrentLocation;

        public GeoPoint getDeliveryLocation() {
            return deliveryLocation;
        }

        public GeoPoint getLorryCurrentLocation() {
            return lorryCurrentLocation;
        }

        public void setDeliveryLocation(GeoPoint deliveryLocation) {
            this.deliveryLocation = deliveryLocation;
        }

        public void setLorryCurrentLocation(GeoPoint lorryCurrentLocation) {
            this.lorryCurrentLocation = lorryCurrentLocation;
        }
    }
}