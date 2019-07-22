package com.vkavetskyy.compass;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mRotation_Vector;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private int mAzimuth;

    private final float[] rotation_Matrix = new float[9];
    private final float[] orientation_Angles = new float[3];

    private ImageView imageView_Protractor;
    private TextView textView_Coordinates;
    private TextView textView_Azimuth;

    private boolean sensorCheck1 = false;
    private boolean sensorCheck2 = false;
    private final float[] mLastAccelerometer = new float[3];
    private final float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView_Protractor = findViewById(R.id.imageView_Protractor);
        textView_Coordinates = findViewById(R.id.textView_Azimuth);
        textView_Azimuth = findViewById(R.id.textView_Direction);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotation_Matrix, sensorEvent.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rotation_Matrix, orientation_Angles)[0]) + 360) % 360;
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotation_Matrix, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rotation_Matrix, orientation_Angles);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rotation_Matrix, orientation_Angles)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        imageView_Protractor.setRotation(-mAzimuth);

        String Azimuth = "N";

        if (mAzimuth >= 350 || mAzimuth <= 10) Azimuth = "N";
        if (mAzimuth < 350 && mAzimuth > 280) Azimuth = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260) Azimuth = "W";
        if (mAzimuth <= 260 && mAzimuth > 190) Azimuth = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170) Azimuth = "S";
        if (mAzimuth <= 170 && mAzimuth > 100) Azimuth = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80) Azimuth = "E";
        if (mAzimuth <= 80 && mAzimuth > 10) Azimuth = "NE";


        textView_Coordinates.setText(mAzimuth + "");
        textView_Azimuth.setText(Azimuth);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void start() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                noSensorsAlert();
            }
            else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                sensorCheck1 = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
                sensorCheck2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
            }
        }
        else{
            mRotation_Vector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            sensorCheck1 = mSensorManager.registerListener(this, mRotation_Vector, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void noSensorsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Sorry, but your device is not supported :(")
                .setCancelable(false)
                .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }

    private void stop() {
        if (sensorCheck1) {
            mSensorManager.unregisterListener(this, mRotation_Vector);
        }
        else {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }
}
