package com.sandun.claybricks.navigation;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.maps.android.PolyUtil;
import com.sandun.claybricks.R;
import com.sandun.claybricks.model.Order2;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final LatLng storeLocation = new LatLng(6.298797595431366, 80.87789404692066);
    private LatLng userLocation;
    private Polyline currentPolyline;
    private FirebaseFirestore db;
    private String orderId;
    private Button btnConfirm, btnCancel;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "LK"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_view);

        // Configure currency format
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("ORDER_ID");

        if (orderId == null) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Views
        TextView tvOrderId = findViewById(R.id.orderIdView);
        TextView userName = findViewById(R.id.userNameOder);
        TextView userMobileTv = findViewById(R.id.userMobileOder);
        TextView deliveryPrice = findViewById(R.id.deliveryPrice);
        TextView distanceKm = findViewById(R.id.distanceKm);
        TextView totalPrice = findViewById(R.id.totalPriceOrder);
        RecyclerView productsRecycler = findViewById(R.id.orderProductView);
        btnConfirm = findViewById(R.id.orderConform);
        btnCancel = findViewById(R.id.orderCancel);

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapViewOder);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mapViewOder, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        // Setup button click listeners
        btnConfirm.setOnClickListener(v -> updateOrderStatus("Confirmed"));
        btnCancel.setOnClickListener(v -> updateOrderStatus("Cancelled"));

        // Load Order Data
        db.collection("clay-bricks-order").document(orderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Order2 order = documentSnapshot.toObject(Order2.class);
                    if (order != null) {
                        // Set order details
                        tvOrderId.setText(order.getOrderId() != null ? order.getOrderId() : "N/A");
                        userName.setText(order.getUserName() != null ? order.getUserName() : "N/A");
                        userMobileTv.setText(order.getUserMobile() != null ? order.getUserMobile() : "N/A");

                        // Handle string prices and format them
                        try {
                            double deliveryPriceValue = order.getDeliveryPrice() != null ?
                                    Double.parseDouble(order.getDeliveryPrice()) : 0.0;
                            deliveryPrice.setText(formatCurrency(deliveryPriceValue));
                        } catch (NumberFormatException e) {
                            deliveryPrice.setText("Rs.0.00");
                            Log.e("OrderView", "Error parsing delivery price", e);
                        }

                        distanceKm.setText(String.format(Locale.US, "%.2f km", order.getDistanceKm()));

                        try {
                            double totalPriceValue = order.getTotalPrice() != null ?
                                    Double.parseDouble(order.getTotalPrice()) : 0.0;
                            totalPrice.setText(formatCurrency(totalPriceValue));
                        } catch (NumberFormatException e) {
                            totalPrice.setText("Rs.0.00");
                            Log.e("OrderView", "Error parsing total price", e);
                        }

                        // Set up products recycler view with null check
                        ProductAdapter adapter = new ProductAdapter(
                                order.getItems() != null ? order.getItems() : List.of()
                        );
                        productsRecycler.setLayoutManager(new LinearLayoutManager(this));
                        productsRecycler.setAdapter(adapter);

                        // Get delivery location from order
                        if (order.getDeliveryLocation() != null) {
                            userLocation = new LatLng(
                                    order.getDeliveryLocation().getLatitude(),
                                    order.getDeliveryLocation().getLongitude()
                            );
                            updateMap();
                        } else {
                            Toast.makeText(this, "Delivery location not available", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading order", Toast.LENGTH_SHORT).show();
                    Log.e("OrderView", "Error loading order", e);
                    finish();
                });
    }

    private String formatCurrency(double amount) {
        return currencyFormat.format(amount).replace("LKR", "Rs.");
    }

    private void updateOrderStatus(String newStatus) {
        db.collection("clay-bricks-order").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    String message = "Order " + newStatus + " successfully!";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating order status", Toast.LENGTH_SHORT).show();
                    Log.e("OrderUpdate", "Error updating status", e);
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (userLocation != null) {
            updateMap();
        }
    }

    private void updateMap() {
        if (mMap == null || userLocation == null) return;

        try {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(storeLocation)
                    .title("Store Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            mMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title("Delivery Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Zoom to fit both locations
            LatLngBounds.Builder builder = new LatLngBounds.Builder()
                    .include(storeLocation)
                    .include(userLocation);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

            // Draw route
            new GetDirectionsTask().execute();
        } catch (Exception e) {
            Log.e("MapUpdate", "Error updating map", e);
            Toast.makeText(this, "Error showing map", Toast.LENGTH_SHORT).show();
        }
    }

    private class GetDirectionsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String origin = storeLocation.latitude + "," + storeLocation.longitude;
                String destination = userLocation.latitude + "," + userLocation.longitude;

                String url = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=" + origin +
                        "&destination=" + destination +
                        "&key=AIzaSyA_CgriS6HjAQIot3hYbuMZ8DwDwWjIrC4";

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                return result.toString();
            } catch (IOException e) {
                Log.e("Directions", "Error fetching route", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if (result == null) throw new Exception("No route data");

                JSONObject json = new JSONObject(result);
                if (!json.getString("status").equals("OK")) {
                    throw new Exception(json.optString("error_message", "Directions request failed"));
                }

                String polyline = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points");
                List<LatLng> decodedPath = PolyUtil.decode(polyline);

                if (currentPolyline != null) currentPolyline.remove();
                currentPolyline = mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(12)
                        .color(Color.BLUE));

                LatLngBounds.Builder builder = new LatLngBounds.Builder()
                        .include(storeLocation)
                        .include(userLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

            } catch (Exception e) {
                Log.e("MapError", "Route drawing failed", e);
//                Toast.makeText(OrderViewActivity.this,
//                        "Error showing route: " + e.getMessage(),
//                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private final List<Map<String, Object>> items;

        ProductAdapter(List<Map<String, Object>> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);

            // Safe handling for product name
            Object nameObj = item.get("productName");
            holder.productName.setText(nameObj != null ? nameObj.toString() : "Unknown Product");

            // Safe handling for quantity
            Object qtyObj = item.get("quantity");
            String quantity = "Qty: ";
            try {
                int qty = 0;
                if (qtyObj instanceof Number) {
                    qty = ((Number) qtyObj).intValue();
                } else if (qtyObj != null) {
                    qty = Integer.parseInt(qtyObj.toString());
                }
                quantity += qty;
            } catch (NumberFormatException e) {
                quantity += "N/A";
                Log.e("ProductAdapter", "Invalid quantity format", e);
            }
            holder.quantity.setText(quantity);

            // Safe handling for price
            Object priceObj = item.get("price");
            String priceText = "Rs.0.00";
            try {
                double price = 0.0;
                if (priceObj instanceof Number) {
                    price = ((Number) priceObj).doubleValue();
                } else if (priceObj != null) {
                    price = Double.parseDouble(priceObj.toString());
                }
                priceText = String.format(Locale.US, "Rs.%.2f", price);
            } catch (NumberFormatException e) {
                Log.e("ProductAdapter", "Invalid price format", e);
            }
            holder.price.setText(priceText);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView productName, quantity, price;

            ViewHolder(View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.product_name);
                quantity = itemView.findViewById(R.id.product_quantity);
                price = itemView.findViewById(R.id.product_price);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (userLocation != null) {
            outState.putDouble("lat", userLocation.latitude);
            outState.putDouble("lng", userLocation.longitude);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("lat") && savedInstanceState.containsKey("lng")) {
            userLocation = new LatLng(
                    savedInstanceState.getDouble("lat"),
                    savedInstanceState.getDouble("lng")
            );
            updateMap();
        }
    }
}