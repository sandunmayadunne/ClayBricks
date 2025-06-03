package com.sandun.claybricks.clientFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.sandun.claybricks.R;
import com.sandun.claybricks.SignlProductViewActivity;
import com.sandun.claybricks.ViewWatchListActivity;
import com.sandun.claybricks.model.Product;
import com.sandun.claybricks.model.ProductType;
import com.sandun.claybricks.model.User;
import com.sandun.claybricks.sqlite_database.WishlistDbHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductFragment extends Fragment {

    // Recycler Views
    private RecyclerView typeRecyclerView, productRecyclerView;

    // Adapters
    private ProductTypeAdapter typeAdapter;
    private ProductGridAdapter productAdapter;

    // Firestore instance
    private FirebaseFirestore firestore;

    // Data lists
    private final List<ProductType> productTypes = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> filteredProducts = new ArrayList<>();

    // UI Components
    private EditText searchField;
    private Button btnPrevious, btnNext;
    private TextView tvPageInfo, watchlistCount;
    private ImageView watchlistButton;
    private String selectedType = "All";
    private int selectedPosition = 0;

    // Database helper
    private WishlistDbHelper dbHelper;

    // Pagination
    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;
    private int totalPages = 1;

    public ProductFragment() {
        super(R.layout.fragment_product);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            initializeViews(view);
            setupRecyclerViews();
            initializeFirestore();
            loadInitialData();
            setupSearchListener();
            setupPaginationListeners();

            // Initialize database helper
            dbHelper = new WishlistDbHelper(requireContext());


            watchlistButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), ViewWatchListActivity.class);
                startActivity(intent);
            });

            // Update watchlist count initially
            updateWatchlistCount();
        } catch (Exception e) {
            Log.e("ProductFragment", "Initialization error: ", e);
        }
    }

    private void initializeViews(View view) {
        typeRecyclerView = view.findViewById(R.id.recyclerViewProducts);
        productRecyclerView = view.findViewById(R.id.gridRecyclerView);
        searchField = view.findViewById(R.id.searchField);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        tvPageInfo = view.findViewById(R.id.tvPageInfo);
        watchlistCount = view.findViewById(R.id.watchlistCount);
        watchlistButton = view.findViewById(R.id.watchlistButton);
    }

    private void updateWatchlistCount() {
        SharedPreferences sp = requireActivity().getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);

        if (userJson != null) {
            try {
                Gson gson = new Gson();
                User user = gson.fromJson(userJson, User.class);
                String userMobile = user.getUser_mobile();

                int count = dbHelper.getWishlistCount(userMobile);
                watchlistCount.setText(String.valueOf(count));
            } catch (Exception e) {
                Log.e("WatchlistCount", "Error getting watchlist count", e);
                watchlistCount.setText("0");
            }
        } else {
            watchlistCount.setText("0");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update count when fragment resumes (in case watchlist was modified elsewhere)
        if (dbHelper != null) {
            updateWatchlistCount();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        typeRecyclerView.setAdapter(null);
        productRecyclerView.setAdapter(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Rest of your existing methods remain exactly the same...
    // (All the methods from setupRecyclerViews() to the end of the file)
    // I'm not showing them here to save space, but they should remain unchanged

    private void setupRecyclerViews() {
        // Product type horizontal list
        typeRecyclerView.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        typeAdapter = new ProductTypeAdapter();
        typeRecyclerView.setAdapter(typeAdapter);

        // Product grid
        productRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        productAdapter = new ProductGridAdapter();
        productRecyclerView.setAdapter(productAdapter);
    }

    private void setupPaginationListeners() {
        btnPrevious.setOnClickListener(v -> loadPreviousPage());
        btnNext.setOnClickListener(v -> loadNextPage());
    }

    private void initializeFirestore() {
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirestoreInit", "Error initializing Firestore: ", e);
        }
    }

    private void loadInitialData() {
        loadProductTypes();
        loadAllProducts();
    }

    private void setupSearchListener() {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    filterProducts(s.toString());
                } catch (Exception e) {
                    Log.e("SearchError", "Filter error: ", e);
                }
            }
        });
    }

    private void loadProductTypes() {
        try {
            firestore.collection("clay-bricks-type")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<ProductType> tempList = new ArrayList<>();
                            tempList.add(new ProductType("All"));

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                try {
                                    ProductType type = doc.toObject(ProductType.class);
                                    tempList.add(type);
                                } catch (Exception e) {
                                    Log.e("TypeMapping", "Error mapping type: ", e);
                                }
                            }

                            productTypes.clear();
                            productTypes.addAll(tempList);
                            typeAdapter.notifyDataSetChanged();
                        } else {
                            Log.e("TypeLoad", "Error loading types: ", task.getException());
                        }
                    });
        } catch (Exception e) {
            Log.e("TypeLoad", "Type load failed: ", e);
        }
    }

    private void loadAllProducts() {
        try {
            firestore.collection("clay-bricks-product")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            allProducts.clear();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                try {
                                    // Add status check here
                                    String productStatus = doc.getString("product_status");
                                    if (!"1".equals(productStatus)) {
                                        continue; // Skip deactivated products
                                    }

                                    Product product = doc.toObject(Product.class);
                                    product.setProduct_id(doc.getId());

                                    if (doc.contains("productName")) {
                                        product.setProduct_name(
                                                Objects.requireNonNull(doc.get("productName")).toString()
                                        );
                                    }
                                    if (doc.contains("price")) {
                                        product.setProduct_price(
                                                Objects.requireNonNull(doc.get("price")).toString()
                                        );
                                    }
                                    if (doc.contains("type")) {
                                        product.setProduct_type(
                                                Objects.requireNonNull(doc.get("type")).toString()
                                        );
                                    }
                                    if (doc.contains("imageUrl")) {
                                        product.setImageUrl(
                                                Objects.requireNonNull(doc.getString("imageUrl"))
                                        );
                                    }

                                    allProducts.add(product);
                                } catch (Exception e) {
                                    Log.e("ProductMapping", "Error mapping product: ", e);
                                }
                            }
                            filterProducts(searchField.getText().toString());
                        } else {
                            Log.e("ProductLoad", "Error loading products: ", task.getException());
                        }
                    });
        } catch (Exception e) {
            Log.e("ProductLoad", "Product load failed: ", e);
        }
    }

    private void filterProducts(String query) {
        try {
            filteredProducts.clear();
            String searchQuery = query.toLowerCase().trim();

            for (Product product : allProducts) {
                try {
                    boolean matchesType = selectedType.equals("All") ||
                            (product.getProduct_type() != null &&
                                    product.getProduct_type().equalsIgnoreCase(selectedType));

                    boolean matchesSearch = (product.getProduct_name() != null &&
                            product.getProduct_name().toLowerCase().contains(searchQuery)) ||
                            (product.getProduct_type() != null &&
                                    product.getProduct_type().toLowerCase().contains(searchQuery));

                    if (matchesType && matchesSearch) {
                        filteredProducts.add(product);
                    }
                } catch (Exception e) {
                    Log.e("FilterError", "Error filtering product: ", e);
                }
            }
            currentPage = 1;
            updatePagination();
            productAdapter.updateProducts(getPaginatedProducts());
        } catch (Exception e) {
            Log.e("Filter", "Filter failed: ", e);
        }
    }

    private List<Product> getPaginatedProducts() {
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredProducts.size());

        if (startIndex >= filteredProducts.size()) {
            return new ArrayList<>();
        }

        return filteredProducts.subList(startIndex, endIndex);
    }

    private void updatePagination() {
        totalPages = (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        tvPageInfo.setText(String.format("Page %d of %d", currentPage, totalPages));
        btnPrevious.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages && !filteredProducts.isEmpty());
    }

    private void loadNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            productAdapter.updateProducts(getPaginatedProducts());
            updatePagination();
        }
    }

    private void loadPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            productAdapter.updateProducts(getPaginatedProducts());
            updatePagination();
        }
    }

    private class ProductTypeAdapter extends RecyclerView.Adapter<ProductTypeAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.product_type__user_product_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                ProductType type = productTypes.get(position);
                holder.textView.setText(type.getName() != null ? type.getName() : "");

                if (position == selectedPosition) {
                    holder.textView.setBackgroundResource(R.drawable.selected_type_background);
                    holder.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_type_text));
                } else {
                    holder.textView.setBackgroundResource(R.drawable.default_type_background);
                    holder.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.default_type_text));
                }

                holder.itemView.setOnClickListener(v -> {
                    try {
                        int previousSelected = selectedPosition;
                        selectedPosition = position;
                        notifyItemChanged(previousSelected);
                        notifyItemChanged(selectedPosition);
                        selectedType = type.getName() != null ? type.getName() : "All";
                        filterProducts(searchField.getText().toString());
                    } catch (Exception e) {
                        Log.e("TypeClick", "Type selection error: ", e);
                    }
                });
            } catch (Exception e) {
                Log.e("TypeBind", "Error binding type: ", e);
            }
        }

        @Override
        public int getItemCount() {
            return productTypes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    textView = itemView.findViewById(R.id.textViewItem);
                } catch (Exception e) {
                    Log.e("ViewHolder", "Error finding textViewItem: ", e);
                }
            }
        }
    }

    private class ProductGridAdapter extends RecyclerView.Adapter<ProductGridAdapter.ProductViewHolder> {
        private List<Product> products = new ArrayList<>();

        public void updateProducts(List<Product> newProducts) {
            this.products = newProducts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_box, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            try {
                Product product = products.get(position);

                holder.itemView.setOnClickListener(v -> {
                    if (product.getProduct_id() != null && !product.getProduct_id().isEmpty()) {
                        Intent intent = new Intent(requireActivity(), SignlProductViewActivity.class);
                        intent.putExtra("PRODUCT_ID", product.getProduct_id());
                        startActivity(intent);
                    } else {
                        Log.e("ProductClick", "Product ID is null or empty");
                    }
                });

                holder.productName.setText(product.getProduct_name() != null ?
                        product.getProduct_name() : "Unknown Product");

                String price = product.getProduct_price() != null ?
                        "Rs." + product.getProduct_price() :
                        "Price not available";
                holder.productPrice.setText(price);

                holder.productType.setText(product.getProduct_type() != null ?
                        product.getProduct_type() :
                        "No Type");

                if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                    Picasso.get()
                            .load(product.getImageUrl())
                            .placeholder(R.drawable.update_product)
                            .error(R.drawable.update_product)
                            .into(holder.productImage);
                } else {
                    holder.productImage.setImageResource(R.drawable.update_product);
                }

            } catch (Exception e) {
                Log.e("ProductBind", "Error binding product: ", e);
            }
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName, productPrice, productType;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                try {
                    productImage = itemView.findViewById(R.id.productImage);
                    productName = itemView.findViewById(R.id.productName);
                    productPrice = itemView.findViewById(R.id.productPrice);
                    productType = itemView.findViewById(R.id.productTypeCart);
                } catch (Exception e) {
                    Log.e("ViewHolder", "Error initializing views: ", e);
                }
            }
        }
    }
}