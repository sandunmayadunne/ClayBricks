package com.sandun.claybricks;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sandun.claybricks.clientFragment.UserHomeFragment;
import com.sandun.claybricks.clientFragment.ProductFragment;
import com.sandun.claybricks.clientFragment.ProfileFragment;

public class HomeActivity extends AppCompatActivity {

    private static final String CURRENT_FRAGMENT_TAG = "current_fragment";
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Apply edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        SharedPreferences prefs = getSharedPreferences("TheamMode", MODE_PRIVATE);

        // Check if we need to restore ProfileFragment after theme change
        if (savedInstanceState == null) {
            handleInitialFragment(prefs.getBoolean("fromProfile", false));
        }

        setupNavigationListener();
    }

    private void handleInitialFragment(boolean fromProfile) {
        Fragment initialFragment = fromProfile ? new ProfileFragment() : new UserHomeFragment();
        int selectedItem = fromProfile ? R.id.nav_profile : R.id.nav_home;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commit();

        bottomNavigationView.setSelectedItemId(selectedItem);
        clearProfileFlagIfNeeded(fromProfile);
    }

    private void clearProfileFlagIfNeeded(boolean fromProfile) {
        if (fromProfile) {
            getSharedPreferences("TheamMode", MODE_PRIVATE)
                    .edit()
                    .remove("fromProfile")
                    .apply();
        }
    }

    private void setupNavigationListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new UserHomeFragment();
            } else if (itemId == R.id.nav_product) {
                selectedFragment = new ProductFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}