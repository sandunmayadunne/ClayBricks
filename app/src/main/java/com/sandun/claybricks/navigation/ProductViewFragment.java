package com.sandun.claybricks.navigation;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sandun.claybricks.R;
import com.sandun.claybricks.model.Product;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProductViewFragment extends Fragment {

    private static final int UPDATE_REQUEST_CODE = 1001;

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;
    private Dialog loadingDialog;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String currentSearchQuery = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_view, container, false);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList);
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        EditText searchProduct = view.findViewById(R.id.searchProduct);
        searchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        showLoadingDialog();
        loadProductsFromFirestore();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            showLoadingDialog();
            loadProductsFromFirestore();
        }
    }

    private void showLoadingDialog() {
        loadingDialog = new Dialog(getContext());
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void filterProducts(String query) {
        currentSearchQuery = query.toLowerCase().trim();
        List<Product> filteredList = new ArrayList<>();
        for (Product product : productList) {
            if (product.getProduct_name().toLowerCase().contains(currentSearchQuery) ||
                    product.getProduct_type().toLowerCase().contains(currentSearchQuery)) {
                filteredList.add(product);
            }
        }
        adapter.updateList(filteredList);
    }

    private void loadProductsFromFirestore() {
        db.collection("clay-bricks-product")
                .get()
                .addOnCompleteListener(task -> {
                    dismissLoadingDialog();
                    if (task.isSuccessful()) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String product_id = document.getId();
                            String product_name = document.getString("productName");
                            String product_imageUrl = document.getString("imageUrl");
                            String product_type = document.getString("type");
                            String product_status = document.getString("product_status");

                            if (product_name != null) {
                                Product product = new Product(
                                        product_id,
                                        product_name,
                                        "",     // width
                                        "",     // height
                                        "",     // length
                                        product_type,
                                        "",     // weight
                                        "",     // quantity
                                        product_imageUrl,
                                        "",     // price
                                        product_status
                                );
                                productList.add(product);
                            }
                        }
                        filterProducts(currentSearchQuery);
                    } else {
                        Log.e("FirestoreError", "Error fetching products", task.getException());
                    }
                });
    }

    private void deleteProduct(Product product) {
        boolean removed = false;
        for (int i = 0; i < productList.size(); i++) {
            if (productList.get(i).getProduct_id().equals(product.getProduct_id())) {
                productList.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            filterProducts(currentSearchQuery);

            // Delete image from storage first
            if (product.getImageUrl() != null && product.getImageUrl().startsWith("http")) {
                StorageReference imageRef = storage.getReferenceFromUrl(product.getImageUrl());
                imageRef.delete().addOnSuccessListener(aVoid -> {
                    // Image deleted, now delete document
                    deleteProductDocument(product);
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show();
                });
            } else {
                // No image or not a URL, just delete document
                deleteProductDocument(product);
            }
        }
    }

    private void deleteProductDocument(Product product) {
        db.collection("clay-bricks-product").document(product.getProduct_id())
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Product deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Delete failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProductStatus(String productId, String newStatus, ProductAdapter.ProductViewHolder holder, int position) {
        db.collection("clay-bricks-product").document(productId)
                .update("product_status", newStatus)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update original productList
                        for (Product p : productList) {
                            if (p.getProduct_id().equals(productId)) {
                                p.setProduct_status(newStatus);
                                break;
                            }
                        }
                        // Update adapter's local list
                        Product updatedProduct = adapter.localProductList.get(position);
                        updatedProduct.setProduct_status(newStatus);
                        // Update ViewHolder UI
                        boolean isActive = "1".equals(newStatus);
                        holder.statusText.setText(isActive ? "Active" : "Deactive");
                        holder.statusText.setTextColor(getResources().getColor(isActive ? R.color.active_green : R.color.inactive_red));
                    } else {
                        // Revert switch on failure
                        holder.statusSwitch.setChecked(!holder.statusSwitch.isChecked());
                        Toast.makeText(getContext(), "Status update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private List<Product> localProductList;

        public ProductAdapter(List<Product> productList) {
            this.localProductList = productList;
        }

        public void updateList(List<Product> newList) {
            localProductList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_product_box, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = localProductList.get(position);
            holder.productName.setText(product.getProduct_name());
            holder.productType.setText(product.getProduct_type());

            // Load image using URL
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("http")) {
                    // Load from Firebase Storage
                    Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.add_icon)
                            .error(R.drawable.add_icon)
                            .into(holder.productImage);
                } else {
                    // Fallback to Base64 (if you still support it)
                    try {
                        byte[] decodedBytes = Base64.decode(imageUrl, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        holder.productImage.setImageBitmap(decodedBitmap);
                    } catch (IllegalArgumentException e) {
                        Log.e("ImageError", "Error decoding image: " + e.getMessage());
                        holder.productImage.setImageResource(R.drawable.add_icon);
                    }
                }
            } else {
                holder.productImage.setImageResource(R.drawable.add_icon);
            }

            boolean isActive = "1".equals(product.getProduct_status());
            // Clear previous listener to avoid recycling issues
            holder.statusSwitch.setOnCheckedChangeListener(null);
            holder.statusSwitch.setChecked(isActive);
            holder.statusText.setText(isActive ? "Active" : "Deactive");
            holder.statusText.setTextColor(getResources().getColor(isActive ? R.color.active_green : R.color.inactive_red));

            holder.statusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String newStatus = isChecked ? "1" : "0";
                updateProductStatus(product.getProduct_id(), newStatus, holder, position);
            });

            holder.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(product));

            holder.itemView.setOnClickListener(v -> {
                if (product.getProduct_id() != null && !product.getProduct_id().isEmpty()) {
                    Intent intent = new Intent(getActivity(), ProductUpdateActivity.class);
                    intent.putExtra("PRODUCT_ID", product.getProduct_id());
                    startActivityForResult(intent, UPDATE_REQUEST_CODE);
                } else {
                    Toast.makeText(getContext(), "Invalid product", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return localProductList.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView productName, statusText, productType;
            ImageView productImage;
            Button deleteButton;
            Switch statusSwitch;

            public ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.productTypeName);
                productType = itemView.findViewById(R.id.productType);
                productImage = itemView.findViewById(R.id.productImage111);
                deleteButton = itemView.findViewById(R.id.deleteProduct);
                statusSwitch = itemView.findViewById(R.id.productStatus);
                statusText = itemView.findViewById(R.id.status_text);
            }
        }
    }

    private void showDeleteConfirmationDialog(Product product) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.admin_delete_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView okButton = dialog.findViewById(R.id.okButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            deleteProduct(product);
        });

        dialog.show();
    }
}