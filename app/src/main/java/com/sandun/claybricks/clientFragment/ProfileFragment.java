//package com.sandun.claybricks;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Switch;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatDelegate;
//import androidx.fragment.app.Fragment;
//
//import com.google.gson.Gson;
//
//public class ProfileFragment extends Fragment {
//
//    private SharedPreferences themePreferences;
//    private Switch themeSwitch;
//
//    public ProfileFragment() {
//        super(R.layout.fragment_profile);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        themePreferences = requireContext().getSharedPreferences("TheamMode", Context.MODE_PRIVATE);
//
//        setupThemeSwitch(view);
//        loadUserData(view);
//        setupLogoutButton(view);
//    }
//
//    private void setupThemeSwitch(View view) {
//        themeSwitch = view.findViewById(R.id.switch1);
//        boolean isNightMode = themePreferences.getBoolean("night", false);
//        themeSwitch.setChecked(isNightMode);
//
//        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            // Save theme preference
//            themePreferences.edit()
//                    .putBoolean("night", isChecked)
//                    .putBoolean("fromProfile", true)
//                    .apply();
//
//            // Apply theme mode
//            AppCompatDelegate.setDefaultNightMode(
//                    isChecked ?
//                            AppCompatDelegate.MODE_NIGHT_YES :
//                            AppCompatDelegate.MODE_NIGHT_NO
//            );
//        });
//    }
//
//    private void loadUserData(View view) {
//        TextView userNameView = view.findViewById(R.id.userName);
//        TextView userFNameView = view.findViewById(R.id.userFirstName);
//        TextView userLNameView = view.findViewById(R.id.userLastName);
//        TextView emailView = view.findViewById(R.id.userEmail);
//        TextView userMobileView = view.findViewById(R.id.userMobileNumber);
//
//        SharedPreferences sp = requireContext().getSharedPreferences(
//                "com.sandun.claybricks.data",
//                Context.MODE_PRIVATE
//        );
//
//        String userJson = sp.getString("user", null);
//        if (userJson != null) {
//            try {
//                User user = new Gson().fromJson(userJson, User.class);
//
//
//                String userName = (user != null && user.getUser_name() != null) ?
//                        user.getUser_name() : "N/A";
//                    userName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
//                userNameView.setText(userName);
//
//                String firstName = (user != null && user.getUser_name() != null) ?
//                        user.getUser_name() : "N/A";
//                userFNameView.setText(firstName);
//
//                String lastName = (user != null && user.getUser_last_name() != null) ?
//                        user.getUser_last_name() : " ";
//                userLNameView.setText(lastName);
//
//                String email = (user != null && user.getUser_email() != null) ?
//                        user.getUser_email() : "N/A";
//                emailView.setText(email);
//
//                String mobile = (user != null && user.getUser_mobile() != null) ?
//                        user.getUser_mobile() : "N/A";
//                userMobileView.setText(mobile);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            // Fallback if user data is not found
//            userFNameView.setText("Guest!");
//            userLNameView.setText("Guest!");
//            emailView.setText("Guest!");
//            userMobileView.setText("Guest!");
//        }
//    }
//
//
//    private void setupLogoutButton(View view) {
//        Button logOutButton = view.findViewById(R.id.logOutButton);
//        logOutButton.setOnClickListener(v -> showLogoutConfirmation());
//    }
//
//    private void showLogoutConfirmation() {
//        new android.app.AlertDialog.Builder(requireContext())
//                .setTitle("Logout")
//                .setMessage("Are you sure you want to logout?")
//                .setPositiveButton("Yes", (dialog, which) -> performLogout())
//                .setNegativeButton("No", null)
//                .show();
//    }
//
//    private void performLogout() {
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        // Clear all user data
//        SharedPreferences userPrefs = requireContext().getSharedPreferences(
//                "com.sandun.claybricks.data",
//                Context.MODE_PRIVATE
//        );
//        userPrefs.edit().clear().apply();
//
//        // Clear theme preferences if needed
//        themePreferences.edit().clear().apply();
//
//        // Redirect to SignInActivity
//        Intent intent = new Intent(requireActivity(), SignInActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//
//        // Finish current activity
//        if (getActivity() != null) {
//            getActivity().finish();
//        }
//    }
//
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        // Clear theme change flag when leaving fragment
//        themePreferences.edit().remove("fromProfile").apply();
//    }
//}

