package com.sandun.claybricks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.sandun.claybricks.model.Product;
import com.sandun.claybricks.model.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerViewCart;
    private TextView tvTotalPrice;
    private CartAdapter adapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private String currentUserId;
    private Button btnOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        db = FirebaseFirestore.getInstance();
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnOrder = findViewById(R.id.btnOrder);

        setupRecyclerView();
        loadCurrentUser();

        btnOrder.setOnClickListener(v -> onPlaceOrderClick());
    }

    public void onPlaceOrderClick() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify all items have sufficient stock
        checkStockBeforeOrder(cartItems, () -> {
            Intent intent = new Intent(this, OrderPageActivity.class);
            intent.putExtra("totalPrice", tvTotalPrice.getText().toString());
            intent.putParcelableArrayListExtra("cartItems", new ArrayList<>(cartItems));
            startActivity(intent);
        });
    }

    private void checkStockBeforeOrder(List<CartItem> items, Runnable onSuccess) {
        final boolean[] allInStock = {true};

        for (CartItem item : items) {
            db.collection("clay-bricks-product")
                    .document(item.getProduct().getProduct_id())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String currentQtyStr = documentSnapshot.getString("quantity");
                        try {
                            int currentQty = Integer.parseInt(currentQtyStr);
                            if (currentQty < item.getQuantity()) {
                                allInStock[0] = false;
                                Toast.makeText(this,
                                        "Not enough stock for " + item.getProduct().getProduct_name(),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (NumberFormatException e) {
                            allInStock[0] = false;
                            Toast.makeText(this, "Invalid quantity format", Toast.LENGTH_SHORT).show();
                        }

                        // If we've checked all items and all are in stock
                        if (items.indexOf(item) == items.size() - 1 && allInStock[0]) {
                            onSuccess.run();
                        }
                    });
        }
    }

    private void setupRecyclerView() {
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter();
        recyclerViewCart.setAdapter(adapter);
    }

    private void loadCurrentUser() {
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);
        if (userJson != null) {
            Gson gson = new Gson();
            User user = gson.fromJson(userJson, User.class);
            currentUserId = user.getUser_mobile();
            loadCartItems();
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadCartItems() {
        db.collection("clay-bricks-cart")
                .whereEqualTo("userMobile", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItems.clear();
                        for (QueryDocumentSnapshot cartDoc : task.getResult()) {
                            String productId = cartDoc.getString("productId");
                            Long quantityLong = cartDoc.getLong("quantity");
                            int quantity = quantityLong != null ? quantityLong.intValue() : 1;

                            // Get product details
                            db.collection("clay-bricks-product")
                                    .document(productId)
                                    .get()
                                    .addOnCompleteListener(productTask -> {
                                        if (productTask.isSuccessful()) {
                                            DocumentSnapshot productDoc = productTask.getResult();
                                            if (productDoc.exists()) {
                                                Product product = productDoc.toObject(Product.class);
                                                if (product != null) {
                                                    product.setProduct_id(productDoc.getId());
                                                    product.setProduct_name(productDoc.getString("productName"));
                                                    product.setProduct_type(productDoc.getString("type"));
                                                    product.setProduct_price(String.valueOf(productDoc.get("price")));
                                                    product.setImageUrl(productDoc.getString("imageUrl"));

                                                    CartItem item = new CartItem(
                                                            cartDoc.getId(),
                                                            product,
                                                            quantity
                                                    );

                                                    cartItems.add(item);
                                                    adapter.notifyDataSetChanged();
                                                    calculateTotal();
                                                }
                                            }
                                        }
                                    });
                        }
                    } else {
                        Log.e("CartError", "Error loading cart", task.getException());
                        Toast.makeText(this, "Failed to load cart items", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            try {
                double price = Double.parseDouble(item.getProduct().getProduct_price());
                total += price * item.getQuantity();
            } catch (NumberFormatException e) {
                Log.e("PriceError", "Invalid price format", e);
            }
        }
        tvTotalPrice.setText(String.format("Total: Rs. %.2f", total));
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cart, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CartItem item = cartItems.get(position);
            Product product = item.getProduct();

            // Load product image
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Picasso.get()
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.update_product)
                        .error(R.drawable.update_product)
                        .into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.drawable.update_product);
            }

            holder.tvProductName.setText(product.getProduct_name());
            holder.tvProductType.setText(product.getProduct_type());
            holder.tvProductPrice.setText("Rs." + product.getProduct_price());
            holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

            // Quantity controls
            holder.btnIncrease.setOnClickListener(v -> updateQuantity(item, 1));
            holder.btnDecrease.setOnClickListener(v -> updateQuantity(item, -1));

            // Remove item
            holder.btnRemove.setOnClickListener(v -> removeItem(item));
        }

        @Override
        public int getItemCount() {
            return cartItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageButton btnIncrease, btnDecrease, btnRemove;
            TextView tvProductName, tvProductType, tvProductPrice, tvQuantity;
            ImageView imgProduct;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgProduct = itemView.findViewById(R.id.imgProduct);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvProductType = itemView.findViewById(R.id.tvProductType);
                tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                btnIncrease = itemView.findViewById(R.id.btnIncrease);
                btnDecrease = itemView.findViewById(R.id.btnDecrease);
                btnRemove = itemView.findViewById(R.id.btnRemove);
            }
        }
    }

    private void updateQuantity(CartItem item, int delta) {
        int newQuantity = item.getQuantity() + delta;

        if (newQuantity < 1) {
            Toast.makeText(this, "Minimum quantity is 1", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newQuantity > 100) {
            Toast.makeText(this, "Maximum quantity is 100", Toast.LENGTH_SHORT).show();
            return;
        }

        // First update Firestore cart
        db.collection("clay-bricks-cart")
                .document(item.getCartId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    // Then update product quantity in inventory
                    updateProductQuantity(item.getProduct().getProduct_id(), delta * -1, () -> {
                        item.setQuantity(newQuantity);
                        adapter.notifyDataSetChanged();
                        calculateTotal();
                        Toast.makeText(this, "Quantity updated", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update quantity", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProductQuantity(String productId, int delta, Runnable onSuccess) {
        db.collection("clay-bricks-product")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentQtyStr = documentSnapshot.getString("quantity");
                    try {
                        int currentQty = Integer.parseInt(currentQtyStr);
                        int newQty = currentQty + delta;

                        // Ensure we don't go negative
                        if (newQty < 0) {
                            Toast.makeText(this, "Not enough stock available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("clay-bricks-product")
                                .document(productId)
                                .update("quantity", String.valueOf(newQty))
                                .addOnSuccessListener(aVoid -> onSuccess.run())
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to update inventory", Toast.LENGTH_SHORT).show();
                                });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid quantity format", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeItem(CartItem item) {
        // First get current quantity to restore
        int quantityToRestore = item.getQuantity();

        db.collection("clay-bricks-cart")
                .document(item.getCartId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Restore product quantity
                    updateProductQuantity(item.getProduct().getProduct_id(), quantityToRestore, () -> {
                        cartItems.remove(item);
                        adapter.notifyDataSetChanged();
                        calculateTotal();
                        Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show();
                });
    }

    public static class CartItem implements Parcelable {
        private final String cartId;
        private final Product product;
        private int quantity;

        public CartItem(String cartId, Product product, int quantity) {
            this.cartId = cartId;
            this.product = product;
            this.quantity = quantity;
        }

        protected CartItem(Parcel in) {
            cartId = in.readString();
            product = in.readParcelable(Product.class.getClassLoader());
            quantity = in.readInt();
        }

        public static final Creator<CartItem> CREATOR = new Creator<CartItem>() {
            @Override
            public CartItem createFromParcel(Parcel in) {
                return new CartItem(in);
            }

            @Override
            public CartItem[] newArray(int size) {
                return new CartItem[size];
            }
        };

        public String getCartId() { return cartId; }
        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(cartId);
            dest.writeParcelable(product, flags);
            dest.writeInt(quantity);
        }
    }
}