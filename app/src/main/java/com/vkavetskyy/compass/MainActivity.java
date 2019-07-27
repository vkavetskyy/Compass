package com.vkavetskyy.compass;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mRotation_Vector;
    private Sensor mMagnetometer;
    private Sensor mOrientation;

    private ImageView imageView_Sensor_Accuracy;
    private ImageView imageView_Protractor;
    private TextView textView_Coordinates;
    private TextView textView_Azimuth;

    private int mAzimuth;
    private final float[] rotation_Matrix = new float[9];
    private final float[] orientation_Angles = new float[3];
    private final float[] mLastAccelerometer = new float[3];
    private final float[] mLastMagnetometer = new float[3];
    private boolean mLastMagnetometerSet = false;
    private float currentDegree = 0f;

    private boolean legacy_mode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        imageView_Sensor_Accuracy = findViewById(R.id.imageView_Sensor_Accuracy);
        imageView_Protractor = findViewById(R.id.imageView_Protractor);
        textView_Coordinates = findViewById(R.id.textView_Azimuth);
        textView_Azimuth = findViewById(R.id.textView_Direction);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(Objects.requireNonNull(mSensorManager).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)
            noSensorsAlert();

        //Legacy mode
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_GAME);
            sensorsAlert();
        }
        //Normal mode
        else {
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mRotation_Vector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, mRotation_Vector, SensorManager.SENSOR_DELAY_GAME);
        }

        //Check if != test device and initialize ads
        if (!isTestDevice()) {
            AdView mAdView = findViewById(R.id.adView);
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //Legacy mode (no rotation vector sensor or other)
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            legacy_mode = true;
            float degree = Math.round(sensorEvent.values[0]);
            RotateAnimation rotateAnimation = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.50f, Animation.RELATIVE_TO_SELF, 0.50f);
            rotateAnimation.setDuration(250);
            rotateAnimation.setFillAfter(false);
            imageView_Protractor.startAnimation(rotateAnimation);
            currentDegree = -degree;
            setText(degree);
        }

        //Normal mode
        else {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotation_Matrix, sensorEvent.values);
                mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rotation_Matrix, orientation_Angles)[0]) + 360) % 360;
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
                mLastMagnetometerSet = true;
            }
            if (mLastMagnetometerSet) {
                SensorManager.getRotationMatrix(rotation_Matrix, null, mLastAccelerometer, mLastMagnetometer);
                SensorManager.getOrientation(rotation_Matrix, orientation_Angles);
                mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rotation_Matrix, orientation_Angles)[0]) + 360) % 360;
            }
            mAzimuth = Math.round(mAzimuth);
            imageView_Protractor.setRotation(-mAzimuth);
            setText(mAzimuth);
        }
    }

    private void setText(float mAzimuth) {
        int coordinates = (int) mAzimuth;
        String Azimuth = "N";
        if (mAzimuth >= 350 || mAzimuth <= 10) Azimuth = "N";
        if (mAzimuth < 350 && mAzimuth > 280) Azimuth = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260) Azimuth = "W";
        if (mAzimuth <= 260 && mAzimuth > 190) Azimuth = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170) Azimuth = "S";
        if (mAzimuth <= 170 && mAzimuth > 100) Azimuth = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80) Azimuth = "E";
        if (mAzimuth <= 80 && mAzimuth > 10) Azimuth = "NE";
        textView_Coordinates.setText(coordinates+"");
        textView_Azimuth.setText(Azimuth);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        switch (i) {
            case 1:
                imageView_Sensor_Accuracy.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_sensor_accuracy_bad));
                break;
            case 2:
                imageView_Sensor_Accuracy.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_sensor_accuracy_average));
                break;
            case 3:
                imageView_Sensor_Accuracy.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_sensor_accuracy_good));
                break;
        }
    }

    private void sensorsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(R.string.app_legacyMode_alert)
                .setCancelable(false)
                .setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        alertDialog.show();
    }

    private void noSensorsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(R.string.app_notSupported_alert)
                .setCancelable(false)
                .setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (legacy_mode) {
            mSensorManager.unregisterListener(this, mOrientation);
        }
        else {
            mSensorManager.unregisterListener(this, mRotation_Vector);
            mSensorManager.unregisterListener(this, mMagnetometer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (legacy_mode) {
            mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_GAME);
        }
        else {
            mSensorManager.registerListener(this, mRotation_Vector, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private boolean isTestDevice() {
        String testLabSetting = Settings.System.getString(getContentResolver(), "firebase.test.lab");
        return "true".equals(testLabSetting);
    }
}
