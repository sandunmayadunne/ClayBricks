package com.sandun.claybricks;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.sandun.claybricks.model.User;

public class SignInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Window splshWindow = this.getWindow();
        splshWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        splshWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        splshWindow.setStatusBarColor(getResources().getColor(R.color.themColour1));
        splshWindow.setNavigationBarColor(getResources().getColor(R.color.themColour1));

        Button signInButton = findViewById(R.id.signInButton);
        signInButton.setOnClickListener(view -> {
            TextInputEditText inputTextMobile = findViewById(R.id.inputTextNewAdminName);
            TextInputEditText inputTextPassword = findViewById(R.id.inputTextPassword);

            String mobile = String.valueOf(inputTextMobile.getText());
            String password = String.valueOf(inputTextPassword.getText());

            if (mobile.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Your Mobile");
            } else if (!mobile.matches("^07[0125678]\\d{7}$")) {
                showCustomWarningAlertDialog("Please Enter a Valid Sri Lankan Mobile Number");
            } else if (password.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Your Password");
            } else if (password.contains(" ")) {
                showCustomWarningAlertDialog("Password Cannot Contain Spaces");
            } else if (password.length() > 10) {
                showCustomWarningAlertDialog("Password Cannot Exceed 10 Characters");
            } else {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();

                firestore.collection("clay-bricks-user")
                        .whereEqualTo("user_mobile", mobile)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean userFound = false;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String Password = document.getString("user_password");
                                    if (Password != null && Password.equals(password)) {
                                        userFound = true;
                                        String name = document.getString("user_name");
                                        Long statusLong = document.getLong("status");
                                        String status = statusLong != null ? String.valueOf(statusLong) : "2";

                                        User user = new User(
                                                document.getString("user_id"),
                                                document.getString("user_name"),
                                                document.getString("user_last_name"),
                                                document.getString("user_email"),
                                                document.getString("user_mobile"),
                                                status,
                                                document.getString("province"),
                                                document.getString("district"),
                                                document.getString("city"),
                                                document.getString("line1"),
                                                document.getString("line2")
                                        );

                                        // Convert User object to JSON using Gson
                                        Gson gson = new Gson();
                                        String userJson = gson.toJson(user);

                                        SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sp.edit();
                                        editor.putString("user", userJson);
                                        editor.apply();

                                        // Check user status
                                        if (status.equals("1")) {
                                            showCustomSuccessAlertDialog("Welcome " + name + "!");
                                        } else {
                                            // Clear invalid user data
                                            editor.remove("user");
                                            editor.apply();
                                            showCustomErrorAlertDialog("Your account is deactivated. Please contact support.");
                                        }
                                    }
                                }
                                if (!userFound) {
                                    showCustomErrorAlertDialog("Invalid Mobile or Password");
                                }
                            } else {
                                Log.e("FirestoreError", "Error: ", task.getException());
                            }
                        });
            }
        });

        TextView goAdminPage = findViewById(R.id.goAdminPage);
        goAdminPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SignInActivity.this, ClayBricksAdminActivity.class);
                startActivity(i);
            }
        });

        TextView goToSignUpActivity = findViewById(R.id.goToSignUpActivity);
        goToSignUpActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlingAnimation flingAnimation = new FlingAnimation(goToSignUpActivity, DynamicAnimation.TRANSLATION_X);
                flingAnimation.setStartVelocity(1000f);
                flingAnimation.start();

                Intent i = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
    private void showCustomWarningAlertDialog(String message) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_warning_dialog, null);

//        // Initialize views from the custom layout
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogButtonOK = dialogView.findViewById(R.id.dialogButtonOK);

        // Set the message
        dialogMessage.setText(message);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        // Remove default background to show rounded corners
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set dialog width to 100dp
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Window window = alertDialog.getWindow();
                if (window != null) {
                    // Convert 100dp to pixels
                    int widthInDp = 300; // Desired width in dp
                    int widthInPixels = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, widthInDp, getResources().getDisplayMetrics());

                    // Set the width and height of the dialog
                    window.setLayout(widthInPixels, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        });

        // Set click listener for the OK button
        dialogButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss(); // Dismiss the dialog
            }
        });

        // Show the dialog
        alertDialog.show();
    }
    private void showCustomSuccessAlertDialog(String message) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_success_dialog, null);

//        // Initialize views from the custom layout
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogButtonOK = dialogView.findViewById(R.id.dialogButtonOK);

        // Set the message
        dialogMessage.setText(message);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        // Remove default background to show rounded corners
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set dialog width to 100dp
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Window window = alertDialog.getWindow();
                if (window != null) {
                    // Convert 100dp to pixels
                    int widthInDp = 300; // Desired width in dp
                    int widthInPixels = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, widthInDp, getResources().getDisplayMetrics());

                    // Set the width and height of the dialog
                    window.setLayout(widthInPixels, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        });

        // Set click listener for the OK button
        dialogButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss(); // Dismiss the dialog

                Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Show the dialog
        alertDialog.show();
    }

    private void showCustomErrorAlertDialog(String message) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_error_dialog, null);

//        // Initialize views from the custom layout
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogButtonOK = dialogView.findViewById(R.id.dialogButtonOK);

        // Set the message
        dialogMessage.setText(message);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        // Remove default background to show rounded corners
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set dialog width to 100dp
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Window window = alertDialog.getWindow();
                if (window != null) {
                    // Convert 100dp to pixels
                    int widthInDp = 300; // Desired width in dp
                    int widthInPixels = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, widthInDp, getResources().getDisplayMetrics());

                    // Set the width and height of the dialog
                    window.setLayout(widthInPixels, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        });

        // Set click listener for the OK button
        dialogButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss(); // Dismiss the dialog
            }
        });

        // Show the dialog
        alertDialog.show();
    }

}