//package com.sandun.claybricks.navigation;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.firebase.firestore.QuerySnapshot;
//import com.google.gson.JsonObject;
//import com.sandun.claybricks.R;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//public class AllAdminViewActivity extends AppCompatActivity {
//
//    private RecyclerView recyclerView;
//    private AdminAdapter adapter;
//    private List<Admin> adminList;
//    private FirebaseFirestore db;
//    private String currentAdminEmail;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_all_admin_view);
//
//        // Get current admin email from SharedPreferences
//        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
//        currentAdminEmail = prefs.getString("admin_email", "");
//
//        // Initialize Firestore
//        db = FirebaseFirestore.getInstance();
//
//        // Setup RecyclerView
//        recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//
//        adminList = new ArrayList<>();
//        adapter = new AdminAdapter(adminList);
//        recyclerView.setAdapter(adapter);
//
//        loadAdminsFromFirestore();
//    }
//
//    private void loadAdminsFromFirestore() {
//        db.collection("clay-bricks-admin")
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        adminList.clear();
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            String adminEmail = document.getString("admin_email");
//
//                            if (adminEmail.equals(currentAdminEmail)) continue;
//
//                            Admin admin = new Admin(
//                                    document.getId(), // Store document ID
//                                    document.getString("admin_name"),
//                                    adminEmail
//                            );
//                            adminList.add(admin);
//                        }
//                        adapter.notifyDataSetChanged();
//                    }
//                });
//    }
//
//    // Admin Model
//    private static class Admin {
//        private String documentId;
//        private String name;
//        private String email;
//
//        public Admin(String documentId,String name, String email) {
//            this.documentId = documentId;
//            this.name = name;
//            this.email = email;
//        }
//        public String getDocumentId() { return documentId; }
//
//        public String getName() { return name; }
//        public String getEmail() { return email; }
//    }
//
//    // RecyclerView Adapter
//    private class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {
//
//        private List<Admin> localAdminList;
//
//        public AdminAdapter(List<Admin> adminList) {
//            this.localAdminList = adminList;
//        }
//
//        @NonNull
//        @Override
//        public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.view_box, parent, false);
//            return new AdminViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
//            Admin currentAdmin = localAdminList.get(position);
//
//            // Set admin details
//            holder.adminName.setText(currentAdmin.getName());
//            holder.adminEmail.setText(currentAdmin.getEmail());
//
//            // Set first letter avatar
//            String name = currentAdmin.getName();
//            String firstLetter = "A"; // Default value
//            if(name != null && !name.isEmpty()) {
//                firstLetter = String.valueOf(name.charAt(0)).toUpperCase();
//            }
//            holder.adminNameFirstWord.setText(firstLetter);
//
//            holder.deleteButton.setOnClickListener(v -> {
//                deleteAdmin(currentAdmin.getDocumentId(), currentAdmin.getEmail());
//                localAdminList.remove(position);
//                notifyItemRemoved(position);
//            });
//        }
//
//        private void deleteAdmin(String documentId, String email) {
//            // Delete from Firestore
//            db.collection("clay-bricks-admin").document(documentId)
//                    .delete()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            sendDeleteEmail(email);
//                            Toast.makeText(AllAdminViewActivity.this,
//                                    "Admin deleted successfully", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(AllAdminViewActivity.this,
//                                    "Delete failed: " + task.getException(), Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        }
//
//        private void sendDeleteEmail(String email) {
//            new Thread(() -> {
//                try {
//                    OkHttpClient client = new OkHttpClient();
//                    MediaType JSON = MediaType.get("application/json; charset=utf-8");
//
//                    JsonObject json = new JsonObject();
//                    json.addProperty("adminEmail", email);
//                    RequestBody body = RequestBody.create(json.toString(), JSON);
//
//                    Request request = new Request.Builder()
//                            .url("http://192.168.8.174:8080/ClayBricksBackend/SendDeleteNotification")
//                            .post(body)
//                            .build();
//
//                    Response response = client.newCall(request).execute();
//                    // Handle response if needed
//                } catch (IOException e) {
//                    runOnUiThread(() ->
//                            Toast.makeText(AllAdminViewActivity.this,
//                                    "Email sending failed", Toast.LENGTH_SHORT).show());
//                }
//            }).start();
//        }
//
//        @Override
//        public int getItemCount() {
//            return localAdminList.size();
//        }
//
//        class AdminViewHolder extends RecyclerView.ViewHolder {
//            TextView adminName, adminEmail, adminNameFirstWord;
//            Button deleteButton;
//
//            public AdminViewHolder(@NonNull View itemView) {
//                super(itemView);
//                adminName = itemView.findViewById(R.id.admin_Name);
//                adminEmail = itemView.findViewById(R.id.admin_Email);
//                adminNameFirstWord = itemView.findViewById(R.id.admin_name_first_word);
//                deleteButton = itemView.findViewById(R.id.delete_Admin);
//            }
//        }
//    }
//}

