package com.sandun.claybricks.adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.sandun.claybricks.R;
import com.sandun.claybricks.model.CompletedOrder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompletedOrdersAdapter extends RecyclerView.Adapter<CompletedOrdersAdapter.ViewHolder> {

    private final List<CompletedOrder> ordersList;

    public CompletedOrdersAdapter(List<CompletedOrder> ordersList) {
        this.ordersList = ordersList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompletedOrder order = ordersList.get(position);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        String deliveryDate = sdf.format(order.getDeliveryDate().toDate());

        holder.orderId.setText(order.getOrderId());
        holder.deliveryDate.setText(deliveryDate);
        holder.totalPrice.setText("Total: LKR " + order.getTotalPrice());
        holder.userName.setText("Customer: " + order.getUserName());

        // Build items string
        StringBuilder itemsBuilder = new StringBuilder();
        for (Map<String, Object> item : order.getItems()) {
            String productName = (String) item.get("productName");
            int quantity = ((Long) item.get("quantity")).intValue();
            String price = (String) item.get("price");
            itemsBuilder.append(productName)
                    .append(" x").append(quantity)
                    .append(" (LKR ").append(price).append(")\n");
        }
        holder.itemsList.setText(itemsBuilder.toString().trim());
    }

    @Override
    public int getItemCount() {
        return ordersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, deliveryDate, totalPrice, itemsList, userName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.orderId);
            deliveryDate = itemView.findViewById(R.id.deliveryDate);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            itemsList = itemView.findViewById(R.id.itemsList);
            userName = itemView.findViewById(R.id.userName);
        }
    }
}