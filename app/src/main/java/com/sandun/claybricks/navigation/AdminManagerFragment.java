package com.sandun.claybricks.navigation;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sandun.claybricks.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminManagerFragment extends Fragment {

    private TextView adminNameText, adminEmailText, adminMobileText;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private TextInputEditText inputTextNewAdminName, inputTextNewAdminEmail, inputTextNewAdminMobile;
    private Button addAdminButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_manager, container, false);

        // Initialize UI elements
        adminNameText = view.findViewById(R.id.adminNameText);
        adminEmailText = view.findViewById(R.id.adminEmailText);
        adminMobileText = view.findViewById(R.id.adminMobileText);


        // Initialize Firestore and SharedPreferences
        firestore = FirebaseFirestore.getInstance();

        // Initialize new admin input fields and button
        inputTextNewAdminName = view.findViewById(R.id.inputTextNewAdminName);
        inputTextNewAdminEmail = view.findViewById(R.id.inputTextNewAdminEmail);
        inputTextNewAdminMobile = view.findViewById(R.id.inputTextNewAdminMobile);
        addAdminButton = view.findViewById(R.id.button);

        // Set up button click listener to add new admin
        addAdminButton.setOnClickListener(v -> addNewAdmin());

        sharedPreferences = requireActivity().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE);

        // Load admin details from SharedPreferences
        loadAdminDetails();

        // Set up edit buttons
        setupEditButton(view, R.id.editadminName, "Name", "admin_name", InputType.TYPE_CLASS_TEXT, true);
        setupEditButton(view, R.id.editAdminMobile, "Mobile", "admin_mobile", InputType.TYPE_CLASS_PHONE, true);

        Button viewAllButton = view.findViewById(R.id.viewAllAdminButton);
        viewAllButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AllAdminViewActivity.class));
        });

        return view;
    }

    private void addNewAdmin() {
        // Get input values
        String name = inputTextNewAdminName.getText().toString().trim();
        String email = inputTextNewAdminEmail.getText().toString().trim();
        String mobile = inputTextNewAdminMobile.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mobile.matches("\\d{10}")) {
            Toast.makeText(getContext(), "Invalid mobile number (10 digits)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if email exists
        firestore.collection("clay-bricks-admin")
                .whereEqualTo("admin_email", email)
                .whereEqualTo("admin_status", 1)
                .get()
                .addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        if (!emailTask.getResult().isEmpty()) {
                            Toast.makeText(getContext(), "Email already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            // Check if mobile exists
                            firestore.collection("clay-bricks-admin")
                                    .whereEqualTo("admin_mobile", mobile)
                                    .whereEqualTo("admin_status", 1)
                                    .get()
                                    .addOnCompleteListener(mobileTask -> {
                                        if (mobileTask.isSuccessful()) {
                                            if (!mobileTask.getResult().isEmpty()) {
                                                Toast.makeText(getContext(), "Mobile number already exists", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // Create and add new admin
                                                Map<String, Object> admin = new HashMap<>();
                                                admin.put("admin_name", name);
                                                admin.put("admin_email", email);
                                                admin.put("admin_mobile", mobile);
                                                admin.put("admin_status", 1);

                                                firestore.collection("clay-bricks-admin")
                                                        .add(admin)
                                                        .addOnSuccessListener(documentReference -> {
                                                            Toast.makeText(getContext(), "New admin added successfully", Toast.LENGTH_SHORT).show();
                                                            clearInputFields();
                                                            sendWelcomeEmail(email, name);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "Failed to add admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                            }
                                        } else {
                                            Toast.makeText(getContext(), "Error checking mobile number", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(getContext(), "Error checking email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendWelcomeEmail(String email, String name) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.get("application/json; charset=utf-8");

                JsonObject json = new JsonObject();
                json.addProperty("adminEmail", email);
                json.addProperty("adminName", name);
                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url("http://192.168.1.3:8080/ClayBricksBackend/SendWelcomeEmail")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                JsonObject responseJson = new Gson().fromJson(responseData, JsonObject.class);

                requireActivity().runOnUiThread(() -> {
                    if (responseJson.get("success").getAsBoolean()) {
                        Toast.makeText(getContext(), "Welcome email sent to " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to send welcome email", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearInputFields() {
        inputTextNewAdminName.setText("");
        inputTextNewAdminEmail.setText("");
        inputTextNewAdminMobile.setText("");
    }


    private void loadAdminDetails() {
        // Retrieve admin details from SharedPreferences
        String adminName = sharedPreferences.getString("admin_name", "No Name");
        String adminEmail = sharedPreferences.getString("admin_email", "No Email");
        String adminMobile = sharedPreferences.getString("admin_mobile", "No Mobile");

        // Set values to TextViews
        adminNameText.setText(adminName);
        adminEmailText.setText(adminEmail);
        adminMobileText.setText(adminMobile);
    }

    private void setupEditButton(View view, int buttonId, String title, String fieldName, int inputType, boolean needsValidation) {
        ImageView editButton = view.findViewById(buttonId);
        editButton.setOnClickListener(v -> showEditDialog(title, fieldName, inputType, needsValidation));
    }

    private void showEditDialog(String title, String fieldName, int inputType, boolean needsValidation) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.custom_dialog_update_user);

        // Set dialog title
        TextView dialogTitle = dialog.findViewById(R.id.userSelectUpdateName);
        dialogTitle.setText("Edit " + title);

        // Configure input field
        EditText updateText = dialog.findViewById(R.id.updateText);
        updateText.setInputType(inputType);
        updateText.setText(sharedPreferences.getString(fieldName, ""));
        updateText.setSelection(updateText.getText().length());

        // Button handlers
        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView updateButton = dialog.findViewById(R.id.updateButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            String newValue = updateText.getText().toString().trim();
            if (validateInput(fieldName, newValue, needsValidation)) {
                updateFirestoreAndSharedPreferences(fieldName, newValue, dialog);
            }
        });

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

        // Show dialog
        dialog.show();
    }

    private boolean validateInput(String fieldName, String newValue, boolean needsValidation) {
        if (newValue.isEmpty()) {
            Toast.makeText(getContext(), "Field cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (needsValidation) {
            if (fieldName.equals("admin_email") && !android.util.Patterns.EMAIL_ADDRESS.matcher(newValue).matches()) {
                Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
                return false;
            } else if (fieldName.equals("admin_mobile") && !newValue.matches("\\d{10}")) {
                Toast.makeText(getContext(), "Invalid mobile number (10 digits)", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void updateFirestoreAndSharedPreferences(String fieldName, String newValue, Dialog dialog) {
        // Get current admin email from SharedPreferences
        String currentEmail = sharedPreferences.getString("admin_email", "");

        // Find Firestore document using the email
        firestore.collection("clay-bricks-admin")
                .whereEqualTo("admin_email", currentEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String documentId = document.getId();

                        // Update Firestore
                        firestore.collection("clay-bricks-admin").document(documentId)
                                .update(fieldName, newValue)
                                .addOnSuccessListener(aVoid -> {
                                    // Fetch updated admin data
                                    firestore.collection("clay-bricks-admin").document(documentId)
                                            .get()
                                            .addOnSuccessListener(updatedDocument -> {
                                                String updatedEmail = updatedDocument.getString("admin_email");
                                                String updatedMobile = updatedDocument.getString("admin_mobile");
                                                String updatedName = updatedDocument.getString("admin_name");

                                                // Save updated data to SharedPreferences
                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                editor.putString("admin_name", updatedName);
                                                editor.putString("admin_email", updatedEmail);
                                                editor.putString("admin_mobile", updatedMobile);
                                                editor.apply();

                                                // Update UI
                                                adminNameText.setText(updatedName);
                                                adminEmailText.setText(updatedEmail);
                                                adminMobileText.setText(updatedMobile);

                                                Toast.makeText(getContext(), "Update successful", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Firestore update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                    } else {
                        Toast.makeText(getContext(), "Admin not found in Firestore", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }
}