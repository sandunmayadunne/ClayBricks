package com.sandun.claybricks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.sandun.claybricks.model.User;

public class SettingActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView goBackHome = findViewById(R.id.goBackHome);
        goBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SettingActivity.this, HomeActivity.class);
                startActivity(i);
                finish();
            }
        });


        SharedPreferences themePreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);

        Switch themeSwitch = findViewById(R.id.switch1);

        boolean isNightMode = themePreferences.getBoolean("night", false);
        themeSwitch.setChecked(isNightMode);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save theme preference
            themePreferences.edit()
                    .putBoolean("night", isChecked)
                    .putBoolean("fromProfile", true)
                    .apply();

            // Apply theme mode
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });


        firestore = FirebaseFirestore.getInstance();

        SharedPreferences userPrefs = getSharedPreferences("com.sandun.claybricks.data", MODE_PRIVATE);
        String userJson = userPrefs.getString("user", "");

        if (!userJson.isEmpty()) {
            currentUser = new Gson().fromJson(userJson, User.class);
        }

        ImageView changePassword = findViewById(R.id.changePassword);
        changePassword.setOnClickListener(view -> showChangePasswordDialog());

        ImageView logOut = findViewById(R.id.logOut);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomLogoutAlertDialog();
            }
        });

        TextView deleteAccount = findViewById(R.id.delect_account);
        deleteAccount.setOnClickListener(v -> showCustomDeleteAlertDialog());

    }


    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.change_passowrd_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextInputLayout currentPasswordLayout = dialogView.findViewById(R.id.currentPassword);
        TextInputEditText inputCurrentPassword = dialogView.findViewById(R.id.inputCurrentPassword);
        TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.newPassword);
        TextInputEditText inputNewPassword = dialogView.findViewById(R.id.inputNewPassword);

        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView okButton = dialogView.findViewById(R.id.okButton);

        newPasswordLayout.setVisibility(View.GONE);

        okButton.setOnClickListener(v -> {
            if (newPasswordLayout.getVisibility() == View.GONE) {
                verifyCurrentPassword(inputCurrentPassword.getText().toString(), newPasswordLayout, currentPasswordLayout);
            } else {
                updatePassword(inputNewPassword.getText().toString(), alertDialog);
            }
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void verifyCurrentPassword(String enteredPassword, TextInputLayout newPasswordLayout, TextInputLayout currentPasswordLayout) {
        firestore.collection("clay-bricks-user")
                .whereEqualTo("user_mobile", currentUser.getUser_mobile())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String storedPassword = document.getString("user_password");

                        if (storedPassword != null && storedPassword.equals(enteredPassword)) {
                            currentPasswordLayout.setVisibility(View.GONE);
                            newPasswordLayout.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, "Incorrect current password!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error checking password", e));
    }

    private void updatePassword(String newPassword, AlertDialog alertDialog) {
        firestore.collection("clay-bricks-user")
                .whereEqualTo("user_mobile", currentUser.getUser_mobile())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        document.getReference().update("user_password", newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update password!", Toast.LENGTH_SHORT).show());
                    }
                });
    }


    private void showCustomLogoutAlertDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_logout_user, null);

        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView okButton = dialogView.findViewById(R.id.okButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set dialog width
        alertDialog.setOnShowListener(dialog -> {
            Window window = alertDialog.getWindow();
            if (window != null) {
                int widthInPixels = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 400, getResources().getDisplayMetrics());
                window.setLayout(widthInPixels, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        okButton.setOnClickListener(v -> {
            // Clear all SharedPreferences data
            SharedPreferences userPrefs = getSharedPreferences("com.sandun.claybricks.data", MODE_PRIVATE);
            SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);

            userPrefs.edit().clear().apply();
            themePrefs.edit().clear().apply();

            // Reset theme to default
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

            // Redirect to SignInActivity
            Intent intent = new Intent(SettingActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();

            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void showCustomDeleteAlertDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_delect_user, null);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView okButton = dialogView.findViewById(R.id.okButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        // Dialog styling (same as logout dialog)
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        alertDialog.setOnShowListener(dialog -> {
            Window window = alertDialog.getWindow();
            if (window != null) {
                int widthInPixels = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 400, getResources().getDisplayMetrics());
                window.setLayout(widthInPixels, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        okButton.setOnClickListener(v -> {
            SharedPreferences userPrefs = getSharedPreferences("com.sandun.claybricks.data", MODE_PRIVATE);
            String userJson = userPrefs.getString("user", "");

            if (userJson.isEmpty()) {
                Toast.makeText(this, "User not found in local storage!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert JSON back to User object
            Gson gson = new Gson();
            User currentUser = gson.fromJson(userJson, User.class);
            String mobile = currentUser.getUser_mobile();

            if (mobile == null || mobile.isEmpty()) {
                Toast.makeText(this, "Mobile number not found!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore.getInstance().collection("clay-bricks-user")
                    .whereEqualTo("user_mobile", mobile)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Clear preferences
                                        userPrefs.edit().clear().apply();
                                        getSharedPreferences("theme_prefs", MODE_PRIVATE).edit().clear().apply();

                                        // Reset theme
                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

                                        // Show success & redirect
                                        Toast.makeText(SettingActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SettingActivity.this, SignInActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(SettingActivity.this, "Deletion failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(SettingActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                        }
                        alertDialog.dismiss();
                    });
        });

        alertDialog.show();
    }

}