package com.sandun.claybricks.clientFragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.sandun.claybricks.R;
import com.sandun.claybricks.model.User;

public class ProfileFragment extends Fragment {

    private SharedPreferences themePreferences;
    //    private Switch themeSwitch;
    private FirebaseFirestore firestore;
    private SharedPreferences userPrefs;
    private User currentUser;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        themePreferences = requireContext().getSharedPreferences("TheamMode", Context.MODE_PRIVATE);
        userPrefs = requireContext().getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
        firestore = FirebaseFirestore.getInstance();

        loadUserData(view);
        setupEditButtons(view);

    }


    private void setupEditButtons(View view) {
        ImageView editFirstName = view.findViewById(R.id.editFirstName);
        editFirstName.setOnClickListener(v -> showEditDialog(
                "First Name",
                currentUser.getUser_name(),
                "user_name",
                view.findViewById(R.id.userFirstName),
                false,
                null
        ));

        ImageView editLastName = view.findViewById(R.id.editLastName);
        editLastName.setOnClickListener(v -> {
            String currentLastName = currentUser.getUser_last_name() != null ?
                    currentUser.getUser_last_name() : "";
            String dialogTitle = currentLastName.isEmpty() ?
                    "Add Last Name" : "Last Name";
            showEditDialog(
                    dialogTitle,
                    currentLastName,
                    "user_last_name",
                    view.findViewById(R.id.userLastName),
                    false,
                    null
            );
        });

        ImageView editEmail = view.findViewById(R.id.editEmail);
        editEmail.setOnClickListener(v -> showEditDialog(
                "Email",
                currentUser.getUser_email(),
                "user_email",
                view.findViewById(R.id.userEmail),
                true,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        ));

        ImageView editMobile = view.findViewById(R.id.editMobile);
        editMobile.setOnClickListener(v -> showEditDialog(
                "Mobile",
                currentUser.getUser_mobile(),
                "user_mobile",
                view.findViewById(R.id.userMobileNumber),
                true,
                InputType.TYPE_CLASS_PHONE
        ));

        ImageView province = view.findViewById(R.id.editProvince);
        province.setOnClickListener(v -> {
            String currentProvince = currentUser.getProvince() != null ?
                    currentUser.getProvince() : "";
            String dialogTitle = currentProvince.isEmpty() ?
                    "Add User Province" : "User Province";
            showEditDialog(
                    dialogTitle,
                    currentProvince,
                    "province",
                    view.findViewById(R.id.userProvince),
                    false,
                    null
            );
        });

        ImageView district = view.findViewById(R.id.editDistrict);
        district.setOnClickListener(v -> {
            String currentDistrict = currentUser.getDistrict() != null ?
                    currentUser.getDistrict() : "";
            String dialogTitle = currentDistrict.isEmpty() ?
                    "Add User District" : "User District";
            showEditDialog(
                    dialogTitle,
                    currentDistrict,
                    "district",
                    view.findViewById(R.id.userDistrict),
                    false,
                    null
            );
        });

        ImageView city = view.findViewById(R.id.editCity);
        city.setOnClickListener(v -> {
            String currentCity = currentUser.getCity() != null ?
                    currentUser.getCity() : "";
            String dialogTitle = currentCity.isEmpty() ?
                    "Add User City" : "User City";
            showEditDialog(
                    dialogTitle,
                    currentCity,
                    "city",
                    view.findViewById(R.id.userCity),
                    false,
                    null
            );
        });

        ImageView line1 = view.findViewById(R.id.editLine1);
        line1.setOnClickListener(v -> {
            String currentLine1 = currentUser.getLine1() != null ?
                    currentUser.getLine1() : "";
            String dialogTitle = currentLine1.isEmpty() ?
                    "Add User Address Line1" : "User Address Line1";
            showEditDialog(
                    dialogTitle,
                    currentLine1,
                    "line1",
                    view.findViewById(R.id.userLine1),
                    false,
                    null
            );
        });

        ImageView line2 = view.findViewById(R.id.editLine2);
        line2.setOnClickListener(v -> {
            String currentline2 = currentUser.getLine2() != null ?
                    currentUser.getLine2() : "";
            String dialogTitle = currentline2.isEmpty() ?
                    "Add User Address Line2" : "User Address Line2";
            showEditDialog(
                    dialogTitle,
                    currentline2,
                    "line2",
                    view.findViewById(R.id.userLine2),
                    false,
                    null
            );
        });

    }

    private void showEditDialog(String title, String currentValue, String fieldName,
                                TextView targetView, boolean needsValidation, Integer inputType) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.custom_dialog_update_user); // Your custom XML

        // Set dialog title
        TextView dialogTitle = dialog.findViewById(R.id.userSelectUpdateName);
        dialogTitle.setText("Edit " + title);

        // Configure input field
        EditText updateText = dialog.findViewById(R.id.updateText);
        if (inputType != null) {
            updateText.setInputType(inputType);
        }
        updateText.setText(currentValue);
        updateText.setSelection(updateText.getText().length());

        // Button handlers
        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView updateButton = dialog.findViewById(R.id.updateButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            String newValue = updateText.getText().toString().trim();
            if (validateInput(fieldName, newValue, needsValidation)) {
                updateUserField(fieldName, newValue, targetView);
                dialog.dismiss();
            }
        });

        // Dialog window properties
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

    private boolean validateInput(String fieldName, String value, boolean needsValidation) {
        if (value.isEmpty()) {
            Toast.makeText(requireContext(), "Field cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (needsValidation) {
            switch (fieldName) {
                case "user_email":
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                        Toast.makeText(requireContext(), "Invalid email address", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    break;
                case "user_mobile":
                    if (!value.matches("^07[0125678]\\d{7}$")) {
                        Toast.makeText(requireContext(), "Invalid Sri Lankan mobile number", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    private void updateUserField(String fieldName, String newValue, TextView targetView) {
        String userMobile = currentUser.getUser_mobile();

        firestore.collection("clay-bricks-user")
                .whereEqualTo("user_mobile", userMobile)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        firestore.collection("clay-bricks-user")
                                .document(documentId)
                                .update(fieldName, newValue)
                                .addOnSuccessListener(aVoid -> {
                                    updateLocalUserData(fieldName, newValue, targetView);
                                    Toast.makeText(requireContext(), "Update successful!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLocalUserData(String fieldName, String newValue, TextView targetView) {
        if (currentUser == null) return; // Prevent null reference errors

        switch (fieldName) {
            case "user_name":
                currentUser.setUser_name(newValue);
                updateUserNameDisplay(newValue, currentUser.getUser_last_name()); // Pass last name
                break;
            case "user_last_name":
                currentUser.setUser_last_name(newValue);
                updateUserNameDisplay(currentUser.getUser_name(), newValue); // Pass first name
                updateIcon(R.id.editLastName, newValue);
                break;
            case "user_email":
                currentUser.setUser_email(newValue);
                break;
            case "user_mobile":
                currentUser.setUser_mobile(newValue);
                break;
            case "province":
                currentUser.setProvince(newValue);
                updateIcon(R.id.editProvince, newValue);
                break;
            case "district":
                currentUser.setDistrict(newValue);
                updateIcon(R.id.editDistrict, newValue);
                break;
            case "city":
                currentUser.setCity(newValue);
                updateIcon(R.id.editCity, newValue);
                break;
            case "line1":
                currentUser.setLine1(newValue);
                updateIcon(R.id.editLine1, newValue);
                break;
            case "line2":
                currentUser.setLine2(newValue);
                updateIcon(R.id.editLine2, newValue);
                break;
        }

        // Save updated user data to SharedPreferences
        Gson gson = new Gson();
        String userJson = gson.toJson(currentUser);
        userPrefs.edit().putString("user", userJson).apply();

        // Update the target TextView
        if (targetView != null) {
            targetView.setText(newValue);
        }
    }

    // Method to update the username and last name display
    private void updateUserNameDisplay(String firstName, String lastName) {
        View rootView = getView();
        if (rootView != null) {
            TextView userName = rootView.findViewById(R.id.userName);
            TextView userName2 = rootView.findViewById(R.id.userName2);

            String formattedFirstName = (firstName == null || firstName.isEmpty()) ? "User" :
                    firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
            String formattedLastName = (lastName == null || lastName.isEmpty()) ? "" :
                    lastName.substring(0, 1).toUpperCase() + lastName.substring(1);

            if (userName != null) userName.setText(formattedFirstName);
            if (userName2 != null) userName2.setText(formattedLastName);
        }
    }

    // Helper method to update the icons based on empty/non-empty values
    private void updateIcon(int imageViewId, String value) {
        View rootView = getView();
        if (rootView != null) {
            ImageView imageView = rootView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(value.isEmpty() ? R.drawable.add_icon : R.drawable.edit_text);
            }
        }
    }


    private void loadUserData(View view) {
        TextView userNameView = view.findViewById(R.id.userName);
        TextView userName2View = view.findViewById(R.id.userName2);
        TextView userFNameView = view.findViewById(R.id.userFirstName);
        TextView userLNameView = view.findViewById(R.id.userLastName);
        TextView emailView = view.findViewById(R.id.userEmail);
        TextView userMobileView = view.findViewById(R.id.userMobileNumber);
        TextView userProvinceView = view.findViewById(R.id.userProvince);
        TextView userDistrictView = view.findViewById(R.id.userDistrict);
        TextView userCityView = view.findViewById(R.id.userCity);
        TextView userLine1View = view.findViewById(R.id.userLine1);
        TextView userLine2View = view.findViewById(R.id.userLine2);

        String userJson = userPrefs.getString("user", null);
        if (userJson != null) {
            try {
                currentUser = new Gson().fromJson(userJson, User.class);

                if (currentUser != null) {
                    // User Name
                    String userName = currentUser.getUser_name() != null ?
                            currentUser.getUser_name() : "N/A";
                    userName = userName.isEmpty() ? "N/A" :
                            userName.substring(0, 1).toUpperCase() + userName.substring(1);
                    userNameView.setText(userName);
                    String userName2 = currentUser.getUser_last_name() != null ?
                            currentUser.getUser_last_name() : "";
                    userName2 = userName2.isEmpty() ? "" :
                            userName2.substring(0, 1).toUpperCase() + userName2.substring(1);
                    userName2View.setText(userName2);

                    // First Name
                    String firstName = currentUser.getUser_name() != null ?
                            currentUser.getUser_name() : "N/A";
                    userFNameView.setText(firstName);

                    // Last Name
                    String lastName = currentUser.getUser_last_name() != null ?
                            currentUser.getUser_last_name() : "";
                    userLNameView.setText(lastName);

                    ImageView editLastName = view.findViewById(R.id.editLastName);
                    if (lastName.isEmpty()) {
                        editLastName.setImageResource(R.drawable.add_icon);
                    } else {
                        editLastName.setImageResource(R.drawable.edit_text);
                    }

                    // Email
                    String email = currentUser.getUser_email() != null ?
                            currentUser.getUser_email() : "N/A";
                    emailView.setText(email);

                    // Mobile
                    String mobile = currentUser.getUser_mobile() != null ?
                            currentUser.getUser_mobile() : "N/A";
                    userMobileView.setText(mobile);

                    //province
                    String userProvince = currentUser.getProvince() != null ?
                            currentUser.getProvince() : "";
                    userProvinceView.setText(userProvince);

                    ImageView editProvince = view.findViewById(R.id.editProvince);
                    if (userProvince.isEmpty()) {
                        editProvince.setImageResource(R.drawable.add_icon);
                    } else {
                        editProvince.setImageResource(R.drawable.edit_text);
                    }

                    //district
                    String userDistrict = currentUser.getDistrict() != null ?
                            currentUser.getDistrict() : "";
                    userDistrictView.setText(userDistrict);

                    ImageView editDistrict = view.findViewById(R.id.editDistrict);
                    if (userDistrict.isEmpty()) {
                        editDistrict.setImageResource(R.drawable.add_icon);
                    } else {
                        editDistrict.setImageResource(R.drawable.edit_text);
                    }

                    //city
                    String userCity = currentUser.getCity() != null ?
                            currentUser.getCity() : "";
                    userCityView.setText(userCity);

                    ImageView editCity = view.findViewById(R.id.editCity);
                    if (userCity.isEmpty()) {
                        editCity.setImageResource(R.drawable.add_icon);
                    } else {
                        editCity.setImageResource(R.drawable.edit_text);
                    }

                    //line1
                    String userLine1 = currentUser.getLine1() != null ?
                            currentUser.getLine1() : "";
                    userLine1View.setText(userLine1);

                    ImageView editLine1 = view.findViewById(R.id.editLine1);
                    if (userLine1.isEmpty()) {
                        editLine1.setImageResource(R.drawable.add_icon);
                    } else {
                        editLine1.setImageResource(R.drawable.edit_text);
                    }

                    //line2
                    String userLine2 = currentUser.getLine2() != null ?
                            currentUser.getLine2() : "";
                    userLine2View.setText(userLine2);

                    ImageView editLine2 = view.findViewById(R.id.editLine2);
                    if (userLine2.isEmpty()) {
                        editLine2.setImageResource(R.drawable.add_icon);
                    } else {
                        editLine2.setImageResource(R.drawable.edit_text);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                handleInvalidUserData();
            }
        } else {
            handleGuestUser(userFNameView, userLNameView, emailView, userMobileView, userProvinceView, userDistrictView, userCityView, userLine1View, userLine2View);
        }
    }

    private void handleInvalidUserData() {
        userPrefs.edit().clear().apply();
        Toast.makeText(requireContext(), "Invalid user data, please login again", Toast.LENGTH_LONG).show();
//        performLogout();
    }

    private void handleGuestUser(TextView fName, TextView lName, TextView email, TextView mobile, TextView province, TextView district, TextView city, TextView line1, TextView line2) {
        fName.setText("Guest!");
        lName.setText("");
        email.setText("guest@example.com");
        mobile.setText("07- -------");
        province.setText("");
        district.setText("");
        city.setText("");
        line1.setText("");
        line2.setText("");
    }

//    private void setupLogoutButton(View view) {
//        Button logOutButton = view.findViewById(R.id.logOutButton);
//        logOutButton.setOnClickListener(v -> showLogoutConfirmation());
//    }
//
//    private void showLogoutConfirmation() {
//        new AlertDialog.Builder(requireContext())
//                .setTitle("Logout")
//                .setMessage("Are you sure you want to logout?")
//                .setPositiveButton("Yes", (dialog, which) -> performLogout())
//                .setNegativeButton("No", null)
//                .show();
//    }
//
//    private void performLogout() {
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        userPrefs.edit().clear().apply();
//        themePreferences.edit().clear().apply();
//
//        Intent intent = new Intent(requireActivity(), SignInActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//
//        if (getActivity() != null) {
//            getActivity().finish();
//        }
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        themePreferences.edit().remove("fromProfile").apply();
    }
}