package com.sandun.claybricks.navigation;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sandun.claybricks.R;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AllAdminViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<Admin> adminList;
    private FirebaseFirestore db;
    private String currentAdminEmail;
    private EditText searchAdmin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_admin_view);

        // Get current admin email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        currentAdminEmail = prefs.getString("admin_email", "");

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchAdmin = findViewById(R.id.searchAdmin);

        adminList = new ArrayList<>();
        adapter = new AdminAdapter(adminList);
        recyclerView.setAdapter(adapter);

        loadAdminsFromFirestore();
        setupSearchFunctionality();
    }

    private void setupSearchFunctionality() {
        searchAdmin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAdmin(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterAdmin(String query) {
        List<Admin> filteredList = new ArrayList<>();
        for (Admin user : adminList) {
            String fullName = (user.getName()).toLowerCase();
            if (fullName.contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        adapter.updateList(filteredList);
    }


    private void loadAdminsFromFirestore() {
        db.collection("clay-bricks-admin")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        adminList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String adminEmail = document.getString("admin_email");

                            // Skip current admin
                            if (adminEmail.equals(currentAdminEmail)) continue;

                            Long statusLong = document.getLong("admin_status");
                            int status = (statusLong != null) ? statusLong.intValue() : 1;

                            Admin admin = new Admin(
                                    document.getId(),
                                    document.getString("admin_name"),
                                    adminEmail,
                                    document.getString("admin_mobile"),
                                    status
                            );
                            adminList.add(admin);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAdminDetailsDialog(Admin admin) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.view_admin_dialog_details);

        // Initialize views
        TextView touchName = dialog.findViewById(R.id.name_touch_admin);
        TextView name = dialog.findViewById(R.id.name);
        TextView email = dialog.findViewById(R.id.email_admin);
        TextView mobile = dialog.findViewById(R.id.mobile_admin);

        // Set admin data
        touchName.setText(admin.getName());
        name.setText(admin.getName());
        email.setText(admin.getEmail());
        mobile.setText(admin.getMobilel());

        // Configure dialog window
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        dialog.show();
    }

    private void showDeleteConfirmationDialog(Admin admin, int position) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.admin_delete_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView okButton = dialog.findViewById(R.id.okButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            deleteAdmin(admin, position);
        });

        dialog.show();
    }

    private void deleteAdmin(Admin admin, int position) {
        db.collection("clay-bricks-admin").document(admin.getDocumentId())
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        adminList.remove(position);
                        adapter.notifyItemRemoved(position);
                        sendDeleteEmail(admin.getEmail());
                        Toast.makeText(this,
                                "Admin deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Delete failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendDeleteEmail(String email) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.get("application/json; charset=utf-8");

                JsonObject json = new JsonObject();
                json.addProperty("adminEmail", email);
                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url("http://192.168.1.7:8080/ClayBricksBackend/SendDeleteNotification")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JsonObject responseJson = new Gson().fromJson(responseBody, JsonObject.class);

                runOnUiThread(() -> {
                    if (!responseJson.get("success").getAsBoolean()) {
                        Toast.makeText(this,
                                "Email sending failed: " + responseJson.get("message"),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Admin Model
    private static class Admin {
        private final String documentId;
        private final String name;
        private final String email;
        private final String mobile;
        private int status;

        public Admin(String documentId, String name, String email,String mobile, int status) {
            this.documentId = documentId;
            this.name = name;
            this.email = email;
            this.mobile = mobile;
            this.status = status;
        }

        public String getDocumentId() { return documentId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getMobilel() { return mobile; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
    }

    // RecyclerView Adapter
    private class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

        private List<Admin> localAdminList;

        public AdminAdapter(List<Admin> adminList) {
            this.localAdminList = adminList;
        }
        public void updateList(List<Admin> newList) {
            localAdminList = new ArrayList<>(newList);
            notifyDataSetChanged();
        }


        @NonNull
        @Override
        public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_box, parent, false);
            return new AdminViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
            Admin currentAdmin = localAdminList.get(position);

            // Set admin details
            holder.adminName.setText(currentAdmin.getName());
            holder.adminEmail.setText(currentAdmin.getEmail());

            // Set first letter avatar
            String name = currentAdmin.getName();
            String firstLetter = "A";
            if (name != null && !name.isEmpty()) {
                firstLetter = String.valueOf(name.charAt(0)).toUpperCase();
            }
            holder.adminNameFirstWord.setText(firstLetter);

            // Set admin status
            int status = currentAdmin.getStatus();
            boolean isActive = (status == 1);

            holder.adminStatusSwitch.setOnCheckedChangeListener(null);
            holder.adminStatusSwitch.setChecked(isActive);
            holder.adminStatusText.setText(isActive ? "Active" : "Deactive");
            holder.adminStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                    isActive ? R.color.active_green : R.color.inactive_red));

            // Set listeners
            holder.adminStatusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int newStatus = isChecked ? 1 : 2;
                updateAdminStatus(currentAdmin.getDocumentId(), newStatus, holder, position);
            });

            holder.itemView.setOnClickListener(v ->
                    showAdminDetailsDialog(currentAdmin));

            holder.deleteButton.setOnClickListener(v ->
                    showDeleteConfirmationDialog(currentAdmin, position));
        }

        private void updateAdminStatus(String documentId, int newStatus, AdminViewHolder holder, int position) {
            db.collection("clay-bricks-admin").document(documentId)
                    .update("admin_status", newStatus)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            localAdminList.get(position).setStatus(newStatus);
                            boolean isActive = (newStatus == 1);
                            holder.adminStatusText.setText(isActive ? "Active" : "Deactive");
                            holder.adminStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                                    isActive ? R.color.active_green : R.color.inactive_red));
                        } else {
                            boolean wasActive = (localAdminList.get(position).getStatus() == 1);
                            holder.adminStatusSwitch.setChecked(wasActive);
                            holder.adminStatusText.setText(wasActive ? "Active" : "Deactive");
                            holder.adminStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                                    wasActive ? R.color.active_green : R.color.inactive_red));
                            Toast.makeText(AllAdminViewActivity.this,
                                    "Update failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        @Override
        public int getItemCount() { return localAdminList.size(); }

        class AdminViewHolder extends RecyclerView.ViewHolder {
            final TextView adminName, adminEmail, adminNameFirstWord, adminStatusText;
            final Button deleteButton;
            final Switch adminStatusSwitch;

            public AdminViewHolder(@NonNull View itemView) {
                super(itemView);
                adminName = itemView.findViewById(R.id.Name);
                adminEmail = itemView.findViewById(R.id.Email);
                adminNameFirstWord = itemView.findViewById(R.id.admin_name_first_word);
                deleteButton = itemView.findViewById(R.id.deleteProduct);
                adminStatusSwitch = itemView.findViewById(R.id.productStatus);
                adminStatusText = itemView.findViewById(R.id.status_text);
            }
        }
    }
}