package com.sandun.claybricks.navigation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sandun.claybricks.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderManagerFragment extends Fragment {

    private RecyclerView ordersRecyclerView;
    private OrdersAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_manager, container, false);

        if (view != null) {
            ordersRecyclerView = view.findViewById(R.id.allOrdersRecycleView);
            if (ordersRecyclerView != null) {
                ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                adapter = new OrdersAdapter(allOrders);
                adapter.setOnOrderLongClickListener((order, position) -> showDeleteDialog(order, position));
                ordersRecyclerView.setAdapter(adapter);
//                loadAllOrders();
            }
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllOrders();
    }

    private void loadAllOrders() {
        db.collection("clay-bricks-order")
                .get()
                .addOnCompleteListener(task -> {
                    if (getActivity() == null) return;

                    if (task.isSuccessful() && task.getResult() != null) {
                        allOrders.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Order order = document.toObject(Order.class);
                                order.setOrderId(document.getId());
                                allOrders.add(order);
                            } catch (Exception e) {
                                Log.e("OrderManager", "Error parsing order: " + document.getId(), e);
                                Toast.makeText(getContext(), "Error loading order: " + document.getId(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (allOrders.isEmpty()) {
                            Toast.makeText(getContext(), "No orders found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading orders", Toast.LENGTH_SHORT).show();
                        if (task.getException() != null) {
                            Log.e("OrderManager", "Error loading orders", task.getException());
                        }
                    }
                });
    }

    private void showDeleteDialog(Order order, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes", (dialog, which) -> deleteOrder(order, position))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteOrder(Order order, int position) {
        String orderId = order.getOrderId();
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Order ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("clay-bricks-order").document(orderId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    allOrders.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Order cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show();
                    Log.e("OrderManager", "Error deleting order", e);
                });
    }

    private static class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
        private final List<Order> allOrders;
        private OnOrderLongClickListener longClickListener;

        public OrdersAdapter(List<Order> allOrders) {
            this.allOrders = allOrders;
        }

        public void setOnOrderLongClickListener(OnOrderLongClickListener listener) {
            this.longClickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_manager, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Order order = allOrders.get(position);
            holder.orderId.setText(order.getOrderId() != null ? "Order ID: " + order.getOrderId() : "N/A");
            holder.orderStatus.setText(order.getStatus() != null ? "Status: " + order.getStatus() : "N/A");
            holder.orderDate.setText("Date: " + formatDate(order.getOrderDate()));

            holder.itemView.setOnClickListener(v -> {
                Context context = holder.itemView.getContext();
                Intent intent = new Intent(context, OrderViewActivity.class);
                intent.putExtra("ORDER_ID", order.getOrderId());
                context.startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onOrderLongClick(order, position);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return allOrders.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderDate;

            ViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.tvOrderId);
                orderStatus = itemView.findViewById(R.id.tvOrderStatus);
                orderDate = itemView.findViewById(R.id.tvOrderDate);
            }
        }

        private String formatDate(Date date) {
            if (date == null) return "Date not available";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(date);
        }

        interface OnOrderLongClickListener {
            void onOrderLongClick(Order order, int position);
        }
    }

    private String formatDate(Date date) {
        if (date == null) return "Date not available";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            Log.e("OrderManager", "Error formatting date", e);
            return "Date error";
        }
    }

    public static class Order {
        private String orderId;
        private String status;
        private Date orderDate;

        public Order() {}

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Date getOrderDate() { return orderDate; }
        public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    }
}