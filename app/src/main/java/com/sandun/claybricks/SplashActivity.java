package com.sandun.claybricks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load the last saved theme
        SharedPreferences sharedTheamMode = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        boolean nightMode = sharedTheamMode.getBoolean("night", false);

        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Window splshWindow = this.getWindow();
        splshWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        splshWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        splshWindow.setNavigationBarColor(getResources().getColor(R.color.themColour1));

        ImageView logoImageView = findViewById(R.id.logoimageview);
        SpringAnimation springAnimation = new SpringAnimation(logoImageView, DynamicAnimation.TRANSLATION_Y);

        SpringForce springForce = new SpringForce();
        springForce.setStiffness(SpringForce.STIFFNESS_VERY_LOW);
        springForce.setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY);
        springForce.setFinalPosition(200f);

        springAnimation.setSpring(springForce);
        springAnimation.start();

        new Handler().postDelayed(() -> {
            // Check if the user is already signed in
            SharedPreferences sp = getSharedPreferences("com.sandun.claybricks.data", Context.MODE_PRIVATE);
            String userJson = sp.getString("user", null);

            Intent intent;
            if (userJson != null) {
                // User is signed in, go to HomeActivity
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                // User is not signed in, go to SignInActivity
                intent = new Intent(SplashActivity.this, SignInActivity.class);
            }

            startActivity(intent);
            finish(); // Close the SplashActivity

        }, 5000);
    }
}
