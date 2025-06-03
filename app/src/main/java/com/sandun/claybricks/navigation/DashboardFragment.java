package com.sandun.claybricks.navigation;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sandun.claybricks.R;
import com.sandun.claybricks.model.Order;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Locale;
import com.google.firebase.Timestamp;

public class DashboardFragment extends Fragment {

    private RecyclerView ordersRecyclerView;
    private PendingOrdersAdapter adapter;
    private List<Order> pendingOrders = new ArrayList<>();
    private FirebaseFirestore db;
    private PieChart pieChartAdmin, pieChartUsers;
    private BarChart barChartWeeklyOrders;
    private Typeface oswaldBold;

    private TextView tvLiveDateCounter;
    private Handler handler = new Handler();
    private Runnable updateTimer;
    private SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
    private Date targetDate;
    private TextView tvTotalProducts, tvProductTypes, tvTotalPayments;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = FirebaseFirestore.getInstance();
        oswaldBold = getResources().getFont(R.font.oswald_bold);

        tvLiveDateCounter = view.findViewById(R.id.tvLiveDateCounter);
        tvTotalProducts = view.findViewById(R.id.tvTotalProducts);
        tvProductTypes = view.findViewById(R.id.tvProductTypes);
        tvTotalPayments = view.findViewById(R.id.tvTotalPayments);

