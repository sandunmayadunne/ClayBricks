package com.sandun.claybricks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.sandun.claybricks.model.Order;
import com.sandun.claybricks.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserOrderActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private RecyclerView deliveryOrdersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private DeliveryOrdersAdapter deliveryOrdersAdapter;
    private List<Order> orders = new ArrayList<>();
    private List<Order> deliveryOrders = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private User currentUser;

    private static final String TAG = "PayHereDemo";

    private String currentProcessingOrderId;

    private final ActivityResultLauncher<Intent> payHereLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            handlePaymentSuccess();
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
        }
    });

    private void sendInvoiceEmail(String userEmail, Map<String, Object> orderData) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.get("application/json; charset=utf-8");

                JSONObject json = new JSONObject();
                json.put("userEmail", userEmail);
                json.put("orderId", orderData.get("orderId"));
                json.put("totalPrice", orderData.get("totalPrice"));
                json.put("deliveryPrice", orderData.get("deliveryPrice"));

                // Handle payment date
                Object paymentDateObj = orderData.get("paymentDate");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String paymentDateStr = "";
                if (paymentDateObj instanceof Timestamp) {
                    paymentDateStr = sdf.format(((Timestamp) paymentDateObj).toDate());
                } else if (paymentDateObj instanceof Date) {
                    paymentDateStr = sdf.format((Date) paymentDateObj);
                }
                json.put("paymentDate", paymentDateStr);

                // Handle items
                JSONArray itemsArray = new JSONArray();
                List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        JSONObject itemJson = new JSONObject();
                        itemJson.put("productName", item.get("productName"));
                        itemJson.put("quantity", item.get("quantity").toString());
                        itemJson.put("price", item.get("price").toString());
                        itemsArray.put(itemJson);
                    }
                }
                json.put("items", itemsArray);

                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url("http://192.168.1.3:8080/ClayBricksBackend/SendInvoiceEmail")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                Log.d(TAG, "Invoice email response: " + response.body().string());
            } catch (Exception e) {
                Log.e(TAG, "Error sending invoice email", e);
            }
        }).start();
    }

    private void handlePaymentSuccess() {
        if (currentProcessingOrderId == null) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String deliveryOrderId = "DEL-" + System.currentTimeMillis();

        db.collection("clay-bricks-order").document(currentProcessingOrderId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> orderData = new HashMap<>(documentSnapshot.getData());
                orderData.put("status", "Paid");
                orderData.put("paymentDate", new Date());
                orderData.put("originalOrderId", currentProcessingOrderId);

                // Create delivery order and delete original
                db.collection("clay-bricks-delivery-order").document(deliveryOrderId).set(orderData).addOnSuccessListener(aVoid -> {
                    db.collection("clay-bricks-order").document(currentProcessingOrderId).delete().addOnSuccessListener(aVoid1 -> {
                        Toast.makeText(this, "Order completed!", Toast.LENGTH_SHORT).show();
                        loadUserOrders();
                        loadDeliveryOrders();
                        sendInvoiceEmail(currentUser.getUser_email(), orderData);
                    }).addOnFailureListener(e -> Log.e(TAG, "Delete error", e));
                }).addOnFailureListener(e -> Log.e(TAG, "Delivery order error", e));
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Fetch error", e));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_order);

        // Check if user is logged in
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);
        if (userJson == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUser = new Gson().fromJson(userJson, User.class);
        currentUserId = currentUser.getUser_mobile();

        // Initialize RecyclerViews
        ordersRecyclerView = findViewById(R.id.orderViewRecycleView);
        deliveryOrdersRecyclerView = findViewById(R.id.deliveryOderRecycleView);

        // Setup orders RecyclerView
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter();
        ordersRecyclerView.setAdapter(ordersAdapter);

        // Setup delivery orders RecyclerView
        deliveryOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deliveryOrdersAdapter = new DeliveryOrdersAdapter();
        deliveryOrdersRecyclerView.setAdapter(deliveryOrdersAdapter);

        loadUserOrders();
        loadDeliveryOrders();
    }

    private void loadUserOrders() {
        db.collection("clay-bricks-order").whereEqualTo("userId", currentUserId).get().addOnSuccessListener(querySnapshot -> {
            orders = querySnapshot.toObjects(Order.class);
            ordersAdapter.setOrders(orders);
        });
    }



    private class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
        private List<Order> orders = new ArrayList<>();

        public void setOrders(List<Order> orders) {
            this.orders = orders;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Order order = orders.get(position);
            holder.orderId.setText(order.getOrderId());
            holder.orderStatus.setText(order.getStatus());

            // Set item click listener
            holder.itemView.setOnClickListener(v -> showOrderDetailsDialog(order));

            // Reset both buttons to GONE first
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnBuyNow.setVisibility(View.GONE);

            // Set status text color
            Context context = holder.itemView.getContext();
            if ("Confirmed".equals(order.getStatus())) {
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, R.color.active_green));
                holder.btnBuyNow.setVisibility(View.VISIBLE);
                holder.btnBuyNow.setOnClickListener(v -> initiatePayment(order));
            } else if ("Pending".equals(order.getStatus())) {
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_light));
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setOnClickListener(v -> cancelOrder(order.getOrderId()));
            } else {
                holder.orderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_light));
            }
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus;
            Button btnCancel, btnBuyNow;

            ViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.orderId);
                orderStatus = itemView.findViewById(R.id.orderStatus);
                btnCancel = itemView.findViewById(R.id.btnCancel);
                btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
            }
        }
    }

    // Add this method in your UserOrderActivity class
    private void showOrderDetailsDialog(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_order_details, null);
        builder.setView(dialogView);

        TextView tvOrderId = dialogView.findViewById(R.id.tvOrderId);
        TextView tvStatus = dialogView.findViewById(R.id.tvOrderStatus);
        TextView tvTotalPrice = dialogView.findViewById(R.id.tvTotalPrice);
        TextView tvDelivery = dialogView.findViewById(R.id.tvDeliveryPrice);
        LinearLayout itemsContainer = dialogView.findViewById(R.id.itemsContainer);
        TextView okBtn = dialogView.findViewById(R.id.okBtn);

        tvOrderId.setText("Order ID : " + order.getOrderId());
        tvStatus.setText("Status: " + order.getStatus());
        tvTotalPrice.setText("Total: Rs." + order.getTotalPrice());
        tvDelivery.setText("Delivery: Rs." + order.getDeliveryPrice());

        // Clear previous items
        itemsContainer.removeAllViews();

        if (order.getItems() != null) {
            for (Map<String, Object> item : order.getItems()) {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_product2, null);
                TextView name = itemView.findViewById(R.id.tvsProductName);
                TextView qty = itemView.findViewById(R.id.tvsQuantity);
                TextView price = itemView.findViewById(R.id.tvsPrice);

                name.setText((String) item.get("productName"));
                qty.setText("Qty: " + item.get("quantity").toString());
                price.setText("Rs." + item.get("price").toString());

                itemsContainer.addView(itemView);
            }
        }

        AlertDialog dialog = builder.create();
        okBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadDeliveryOrders() {
        db.collection("clay-bricks-delivery-order")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Order> orders = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        order.setDeliveryId(doc.getId());
                        order.setStatus(doc.getString("status"));  // Make sure status is loaded
                        orders.add(order);
                    }
                    deliveryOrdersAdapter.setDeliveryOrders(orders);
                });
    }

    private class DeliveryOrdersAdapter extends RecyclerView.Adapter<DeliveryOrdersAdapter.ViewHolder> {
        private List<Order> deliveryOrders = new ArrayList<>();

        public void setDeliveryOrders(List<Order> orders) {
            this.deliveryOrders = orders;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_order, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Order order = deliveryOrders.get(position);
            holder.orderId.setText(order.getOrderId());
            holder.orderStatus.setText(order.getStatus());

            // Set status color
            int statusColor = "Delivered".equals(order.getStatus()) ?
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.active_green) :
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.primary_text_light);
            holder.orderStatus.setTextColor(statusColor);

            // Handle button visibility - FIXED CONDITION
            if ("completed".equals(order.getStatus())) { // Now checking correct status field
                holder.btnViewDelivery.setVisibility(View.GONE);
                holder.btnRateProduct.setVisibility(View.VISIBLE);
                holder.btnRateProduct.setOnClickListener(v -> showRatingDialog(order.getDeliveryId()));
            } else {
                holder.btnViewDelivery.setVisibility(View.VISIBLE);
                holder.btnRateProduct.setVisibility(View.GONE);
                holder.btnViewDelivery.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), ViewDeliveryMapActivity.class);
                    intent.putExtra("DELIVERY_ID", order.getDeliveryId());
                    v.getContext().startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return deliveryOrders.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus;
            Button btnViewDelivery, btnRateProduct;

            ViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.tvOrderId);
                orderStatus = itemView.findViewById(R.id.tvOrderStatus);
                btnViewDelivery = itemView.findViewById(R.id.btnViewDelivery);
                btnRateProduct = itemView.findViewById(R.id.btnRateProduct);
            }
        }
    }

    private void showRatingDialog(String deliveryId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.rating_dialog, null);
        builder.setView(dialogView);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating > 0) {
                saveRatingAndCompleteDelivery(deliveryId, rating);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a rating first", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveRatingAndCompleteDelivery(String deliveryId, float rating) {
        // Save rating
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("deliveryId", deliveryId);
        ratingData.put("rating", rating);
        ratingData.put("userId", currentUserId);
        ratingData.put("timestamp", new Date());

        db.collection("order_ratings").add(ratingData)
                .addOnSuccessListener(documentReference -> {
                    // Move to completed deliveries
                    moveToCompletedDeliveries(deliveryId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Rating failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void moveToCompletedDeliveries(String deliveryId) {
        db.collection("clay-bricks-delivery-order").document(deliveryId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        data.put("status", "Completed");

                        db.collection("completed-deliveries").document(deliveryId)
                                .set(data)
                                .addOnSuccessListener(aVoid -> {
                                    // Delete from delivery orders
                                    db.collection("clay-bricks-delivery-order").document(deliveryId)
                                            .delete()
                                            .addOnSuccessListener(aVoid1 -> {
                                                loadDeliveryOrders();
                                                Toast.makeText(this, "Delivery completed!", Toast.LENGTH_SHORT).show();
                                            });
                                });
                    }
                });
    }

    private void initiatePayment(Order order) {
        currentProcessingOrderId = order.getOrderId();

        db.collection("clay-bricks-order").document(order.getOrderId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                try {
                    InitRequest req = new InitRequest();
                    req.setMerchantId("1228244");
                    req.setCurrency("LKR");
                    double amount = Double.parseDouble(order.getTotalPrice());
                    req.setAmount(amount);
                    req.setOrderId(order.getOrderId());
                    req.setItemsDescription(order.getOrderId());

                    req.getCustomer().setFirstName(currentUser.getUser_name());
                    req.getCustomer().setLastName(currentUser.getUser_last_name());
                    req.getCustomer().setPhone(currentUser.getUser_mobile());
                    req.getCustomer().setEmail(currentUser.getUser_email());

                    req.getCustomer().getAddress().setAddress("No Address Provided");
                    req.getCustomer().getAddress().setCity("");
                    req.getCustomer().getAddress().setCountry("Sri Lanka");

                    List<Map<String, Object>> items = (List<Map<String, Object>>) documentSnapshot.get("items");
                    if (items != null) {
                        for (Map<String, Object> item : items) {
                            String productName = (String) item.get("productName");
                            int quantity = ((Long) item.get("quantity")).intValue();
                            double price = Double.parseDouble((String) item.get("price"));
                            req.getItems().add(new Item(null, productName, quantity, price));
                        }
                    }

                    req.setNotifyUrl("https://https://0152-175-157-42-7.ngrok-free.app/payhere-callback");

                    Intent intent = new Intent(this, PHMainActivity.class);
                    intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
                    PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);

                    payHereLauncher.launch(intent);

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Error processing payment amount", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Number format error", e);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load order details", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error loading order", e);
        });
    }

    private void cancelOrder(String orderId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("clay-bricks-order").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            Order order = documentSnapshot.toObject(Order.class);
            if (order == null || order.getItems() == null) return;

            List<Task<Void>> stockUpdates = new ArrayList<>();

            for (Map<String, Object> item : order.getItems()) {
                String productId = (String) item.get("productId");
                Object quantityObj = item.get("quantity");

                if (productId != null && quantityObj != null) {
                    String orderQtyStr = quantityObj.toString();

                    Task<Void> updateTask = db.collection("clay-bricks-product").document(productId).get().continueWithTask(task -> {
                        DocumentSnapshot productDoc = task.getResult();
                        String currentQtyStr = productDoc.getString("quantity");
                        int currentQty = 0;
                        int orderQty = 0;

                        try {
                            currentQty = Integer.parseInt(currentQtyStr);
                            orderQty = Integer.parseInt(orderQtyStr);
                        } catch (NumberFormatException e) {
                            return Tasks.forException(new Exception("Invalid quantity format"));
                        }

                        int updatedQty = currentQty + orderQty;

                        return db.collection("clay-bricks-product").document(productId).update("quantity", String.valueOf(updatedQty));
                    });

                    stockUpdates.add(updateTask);
                }
            }

            Tasks.whenAll(stockUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    db.collection("clay-bricks-order").document(orderId).delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Order canceled", Toast.LENGTH_SHORT).show();
                        loadUserOrders();
                    });
                } else {
                    Toast.makeText(this, "Failed to update stock", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onBackPressed() {
        // Navigate back to MainActivity which hosts UserHomeFragment
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

}