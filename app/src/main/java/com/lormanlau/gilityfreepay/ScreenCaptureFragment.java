package com.lormanlau.gilityfreepay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.nio.ByteBuffer;

public class ScreenCaptureFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "ScreenCaptureFragment";

    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private DisplayMetrics metrics;

    private int mResultCode;
    private Intent mResultData;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mMediaProjectionManager;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mMediaProjectionManager = (MediaProjectionManager)
                activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
        }
    }

    public void askPermissions(){
        startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled");
                return;
            }
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            Log.i(TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScreenCapture();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tearDownMediaProjection();
    }

    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public void startScreenCapture() {
        if (mMediaProjection != null) {
            setUpVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();
        } else {
            Log.i(TAG, "Requesting confirmation");
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    private void setUpVirtualDisplay() {
        ImageReader imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888 , 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture", metrics.widthPixels
                , metrics.heightPixels, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * image.getWidth();
                    Bitmap bitmap = Bitmap.createBitmap(image.getWidth()+rowPadding/pixelStride, image.getHeight(), Bitmap.Config.RGB_565);
                    bitmap.copyPixelsFromBuffer(buffer);
                    Log.i(TAG, "onImageAvailable: " + bitmap);
                    grabTextFromPhoto(bitmap);
                } catch (Exception e){

                } finally {
                    image.close();
                    tearDownMediaProjection();
                }
            }
        }, null);
    }

    private void grabTextFromPhoto(Bitmap bitmap){
        TextRecognizer textRecognizer = new TextRecognizer.Builder(mContext).build();
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> text = textRecognizer.detect(frame);
        Log.i(TAG, "grabTextFromPhoto");
        for (int i = 0; i < text.size(); i++) {
            TextBlock textBlock = text.valueAt(i);
            if (textBlock.toString().matches(".*\\d+.*")){
                Log.i(TAG, "numbers: " + textBlock.getValue());
                StringBuilder sb = new StringBuilder();
                boolean found = false;
                for(String letter : textBlock.getValue().split("")){
                    if(letter.matches("[0-9]") || letter.matches("[.]")){
                        sb.append(letter);
                        found = true;
                    } else if(found){
                        // If we already found a digit before and this char is not a digit, stop looping
                        break;
                    }
                }
                if (sb.toString().contains(".")) {
                    sendFoundPriceBroadcast(sb.toString());
                }
            }
        }
    }

    private void sendFoundPriceBroadcast(String stringPrice){
        try {
            double price = Double.parseDouble(stringPrice);
            Intent intent = new Intent("UPDATE_WIDGET");
            intent.putExtra("price", price);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        } catch (Exception e) {

        }
    }


    private void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

}
