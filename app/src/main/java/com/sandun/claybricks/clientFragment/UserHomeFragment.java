//package com.sandun.claybricks.clientFragment;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.gson.Gson;
//import com.sandun.claybricks.R;
//import com.sandun.claybricks.SettingActivity;
//import com.sandun.claybricks.model.ProductType;
//import com.sandun.claybricks.model.User;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class UserHomeFragment extends Fragment {
//
//    private RecyclerView typeRecyclerView;
//    private ProductTypeAdapter adapter;
//    private final List<ProductType> clayBricksType = new ArrayList<>();
//    private FirebaseFirestore firestore;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_user_home, container, false);
//
//        // Initialize Firebase
//        firestore = FirebaseFirestore.getInstance();
//
//        // Initialize RecyclerView
//        typeRecyclerView = rootView.findViewById(R.id.typeViewRecyclerView);
//        adapter = new ProductTypeAdapter(clayBricksType);
//
//        // Set horizontal layout manager
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
//                LinearLayoutManager.HORIZONTAL, false);
//        typeRecyclerView.setLayoutManager(layoutManager);
//        typeRecyclerView.setAdapter(adapter);
//
//        // Load product types from Firestore
//        loadProductTypes();
//
//        // Load user name
//        TextView viewLogUserName = rootView.findViewById(R.id.logUseName);
//        SharedPreferences sp = requireContext().getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
//        String userJson = sp.getString("user", null);
//
//        if (userJson != null) {
//            Gson gson = new Gson();
//            try {
//                User user = gson.fromJson(userJson, User.class);
//                String userName = (user != null && user.getUser_name() != null) ? user.getUser_name() : "User!";
//                String formattedName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
//                viewLogUserName.setText(formattedName);
//            } catch (Exception e) {
//                viewLogUserName.setText("Error loading user!");
//                e.printStackTrace();
//            }
//        } else {
//            viewLogUserName.setText("Guest!");
//        }
//
//        // Handle setting icon click
//        ImageView setting = rootView.findViewById(R.id.settingIcon);
//        setting.setOnClickListener(view -> {
//            Intent i = new Intent(requireContext(), SettingActivity.class);
//            startActivity(i);
//        });
//
//        return rootView;
//    }
//
//    private void loadProductTypes() {
//        firestore.collection("clay-bricks-type")
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        clayBricksType.clear();
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            ProductType productType = document.toObject(ProductType.class);
//                            clayBricksType.add(productType);
//                        }
//                        adapter.notifyDataSetChanged();
//                    } else {
//                        Log.e("Firestore", "Error getting documents: ", task.getException());
//                    }
//                });
//    }
//
//    // Adapter class inside UserHomeFragment
//    private class ProductTypeAdapter extends RecyclerView.Adapter<ProductTypeAdapter.ViewHolder> {
//        private final List<ProductType> productTypes;
//
//        public ProductTypeAdapter(List<ProductType> productTypes) {
//            this.productTypes = productTypes;
//        }
//
//        @NonNull
//        @Override
//        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            // Inflate your RecyclerView item design
//            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_home_product_type_view_box, parent, false);
//            return new ViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//            ProductType productType = productTypes.get(position);
//            holder.productTypeName.setText(productType.getName());
//
//        }
//
//        @Override
//        public int getItemCount() {
//            return productTypes.size();
//        }
//
//        public class ViewHolder extends RecyclerView.ViewHolder {
//            final TextView productTypeName;
//            final ImageView imageView;
//
//            public ViewHolder(View view) {
//                super(view);
//                // Bind views from your RecyclerView item design
//                productTypeName = view.findViewById(R.id.productTypeName);
//                imageView = view.findViewById(R.id.imageView20);
//            }
//        }
//    }
//}
//
//package com.sandun.claybricks.clientFragment;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.gson.Gson;
//import com.sandun.claybricks.R;
//import com.sandun.claybricks.SettingActivity;
//import com.sandun.claybricks.model.ProductType;
//import com.sandun.claybricks.model.User;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class UserHomeFragment extends Fragment {
//
//    private RecyclerView gridRecyclerView;
//    private GridProductAdapter gridAdapter;
//    private final List<ProductType> gridList = new ArrayList<>();
//    private FirebaseFirestore firestore;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_user_home, container, false);
//
//        // Initialize Firebase
//        firestore = FirebaseFirestore.getInstance();
//
//        // Initialize RecyclerView
//        gridRecyclerView = rootView.findViewById(R.id.gridRecyclerView);
//        gridRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
//
//        // Initialize Adapter
//        gridAdapter = new GridProductAdapter(gridList);
//        gridRecyclerView.setAdapter(gridAdapter);
//
//        // Load product types from Firestore
//        loadProductTypes();
//
//        // Load user name
//        TextView viewLogUserName = rootView.findViewById(R.id.logUseName);
//        SharedPreferences sp = requireContext().getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
//        String userJson = sp.getString("user", null);
//
//        if (userJson != null) {
//            Gson gson = new Gson();
//            try {
//                User user = gson.fromJson(userJson, User.class);
//                String userName = (user != null && user.getUser_name() != null) ? user.getUser_name() : "User!";
//                String formattedName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
//                viewLogUserName.setText(formattedName);
//            } catch (Exception e) {
//                viewLogUserName.setText("Error loading user!");
//                e.printStackTrace();
//            }
//        } else {
//            viewLogUserName.setText("Guest!");
//        }
//
//        // Handle setting icon click
//        ImageView setting = rootView.findViewById(R.id.settingIcon);
//        setting.setOnClickListener(view -> {
//            Intent i = new Intent(requireContext(), SettingActivity.class);
//            startActivity(i);
//        });
//
//        return rootView;
//    }
//
//    private void loadProductTypes() {
//        firestore.collection("clay-bricks-type")
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        gridList.clear();
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            ProductType productType = document.toObject(ProductType.class);
//                            gridList.add(productType);
//                        }
//                        gridAdapter.notifyDataSetChanged();
//                    } else {
//                        Log.e("Firestore", "Error getting documents: ", task.getException());
//                    }
//                });
//    }
//
//    // Adapter for Grid RecyclerView
//    private class GridProductAdapter extends RecyclerView.Adapter<GridProductAdapter.ViewHolder> {
//        private final List<ProductType> productTypes;
//
//        public GridProductAdapter(List<ProductType> productTypes) {
//            this.productTypes = productTypes;
//        }
//
//        @NonNull
//        @Override
//        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_home_product_type_view_box, parent, false);
//            return new ViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//            ProductType productType = productTypes.get(position);
//            holder.productTypeName.setText(productType.getName());
//        }
//
//        @Override
//        public int getItemCount() {
//            return productTypes.size();
//        }
//
//        public class ViewHolder extends RecyclerView.ViewHolder {
//            final TextView productTypeName;
//            final ImageView imageView;
//
//            public ViewHolder(View view) {
//                super(view);
//                productTypeName = view.findViewById(R.id.productTypeName);
//                imageView = view.findViewById(R.id.imageView20);
//            }
//        }
//    }
//}
//


