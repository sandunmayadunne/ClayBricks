package com.sandun.claybricks.navigation;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sandun.claybricks.R;
import com.sandun.claybricks.model.User;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserManegerFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private EditText searchUser;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_maneger, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchUser = view.findViewById(R.id.searchUser);

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList, this);
        recyclerView.setAdapter(adapter);

        loadUsersFromFirestore();
        setupSearchFunctionality();

        return view;
    }

    private void setupSearchFunctionality() {
        searchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterUsers(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : userList) {
            String fullName = (user.getUser_name() + " " + user.getUser_last_name()).toLowerCase();
            if (fullName.contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        adapter.updateList(filteredList);
    }

    private void loadUsersFromFirestore() {
        db.collection("clay-bricks-user")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String user_id = document.getId();
                            String user_name = document.getString("user_name");
                            String user_last_name = document.getString("user_last_name");
                            String user_email = document.getString("user_email");
                            String user_mobile = document.getString("user_mobile");
                            Long statusLong = document.getLong("status");
                            String status = (statusLong != null) ? String.valueOf(statusLong) : "1";
                            String province = document.getString("province");
                            String district = document.getString("district");
                            String city = document.getString("city");
                            String line1 = document.getString("line1");
                            String line2 = document.getString("line2");

                            User user = new User(user_id, user_name, user_last_name, user_email,
                                    user_mobile, status, province, district, city, line1, line2);
                            userList.add(user);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    public void showUserDetailsDialog(User user) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.view_user_dialog_details);

        // Initialize all text views
        TextView touchUser = dialog.findViewById(R.id.name_touch_user);
        TextView fname = dialog.findViewById(R.id.fname);
        TextView lname = dialog.findViewById(R.id.lname);
        TextView email = dialog.findViewById(R.id.email_user);
        TextView mobile = dialog.findViewById(R.id.mobile_user);
        TextView province = dialog.findViewById(R.id.province_user);
        TextView district = dialog.findViewById(R.id.dristict_user);
        TextView city = dialog.findViewById(R.id.city_user);
        TextView line1 = dialog.findViewById(R.id.line1_user);
        TextView line2 = dialog.findViewById(R.id.line2_user);

        // Set user data
        touchUser.setText(user.getUser_name() != null ? user.getUser_name() : " ");
        fname.setText(user.getUser_name() != null ? user.getUser_name() : " ");
        lname.setText(user.getUser_last_name() != null ? user.getUser_last_name() : " ");
        email.setText(user.getUser_email() != null ? user.getUser_email() : " ");
        mobile.setText(user.getUser_mobile() != null ? user.getUser_mobile() : " ");
        province.setText(user.getProvince() != null ? user.getProvince() : " ");
        district.setText(user.getDistrict() != null ? user.getDistrict() : " ");
        city.setText(user.getCity() != null ? user.getCity() : " ");
        line1.setText(user.getLine1() != null ? user.getLine1() : " ");
        line2.setText(user.getLine2() != null ? user.getLine2() : " ");

        // Set dialog background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.rounded_corners);

            // Convert DP to pixels
            int widthInDp = 400; // Set your desired width here
            int widthInPixels = (int) (widthInDp * getResources().getDisplayMetrics().density);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = widthInPixels; // Set calculated width
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            // Optional: Center the dialog
            layoutParams.gravity = Gravity.CENTER;

            window.setAttributes(layoutParams);
        }

        dialog.show();
    }

    private void showDeleteConfirmationDialog(User user, int position) {
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
            deleteUser(user, position);
        });

        dialog.show();
    }

    private void deleteUser(User user, int position) {
        db.collection("clay-bricks-user").document(user.getUser_id())
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.remove(position);
                        adapter.notifyItemRemoved(position);
                        sendDeleteEmail(user.getUser_email());
                        Toast.makeText(getContext(),
                                "User deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
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
                json.addProperty("userEmail", email);
                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url("http://192.168.1.3:8080/ClayBricksBackend/SendDeleteNotificationUser")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JsonObject responseJson = new Gson().fromJson(responseBody, JsonObject.class);

                requireActivity().runOnUiThread(() -> {
                    if (!responseJson.get("success").getAsBoolean()) {
                        Toast.makeText(getContext(),
                                "Email sending failed: " + responseJson.get("message"),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // RecyclerView Adapter
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private List<User> localUserList;

        private Fragment fragment;

        public UserAdapter(List<User> userList, Fragment fragment) {
            this.localUserList = userList;
            this.fragment = fragment;
        }

        public void updateList(List<User> newList) {
            localUserList = new ArrayList<>(newList);
            notifyDataSetChanged();
        }


        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_box, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User currentUser = localUserList.get(position);

            String firstName = currentUser.getUser_name() != null ? currentUser.getUser_name().trim() : "";
            String lastName = currentUser.getUser_last_name() != null ? currentUser.getUser_last_name().trim() : "";

            // Set user details
            String fullName = firstName + " " + lastName;
            holder.userName.setText(fullName);
            holder.userEmail.setText(currentUser.getUser_email());

            // Set first letter avatar
            String userName = currentUser.getUser_name();
            String firstLetter = "U";
            if (userName != null && !userName.isEmpty()) {
                firstLetter = String.valueOf(userName.charAt(0)).toUpperCase();
            }
            holder.userInitial.setText(firstLetter);

            // Set user status
            String status = currentUser.getStatus();
            boolean isActive = "1".equals(status);

            holder.userStatusSwitch.setChecked(isActive);
            holder.statusText.setText(isActive ? "Active" : "Deactive");
            holder.statusText.setTextColor(isActive ?
                    getResources().getColor(R.color.active_green) :
                    getResources().getColor(R.color.inactive_red));

            holder.itemView.setOnClickListener(v -> {
                ((UserManegerFragment) fragment).showUserDetailsDialog(currentUser);
            });

            // Set listeners
            holder.userStatusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int newStatus = isChecked ? 1 : 2;
                updateUserStatus(currentUser.getUser_id(), newStatus, holder, position);
            });

            holder.deleteButton.setOnClickListener(v ->
                    showDeleteConfirmationDialog(currentUser, position));
        }

        private void updateUserStatus(String documentId, int newStatus, UserViewHolder holder, int position) {
            db.collection("clay-bricks-user").document(documentId)
                    .update("status", newStatus)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            localUserList.get(position).setStatus(String.valueOf(newStatus));
                            boolean isActive = (newStatus == 1);
                            holder.statusText.setText(isActive ? "Active" : "Deactive");
                            holder.statusText.setTextColor(isActive ?
                                    getResources().getColor(R.color.active_green) :
                                    getResources().getColor(R.color.inactive_red));
                        } else {
                            String currentStatus = localUserList.get(position).getStatus();
                            boolean wasActive = "1".equals(currentStatus);
                            holder.userStatusSwitch.setChecked(wasActive);
                            holder.statusText.setText(wasActive ? "Active" : "Deactive");
                            holder.statusText.setTextColor(wasActive ?
                                    getResources().getColor(R.color.active_green) :
                                    getResources().getColor(R.color.inactive_red));
                            Toast.makeText(getContext(),
                                    "Update failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        @Override
        public int getItemCount() { return localUserList.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView userName, userEmail, userInitial, statusText;
            Button deleteButton;
            Switch userStatusSwitch;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.Name);
                userEmail = itemView.findViewById(R.id.Email);
                userInitial = itemView.findViewById(R.id.admin_name_first_word);
                deleteButton = itemView.findViewById(R.id.deleteProduct);
                userStatusSwitch = itemView.findViewById(R.id.productStatus);
                statusText = itemView.findViewById(R.id.status_text);

                itemView.setClickable(true);
                itemView.setFocusable(true);
            }
        }
    }
}