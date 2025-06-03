package com.sandun.claybricks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.sandun.claybricks.model.Product;
import com.sandun.claybricks.model.User;
import com.sandun.claybricks.sqlite_database.WishlistDbHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignlProductViewActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String productId;
    private Product currentProduct;

    // UI Components
    private ImageView ivProductImage;
    private ImageButton btnWishlist;
    private TextView tvProductName, tvProductWidth, tvProductHeight,
            tvProductLength, tvProductWeight, tvProductType,
            tvQuantity, ourProductQuantity, productPrice;
    private Button btnAddToCart, btnBuyNow;
    private RecyclerView similarProductsRecycler;
    private int quantity = 1;
    private boolean isWishlisted = false;
    private WishlistDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signl_product_view);

        dbHelper = new WishlistDbHelper(this);
        initializeViews();
        initializeFirestore();
        handleIntent();
        setupClickListeners();
    }

    private void initializeViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        btnWishlist = findViewById(R.id.btnWishlist);
        tvProductName = findViewById(R.id.tvProductName);
        tvProductWidth = findViewById(R.id.tvProductWidth);
        tvProductHeight = findViewById(R.id.tvProductHeight);
        tvProductLength = findViewById(R.id.tvProductLength);
        tvProductWeight = findViewById(R.id.tvProductWeight);
        tvProductType = findViewById(R.id.tvProductType);
        tvQuantity = findViewById(R.id.tvQuantity);
        ourProductQuantity = findViewById(R.id.ourProductQuantity);
        productPrice = findViewById(R.id.productPrice);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        similarProductsRecycler = findViewById(R.id.recyclerViewSimilarProducts);

        // Setup RecyclerView for similar products
        similarProductsRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
    }

    private void handleIntent() {
        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null || productId.isEmpty()) {
            showErrorAndFinish("Invalid product ID");
            return;
        }
        loadProductData();
    }

    private void loadProductData() {
        db.collection("clay-bricks-product")
                .document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Log.d("FirestoreData", "Document Data: " + document.getData());

                            currentProduct = new Product();
                            currentProduct.setProduct_id(document.getId());
                            currentProduct.setProduct_name(document.getString("productName"));
                            currentProduct.setProduct_width(document.getString("width"));
                            currentProduct.setProduct_height(document.getString("height"));
                            currentProduct.setProduct_length(document.getString("length"));
                            currentProduct.setProduct_type(document.getString("type"));
                            currentProduct.setProduct_weight(document.getString("weight"));
                            currentProduct.setProduct_quantity(document.getString("quantity"));
                            currentProduct.setImageUrl(document.getString("imageUrl"));
                            currentProduct.setProduct_status(document.getString("product_status"));

                            // Handle numeric price (long/double)
                            if (document.contains("price")) {
                                Object priceObj = document.get("price");
                                if (priceObj instanceof Long) {
                                    long priceLong = (Long) priceObj;
                                    currentProduct.setProduct_price(String.valueOf(priceLong));
                                } else if (priceObj instanceof Double) {
                                    double priceDouble = (Double) priceObj;
                                    currentProduct.setProduct_price(String.valueOf((long) priceDouble));
                                } else {
                                    currentProduct.setProduct_price("N/A");
                                }
                            } else {
                                currentProduct.setProduct_price("N/A");
                            }

                            updateUI();
                            loadSimilarProducts();
                        } else {
                            showErrorAndFinish("Product not found");
                        }
                    } else {
                        Log.e("FirestoreError", "Error loading product: " + task.getException());
                        showErrorAndFinish("Error loading product");
                    }
                });
    }

    private void updateUI() {
        try {
            // Product Image
            if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
                Picasso.get()
                        .load(currentProduct.getImageUrl())
                        .placeholder(R.drawable.update_product)
                        .error(R.drawable.update_product)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.update_product);
            }

            // Product Details
            tvProductName.setText(currentProduct.getProduct_name() != null ?
                    currentProduct.getProduct_name() : "N/A");
            tvProductWidth.setText("Width: " + (currentProduct.getProduct_width() != null ?
                    currentProduct.getProduct_width() + "cm" : "N/A"));
            tvProductHeight.setText("Height: " + (currentProduct.getProduct_height() != null ?
                    currentProduct.getProduct_height() + "cm" : "N/A"));
            tvProductLength.setText("Length: " + (currentProduct.getProduct_length() != null ?
                    currentProduct.getProduct_length() + "cm" : "N/A"));
            tvProductWeight.setText("Weight: " + (currentProduct.getProduct_weight() != null ?
                    currentProduct.getProduct_weight() + "kg" : "N/A"));
            tvProductType.setText("Type: " + (currentProduct.getProduct_type() != null ?
                    currentProduct.getProduct_type() : "N/A"));
            ourProductQuantity.setText("We Have Product Quantity: " +
                    (currentProduct.getProduct_quantity() != null ?
                            currentProduct.getProduct_quantity() : "N/A"));
            productPrice.setText("Rs." + (currentProduct.getProduct_price() != null ?
                    currentProduct.getProduct_price() : "N/A"));

            checkWishlistStatus();
        } catch (Exception e) {
            Log.e("UpdateUI", "Error updating UI: " + e.getMessage());
        }
    }

    private void checkWishlistStatus() {
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);
        if (userJson == null) return;

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        String userMobile = user.getUser_mobile();

        isWishlisted = dbHelper.isInWishlist(userMobile, productId);
        updateWishlistButton();
    }

    private void addToWishlist() {
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);
        if (userJson == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        String userMobile = user.getUser_mobile();

        long result = dbHelper.addToWishlist(userMobile, currentProduct);
        if (result != -1) {
            Toast.makeText(this, "Added to wishlist!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add to wishlist", Toast.LENGTH_SHORT).show();
            isWishlisted = false;
            updateWishlistButton();
        }
    }

    private void removeFromWishlist() {
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);
        if (userJson == null) return;

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        String userMobile = user.getUser_mobile();

        int deletedRows = dbHelper.removeFromWishlist(userMobile, productId);
        if (deletedRows > 0) {
            Toast.makeText(this, "Removed from wishlist!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show();
            isWishlisted = true;
            updateWishlistButton();
        }
    }

    private void updateWishlistButton() {
        btnWishlist.setImageResource(
                isWishlisted ? R.drawable.watchlist_icon_on : R.drawable.watchlist_icon
        );
    }

    private void setupClickListeners() {
        findViewById(R.id.btnDecrease).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        findViewById(R.id.btnIncrease).setOnClickListener(v -> {
            if (quantity < 100) { // Max quantity 100
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(this, "Maximum quantity is 100", Toast.LENGTH_SHORT).show();
            }
        });

        btnWishlist.setOnClickListener(v -> toggleWishlist());
        btnAddToCart.setOnClickListener(v -> addToCart());
        btnBuyNow.setOnClickListener(v -> proceedToCheckout());
    }

    private void toggleWishlist() {
        isWishlisted = !isWishlisted;
        updateWishlistButton();

        if (isWishlisted) {
            addToWishlist();
        } else {
            removeFromWishlist();
        }
    }

    private void addToCart() {
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);

        if (userJson == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);
        final String userMobile = user.getUser_mobile();
        final DocumentReference productRef = db.collection("clay-bricks-product").document(productId);

        db.runTransaction(transaction -> {
            // Get current product data
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            // Handle quantity as String
            String quantityStr = productSnapshot.getString("quantity");
            if (quantityStr == null) {
                throw new FirebaseFirestoreException(
                        "Product quantity not found",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            try {
                long currentQuantity = Long.parseLong(quantityStr);
                if (currentQuantity < quantity) {
                    throw new FirebaseFirestoreException(
                            "Only " + currentQuantity + " items available",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }

                // Update quantity (still as String)
                transaction.update(productRef, "quantity", String.valueOf(currentQuantity - quantity));
                return null;
            } catch (NumberFormatException e) {
                throw new FirebaseFirestoreException(
                        "Invalid quantity format",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }
        }).addOnSuccessListener(aVoid -> {
            // Proceed with cart addition after successful quantity update
            addToCartAfterQuantityCheck(userMobile);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void addToCartAfterQuantityCheck(String userMobile) {
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("userMobile", userMobile);
        cartItem.put("productId", productId);
        cartItem.put("quantity", quantity);
        cartItem.put("timestamp", FieldValue.serverTimestamp());

        db.collection("clay-bricks-cart")
                .add(cartItem)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show();
                    updateUIQuantity(); // Update displayed quantity
                    sendCartUpdateBroadcast();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show();
                    rollbackQuantityUpdate();
                });
    }

    private void updateUIQuantity() {
        runOnUiThread(() -> {
            try {
                long currentQty = Long.parseLong(currentProduct.getProduct_quantity());
                long newQuantity = currentQty - quantity;
                ourProductQuantity.setText("We Have Product Quantity: " + newQuantity);
                currentProduct.setProduct_quantity(String.valueOf(newQuantity));
            } catch (NumberFormatException e) {
                ourProductQuantity.setText("Quantity update error");
                Log.e("QuantityUpdate", "Error parsing quantity", e);
            }
        });
    }

    private void rollbackQuantityUpdate() {
        db.collection("clay-bricks-product")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentQtyStr = documentSnapshot.getString("quantity");
                    try {
                        long currentQty = Long.parseLong(currentQtyStr);
                        db.collection("clay-bricks-product")
                                .document(productId)
                                .update("quantity", String.valueOf(currentQty + quantity));
                    } catch (NumberFormatException e) {
                        Log.e("RollbackError", "Failed to parse quantity for rollback", e);
                    }
                });
    }

    private void sendCartUpdateBroadcast() {
        Intent intent = new Intent("CART_UPDATE_ACTION");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void proceedToCheckout() {
        // Implement your checkout logic here
        Intent intent = new Intent(SignlProductViewActivity.this, CartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void loadSimilarProducts() {
        if (currentProduct == null || currentProduct.getProduct_type() == null) return;

        final String targetType = currentProduct.getProduct_type();
        final String currentDocId = currentProduct.getProduct_id();

        Log.d("SimilarProducts", "Searching for type: " + targetType + ", excluding doc: " + currentDocId);

        db.collection("clay-bricks-product")
                .whereEqualTo("type", targetType)
                .whereEqualTo("product_status", "1") // Add this line to filter active products
                .whereNotEqualTo(FieldPath.documentId(), currentDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> similarProducts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Additional status check (optional but recommended)
                                String status = document.getString("product_status");
                                if (!"1".equals(status)) continue;

                                // Rest of your existing document processing code
                                if (!document.contains("productName") || !document.contains("type")) {
                                    continue;
                                }

                                Product product = new Product();
                                product.setProduct_id(document.getId());
                                product.setProduct_name(document.getString("productName"));
                                product.setProduct_type(document.getString("type"));
                                product.setImageUrl(document.getString("imageUrl"));

                                // Add status to product object if needed
                                product.setProduct_status(status);

                                if (document.contains("price")) {
                                    Object price = document.get("price");
                                    if (price instanceof Number) {
                                        product.setProduct_price("Rs. " + ((Number) price).longValue());
                                    }
                                }

                                similarProducts.add(product);
                            } catch (Exception e) {
                                Log.e("SimilarError", "Error processing doc", e);
                            }
                        }
                        updateSimilarProductsUI(similarProducts);
                    } else {
                        Log.e("SimilarError", "Query failed", task.getException());
                    }
                });
    }

    private void updateSimilarProductsUI(List<Product> products) {
        runOnUiThread(() -> {
            if (products.isEmpty()) {
                findViewById(R.id.tvNoSimilarProducts).setVisibility(View.VISIBLE);
                similarProductsRecycler.setVisibility(View.GONE);
            } else {
                findViewById(R.id.tvNoSimilarProducts).setVisibility(View.GONE);
                similarProductsRecycler.setVisibility(View.VISIBLE);
                SimilarProductsAdapter adapter = new SimilarProductsAdapter(products);
                similarProductsRecycler.setAdapter(adapter);
            }
        });
    }

    private class SimilarProductsAdapter extends RecyclerView.Adapter<SimilarProductsAdapter.ViewHolder> {
        private final List<Product> products;

        public SimilarProductsAdapter(List<Product> products) {
            this.products = products;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_box_singal_product_view, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = products.get(position);

            // Set click listener for the entire item
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SignlProductViewActivity.this, SignlProductViewActivity.class);
                intent.putExtra("PRODUCT_ID", product.getProduct_id());
                startActivity(intent);
            });

            // Rest of your existing binding code
            holder.productName.setText(product.getProduct_name() != null ?
                    product.getProduct_name() : "Product");
            holder.productPrice.setText(product.getProduct_price() != null ?
                    product.getProduct_price() : "Rs.0");

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Picasso.get()
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.update_product)
                        .error(R.drawable.update_product)
                        .into(holder.productImage);
            } else {
                holder.productImage.setImageResource(R.drawable.update_product);
            }
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName, productPrice;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.productImage);
                productName = itemView.findViewById(R.id.productName);
                productPrice = itemView.findViewById(R.id.productPrice);
            }
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}