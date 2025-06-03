package com.sandun.claybricks;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
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


        Button signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TextInputEditText inputTextName = findViewById(R.id.inputTextName);
                TextInputEditText inputTextEmail = findViewById(R.id.inputTextEmail);
                TextInputEditText inputTextMobile = findViewById(R.id.inputTextNewAdminName);
                TextInputEditText inputTextPassword = findViewById(R.id.inputTextPassword);

                String name = String.valueOf(inputTextName.getText());
                String email = String.valueOf(inputTextEmail.getText());
                String mobile = String.valueOf(inputTextMobile.getText());
                String password = String.valueOf(inputTextPassword.getText());

                if (name.isEmpty()) {
                    showCustomWarningAlertDialog("Please Enter Your Name");
                } else if (name.contains(" ")) {
                    showCustomWarningAlertDialog("Name Cannot Contain Spaces");
                } else if (email.isEmpty()) {
                    showCustomWarningAlertDialog("Please Enter Your Email");
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showCustomWarningAlertDialog("Please Enter a Valid Email");
                } else if (mobile.isEmpty()) {
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

//                    firestore.collection("clay-bricks-user")
//                            .whereEqualTo("user_mobile", mobile)
//                            .get()
//                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                                @Override
//                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                    if (task.isSuccessful()) {
//                                        if (task.getResult().isEmpty()) {
//                                            // Mobile number does not exist, proceed with registration
//                                            HashMap<String, Object> userSignUpMap = new HashMap<>();
//                                            userSignUpMap.put("user_name", name);
//                                            userSignUpMap.put("user_email", email);
//                                            userSignUpMap.put("user_mobile", mobile);
//                                            userSignUpMap.put("user_password", password);
//                                            userSignUpMap.put("status", 1L);
//
//                                            firestore.collection("clay-bricks-user").add(userSignUpMap)
//                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                                                        @Override
//                                                        public void onSuccess(DocumentReference documentReference) {
//                                                            Log.i("claybrigsss", "User added with ID: " + documentReference.getId());
//                                                            showCustomSuccessAlertDialog("Sign-up Successful!");
//                                                        }
//                                                    })
//                                                    .addOnFailureListener(new OnFailureListener() {
//                                                        @Override
//                                                        public void onFailure(@NonNull Exception e) {
//                                                            Log.i("claybrigsss", "Failed to add user");
//                                                            showCustomErrorAlertDialog("Sign-up Failed!");
//                                                        }
//                                                    });
//                                        } else {
//                                            // Mobile number already exists
//                                            showCustomWarningAlertDialog("This mobile number is already registered!");
//                                        }
//                                    } else {
//                                        // Firestore query failed
//                                        Log.i("claybrigsss", "Firestore Error: " + task.getException().getMessage());
//                                        showCustomErrorAlertDialog("An error occurred. Please try again.");
//                                    }
//                                }
//                            });

                    firestore.collection("clay-bricks-user")
                            .whereEqualTo("user_mobile", mobile)
                            .get()
                            .addOnCompleteListener(mobileTask -> {
                                if (mobileTask.isSuccessful()) {
                                    if (!mobileTask.getResult().isEmpty()) {
                                        // Mobile already exists
                                        showCustomWarningAlertDialog("This mobile number is already registered!");
                                    } else {
                                        // Check email if mobile is unique
                                        firestore.collection("clay-bricks-user")
                                                .whereEqualTo("user_email", email)
                                                .get()
                                                .addOnCompleteListener(emailTask -> {
                                                    if (emailTask.isSuccessful()) {
                                                        if (!emailTask.getResult().isEmpty()) {
                                                            // Email already exists
                                                            showCustomWarningAlertDialog("This email is already registered!");
                                                        } else {
                                                            // Both mobile and email are unique - proceed with sign-up
                                                            HashMap<String, Object> userSignUpMap = new HashMap<>();
                                                            userSignUpMap.put("user_name", name);
                                                            userSignUpMap.put("user_email", email);
                                                            userSignUpMap.put("user_mobile", mobile);
                                                            userSignUpMap.put("user_password", password);
                                                            userSignUpMap.put("status", 1L);

                                                            firestore.collection("clay-bricks-user").add(userSignUpMap)
                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                        @Override
                                                                        public void onSuccess(DocumentReference documentReference) {
                                                                            Log.i("claybrigsss", "User added with ID: " + documentReference.getId());
                                                                            showCustomSuccessAlertDialog("Sign-up Successful!");
                                                                        }
                                                                    })
                                                                    .addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception e) {
                                                                            Log.i("claybrigsss", "Failed to add user");
                                                                            showCustomErrorAlertDialog("Sign-up Failed!");
                                                                        }
                                                                    });
                                                        }
                                                    } else {
                                                        Log.e("FirestoreError", "Email check failed", emailTask.getException());
                                                        showCustomErrorAlertDialog("Error checking email availability");
                                                    }
                                                });
                                    }
                                } else {
                                    Log.e("FirestoreError", "Mobile check failed", mobileTask.getException());
                                    showCustomErrorAlertDialog("Error checking mobile availability");
                                }
                            });


                }


            }
        });


        TextView goToSignInActivity = findViewById(R.id.goToSignInActivity);
        goToSignInActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FlingAnimation flingAnimation = new FlingAnimation(goToSignInActivity, DynamicAnimation.TRANSLATION_X);
                flingAnimation.setStartVelocity(1000f);
//                    flingAnimation.setFriction(0.1f);
                flingAnimation.start();

                Intent i = new Intent(SignUpActivity.this, SignInActivity.class);
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

                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
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