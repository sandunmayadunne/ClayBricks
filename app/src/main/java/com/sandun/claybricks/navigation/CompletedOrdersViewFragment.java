package com.sandun.claybricks.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sandun.claybricks.R;
import com.sandun.claybricks.adapters.CompletedOrdersAdapter;
import com.sandun.claybricks.model.CompletedOrder;

import java.util.ArrayList;
import java.util.List;

public class CompletedOrdersViewFragment extends Fragment {

    private RecyclerView recyclerView;
    private CompletedOrdersAdapter adapter;
    private final List<CompletedOrder> ordersList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_completed_orders_view, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.completedOrdersViewRecycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CompletedOrdersAdapter(ordersList);
        recyclerView.setAdapter(adapter);

        loadCompletedOrders();

        return view;
    }

    private void loadCompletedOrders() {
        db.collection("completed-deliveries")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ordersList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CompletedOrder order = document.toObject(CompletedOrder.class);
                            ordersList.add(order);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}