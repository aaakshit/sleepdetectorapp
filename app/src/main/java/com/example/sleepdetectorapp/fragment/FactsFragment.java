package com.example.sleepdetectorapp.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import com.example.sleepdetectorapp.R;
import com.example.sleepdetectorapp.activity.NavBar_Activity;


public class FactsFragment extends AppCompatActivity {
    private static final String TAG = "FactsFragment";

    private ImageButton hamburgerImageButton;

    private Animation button_click;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_facts);
        Log.i(TAG,"Created");
        button_click = AnimationUtils.loadAnimation(this,R.anim.button_click);

        hamburgerImageButton = (ImageButton) findViewById(R.id.hamburgerImageButton);

        hamburgerImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hamburgerImageButton.setAnimation(button_click);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(FactsFragment.this, NavBar_Activity.class);
                        startActivity(intent);
                        finish();
                    }
                },200);
            }
        });

    }
}