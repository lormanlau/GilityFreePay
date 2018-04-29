package com.lormanlau.gilityfreepay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.text.DecimalFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class FloatingWidgetService extends Service {

    private View mFloatingView, infoView, containerView;
    private WindowManager mWindowManager;
    private String TAG = "FloatingWidgetService";
    private org.stellar.sdk.KeyPair mPair;
    private org.stellar.sdk.KeyPair mPair2;
    private Server server;


    private void createStellarAccount(){
        server = new Server("https://horizon-testnet.stellar.org");
        mPair = org.stellar.sdk.KeyPair.random();
        String friendbotUrl = String.format(
                "https://friendbot.stellar.org/?addr=%s",
                mPair.getAccountId());
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(friendbotUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                Log.i(TAG, "onResponse: " + response);
                AccountResponse account = null;
                try {
                    account = server.accounts().account(mPair);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Balances for account " + mPair.getSecretSeed());
                for (AccountResponse.Balance balance : account.getBalances()) {
                    Log.i(TAG, String.format(
                            "Type: %s, Code: %s, Balance: %s",
                            balance.getAssetType(),
                            balance.getAssetCode(),
                            balance.getBalance()));
                }
            }
        });
        mPair2 = org.stellar.sdk.KeyPair.random();
        friendbotUrl = String.format(
                "https://friendbot.stellar.org/?addr=%s",
                mPair2.getAccountId());
        request = new Request.Builder()
                .url(friendbotUrl)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                Log.i(TAG, "onResponse: " + response);
//
                AccountResponse account = null;
                try {
                    account = server.accounts().account(mPair2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Balances for account " + mPair2.getSecretSeed());
                for (AccountResponse.Balance balance : account.getBalances()) {
                    Log.i(TAG, String.format(
                            "Type: %s, Code: %s, Balance: %s",
                            balance.getAssetType(),
                            balance.getAssetCode(),
                            balance.getBalance()));
                }
            }
        });

    };

    private void stellarTransaction(String amount) throws IOException {
        org.stellar.sdk.Network.useTestNetwork();

        KeyPair source = KeyPair.fromSecretSeed(mPair2.getSecretSeed());
        KeyPair destination = KeyPair.fromAccountId(mPair.getAccountId());
        // First, check to make sure that the destination account exists.
        // You could skip this, but if the account does not exist, you will be charged
        // the transaction fee when the transaction fails.
        // It will throw HttpResponseException if account does not exist or there was another error.
        server.accounts().account(destination);
        // If there was no error, load up-to-date information on your account.
        AccountResponse sourceAccount = server.accounts().account(source);
        // Start building the transaction.
        Transaction transaction = new Transaction.Builder(sourceAccount)
                .addOperation(new PaymentOperation.Builder(destination, new AssetTypeNative(), "10").build())
                .addMemo(Memo.text("Test Transaction"))
                .build();
        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        // And finally, send it off to Stellar!
        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);
            Log.i(TAG, "createStellarNetwork: " + response);
        } catch (Exception e) {
            Log.i(TAG, "error: " + e);
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        }
    }




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

        createStellarAccount();
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
                    DecimalFormat df = new DecimalFormat("0.00");

//                    stellarTransaction(df.format(price).toString());
                    
                    Log.i(TAG, "price: " + Double.toString(price));
                    Log.i(TAG, "cost: " + calcThePrice(price));
                    TextView costLabel = infoView.findViewById(R.id.costLabel);
                    TextView priceLabel = infoView.findViewById(R.id.priceLabel);
                    priceLabel.setText("Your Purchase was: $"+ df.format(price));
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
