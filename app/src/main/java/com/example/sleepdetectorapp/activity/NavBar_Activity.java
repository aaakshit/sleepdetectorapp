package com.example.sleepdetectorapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sleepdetectorapp.R;
import com.example.sleepdetectorapp.fragment.AboutFragment;
import com.example.sleepdetectorapp.fragment.FactsFragment;
import com.example.sleepdetectorapp.fragment.HomeFragment;

public class NavBar_Activity extends AppCompatActivity {
    private static final String TAG = "NavBarActivity";
    private TextView home;
    private TextView about_us;
    private TextView facts;
    private TextView nmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navbar_layout);
        Log.i(TAG,"Created");
        home = (TextView) findViewById(R.id.home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavBar_Activity.this,HomeFragment.class);
                startActivity(intent);
                finish();
            }
        });
        facts = (TextView) findViewById(R.id.facts);
        facts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavBar_Activity.this, FactsFragment.class);
                startActivity(intent);
                finish();
            }
        });
        about_us = (TextView) findViewById(R.id.about);
        about_us.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NavBar_Activity.this, AboutFragment.class);
                startActivity(intent);
                finish();
            }
        });


        nmap=(TextView) findViewById(R.id.map);
        nmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(NavBar_Activity.this,newmaps.class);
                startActivity(intent);
                finish();
            }
        });





    }
}
