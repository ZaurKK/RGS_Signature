package com.zaurkandokhov.signature;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000;

    public static SharedPreferences sharedPreferences;
    public static final String MY_PREF = "MY_PREF";
    public static final String NAME_KEY = "NAME_KEY";
    public static final String EMAIL_KEY = "EMAIL_KEY";
    public static final String PASSWORD_KEY = "PASSWORD_KEY";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Context mContext = this;

        sharedPreferences = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);

        ImageView logoImg = findViewById(R.id.company_logo);
        TextView textView = findViewById(R.id.splash_text);

        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "Exo2-Light.otf");
        textView.setTypeface(typeface);

        AlphaAnimation animation = new AlphaAnimation(0.2f, 1.0f);
        animation.setDuration(SPLASH_TIME_OUT);
        animation.setStartOffset(100);
        animation.setFillAfter(true);
        logoImg.startAnimation(animation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                if (!sharedPreferences.contains(EMAIL_KEY))
//                    startActivity(LoginActivity.class);
//                else
//                startActivity(Signature.class);
                //startActivity(Signature.class);
                startActivity(SettingsActivity.class);
            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    public void onBackPressed() {
    }

    public void startActivity(Class<?> activityClass) {
        Intent i = new Intent(SplashActivity.this, activityClass);
        startActivity(i);
        finish();
    }
}