        try {
            targetDate = targetDateFormat.parse("2025/04/01 00:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (view != null) {
            // Initialize Admin PieChart
            pieChartAdmin = view.findViewById(R.id.pieChart);
            configurePieChart(pieChartAdmin);

            // Initialize Users PieChart
            pieChartUsers = view.findViewById(R.id.pieChartUsers);
            configurePieChart(pieChartUsers);

            // Initialize BarChart
            barChartWeeklyOrders = view.findViewById(R.id.barChartWeeklyOrders);
            configureBarChart();

            // Initialize RecyclerView
            ordersRecyclerView = view.findViewById(R.id.recyclerView);
            if (ordersRecyclerView != null) {
                ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                adapter = new PendingOrdersAdapter();
                ordersRecyclerView.setAdapter(adapter);
                loadPendingOrders();
            }

            // Load statistics
            loadAdminStatistics();
            loadUserStatistics();
            loadWeeklyCompletedOrders();

            loadTotalProducts();
            loadProductTypes();
            loadTotalPayments();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startLiveCounter();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLiveCounter();
    }

    private void startLiveCounter() {
        updateTimer = new Runnable() {
            @Override
            public void run() {
                if (targetDate != null) {
                    long currentTime = System.currentTimeMillis();
                    long difference = currentTime - targetDate.getTime();

                    // Convert milliseconds to time units
                    long seconds = Math.abs(difference / 1000);
                    long days = seconds / 86400;
                    seconds -= days * 86400;
                    long hours = seconds / 3600;
                    seconds -= hours * 3600;
                    long minutes = seconds / 60;
                    seconds -= minutes * 60;

                    String counterText = String.format(Locale.getDefault(),
                            "Running Time:\n%d Days %02d:%02d:%02d",
                            days, hours, minutes, seconds);

                    tvLiveDateCounter.setText(counterText);
                }

                // Update every second
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateTimer);
    }

    private void stopLiveCounter() {
        if (handler != null && updateTimer != null) {
            handler.removeCallbacks(updateTimer);
        }
    }

    private void loadTotalProducts() {
        db.collection("clay-bricks-product")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        tvTotalProducts.setText(String.valueOf(count));
                    } else {
                        showToast("Failed to load products");
                    }
                });
    }

    private void loadProductTypes() {
        db.collection("clay-bricks-type")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        tvProductTypes.setText(String.valueOf(count));
                    } else {
                        showToast("Failed to load product types");
                    }
                });
    }

    private void loadTotalPayments() {
        db.collection("completed-deliveries")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double total = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String paymentStr = doc.getString("totalPrice");

                            if (paymentStr != null && !paymentStr.isEmpty()) {
                                try {
                                    String cleanedPayment = paymentStr.replaceAll("[^\\d.]", "");
                                    double payment = Double.parseDouble(cleanedPayment);
                                    total += payment;
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        tvTotalPayments.setText(String.format("Rs.%.2f", total));
                    } else {
                        showToast("Failed to load payments");
                    }
                });
    }

    private void configurePieChart(PieChart chart) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(5, 10, 5, 5);
        chart.setDragDecelerationFrictionCoef(0.95f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(android.R.color.transparent);
        chart.setTransparentCircleRadius(61f);
        chart.setEntryLabelColor(getResources().getColor(android.R.color.black));
        chart.setEntryLabelTextSize(12f);
        chart.setEntryLabelTypeface(oswaldBold);
        chart.getLegend().setTypeface(oswaldBold);
    }

    private void loadAdminStatistics() {
        db.collection("clay-bricks-admin")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int activeCount = 0;
                        int deactiveCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Long status = document.getLong("admin_status");
                            if (status != null) {
                                if (status == 1) activeCount++;
                                else if (status == 2) deactiveCount++;
                            }
                        }

                        updateAdminPieChart(activeCount, deactiveCount);
                    } else {
                        showToast("Failed to load admin statistics");
                    }
                })
                .addOnFailureListener(e -> showToast("Error: " + e.getMessage()));
    }

    private void loadUserStatistics() {
        db.collection("clay-bricks-user")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int activeCount = 0;
                        int deactiveCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Long status = document.getLong("status");
                            if (status != null) {
                                if (status == 1) activeCount++;
                                else if (status == 2) deactiveCount++;
                            }
                        }

                        updateUserPieChart(activeCount, deactiveCount);
                    } else {
                        showToast("Failed to load user statistics");
                    }
                })
                .addOnFailureListener(e -> showToast("Error: " + e.getMessage()));
    }

    private void configureBarChart() {
        barChartWeeklyOrders.getDescription().setEnabled(false);
        barChartWeeklyOrders.setDragEnabled(true);
        barChartWeeklyOrders.setScaleEnabled(true);
        barChartWeeklyOrders.setPinchZoom(true);
        barChartWeeklyOrders.setDrawGridBackground(false);
        barChartWeeklyOrders.getLegend().setTypeface(oswaldBold);

        XAxis xAxis = barChartWeeklyOrders.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTypeface(oswaldBold);
        xAxis.setLabelCount(6);

        YAxis leftAxis = barChartWeeklyOrders.getAxisLeft();
        leftAxis.setTypeface(oswaldBold);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Y-axis shows integers
            }
        });

        barChartWeeklyOrders.getAxisRight().setEnabled(false);
    }

    private void loadWeeklyCompletedOrders() {
        int[] dailyCounts = new int[7]; // Index 0: 6 days ago, ..., 6: today

        // Calculate 7 days ago at 00:00:00
        Calendar sevenDaysAgoCal = Calendar.getInstance();
        sevenDaysAgoCal.add(Calendar.DAY_OF_YEAR, -6);
        sevenDaysAgoCal.set(Calendar.HOUR_OF_DAY, 0);
        sevenDaysAgoCal.set(Calendar.MINUTE, 0);
        sevenDaysAgoCal.set(Calendar.SECOND, 0);
        sevenDaysAgoCal.set(Calendar.MILLISECOND, 0);
        Timestamp sevenDaysAgoTimestamp = new Timestamp(sevenDaysAgoCal.getTime());

        db.collection("completed-deliveries")
                .whereGreaterThanOrEqualTo("deliveryDate", sevenDaysAgoTimestamp)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Calendar todayCal = Calendar.getInstance();
                        todayCal.set(Calendar.HOUR_OF_DAY, 0);
                        todayCal.set(Calendar.MINUTE, 0);
                        todayCal.set(Calendar.SECOND, 0);
                        todayCal.set(Calendar.MILLISECOND, 0);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Timestamp deliveryTimestamp = document.getTimestamp("deliveryDate");
                            if (deliveryTimestamp != null) {
                                Date deliveryDate = deliveryTimestamp.toDate();
                                Calendar deliveryCal = Calendar.getInstance();
                                deliveryCal.setTime(deliveryDate);
                                deliveryCal.set(Calendar.HOUR_OF_DAY, 0);
                                deliveryCal.set(Calendar.MINUTE, 0);
                                deliveryCal.set(Calendar.SECOND, 0);
                                deliveryCal.set(Calendar.MILLISECOND, 0);

                                long diffMillis = todayCal.getTimeInMillis() - deliveryCal.getTimeInMillis();
                                long daysDiff = diffMillis / (24 * 60 * 60 * 1000);

                                if (daysDiff >= 0 && daysDiff < 7) {
                                    int dayIndex = 6 - (int) daysDiff;
                                    dailyCounts[dayIndex]++;
                                }
                            }
                        }

                        // Generate date labels (dd/MM format)
                        String[] dailyLabels = new String[7];
                        Calendar labelCal = Calendar.getInstance();
                        labelCal.add(Calendar.DAY_OF_YEAR, -6); // Start from 6 days ago
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                        for (int i = 0; i < 7; i++) {
                            dailyLabels[i] = sdf.format(labelCal.getTime());
                            labelCal.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        updateDailyOrdersChart(dailyCounts, dailyLabels);
                    }
                })
                .addOnFailureListener(e -> showToast("Error loading orders: " + e.getMessage()));
    }

    private void updateDailyOrdersChart(int[] dailyCounts, String[] dailyLabels) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dailyCounts.length; i++) {
            entries.add(new BarEntry(i, dailyCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Orders");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black));
        dataSet.setValueTypeface(oswaldBold);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Show integer values
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        XAxis xAxis = barChartWeeklyOrders.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dailyLabels));
        xAxis.setLabelCount(7);

        YAxis leftAxis = barChartWeeklyOrders.getAxisLeft();
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Ensure Y-axis labels are integers
            }
        });

        barChartWeeklyOrders.setData(barData);
        barChartWeeklyOrders.setFitBars(true);
        barChartWeeklyOrders.animateY(1000);
        barChartWeeklyOrders.invalidate();
    }

    private void updateAdminPieChart(int active, int deactive) {
        List<PieEntry> entries = new ArrayList<>();

        if (active > 0) entries.add(new PieEntry(active, "Active"));
        if (deactive > 0) entries.add(new PieEntry(deactive, "Deactivated"));

        if (entries.isEmpty()) {
            pieChartAdmin.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Admin Status");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(new int[] {
                getResources().getColor(R.color.active_green),
                getResources().getColor(R.color.themColour1)
        });

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartAdmin));
        data.setValueTextSize(11f);
        data.setValueTypeface(oswaldBold);
        data.setValueTextColor(getResources().getColor(android.R.color.black));

        pieChartAdmin.setData(data);
        pieChartAdmin.setCenterTextTypeface(oswaldBold);
        pieChartAdmin.setCenterText("Admins\n" + (active + deactive));
        pieChartAdmin.setCenterTextSize(14f);
        pieChartAdmin.animateY(1000);
        pieChartAdmin.invalidate();
    }

    private void updateUserPieChart(int active, int deactive) {
        List<PieEntry> entries = new ArrayList<>();

        if (active > 0) entries.add(new PieEntry(active, "Active"));
        if (deactive > 0) entries.add(new PieEntry(deactive, "Inactive"));

        if (entries.isEmpty()) {
            pieChartUsers.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "User Status");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(new int[] {
                getResources().getColor(R.color.user_active),
                getResources().getColor(R.color.user_inactive)
        });

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartUsers));
        data.setValueTextSize(11f);
        data.setValueTypeface(oswaldBold);
        data.setValueTextColor(getResources().getColor(android.R.color.black));

        pieChartUsers.setData(data);
        pieChartUsers.setCenterTextTypeface(oswaldBold);
        pieChartUsers.setCenterText("Users\n" + (active + deactive));
        pieChartUsers.setCenterTextSize(14f);
        pieChartUsers.animateY(1000);
        pieChartUsers.invalidate();
    }

    private void loadPendingOrders() {
        db.collection("clay-bricks-order")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        pendingOrders.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Order order = document.toObject(Order.class);
                                if (order != null) {
                                    pendingOrders.add(order);
                                }
                            } catch (Exception e) {
                                showToast("Error parsing order data");
                            }
                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        showToast("Failed to load orders");
                    }
                })
                .addOnFailureListener(e -> showToast("Network error: " + e.getMessage()));
    }

    private class PendingOrdersAdapter extends RecyclerView.Adapter<PendingOrdersAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_order, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < pendingOrders.size()) {
                Order order = pendingOrders.get(position);
                if (order != null) {
                    holder.orderId.setText(order.getOrderId() != null ? "Order ID: " + order.getOrderId() : "N/A");
                    holder.userMobile.setText(order.getUserMobile() != null ? "Order User Mobile: " + order.getUserMobile() : "N/A");
                }
            }
        }

        @Override
        public int getItemCount() {
            return pendingOrders.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, userMobile;

            ViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.tvOrderId);
                userMobile = itemView.findViewById(R.id.tvUserMobile);
            }
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}