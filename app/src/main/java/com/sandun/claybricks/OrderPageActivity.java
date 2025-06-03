package com.sandun.claybricks;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sandun.claybricks.model.Product;
import com.sandun.claybricks.model.User;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderPageActivity extends AppCompatActivity {

    private static final int MAP_ACTIVITY_REQUEST_CODE = 1001;
    private FirebaseFirestore db;
    private RecyclerView orderRecyclerView;
    private OrderAdapter adapter;
    private List<CartActivity.CartItem> cartItems = new ArrayList<>();
    private String currentUserId;
    private User currentUser;
    private double distanceKm = 0;
    private double pricePerKm = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_page);

        db = FirebaseFirestore.getInstance();
        loadCurrentUser();
        setupViews();
        loadDeliveryPricePerKm();
        loadUserDistance();

        TextView viewMap = findViewById(R.id.viewMap);
        viewMap.setOnClickListener(view -> {
            Intent i = new Intent(OrderPageActivity.this, MapActivity.class);
            startActivityForResult(i, MAP_ACTIVITY_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            loadUserDistance();
            loadDeliveryPricePerKm();
        }
    }

    private void setupViews() {
        orderRecyclerView = findViewById(R.id.orderRecyclerView);
        orderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter();
        orderRecyclerView.setAdapter(adapter);

        // Get data from intent
        if (getIntent() != null && getIntent().hasExtra("cartItems")) {
            cartItems = getIntent().getParcelableArrayListExtra("cartItems");
        }

        String total = "";
        if (getIntent() != null && getIntent().hasExtra("totalPrice")) {
            total = getIntent().getStringExtra("totalPrice");
            // Remove "Total: Rs." prefix if present
            if (total != null && total.startsWith("Total: Rs.")) {
                total = total.replace("Total: Rs.", "").trim();
            }
        }

        TextView tvProductTotal = findViewById(R.id.orderProductPrice);
        tvProductTotal.setText(String.format("Rs.%s", total));

        // Set item count
        TextView itemCount = findViewById(R.id.itemCount);
        itemCount.setText(String.valueOf(cartItems != null ? cartItems.size() : 0));

        // Set user info
        TextView userName = findViewById(R.id.orderUserName);
        TextView userMobile = findViewById(R.id.orderUseMobile);
        if (currentUser != null) {
            userName.setText(currentUser.getUser_name());
            userMobile.setText(currentUser.getUser_mobile());
        }

        // Generate order ID
        TextView orderId = findViewById(R.id.autoGenerateId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String orderIdStr = "ORD-" + sdf.format(new Date());
        orderId.setText(orderIdStr);

        Button orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> placeOrder(orderIdStr));
    }

    private void loadCurrentUser() {
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", MODE_PRIVATE);
        String userJson = sp.getString("user", null);
        if (userJson != null) {
            Gson gson = new Gson();
            currentUser = gson.fromJson(userJson, User.class);
            currentUserId = currentUser.getUser_mobile();
        }
    }

    private void loadUserDistance() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("delivery_locations")
                .whereEqualTo("user_mobile", currentUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        if (doc.contains("distance_km")) {
                            distanceKm = doc.getDouble("distance_km");
                            updateDeliveryInfo();
                        } else {
                            Toast.makeText(this, "Distance not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Please set your delivery location first", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderPage", "Error loading user distance", e);
                    Toast.makeText(this, "Error loading delivery location", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDeliveryPricePerKm() {
        db.collection("clay-bricks-delivery-price")
                .document("current_price")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("pricePerKm")) {
                        pricePerKm = documentSnapshot.getDouble("pricePerKm");
                        // Added this line to update price per km display
                        TextView tvPricePerKm = findViewById(R.id.deliveryPriceOneKM);
                        tvPricePerKm.setText(String.format(Locale.getDefault(), "Rs.%.2f", pricePerKm));
                        updateDeliveryInfo();
                    } else {
                        Toast.makeText(this, "Delivery price not configured", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderPage", "Error loading delivery price per km", e);
                    Toast.makeText(this, "Failed to load delivery price", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateDeliveryInfo() {
        if (pricePerKm <= 0 || distanceKm <= 0) {
            return;
        }

        // Calculate delivery price
        double deliveryPrice = pricePerKm * distanceKm;

        // Update UI
        TextView tvDistance = findViewById(R.id.deliverKilometer);
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));

        TextView tvDeliveryPrice = findViewById(R.id.calculateDeliveryPrice);
        tvDeliveryPrice.setText(String.format(Locale.getDefault(), "Rs.%.2f", deliveryPrice));

        // Calculate total price
        calculateTotalPrices(deliveryPrice);
    }

    private void calculateTotalPrices(double deliveryPrice) {
        try {
            TextView tvProductTotal = findViewById(R.id.orderProductPrice);
            String productTotalStr = tvProductTotal.getText().toString().replace("Rs.", "").trim();

            if (productTotalStr.isEmpty()) {
                throw new NumberFormatException("Empty product total");
            }

            double productTotal = Double.parseDouble(productTotalStr);
            double total = productTotal + deliveryPrice;

            TextView tvTotal = findViewById(R.id.totalPrice);
            tvTotal.setText(String.format(Locale.getDefault(), "Rs.%.2f", total));

            // Update delivery price display
            TextView tvDelivery = findViewById(R.id.calculateDeliveryPrice);
            tvDelivery.setText(String.format(Locale.getDefault(), "Rs.%.2f", deliveryPrice));
        } catch (NumberFormatException e) {
            Log.e("PriceError", "Error parsing prices", e);
            Toast.makeText(this, "Error calculating total price", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendOrderEmailNotification(String orderId) {
        db.collection("clay-bricks-admin")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        JsonArray adminEmails = new JsonArray();

                        // Collect all admin emails
                        for (DocumentSnapshot adminDoc : queryDocumentSnapshots.getDocuments()) {
                            String email = adminDoc.getString("admin_email");
                            if (email != null && !email.isEmpty()) {
                                adminEmails.add(email);
                            }
                        }

                        if (adminEmails.size() > 0) {
                            // Prepare email content
                            JsonObject emailData = new JsonObject();
                            emailData.addProperty("orderId", orderId);
                            emailData.addProperty("userName", currentUser.getUser_name());
                            emailData.addProperty("userMobile", currentUser.getUser_mobile());
                            emailData.addProperty("totalPrice",
                                    ((TextView) findViewById(R.id.totalPrice)).getText().toString()
                                            .replace("Rs.", "").trim());

                            JsonArray itemsArray = new JsonArray();
                            for (CartActivity.CartItem item : cartItems) {
                                JsonObject productJson = new JsonObject();
                                productJson.addProperty("name", item.getProduct().getProduct_name());
                                productJson.addProperty("quantity", item.getQuantity());
                                productJson.addProperty("price", item.getProduct().getProduct_price());
                                itemsArray.add(productJson);
                            }
                            emailData.add("items", itemsArray);
                            emailData.add("adminEmails", adminEmails);

                            // Send HTTP request
                            new Thread(() -> {
                                OkHttpClient client = new OkHttpClient();
                                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                                RequestBody body = RequestBody.create(emailData.toString(), JSON);

                                Request request = new Request.Builder()
                                        .url("http://192.168.1.3:8080/ClayBricksBackend/SendOrderNotification")
                                        .post(body)
                                        .build();

                                try (Response response = client.newCall(request).execute()) {
                                    Log.d("EmailNotification", "Email send status: " + response.code());
                                } catch (IOException e) {
                                    Log.e("EmailNotification", "Error sending email", e);
                                }
                            }).start();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("EmailNotification", "Error getting admin emails", e));
    }

    private void placeOrder(String orderId) {
        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "No items to order", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get delivery location first
        db.collection("delivery_locations")
                .whereEqualTo("user_mobile", currentUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot locationDoc = queryDocumentSnapshots.getDocuments().get(0);
                        GeoPoint deliveryLocation = locationDoc.getGeoPoint("location");
                        double distanceKm = locationDoc.getDouble("distance_km");

                        // Get delivery price
                        TextView tvDelivery = findViewById(R.id.calculateDeliveryPrice);
                        String deliveryPrice = tvDelivery.getText().toString().replace("Rs.", "").trim();

                        // Get total price
                        TextView tvTotal = findViewById(R.id.totalPrice);
                        String totalPrice = tvTotal.getText().toString().replace("Rs.", "").trim();

                        Map<String, Object> order = new HashMap<>();
                        order.put("orderId", orderId);
                        order.put("userId", currentUserId);
                        order.put("userName", currentUser.getUser_name());
                        order.put("userMobile", currentUser.getUser_mobile());
                        order.put("orderDate", new Date());
                        order.put("status", "Pending");
                        order.put("deliveryPrice", deliveryPrice);
                        order.put("totalPrice", totalPrice);
                        order.put("distanceKm", distanceKm);
                        order.put("pricePerKm", pricePerKm);
                        order.put("deliveryLocation", deliveryLocation); // Add location to order

                        List<Map<String, Object>> items = new ArrayList<>();
                        for (CartActivity.CartItem item : cartItems) {
                            Map<String, Object> product = new HashMap<>();
                            product.put("productId", item.getProduct().getProduct_id());
                            product.put("productName", item.getProduct().getProduct_name());
                            product.put("quantity", item.getQuantity());
                            product.put("price", item.getProduct().getProduct_price());
                            items.add(product);
                        }
                        order.put("items", items);

                        db.collection("clay-bricks-order")
                                .document(orderId)
                                .set(order)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                                    clearCart();
                                    sendOrderEmailNotification(orderId);
                                    startActivity(new Intent(this, UserOrderActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Please set delivery location first", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error getting delivery location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void clearCart() {
        if (cartItems == null) return;

        for (CartActivity.CartItem item : cartItems) {
            if (item.getCartId() != null) {
                db.collection("clay-bricks-cart")
                        .document(item.getCartId())
                        .delete()
                        .addOnFailureListener(e -> {
                            Log.e("OrderPage", "Error removing cart item", e);
                        });
            }
        }
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (cartItems != null && position < cartItems.size()) {
                CartActivity.CartItem item = cartItems.get(position);
                Product product = item.getProduct();

                holder.tvProductName.setText(product.getProduct_name());
                holder.tvQuantity.setText(String.format("Qty: %d", item.getQuantity()));
                holder.tvPrice.setText(String.format("Rs.%s", product.getProduct_price()));
            }
        }

        @Override
        public int getItemCount() {
            return cartItems != null ? cartItems.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProductName, tvQuantity, tvPrice;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvPrice = itemView.findViewById(R.id.tvPrice);
            }
        }
    }
}