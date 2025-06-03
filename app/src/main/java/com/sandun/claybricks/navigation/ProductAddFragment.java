//package com.sandun.claybricks.navigation;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Base64;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.Spinner;
//import android.widget.TextView;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.fragment.app.Fragment;
//
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.firebase.firestore.QuerySnapshot;
//import com.sandun.claybricks.R;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class ProductAddFragment extends Fragment {
//
//    private FirebaseFirestore firestore;
//    private ImageView addProductImage;
//    private Uri selectedImageUri;
//    private Spinner selectProductType, selectProductType2;
//    private ArrayAdapter<String> clayBricksAdapter, deleteTypeAdapter;
//
//    // Activity Result Launcher for selecting image
//    private final ActivityResultLauncher<Intent> imagePickerLauncher =
//            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                    Uri imageUri = result.getData().getData();
//                    if (imageUri != null) {
//                        selectedImageUri = imageUri;
//                        addProductImage.setImageURI(imageUri);
//                    }
//                }
//            });
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_product_manager, container, false);
//
//        firestore = FirebaseFirestore.getInstance();
//        addProductImage = view.findViewById(R.id.addProductImage);
//
//        // Open file picker when ImageView is clicked
//        addProductImage.setOnClickListener(v -> openImagePicker());
//
//        selectProductType = view.findViewById(R.id.selectProductType);
//        List<String> clayBricksType = new ArrayList<>();
//        clayBricksType.add("---- Select Type ----");
//
//        ArrayAdapter<String> clayBricksArrayAdapter = new ArrayAdapter<>(
//                requireContext(),
//                R.layout.custom_spinner_item,
//                clayBricksType
//        );
//        selectProductType.setAdapter(clayBricksArrayAdapter);
//
//        firestore.collection("clay-bricks-type").get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                String brickType = document.getString("name");
//                                if (brickType != null) {
//                                    clayBricksType.add(brickType);
//                                }
//                            }
//                            clayBricksArrayAdapter.notifyDataSetChanged();
//                        }
//                    }
//                });
//
//        Button productAddButton = view.findViewById(R.id.productAddButton);
//        productAddButton.setOnClickListener(v -> {
//
//            EditText productName = view.findViewById(R.id.productTypeName);
//            EditText productWidth = view.findViewById(R.id.productWidth);
//            EditText productHeight = view.findViewById(R.id.productHeight);
//            EditText productLength = view.findViewById(R.id.productLength);
//            EditText productWeight = view.findViewById(R.id.productWeight);
//            EditText productQuantity = view.findViewById(R.id.productQuantity);
//            EditText productPrice = view.findViewById(R.id.productPrice);
//
//            String name = productName.getText().toString().trim();
//            String width = productWidth.getText().toString().trim();
//            String height = productHeight.getText().toString().trim();
//            String length = productLength.getText().toString().trim();
//            String type = selectProductType.getSelectedItem().toString().trim();
//            String weight = productWeight.getText().toString().trim();
//            String qty = productQuantity.getText().toString().trim();
//            String priceStr = productPrice.getText().toString().trim();
//
//            if (selectedImageUri == null) {
//                showCustomWarningAlertDialog("Please Select an Image");
//                return;
//            }
//            if (name.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Name");
//                return;
//            }
//            if (name.contains(" ")) {
//                showCustomWarningAlertDialog("Product Name Cannot Contain Spaces");
//                return;
//            }
//            if (width.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Width");
//                return;
//            }
//            if (height.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Height");
//                return;
//            }
//            if (length.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Length");
//                return;
//            }
//            if (type.equals("---- Select Type ----")) {
//                showCustomWarningAlertDialog("Please Select a Product Type");
//                return;
//            }
//            if (weight.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Weight");
//                return;
//            }
//            if (qty.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Quantity");
//                return;
//            }
//            if (qty.length() > 10) {
//                showCustomWarningAlertDialog("Product Quantity Cannot Exceed 10 Digits");
//                return;
//            }
//            if (priceStr.isEmpty()) {
//                showCustomWarningAlertDialog("Please Enter Product Price");
//                return;
//            }
//
//            double price;
//            try {
//                price = Double.parseDouble(priceStr);  // Convert to double
//            } catch (NumberFormatException e) {
//                showCustomWarningAlertDialog("Invalid Product Price. Please enter a valid number.");
//                return;
//            }
//
//            String base64Image = encodeImageToBase64(selectedImageUri);
//            if (base64Image == null) {
//                showCustomWarningAlertDialog("Failed to encode image. Try again.");
//                return;
//            }
//
//            Map<String, Object> product = new HashMap<>();
//            product.put("productName", name);
//            product.put("type", type);
//            product.put("width", width);
//            product.put("height", height);
//            product.put("length", length);
//            product.put("weight", weight);
//            product.put("quantity", qty);
//            product.put("price", price);  // Save as Double in Firestore
//            product.put("imageBase64", base64Image);
//            product.put("timestamp", FieldValue.serverTimestamp());
//            product.put("product_status", "1");
//
//            firestore.collection("clay-bricks-product")
//                    .add(product)
//                    .addOnSuccessListener(documentReference -> {
//                        productName.setText("");
//                        productWidth.setText("");
//                        productHeight.setText("");
//                        productLength.setText("");
//                        productWeight.setText("");
//                        productQuantity.setText("");
//                        productPrice.setText("");
//                        selectProductType.setSelection(0);
//                        addProductImage.setImageResource(R.drawable.add_icon);
//                        selectedImageUri = null;
//                        showCustomSuccessAlertDialog("Product added successfully!");
//                    })
//                    .addOnFailureListener(e ->
//                            showCustomErrorAlertDialog("Error: " + e.getMessage()));
//        });
//
//        EditText newTypeText = view.findViewById(R.id.newTypeText);
//        Button newTypeAdd = view.findViewById(R.id.newTypeAdd);
//
//        newTypeAdd.setOnClickListener(v -> {
//            String newType = newTypeText.getText().toString().trim();
//
//            if (newType.isEmpty()) {
//                showCustomWarningAlertDialog("Please enter a new type.");
//                return;
//            }
//
//            // Check if type already exists to avoid duplicates
//            firestore.collection("clay-bricks-type")
//                    .whereEqualTo("name", newType)
//                    .get()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
//                            showCustomWarningAlertDialog("This type already exists.");
//                        } else {
//                            // Add new type to Firestore
//                            Map<String, Object> typeData = new HashMap<>();
//                            typeData.put("name", newType);
//                            typeData.put("timestamp", FieldValue.serverTimestamp());
//
//                            firestore.collection("clay-bricks-type")
//                                    .add(typeData)
//                                    .addOnSuccessListener(documentReference -> {
//                                        newTypeText.setText(""); // Clear the input field
//                                        showCustomSuccessAlertDialog("New type added successfully!");
//
//                                        // Refresh Spinner list
//                                        clayBricksType.add(newType);
//                                        clayBricksArrayAdapter.notifyDataSetChanged();
//                                        updateSpinners();
//                                    })
//                                    .addOnFailureListener(e ->
//                                            showCustomErrorAlertDialog("Error: " + e.getMessage()));
//                        }
//                    });
//        });
//
//
//
//        selectProductType2 = view.findViewById(R.id.selectProductType2);
//        Button newTypeDelete = view.findViewById(R.id.newTypeDelete);
//
//        List<String> deleteTypeList = new ArrayList<>();
//        deleteTypeList.add("---- Select Type ----");
//
//        ArrayAdapter<String> deleteTypeAdapter = new ArrayAdapter<>(
//                requireContext(),
//                R.layout.custom_spinner_item,
//                deleteTypeList
//        );
//        selectProductType2.setAdapter(deleteTypeAdapter);
//
//        firestore.collection("clay-bricks-type").get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            String typeName = document.getString("name");
//                            if (typeName != null) {
//                                deleteTypeList.add(typeName);
//                            }
//                        }
//                        deleteTypeAdapter.notifyDataSetChanged();
//                    }
//                });
//
//        newTypeDelete.setOnClickListener(v -> {
//            String selectedType = selectProductType2.getSelectedItem().toString().trim();
//
//            if (selectedType.equals("---- Select Type ----")) {
//                showCustomWarningAlertDialog("Please select a type to delete.");
//                return;
//            }
//
//            // Find and delete the selected type from Firestore
//            firestore.collection("clay-bricks-type")
//                    .whereEqualTo("name", selectedType)
//                    .get()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                document.getReference().delete()
//                                        .addOnSuccessListener(aVoid -> {
//                                            deleteTypeList.remove(selectedType);
//                                            deleteTypeAdapter.notifyDataSetChanged();
//                                            selectProductType2.setSelection(0);
//                                            showCustomSuccessAlertDialog("Type deleted successfully!");
//                                            updateSpinners();
//                                        })
//                                        .addOnFailureListener(e ->
//                                                showCustomErrorAlertDialog("Error: " + e.getMessage()));
//                            }
//                        } else {
//                            showCustomErrorAlertDialog("Type not found.");
//                        }
//                    });
//        });
//
//
//
//        return view;
//    }
//
//    private void updateSpinners() {
//        List<String> clayBricksType = new ArrayList<>();
//        clayBricksType.add("---- Select Type ----");
//
//        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(
//                requireContext(),
//                R.layout.custom_spinner_item,
//                clayBricksType
//        );
//        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
//                requireContext(),
//                R.layout.custom_spinner_item,
//                clayBricksType
//        );
//
//        selectProductType.setAdapter(adapter1);
//        selectProductType2.setAdapter(adapter2);
//
//        firestore.collection("clay-bricks-type")
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            String brickType = document.getString("name");
//                            if (brickType != null) {
//                                clayBricksType.add(brickType);
//                            }
//                        }
//                        adapter1.notifyDataSetChanged();
//                        adapter2.notifyDataSetChanged();
//                    }
//                });
//    }
//
//
//
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/*");
//        imagePickerLauncher.launch(intent);
//    }
//
//    private String encodeImageToBase64(Uri imageUri) {
//        try {
//            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//            byte[] byteArray = byteArrayOutputStream.toByteArray();
//            return Base64.encodeToString(byteArray, Base64.DEFAULT);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private void showCustomWarningAlertDialog(String message) {
//        showAlertDialog(message, R.layout.custom_alert_warning_dialog);
//    }
//
//    private void showCustomSuccessAlertDialog(String message) {
//        showAlertDialog(message, R.layout.custom_alert_success_dialog);
//    }
//
//    private void showCustomErrorAlertDialog(String message) {
//        showAlertDialog(message, R.layout.custom_alert_error_dialog);
//    }
//
//    private void showAlertDialog(String message, int layoutId) {
//        View dialogView = LayoutInflater.from(requireContext()).inflate(layoutId, null);
//        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
//        Button dialogButtonOK = dialogView.findViewById(R.id.dialogButtonOK);
//        dialogMessage.setText(message);
//
//        AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
//        alertDialog.show();
//
//        dialogButtonOK.setOnClickListener(v -> alertDialog.dismiss());
//    }
//}


package com.sandun.claybricks.navigation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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

public class ProductAddFragment extends Fragment {

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private ImageView addProductImage;
    private Uri selectedImageUri;
    private Spinner selectProductType, selectProductType2;
    private ArrayAdapter<String> clayBricksAdapter, deleteTypeAdapter;

    // Activity Result Launcher for selecting image
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImageUri = imageUri;
                        addProductImage.setImageURI(imageUri);
                    }
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_manager, container, false);

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        addProductImage = view.findViewById(R.id.addProductImage);

        // Open file picker when ImageView is clicked
        addProductImage.setOnClickListener(v -> openImagePicker());

        selectProductType = view.findViewById(R.id.selectProductType);
        List<String> clayBricksType = new ArrayList<>();
        clayBricksType.add("---- Select Type ----");

        ArrayAdapter<String> clayBricksArrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.custom_spinner_item,
                clayBricksType
        );
        selectProductType.setAdapter(clayBricksArrayAdapter);

        firestore.collection("clay-bricks-type").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String brickType = document.getString("name");
                                if (brickType != null) {
                                    clayBricksType.add(brickType);
                                }
                            }
                            clayBricksArrayAdapter.notifyDataSetChanged();
                        }
                    }
                });

        Button productAddButton = view.findViewById(R.id.productAddButton);
        productAddButton.setOnClickListener(v -> {

            EditText productName = view.findViewById(R.id.productTypeName);
            EditText productWidth = view.findViewById(R.id.productWidth);
            EditText productHeight = view.findViewById(R.id.productHeight);
            EditText productLength = view.findViewById(R.id.productLength);
            EditText productWeight = view.findViewById(R.id.productWeight);
            EditText productQuantity = view.findViewById(R.id.productQuantity);
            EditText productPrice = view.findViewById(R.id.productPrice);

            String name = productName.getText().toString().trim();
            String width = productWidth.getText().toString().trim();
            String height = productHeight.getText().toString().trim();
            String length = productLength.getText().toString().trim();
            String type = selectProductType.getSelectedItem().toString().trim();
            String weight = productWeight.getText().toString().trim();
            String qty = productQuantity.getText().toString().trim();
            String priceStr = productPrice.getText().toString().trim();

            if (selectedImageUri == null) {
                showCustomWarningAlertDialog("Please Select an Image");
                return;
            }
            if (name.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Name");
                return;
            }
            if (name.contains(" ")) {
                showCustomWarningAlertDialog("Product Name Cannot Contain Spaces");
                return;
            }
            if (width.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Width");
                return;
            }
            if (height.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Height");
                return;
            }
            if (length.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Length");
                return;
            }
            if (type.equals("---- Select Type ----")) {
                showCustomWarningAlertDialog("Please Select a Product Type");
                return;
            }
            if (weight.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Weight");
                return;
            }
            if (qty.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Quantity");
                return;
            }
            if (qty.length() > 10) {
                showCustomWarningAlertDialog("Product Quantity Cannot Exceed 10 Digits");
                return;
            }
            if (priceStr.isEmpty()) {
                showCustomWarningAlertDialog("Please Enter Product Price");
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                showCustomWarningAlertDialog("Invalid Product Price. Please enter a valid number.");
                return;
            }

            // Show loading dialog
            AlertDialog loadingDialog = showLoadingDialog("Uploading product...");

            // Upload image to Firebase Storage
            StorageReference imageRef = storageReference.child("product_images/" + System.currentTimeMillis() + ".jpg");
            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            Map<String, Object> product = new HashMap<>();
                            product.put("productName", name);
                            product.put("type", type);
                            product.put("width", width);
                            product.put("height", height);
                            product.put("length", length);
                            product.put("weight", weight);
                            product.put("quantity", qty);
                            product.put("price", price);
                            product.put("imageUrl", imageUrl);
                            product.put("timestamp", FieldValue.serverTimestamp());
                            product.put("product_status", "1");

                            firestore.collection("clay-bricks-product")
                                    .add(product)
                                    .addOnSuccessListener(documentReference -> {
                                        productName.setText("");
                                        productWidth.setText("");
                                        productHeight.setText("");
                                        productLength.setText("");
                                        productWeight.setText("");
                                        productQuantity.setText("");
                                        productPrice.setText("");
                                        selectProductType.setSelection(0);
                                        addProductImage.setImageResource(R.drawable.add_icon);
                                        selectedImageUri = null;
                                        loadingDialog.dismiss();
                                        showCustomSuccessAlertDialog("Product added successfully!");
                                    })
                                    .addOnFailureListener(e -> {
                                        loadingDialog.dismiss();
                                        showCustomErrorAlertDialog("Error: " + e.getMessage());
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        showCustomErrorAlertDialog("Image upload failed: " + e.getMessage());
                    });
        });

        EditText newTypeText = view.findViewById(R.id.newTypeText);
        Button newTypeAdd = view.findViewById(R.id.newTypeAdd);

        newTypeAdd.setOnClickListener(v -> {
            String newType = newTypeText.getText().toString().trim();

            if (newType.isEmpty()) {
                showCustomWarningAlertDialog("Please enter a new type.");
                return;
            }

            firestore.collection("clay-bricks-type")
                    .whereEqualTo("name", newType)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            showCustomWarningAlertDialog("This type already exists.");
                        } else {
                            Map<String, Object> typeData = new HashMap<>();
                            typeData.put("name", newType);
                            typeData.put("timestamp", FieldValue.serverTimestamp());

                            firestore.collection("clay-bricks-type")
                                    .add(typeData)
                                    .addOnSuccessListener(documentReference -> {
                                        newTypeText.setText("");
                                        showCustomSuccessAlertDialog("New type added successfully!");

                                        clayBricksType.add(newType);
                                        clayBricksArrayAdapter.notifyDataSetChanged();
                                        updateSpinners();
                                    })
                                    .addOnFailureListener(e ->
                                            showCustomErrorAlertDialog("Error: " + e.getMessage()));
                        }
                    });
        });

        selectProductType2 = view.findViewById(R.id.selectProductType2);
        Button newTypeDelete = view.findViewById(R.id.newTypeDelete);

        List<String> deleteTypeList = new ArrayList<>();
        deleteTypeList.add("---- Select Type ----");

        ArrayAdapter<String> deleteTypeAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.custom_spinner_item,
                deleteTypeList
        );
        selectProductType2.setAdapter(deleteTypeAdapter);

        firestore.collection("clay-bricks-type").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String typeName = document.getString("name");
                            if (typeName != null) {
                                deleteTypeList.add(typeName);
                            }
                        }
                        deleteTypeAdapter.notifyDataSetChanged();
                    }
                });

        newTypeDelete.setOnClickListener(v -> {
            String selectedType = selectProductType2.getSelectedItem().toString().trim();

            if (selectedType.equals("---- Select Type ----")) {
                showCustomWarningAlertDialog("Please select a type to delete.");
                return;
            }

            firestore.collection("clay-bricks-type")
                    .whereEqualTo("name", selectedType)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            deleteTypeList.remove(selectedType);
                                            deleteTypeAdapter.notifyDataSetChanged();
                                            selectProductType2.setSelection(0);
                                            showCustomSuccessAlertDialog("Type deleted successfully!");
                                            updateSpinners();
                                        })
                                        .addOnFailureListener(e ->
                                                showCustomErrorAlertDialog("Error: " + e.getMessage()));
                            }
                        } else {
                            showCustomErrorAlertDialog("Type not found.");
                        }
                    });
        });

        return view;
    }

    private void updateSpinners() {
        List<String> clayBricksType = new ArrayList<>();
        clayBricksType.add("---- Select Type ----");

        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(
                requireContext(),
                R.layout.custom_spinner_item,
                clayBricksType
        );
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                requireContext(),
                R.layout.custom_spinner_item,
                clayBricksType
        );

        selectProductType.setAdapter(adapter1);
        selectProductType2.setAdapter(adapter2);

        firestore.collection("clay-bricks-type")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String brickType = document.getString("name");
                            if (brickType != null) {
                                clayBricksType.add(brickType);
                            }
                        }
                        adapter1.notifyDataSetChanged();
                        adapter2.notifyDataSetChanged();
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private AlertDialog showLoadingDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(R.layout.loading);
        AlertDialog dialog = builder.create();
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    private void showCustomWarningAlertDialog(String message) {
        showAlertDialog(message, R.layout.custom_alert_warning_dialog);
    }

    private void showCustomSuccessAlertDialog(String message) {
        showAlertDialog(message, R.layout.custom_alert_success_dialog);
    }

    private void showCustomErrorAlertDialog(String message) {
        showAlertDialog(message, R.layout.custom_alert_error_dialog);
    }

    private void showAlertDialog(String message, int layoutId) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(layoutId, null);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogButtonOK = dialogView.findViewById(R.id.dialogButtonOK);
        dialogMessage.setText(message);

        AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        alertDialog.show();

        dialogButtonOK.setOnClickListener(v -> alertDialog.dismiss());
    }
}