package com.sandun.claybricks.clientFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.sandun.claybricks.CartActivity;
import com.sandun.claybricks.R;
import com.sandun.claybricks.SettingActivity;
import com.sandun.claybricks.UserOrderActivity;
import com.sandun.claybricks.model.ProductType;
import com.sandun.claybricks.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserHomeFragment extends Fragment {

    private RecyclerView gridRecyclerView;
    private GridProductAdapter gridAdapter;
    private final List<ProductType> gridList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private TextView cartItemCount;
    private ListenerRegistration cartListener;
    private BroadcastReceiver cartUpdateReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_user_home, container, false);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        gridRecyclerView = rootView.findViewById(R.id.gridRecyclerView);
        gridRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize Adapter
        gridAdapter = new GridProductAdapter(gridList);
        gridRecyclerView.setAdapter(gridAdapter);

        // Initialize cart item count
        cartItemCount = rootView.findViewById(R.id.cartItemCount);

        // Load product types from Firestore
        loadProductTypes();

        // Load user name and setup cart counter
        TextView viewLogUserName = rootView.findViewById(R.id.logUseName);
        SharedPreferences sp = requireContext().getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);

        if (userJson != null) {
            Gson gson = new Gson();
            try {
                User user = gson.fromJson(userJson, User.class);
                String userName = (user != null && user.getUser_name() != null) ? user.getUser_name() : "User!";
                String formattedName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
                viewLogUserName.setText(formattedName);

                // Setup cart counter if user exists
                if (user.getUser_mobile() != null) {
                    setupCartCounter(user.getUser_mobile());
                }
            } catch (Exception e) {
                viewLogUserName.setText("Error loading user!");
                e.printStackTrace();
            }
        } else {
            viewLogUserName.setText("Guest!");
        }

        ImageView orderPage = rootView.findViewById(R.id.ordersPage);
        orderPage.setOnClickListener(view -> {
            Intent ordersIntent = new Intent(requireContext(), UserOrderActivity.class);
            startActivity(ordersIntent);
        });

        ImageView cartIcon = rootView.findViewById(R.id.cartItem);
        cartIcon.setOnClickListener(view -> {
            Intent cartIntent = new Intent(requireContext(), CartActivity.class);
            startActivity(cartIntent);
        });

        // Handle setting icon click
        ImageView setting = rootView.findViewById(R.id.settingIcon);
        setting.setOnClickListener(view -> {
            Intent i = new Intent(requireContext(), SettingActivity.class);
            startActivity(i);
        });

        // Register broadcast receiver
        cartUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshCartCount();
            }
        };

        return rootView;
    }

    private void setupCartCounter(String userMobile) {
        cartListener = firestore.collection("clay-bricks-cart")
                .whereEqualTo("userMobile", userMobile)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Listen failed: ", error);
                        return;
                    }

                    if (value != null) {
                        int count = value.size();
                        cartItemCount.setText(String.valueOf(count));
                    } else {
                        cartItemCount.setText("0");
                    }
                });
    }

    private void refreshCartCount() {
        SharedPreferences sp = requireContext().getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        String userJson = sp.getString("user", null);

        if (userJson != null) {
            Gson gson = new Gson();
            try {
                User user = gson.fromJson(userJson, User.class);
                if (user.getUser_mobile() != null) {
                    setupCartCounter(user.getUser_mobile());
                }
            } catch (Exception e) {
                Log.e("UserHomeFragment", "Error refreshing cart count", e);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(cartUpdateReceiver, new IntentFilter("CART_UPDATE_ACTION"));
        refreshCartCount();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(cartUpdateReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cartListener != null) {
            cartListener.remove();
        }
    }

    // Existing loadProductTypes() method remains unchanged
    private void loadProductTypes() {
        firestore.collection("clay-bricks-type")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        gridList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductType productType = document.toObject(ProductType.class);
                            gridList.add(productType);
                        }
                        gridAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                    }
                });
    }

    // Existing GridProductAdapter class remains unchanged
    private class GridProductAdapter extends RecyclerView.Adapter<GridProductAdapter.ViewHolder> {
        private final List<ProductType> productTypes;

        public GridProductAdapter(List<ProductType> productTypes) {
            this.productTypes = productTypes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_home_product_type_view_box, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductType productType = productTypes.get(position);
            holder.productTypeName.setText(productType.getName());
        }

        @Override
        public int getItemCount() {
            return productTypes.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final TextView productTypeName;
            final ImageView imageView;

            public ViewHolder(View view) {
                super(view);
                productTypeName = view.findViewById(R.id.productTypeName);
                imageView = view.findViewById(R.id.imageView20);
            }
        }
    }
}