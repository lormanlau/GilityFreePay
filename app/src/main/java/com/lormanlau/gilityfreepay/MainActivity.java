package com.lormanlau.gilityfreepay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class MainActivity extends AppCompatActivity {

    Switch toggle;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private ScreenCaptureFragment screenCaptureFragment;
    private String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggle = findViewById(R.id.toggle);
        final SharedPreferences sharedPreferences = this.getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        toggle.setChecked(sharedPreferences.getBoolean("toggle", false));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        }

        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            screenCaptureFragment = new ScreenCaptureFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, screenCaptureFragment, "ScreenCapture")
                    .commit();
        }

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    toggle.setTextOn("ON");
                    startWidget();
                } else {
                    toggle.setTextOff("OFF");
                    stopWidget();
                }
                sharedPreferences.edit().putBoolean("toggle", isChecked).apply();
            }
        });

        registerBoardcastReciever();
    }

    @Override
    protected void onStart() {
        super.onStart();
        screenCaptureFragment.askPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                startWidget();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startWidget(){
        startService(new Intent(MainActivity.this, FloatingWidgetService.class));
        finish();
    }

    private void stopWidget(){
        stopService(new Intent(MainActivity.this, FloatingWidgetService.class));
    }

    private void registerBoardcastReciever(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("SCREENSHOT");
        filter.addAction("GET_TEXT");
        LocalBroadcastManager.getInstance(this).registerReceiver(LocalBroadcastReciver,filter);
    }

    BroadcastReceiver LocalBroadcastReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case "SCREENSHOT":
                    Log.i(TAG, "onReceive");
                    new CountDownTimer(1000,500) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }
                        @Override
                        public void onFinish() {
                            screenCaptureFragment.startScreenCapture();
                        }
                    }.start();
                    break;
                case "GET_TEXT":

            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
