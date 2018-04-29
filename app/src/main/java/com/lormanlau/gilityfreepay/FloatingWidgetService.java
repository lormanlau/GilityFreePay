package com.lormanlau.gilityfreepay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class FloatingWidgetService extends Service {

    private View mFloatingView, infoView, containerView;
    private WindowManager mWindowManager;
    private String TAG = "FloatingWidgetService";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget_layout , null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                 WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        setOnClickRootView();
        createLocalBoardcastReciever();

        infoView = mFloatingView.findViewById(R.id.info_view);
        containerView = mFloatingView.findViewById(R.id.container_view);
    }

    private void setOnClickRootView(){
        mFloatingView.findViewById(R.id.container_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendScreenshotBroadcast();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendScreenshotBroadcast(){
        Log.i(TAG, "sendScreenshotBroadcast");
        Intent intent = new Intent("SCREENSHOT");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void createLocalBoardcastReciever(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_WIDGET");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(LocalBroadcastReciever, filter);
    }

    BroadcastReceiver LocalBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case "UPDATE_WIDGET":
                    //update calculation
                    double price = intent.getDoubleExtra("price", 0);
                    Log.i(TAG, "price: " + Double.toString(price));
                    Log.i(TAG, "cost: " + calcThePrice(price));
                    TextView costLabel = infoView.findViewById(R.id.costLabel);
                    TextView priceLabel = infoView.findViewById(R.id.priceLabel);
                    priceLabel.setText("Your Purchase was: $"+ Double.toString(price));
                    costLabel.setText("Veridium donated: $" + calcThePrice(price));

                    infoView.setVisibility(View.VISIBLE);
                    containerView.setVisibility(View.GONE);

                    new CountDownTimer(10000, 1000){
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            infoView.setVisibility(View.GONE);
                            containerView.setVisibility(View.VISIBLE);
                        }
                    }.start();
                    break;
            }
        }
    };

    private String calcThePrice(double price){
        DecimalFormat df = new DecimalFormat("0.00");
        double cost = price * 0.005;
        return df.format(cost);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}
