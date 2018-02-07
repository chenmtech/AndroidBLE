package com.cmtech.android.androidble;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.cmtech.android.ble.ViseBle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViseBle.getInstance();
    }
}
