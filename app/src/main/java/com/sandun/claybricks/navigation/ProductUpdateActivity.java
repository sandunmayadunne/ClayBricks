package com.sandun.claybricks.navigation;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sandun.claybricks.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductUpdateActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String productId;
    private Dialog loadingDialog;
    private ProgressDialog progressDialog;
    private TextView productNameHeader, viewProductName, viewProductWidth,
            viewProductHeight, viewProductLength, viewProductType,
            viewProductWeight, viewProductQuantity, viewProductPrice;
    private ImageView updateViewProductImage, editProductName, editProductWidth,
            editProductHeight, editProductLength, editProductType, editProductWeight,
            editProductQuantity, editProductPrice;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private boolean isImageSelected = false;
    private String currentImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_update);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            updateViewProductImage.setImageBitmap(bitmap);
                            isImageSelected = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        initializeViews();
        initializeFirebase();
        setupEditListeners();
        handleIntent();
        setupImageListeners();
    }

    private void initializeViews() {
        productNameHeader = findViewById(R.id.productNameHeader);
        viewProductName = findViewById(R.id.viewProductName);
        viewProductWidth = findViewById(R.id.viewProductWidth);
        viewProductHeight = findViewById(R.id.viewProductHeight);
        viewProductLength = findViewById(R.id.viewProductLength);
        viewProductType = findViewById(R.id.viewProductType);
        viewProductWeight = findViewById(R.id.viewProductWeight);
        viewProductQuantity = findViewById(R.id.viewProductQuantity);
        viewProductPrice = findViewById(R.id.viewProductPrice);
        updateViewProductImage = findViewById(R.id.updateViewProductImage);

        editProductName = findViewById(R.id.editProductName);
        editProductWidth = findViewById(R.id.editProductWidth);
        editProductHeight = findViewById(R.id.editProductHeight);
        editProductLength = findViewById(R.id.editProductLength);
        editProductType = findViewById(R.id.editProductType);
        editProductWeight = findViewById(R.id.editProductWeight);
        editProductQuantity = findViewById(R.id.editProductQuantity);
        editProductPrice = findViewById(R.id.editProductPrice);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void setupImageListeners() {
        // Image selection
        ImageView editProductImage = findViewById(R.id.editProductImage);
        editProductImage.setOnClickListener(v -> openImagePicker());

        // Image update
        Button imageUpdateButton = findViewById(R.id.imageUpdateButton);
        imageUpdateButton.setOnClickListener(v -> uploadImageToFirebase());

        // Cancel image selection
        Button imageUploadCancelButton = findViewById(R.id.imageUploadCancelButton);
        imageUploadCancelButton.setOnClickListener(v -> {
            refreshData();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase() {
        if (!isImageSelected) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Updating Image...");

        // Create reference to new image location
        StorageReference imageRef = storageRef.child("product_images/" + System.currentTimeMillis() + ".jpg");

        // Upload the file to Firebase Storage
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String newImageUrl = uri.toString();

                        // Update Firestore with new image URL
                        db.collection("clay-bricks-product")
                                .document(productId)
                                .update("imageUrl", newImageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    // Delete old image if it exists
                                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                                        StorageReference oldImageRef = storage.getReferenceFromUrl(currentImageUrl);
                                        oldImageRef.delete().addOnSuccessListener(aVoid1 -> {
                                            Log.d("Storage", "Old image deleted successfully");
                                        }).addOnFailureListener(e -> {
                                            Log.w("Storage", "Failed to delete old image", e);
                                        });
                                    }

                                    dismissProgressDialog();
                                    Toast.makeText(this, "Image updated successfully", Toast.LENGTH_SHORT).show();
                                    isImageSelected = false;
                                    currentImageUrl = newImageUrl;
                                })
                                .addOnFailureListener(e -> {
                                    dismissProgressDialog();
                                    Toast.makeText(this, "Image update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading: " + (int) progress + "%");
                });
    }

    private void setupEditListeners() {
        editProductName.setOnClickListener(v -> showEditDialog(
                "Product Name",
                "productName",
                viewProductName.getText().toString(),
                android.text.InputType.TYPE_CLASS_TEXT,
                false
        ));

        editProductWidth.setOnClickListener(v -> showEditDialog(
                "Width",
                "width",
                viewProductWidth.getText().toString().replace(" MM", ""),
                android.text.InputType.TYPE_CLASS_NUMBER,
                true
        ));

        editProductHeight.setOnClickListener(v -> showEditDialog(
                "Height",
                "height",
                viewProductHeight.getText().toString().replace(" MM", ""),
                android.text.InputType.TYPE_CLASS_NUMBER,
                true
        ));

        editProductLength.setOnClickListener(v -> showEditDialog(
                "Length",
                "length",
                viewProductLength.getText().toString().replace(" MM", ""),
                android.text.InputType.TYPE_CLASS_NUMBER,
                true
        ));

        editProductType.setOnClickListener(v -> showUpdateProductTypeDialog());

        editProductWeight.setOnClickListener(v -> showEditDialog(
                "Weight",
                "weight",
                viewProductWeight.getText().toString().replace(" KG", ""),
                android.text.InputType.TYPE_CLASS_NUMBER,
                true
        ));

        editProductQuantity.setOnClickListener(v -> showEditDialog(
                "Quantity",
                "quantity",
                viewProductQuantity.getText().toString().replace(" Units", ""),
                android.text.InputType.TYPE_CLASS_NUMBER,
                true
        ));

        editProductPrice.setOnClickListener(v -> showEditDialog(
                "Price",
                "price",
                viewProductPrice.getText().toString().replace("Rs. ", ""),
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL,
                true
        ));
    }

    private void showUpdateProductTypeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.product_update_dialog);
        dialog.setCancelable(true);

        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView updateButton = dialog.findViewById(R.id.updateButton);
        Spinner productTypeSpinner = dialog.findViewById(R.id.selectProductType);

        loadProductTypes(productTypeSpinner, dialog);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            String selectedType = productTypeSpinner.getSelectedItem().toString();
            if (!selectedType.equals("---- Select Type ----") && !selectedType.equals("No types available")) {
                updateProductType(selectedType, dialog);
            } else {
                Toast.makeText(this, "Please select a valid product type", Toast.LENGTH_SHORT).show();
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.rounded_corners);

            int widthInDp = 400;
            int widthInPixels = (int) (widthInDp * getResources().getDisplayMetrics().density);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = widthInPixels;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;

            window.setAttributes(layoutParams);
        }

        dialog.show();
    }

    private void loadProductTypes(Spinner spinner, Dialog dialog) {
        List<String> productTypes = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.custom_spinner_item,
                productTypes
        );
        spinner.setAdapter(adapter);

        productTypes.add("---- Select Type ----");
        adapter.notifyDataSetChanged();

        db.collection("clay-bricks-type")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productTypes.clear();
                    productTypes.add("---- Select Type ----");

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        String brickType = snapshot.getString("name");
                        if (brickType != null && !brickType.isEmpty()) {
                            productTypes.add(brickType);
                        }
                    }

                    if (productTypes.size() == 1) {
                        productTypes.add("No types available");
                    }

                    adapter.notifyDataSetChanged();

                    String currentType = viewProductType.getText().toString();
                    if (!currentType.isEmpty() && productTypes.contains(currentType)) {
                        spinner.setSelection(productTypes.indexOf(currentType));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load types: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
    }

    private void updateProductType(String newType, Dialog dialog) {
        showProgressDialog("Updating Product Type...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("type", newType);

        db.collection("clay-bricks-product")
                .document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    viewProductType.setText(newType);
                    Toast.makeText(this, "Product type updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditDialog(String title, String fieldName, String currentValue, int inputType, boolean needsValidation) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog_update_user);

        TextView dialogTitle = dialog.findViewById(R.id.userSelectUpdateName);
        EditText updateText = dialog.findViewById(R.id.updateText);
        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView updateButton = dialog.findViewById(R.id.updateButton);

        dialogTitle.setText("Edit " + title);
        updateText.setInputType(inputType);
        updateText.setText(currentValue);
        updateText.setSelection(updateText.getText().length());

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            String newValue = updateText.getText().toString().trim();
            if (validateInput(fieldName, newValue, needsValidation)) {
                updateFirestore(fieldName, newValue, dialog);
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.rounded_corners);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (400 * getResources().getDisplayMetrics().density);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);
        }

        dialog.show();
    }

    private boolean validateInput(String fieldName, String value, boolean needsValidation) {
        if (value.isEmpty()) {
            Toast.makeText(this, "Field cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (needsValidation) {
            try {
                double numericValue = Double.parseDouble(value);
                if (numericValue <= 0) {
                    Toast.makeText(this, "Value must be greater than 0", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void updateFirestore(String fieldName, String newValue, Dialog dialog) {
        showProgressDialog("Updating...");

        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, fieldName.equals("price") ? Double.parseDouble(newValue) : newValue);

        db.collection("clay-bricks-product")
                .document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show();
                    refreshData();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void refreshData() {
        fetchProductData();
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleIntent() {
        productId = getIntent().getStringExtra("PRODUCT_ID");

        if (productId == null || productId.isEmpty()) {
            showErrorAndFinish("Invalid product ID");
            return;
        }

        showLoadingDialog();
        fetchProductData();
    }

    private void showLoadingDialog() {
        loadingDialog = new Dialog(this);
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

    private void fetchProductData() {
        db.collection("clay-bricks-product")
                .document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    dismissLoadingDialog();

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            updateUI(document);
                        } else {
                            showErrorAndFinish("Product not found");
                        }
                    } else {
                        showErrorAndFinish("Error: " + task.getException().getMessage());
                    }
                });
    }

    private void updateUI(DocumentSnapshot document) {
        runOnUiThread(() -> {
            try {
                Log.d("FirestoreDebug", "Document Data: " + document.getData());

                String productName = document.getString("productName");
                String width = document.getString("width");
                String height = document.getString("height");
                String length = document.getString("length");
                String type = document.getString("type");
                String weight = document.getString("weight");
                String quantity = document.getString("quantity");
                String price = document.get("price") != null ? String.valueOf(document.get("price")) : null;
                currentImageUrl = document.getString("imageUrl");

                productNameHeader.setText(productName != null ? productName : " ");
                viewProductName.setText(productName != null ? productName : "N/A");
                viewProductWidth.setText(width != null ? width + " MM" : "N/A");
                viewProductHeight.setText(height != null ? height + " MM" : "N/A");
                viewProductLength.setText(length != null ? length + " MM" : "N/A");
                viewProductType.setText(type != null ? type : "N/A");
                viewProductWeight.setText(weight != null ? weight + " KG" : "N/A");
                viewProductQuantity.setText(quantity != null ? quantity + " Units" : "N/A");
                viewProductPrice.setText(price != null ? "Rs. " + price : "N/A");

                // Load Image from URL
                loadProductImage(currentImageUrl);

            } catch (Exception e) {
                Log.e("FirestoreDebug", "Error updating UI", e);
                showErrorAndFinish("UI update failed: " + e.getMessage());
            }
        });
    }

    private void loadProductImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Use Glide or Picasso for better image loading
            // For simplicity, we'll use a basic approach here
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    runOnUiThread(() -> updateViewProductImage.setImageBitmap(bitmap));
                } catch (Exception e) {
                    Log.e("ImageError", "Error loading image from URL: " + e.getMessage());
                    runOnUiThread(this::setDefaultImage);
                }
            }).start();
        } else {
            setDefaultImage();
        }
    }

    private void setDefaultImage() {
        updateViewProductImage.setImageResource(R.drawable.add_icon);
    }

    private void showErrorAndFinish(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        productNameHeader.setText("");
        viewProductName.setText("");
        viewProductWidth.setText("");
        viewProductHeight.setText("");
        viewProductLength.setText("");
        viewProductType.setText("");
        viewProductWeight.setText("");
        viewProductQuantity.setText("");
        viewProductPrice.setText("");
        setDefaultImage();
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}