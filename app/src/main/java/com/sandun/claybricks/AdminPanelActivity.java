package com.sandun.claybricks;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.sandun.claybricks.navigation.AdminManagerFragment;
import com.sandun.claybricks.navigation.CompletedOrdersViewFragment;
import com.sandun.claybricks.navigation.DashboardFragment;
import com.sandun.claybricks.navigation.DeliveryManagerFragment;
import com.sandun.claybricks.navigation.OrderManagerFragment;
import com.sandun.claybricks.navigation.ProductAddFragment;
import com.sandun.claybricks.navigation.ProductViewFragment;
import com.sandun.claybricks.navigation.UserManegerFragment;

public class AdminPanelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_panel);

        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        String adminName = prefs.getString("admin_name", "Admin Name");
        String adminEmail = prefs.getString("admin_email", "admin@example.com");

        // Update navigation header
        NavigationView navigationView11 = findViewById(R.id.navigationView1);
        View headerView = navigationView11.getHeaderView(0);

        TextView tvAdminName = headerView.findViewById(R.id.adminName);
        TextView tvAdminEmail = headerView.findViewById(R.id.adminEmail);

        tvAdminName.setText(adminName);
        tvAdminEmail.setText(adminEmail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout1), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Find views
        DrawerLayout drawerLayout1 = findViewById(R.id.drawerLayout1);
        TextView toolbarText = findViewById(R.id.toolbarText);
        ImageView navSelector = findViewById(R.id.nav_selector);
        NavigationView navigationView1 = findViewById(R.id.navigationView1);

        // Load DashboardFragment by default
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            toolbarText.setText("Dashboard");
            navigationView1.setCheckedItem(R.id.nav_dashboard);
        }

        // Open Navigation Drawer on Image Click
        navSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout1.openDrawer(GravityCompat.START);

            }
        });
        navigationView1.setNavigationItemSelectedListener(item -> {
            drawerLayout1.closeDrawer(GravityCompat.START);
            return true;
        });

        // Handle Navigation Item Clicks
        navigationView1.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_dashboard) {
                    loadFragment(new DashboardFragment());
                } else if (item.getItemId() == R.id.nav_admin_manager) {
                    loadFragment(new AdminManagerFragment());
                } else if (item.getItemId() == R.id.nav_user_manager) {
                    loadFragment(new UserManegerFragment());
                } else if (item.getItemId() == R.id.nav_product_add) {
                    loadFragment(new ProductAddFragment());
                } else if (item.getItemId() == R.id.nav_product_view) {
                    loadFragment(new ProductViewFragment());
                } else if (item.getItemId() == R.id.nav_order_manager) {
                    loadFragment(new OrderManagerFragment());
                } else if (item.getItemId() == R.id.nav_delivery_manager) {
                    loadFragment(new DeliveryManagerFragment());
                } else if (item.getItemId() == R.id.nav_completed_orders) {
                    loadFragment(new CompletedOrdersViewFragment());
                }
                toolbarText.setText(item.getTitle());
                drawerLayout1.closeDrawers();

                return true;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container_view1, fragment, null)
                .setReorderingAllowed(true)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Exit Admin Panel")
                .setMessage("Do you want to exit the admin panel?")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Clear SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();

                    // Go to SignActivity
                    Intent intent = new Intent(AdminPanelActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
