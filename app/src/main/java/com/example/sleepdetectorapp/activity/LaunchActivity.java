package com.example.sleepdetectorapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sleepdetectorapp.R;
import com.example.sleepdetectorapp.fragment.HomeFragment;

public class LaunchActivity extends AppCompatActivity {
    private static final String TAG = "LaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Log.i(TAG,"Created");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "TIGHTEN YOUR SEAT BELTS", Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Resumed");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"Moving to MainActivity");
                Intent intent = new Intent(LaunchActivity.this, HomeFragment.class);
                startActivity(intent);
                finish();
            }
        },4000);
    }

}