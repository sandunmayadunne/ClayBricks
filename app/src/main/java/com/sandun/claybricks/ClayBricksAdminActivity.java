package com.sandun.claybricks;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClayBricksAdminActivity extends AppCompatActivity {

    private TextInputEditText inputAdminEmail;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clay_bricks_admin);

        inputAdminEmail = findViewById(R.id.inputAdminEmail);
        firestore = FirebaseFirestore.getInstance();

        Button sendVerificationCode = findViewById(R.id.sentVerificationCode);
        sendVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ClayBricksAdminActivity.this, AdminPanelActivity.class);
                startActivity(i);

            }
        });

    }
//
}







//
//package com.sandun.claybricks;
//
//import android.app.Dialog;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//import java.io.IOException;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//public class ClayBricksAdminActivity extends AppCompatActivity {
//
//    private TextInputEditText inputAdminEmail;
//    private FirebaseFirestore firestore;
//    private String adminDocumentId;
//    private final OkHttpClient httpClient = new OkHttpClient();
//    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_clay_bricks_admin);
//
//        initializeComponents();
//        setupVerificationButton();
//    }
//
//    private void initializeComponents() {
//        inputAdminEmail = findViewById(R.id.inputAdminEmail);
//        firestore = FirebaseFirestore.getInstance();
//    }
//
//    private void setupVerificationButton() {
//        findViewById(R.id.sentVerificationCode).setOnClickListener(view -> validateAndProcessEmail());
//    }
//
//    private void validateAndProcessEmail() {
//        String email = inputAdminEmail.getText().toString().trim();
//        if (email.isEmpty()) {
//            showToast("Please enter an email");
//            return;
//        }
//        checkAdminStatus(email);
//    }
//
//    private void checkAdminStatus(String email) {
//        firestore.collection("clay-bricks-admin")
//                .whereEqualTo("admin_email", email)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
//                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
//                        adminDocumentId = document.getId();
//                        verifyAdminStatus(document);
//                    } else {
//                        showToast("Admin email not found");
//                    }
//                });
//    }
//
//    private void verifyAdminStatus(DocumentSnapshot document) {
//        Long status = document.getLong("admin_status");
//        if (status != null && status == 2) {
//            showToast("Your account has been deactivated");
//        } else {
//            sendVerificationRequest(document.getString("admin_email"));
//        }
//    }
//
//    private void sendVerificationRequest(String email) {
//        showProgressDialog();
//        new Thread(() -> {
//            try {
//                JsonObject json = new JsonObject();
//                json.addProperty("adminEmail", email);
//                Request request = new Request.Builder()
//                        .url("http://192.168.1.3:8080/ClayBricksBackend/SendVerificationCode")
//                        .post(RequestBody.create(json.toString(), JSON))
//                        .build();
//
//                Response response = httpClient.newCall(request).execute();
//                handleVerificationResponse(response, email);
//            } catch (IOException e) {
//                runOnUiThread(() -> {
//                    dismissProgressDialog();
//                    showToast("Network error: " + e.getMessage());
//                });
//            }
//        }).start();
//    }
//
//    private void handleVerificationResponse(Response response, String email) throws IOException {
//        String responseBody = response.body().string();
//        JsonObject responseJson = new Gson().fromJson(responseBody, JsonObject.class);
//
//        runOnUiThread(() -> {
//            dismissProgressDialog();
//            if (responseJson.get("success").getAsBoolean()) {
//                updateFirestoreWithCode(responseJson.get("verificationCode").getAsString());
//            } else {
//                showToast("Verification failed: " + responseJson.get("message").getAsString());
//            }
//        });
//    }
//
//    private void updateFirestoreWithCode(String code) {
//        firestore.collection("clay-bricks-admin")
//                .document(adminDocumentId)
//                .update("verificationCode", code)
//                .addOnSuccessListener(aVoid -> showVerificationDialog())
//                .addOnFailureListener(e -> showToast("Failed to save code: " + e.getMessage()));
//    }
//
//    private void showVerificationDialog() {
//        Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.admin_verification_code_add_dialog);
//        dialog.setCancelable(false);
//
//        TextInputEditText inputCode = dialog.findViewById(R.id.inputCurrentPassword);
//        dialog.findViewById(R.id.okButton).setOnClickListener(view -> {
//            String code = inputCode.getText().toString().trim();
//            if (!code.isEmpty()) {
//                verifyCode(code, dialog);
//            } else {
//                showToast("Please enter verification code");
//            }
//        });
//        dialog.show();
//    }
//
//    private void verifyCode(String code, Dialog dialog) {
//        firestore.collection("clay-bricks-admin")
//                .document(adminDocumentId)
//                .get()
//                .addOnSuccessListener(document -> {
//                    if (code.equals(document.getString("verificationCode"))) {
//                        saveAdminPreferences(document);
//                        startAdminPanel();
//                        dialog.dismiss();
//                    } else {
//                        showToast("Invalid verification code");
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    showToast("Verification failed: " + e.getMessage());
//                    dialog.dismiss();
//                });
//    }
//
//    private void saveAdminPreferences(DocumentSnapshot document) {
//        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
//        prefs.edit()
//                .putString("admin_name", document.getString("admin_name"))
//                .putString("admin_email", document.getString("admin_email"))
//                .putString("admin_mobile", document.getString("admin_mobile"))
//                .apply();
//    }
//
//    private void startAdminPanel() {
//        startActivity(new Intent(this, AdminPanelActivity.class));
//        finish();
//    }
//
//    private void showToast(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }
//
//    // Progress dialog helpers
//    private Dialog progressDialog;
//
//    private void showProgressDialog() {
//        progressDialog = new Dialog(this);
//        progressDialog.setContentView(R.layout.admin_verification_code_add_dialog);
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//    }
//
//    private void dismissProgressDialog() {
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
//    }
//}