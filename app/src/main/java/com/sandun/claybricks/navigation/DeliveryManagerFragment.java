package com.sandun.claybricks.navigation;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sandun.claybricks.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeliveryManagerFragment extends Fragment {

    private RecyclerView deliveryRecyclerView;
    private DeliveriesAdapter adapter;
    private List<Delivery> allDeliveries = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvCurrentPrice;
    private ImageView updateDeliveryPrice;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_manager, container, false);

        // Initialize views
        tvCurrentPrice = view.findViewById(R.id.textView61);
        updateDeliveryPrice = view.findViewById(R.id.updateDeliveryPrice);

        // Set click listener for price update
        updateDeliveryPrice.setOnClickListener(v -> showUpdateDialog());

        // Load current delivery price
        loadDeliveryPrice();

        // Existing delivery list setup
        deliveryRecyclerView = view.findViewById(R.id.deliverManagerRecycleView);
        deliveryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeliveriesAdapter(allDeliveries);
        adapter.setOnDeliveryLongClickListener((delivery, position) -> showDeleteDialog(delivery, position));
        deliveryRecyclerView.setAdapter(adapter);

        loadAllDeliveries();

        return view;
    }

    private void loadDeliveryPrice() {
        db.collection("clay-bricks-delivery-price")
                .document("current_price")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double price = documentSnapshot.getDouble("pricePerKm");
                        tvCurrentPrice.setText(String.format("Current Delivery Price: Rs.%.2f/km", price));
                    } else {
                        tvCurrentPrice.setText("Delivery price not set");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading delivery price", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUpdateDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.custom_delivery_price_dialog);

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView currentPriceText = dialog.findViewById(R.id.currentPriceText);
        EditText newPriceInput = dialog.findViewById(R.id.newPriceInput);
        Button saveButton = dialog.findViewById(R.id.saveButton);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);

        dialogTitle.setText("Update Delivery Price");
        newPriceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Load current price
        db.collection("clay-bricks-delivery-price")
                .document("current_price")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double currentPrice = documentSnapshot.getDouble("pricePerKm");
                        currentPriceText.setText(String.format("Current Price: Rs.%.2f/km", currentPrice));
                    }
                });

        saveButton.setOnClickListener(v -> {
            String newPriceStr = newPriceInput.getText().toString().trim();
            if (validatePrice(newPriceStr)) {
                updatePriceInFirestore(Double.parseDouble(newPriceStr), dialog);
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Set dialog dimensions
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(R.drawable.rounded_corners);
        }

        dialog.show();
    }

    private boolean validatePrice(String priceStr) {
        if (priceStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a price", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                Toast.makeText(getContext(), "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid price format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void updatePriceInFirestore(double newPrice, Dialog dialog) {
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("pricePerKm", newPrice);
        priceData.put("updatedAt", new Date());

        db.collection("clay-bricks-delivery-price")
                .document("current_price")
                .set(priceData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Price updated successfully", Toast.LENGTH_SHORT).show();
                    loadDeliveryPrice();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update price: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllDeliveries() {
        db.collection("clay-bricks-delivery-order")
                .get()
                .addOnCompleteListener(task -> {
                    if (getActivity() == null) return;

                    if (task.isSuccessful() && task.getResult() != null) {
                        allDeliveries.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Delivery delivery = document.toObject(Delivery.class);
                                delivery.setDeliveryId(document.getId());

                                // Add location handling
                                if (document.contains("deliveryLocation")) {
                                    delivery.setDeliveryLocation(document.getGeoPoint("deliveryLocation"));
                                }

                                // Handle phone number
                                if (document.contains("userMobile")) {
                                    delivery.setPhoneNumber(document.getString("userMobile"));
                                }

                                // Handle payment date
                                if (document.get("paymentDate") != null) {
                                    delivery.setPaymentDate(document.getDate("paymentDate"));
                                }

                                allDeliveries.add(delivery);
                            } catch (Exception e) {
                                Log.e("DeliveryManager", "Error parsing delivery: " + document.getId(), e);
                                Toast.makeText(getContext(), "Error loading delivery: " + document.getId(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (allDeliveries.isEmpty()) {
                            Toast.makeText(getContext(), "No deliveries found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading deliveries", Toast.LENGTH_SHORT).show();
                        if (task.getException() != null) {
                            Log.e("DeliveryManager", "Error loading deliveries", task.getException());
                        }
                    }
                });
    }

    private void showDeleteDialog(Delivery delivery, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Delivery")
                .setMessage("Are you sure you want to delete this delivery record?")
                .setPositiveButton("Yes", (dialog, which) -> deleteDelivery(delivery, position))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteDelivery(Delivery delivery, int position) {
        String deliveryId = delivery.getDeliveryId();
        if (deliveryId == null || deliveryId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Delivery ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("clay-bricks-delivery-order").document(deliveryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    allDeliveries.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Delivery deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete delivery", Toast.LENGTH_SHORT).show();
                    Log.e("DeliveryManager", "Error deleting delivery", e);
                });
    }

    private static class DeliveriesAdapter extends RecyclerView.Adapter<DeliveriesAdapter.ViewHolder> {
        private final List<Delivery> allDeliveries;
        private OnDeliveryLongClickListener longClickListener;


        public DeliveriesAdapter(List<Delivery> allDeliveries) {
            this.allDeliveries = allDeliveries;
        }

        public void setOnDeliveryLongClickListener(OnDeliveryLongClickListener listener) {
            this.longClickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_delivery_manager, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Delivery delivery = allDeliveries.get(position);
            holder.deliveryId.setText(delivery.getDeliveryId() != null ? "Delivery ID: " + delivery.getDeliveryId() : "N/A");
            holder.deliveryStatus.setText(delivery.getStatus() != null ? "Status: " + delivery.getStatus() : "N/A");
            holder.deliveryDate.setText("Payment Date: " + formatDate(delivery.getPaymentDate()));
            holder.deliveryPhone.setText("Contact: " + (delivery.getPhoneNumber() != null ? delivery.getPhoneNumber() : "N/A"));

            // Handle Deliver Done button
            holder.btnDeliverDone.setOnClickListener(v -> {
                updateDeliveryStatus(holder, delivery, position);
            });

            if ("completed".equals(delivery.getStatus())) {
                holder.btnDeliverDone.setVisibility(View.GONE);
            } else {
                holder.btnDeliverDone.setVisibility(View.VISIBLE);
            }

            // Handle Call User button
            holder.btnCallUser.setOnClickListener(v -> {
                String phone = delivery.getPhoneNumber();
                if (phone != null && !phone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    holder.itemView.getContext().startActivity(intent);
                } else {
                    Toast.makeText(holder.itemView.getContext(),
                            "Phone number not available", Toast.LENGTH_SHORT).show();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onDeliveryLongClick(delivery, position);
                }
                return true;
            });

            holder.itemView.setOnClickListener(v -> {
                Context context = holder.itemView.getContext();
                Intent intent = new Intent(context, DeliveryMapActivity.class);
                intent.putExtra("DELIVERY_ID", delivery.getDeliveryId());
                context.startActivity(intent);
            });
        }

        private void updateDeliveryStatus(ViewHolder holder, Delivery delivery, int position) {
            // Show confirmation dialog
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Complete Delivery")
                    .setMessage("Are you sure you want to mark this delivery as completed?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        proceedWithDeliveryCompletion(holder, delivery, position);
                    })
                    .setNegativeButton("No", null)
                    .show();
        }

        private void proceedWithDeliveryCompletion(ViewHolder holder, Delivery delivery, int position) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String deliveryId = delivery.getDeliveryId();

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "completed");
            updates.put("deliveryDate", new Date());

            db.collection("clay-bricks-delivery-order").document(deliveryId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Update local delivery status and date
                        delivery.setStatus("completed");
                        delivery.setDeliveryDate(new Date());
                        // Refresh the item
                        notifyItemChanged(position);
                        Toast.makeText(holder.itemView.getContext(),
                                "Delivery marked as completed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(),
                                "Error updating delivery", Toast.LENGTH_SHORT).show();
                        Log.e("DeliveryAdapter", "Error updating status", e);
                    });
        }


        @Override
        public int getItemCount() {
            return allDeliveries.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView deliveryId, deliveryStatus, deliveryDate, deliveryPhone;
            Button btnDeliverDone, btnCallUser;

            ViewHolder(View itemView) {
                super(itemView);
                deliveryId = itemView.findViewById(R.id.tvDeliveryId);
                deliveryStatus = itemView.findViewById(R.id.tvDeliveryStatus);
                deliveryDate = itemView.findViewById(R.id.tvDeliveryDate);
                deliveryPhone = itemView.findViewById(R.id.tvDeliveryPhone);
                btnDeliverDone = itemView.findViewById(R.id.btnDeliverDone);
                btnCallUser = itemView.findViewById(R.id.btnCallUser);

                // Set font family for text views
                setOswaldBoldFont(deliveryId);
                setOswaldBoldFont(deliveryStatus);
                setOswaldBoldFont(deliveryDate);
                setOswaldBoldFont(deliveryPhone);
                setOswaldBoldFont(btnDeliverDone);
                setOswaldBoldFont(btnCallUser);
            }

            private void setOswaldBoldFont(TextView textView) {
                try {
                    Typeface typeface = ResourcesCompat.getFont(itemView.getContext(), R.font.oswald_bold);
                    textView.setTypeface(typeface);
                } catch (Exception e) {
                    Log.e("FontError", "Error setting font", e);
                }
            }
        }

        private String formatDate(Date date) {
            if (date == null) return "Date not available";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(date);
        }

        interface OnDeliveryLongClickListener {
            void onDeliveryLongClick(Delivery delivery, int position);
        }
    }

    public static class Delivery {
        private String deliveryId;
        private String status;
        private Date deliveryDate;
        private Date paymentDate;
        private String phoneNumber;
        private GeoPoint deliveryLocation;

        public Delivery() {}

        public String getDeliveryId() { return deliveryId; }
        public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Date getDeliveryDate() { return deliveryDate; }
        public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }
        public Date getPaymentDate() { return paymentDate; }
        public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public GeoPoint getDeliveryLocation() { return deliveryLocation; }
        public void setDeliveryLocation(GeoPoint deliveryLocation) { this.deliveryLocation = deliveryLocation; }
    }
}