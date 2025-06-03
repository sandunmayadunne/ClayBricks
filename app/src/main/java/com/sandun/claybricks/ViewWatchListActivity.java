package com.sandun.claybricks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.sandun.claybricks.model.User;
import com.sandun.claybricks.sqlite_database.WishlistDbHelper;
import com.squareup.picasso.Picasso;

public class ViewWatchListActivity extends AppCompatActivity {

    private RecyclerView recyclerWatchlist;
    private WishlistDbHelper dbHelper;
    private FirebaseFirestore firestore;
    private String userMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_watch_list);

        // Initialize views
        recyclerWatchlist = findViewById(R.id.recyclerWatchlist);
        recyclerWatchlist.setLayoutManager(new LinearLayoutManager(this));

        // Initialize database helper and Firestore
        dbHelper = new WishlistDbHelper(this);
        firestore = FirebaseFirestore.getInstance();

        // Get user mobile from SharedPreferences
        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);

        if (userJson != null) {
            Gson gson = new Gson();
            User user = gson.fromJson(userJson, User.class);
            userMobile = user.getUser_mobile();
            loadWatchlistItems();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadWatchlistItems() {
        Cursor cursor = dbHelper.getAllWishlistItems(userMobile);
        if (cursor != null && cursor.getCount() > 0) {
            WatchlistAdapter adapter = new WatchlistAdapter(this, cursor);
            recyclerWatchlist.setAdapter(adapter);
        } else {
            Toast.makeText(this, "Your watchlist is empty", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Nested Adapter Class
    private class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder> {

        private final Context context;
        private Cursor cursor;

        public WatchlistAdapter(Context context, Cursor cursor) {
            this.context = context;
            this.cursor = cursor;
        }

        @NonNull
        @Override
        public WatchlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.watchlist_item, parent, false);
            return new WatchlistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WatchlistViewHolder holder, int position) {
            if (!cursor.moveToPosition(position)) {
                return;
            }

            String productId = cursor.getString(cursor.getColumnIndexOrThrow(WishlistDbHelper.COLUMN_PRODUCT_ID));
            String productName = cursor.getString(cursor.getColumnIndexOrThrow(WishlistDbHelper.COLUMN_PRODUCT_NAME));
            String productPrice = cursor.getString(cursor.getColumnIndexOrThrow(WishlistDbHelper.COLUMN_PRODUCT_PRICE));

            holder.productNameTV.setText(productName);
            holder.productPriceTV.setText("Rs." + productPrice);

            // Fetch product image from Firestore
            firestore.collection("clay-bricks-product")
                    .document(productId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("imageUrl")) {
                            String imageUrl = documentSnapshot.getString("imageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(imageUrl)
                                        .placeholder(R.drawable.update_product)
                                        .error(R.drawable.update_product)
                                        .into(holder.productImageIV);
                            }
                        }
                    });

            // Set click listener for the entire item
            holder.itemView.setOnClickListener(v -> {
                // Navigate to SignlProductViewActivity with product ID
                Intent intent = new Intent(context, SignlProductViewActivity.class);
                intent.putExtra("PRODUCT_ID", productId);
                context.startActivity(intent);
            });

            holder.btnRemoveWatchlist.setOnClickListener(v -> {
                int deletedRows = dbHelper.removeFromWishlist(userMobile, productId);
                if (deletedRows > 0) {
                    Toast.makeText(context, "Removed from watchlist", Toast.LENGTH_SHORT).show();
                    // Refresh the data
                    Cursor newCursor = dbHelper.getAllWishlistItems(userMobile);
                    swapCursor(newCursor);
                } else {
                    Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        public void swapCursor(Cursor newCursor) {
            if (cursor != null) {
                cursor.close();
            }
            cursor = newCursor;
            if (newCursor != null) {
                notifyDataSetChanged();
            }
        }

        class WatchlistViewHolder extends RecyclerView.ViewHolder {
            ImageView productImageIV;
            TextView productNameTV, productPriceTV;
            ImageButton btnRemoveWatchlist;
            View itemView;

            public WatchlistViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                productImageIV = itemView.findViewById(R.id.productImageIV);
                productNameTV = itemView.findViewById(R.id.productNameTV);
                productPriceTV = itemView.findViewById(R.id.productTypeTV);
                btnRemoveWatchlist = itemView.findViewById(R.id.btnRemoveWatchlist);
            }
        }
    }
}