package com.project.luo.stepcounter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class CounterActivity extends Activity implements SensorEventListener{

    private final static String TAG = "CounterActivity";

    private TextView totalSteps;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private final float alpha = 0.8f;

    // Arrays for storing filtered values
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    private int stepCount = 0;

    private final int WINDOW_SIZE = 100;

    private final int FILTERING_SIZE = 9;

    private float minThreshold = 0.3f;
    private float maxThreshold = -0.2f;

    ArrayList<Float> dataNeedFilter = new ArrayList<Float>();

    // The youtube tutorial says the sensors' sample rate is 100 HZ, so I created a Filter
    // Coefficients with filter_coefficients.py with cutoff frequency equals to 150 HZ
    private float[] coeff = {0.0060877f,   0.00729125f,  0.010808f,    0.01640669f,  0.02371175f,  0.03222883f,
            0.04137881f,  0.05053756f,  0.05907904f,  0.06641854f,  0.07205308f,  0.07559626f,
            0.07680497f,  0.07559626f,  0.07205308f,  0.06641854f,  0.05907904f,  0.05053756f,
            0.04137881f,  0.03222883f,  0.02371175f,  0.01640669f,  0.010808f,    0.00729125f,
            0.0060877f,};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter);

        totalSteps = (TextView) findViewById(R.id.totalStep);

        Button buttonStart = (Button) findViewById(R.id.buttonStart);
        Button buttonStop = (Button) findViewById(R.id.buttonStop);
        Button buttonReset = (Button) findViewById(R.id.buttonReset);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(CounterActivity.this, "Start counting", Toast.LENGTH_SHORT).show();
                dataNeedFilter.clear();
                sensorManager.registerListener(CounterActivity.this, accelerometer,
                        SensorManager.SENSOR_DELAY_FASTEST);

            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(CounterActivity.this, "Stop counting", Toast.LENGTH_SHORT).show();
                sensorManager.unregisterListener(CounterActivity.this);
            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.unregisterListener(CounterActivity.this);
                resetCounter();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Get reference to Accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            finish();
        }

    }

    // Unregister listener
    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);

        resetCounter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_counter, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.info:
                popUpAlert();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            performStepCount(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // N/A
    }

    // Show More Information dialog
    private void popUpAlert(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(CounterActivity.this);

        alertBuilder.setTitle("More Information");
        alertBuilder.setMessage("The application works if the device is lying flat, face-up on a hand.\n" +
                "Star Button: start counting\n" +
                "Stop Button: stop counting\n" +
                "Reset Button: Reset counter to 0");


        alertBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = alertBuilder.create();

        alertDialog.show();

    }

    // Perform Step Count
    private void performStepCount(SensorEvent event) {
        float[] values = event.values;

        // Save the values from the three axes into their corresponding variables
        float x = values[0];
        float y = values[1];
        float z = values[2];

        //Log.d(TAG, x + "," + y + "," + z);

        //Log.d(TAG, "Gravity is " + gravity[0]);

        // Apply low-pass filter as SensorEvent java doc suggests
        gravity[0] = lowPass(x, gravity[0]);
        gravity[1] = lowPass(y, gravity[1]);
        gravity[2] = lowPass(z, gravity[2]);

        // Apply high-pass filter as SensorEvent java doc suggests
        linear_acceleration[0] = highPass(x, gravity[0]);
        linear_acceleration[1] = highPass(y, gravity[1]);
        linear_acceleration[2] = highPass(z, gravity[2]);

        //Log.i(TAG, "filter y is " + linear_acceleration[1]);


        if (dataNeedFilter.size() == WINDOW_SIZE){

            //Log.i(TAG, "Before filtering");
            //Log.i(TAG, filterList.toString());
            //Log.i(TAG, "After filtering");
            //Log.i(TAG, Arrays.toString(medianFilter(filterList)));
            //Log.i(TAG, "De-mean value");
            //Log.i(TAG, String.valueOf(deMeanValue(filterList)));
            //Log.i(TAG, deMean(filterList).toString());

            float[] filteredData = medianFilter(deMean(dataNeedFilter));

            //float[] diffList = diffNumbers(filterList);
            //Log.i(TAG, Arrays.toString(diffList));
            //Log.i(TAG, Arrays.toString(filteredData));

            //zeroCrossing(diffList);
            zeroCrossing(filteredData);
            //Log.i(TAG, String.valueOf(stepCount));

            dataNeedFilter.clear();

        }

        // The application works in a condition that the device is lying flat, face-up on a hand
        // The Y Axis data from accelerometer is used for step counting. X and Z Axis are ignore.
        dataNeedFilter.add(linear_acceleration[1]);
    }

    // Deemphasize transient forces as SensorEvent Java Doc suggests
    private float lowPass(float current, float gravity) {

        return gravity * alpha + current * (1 - alpha);

    }

    // Deemphasize constant forces as SensorEvent Java Doc suggests
    private float highPass(float current, float gravity) {

        return current - gravity;

    }

    /**
     * Implement medianFiltering on a list of numbers
     * Modify python codes from https://gist.github.com/bhawkins/3535131 to Java
     *
     * @param  fList   the list of numbers that need medianFiltering
     * @return         the array of numbers after medianFiltering
     */
    private float[] medianFilter(ArrayList<Float> fList){

        int length = fList.size();
        int k = (FILTERING_SIZE - 1) / 2;
        int medianIndex = (FILTERING_SIZE - 1) / 2;

        float[] result = new float[length];
        float[][] y = new float[length][FILTERING_SIZE];

        for (int i = 0; i < length; i ++){
            y[i][k] = fList.get(i);
        }

        int j, l;
        for (int i = 0; i < k; i ++){
            j = k - i;
            l = 0;
            for (int m = j; m < length; m++){
                y[m][i] = fList.get(l);
                l ++;
            }
            for (int m = 0; m < j; m++){
                y[m][i] = fList.get(0);
            }
            l = j;
            for (int m = 0; m < length - j; m ++){
                y[m][FILTERING_SIZE - (i+1)] = fList.get(l);
                l ++;
            }
            for (int m = length - j; m < length; m ++){
                y[m][FILTERING_SIZE - (i+1)] = fList.get(length - 1);
            }

        }

        for (int i = 0; i < length; i ++){
            Arrays.sort(y[i]);
            result[i] = y[i][medianIndex];
        }

        return result;
    }

    /**
     * Implement deMean on a list of numbers
     *
     * @param  dList   the list of numbers that need deMean
     * @return         the list of numbers after deMean
     */
    private ArrayList<Float> deMean(ArrayList<Float> dList){

        float sum = 0;

        for( Float f : dList){
            sum = sum + f.floatValue();
        }

        float deMeanValue = (sum/(dList.size()));

        float afterDeMean;

        int length = dList.size();

        for( int i = 0; i < length; i ++){

            afterDeMean = dList.get(i) - deMeanValue;
            dList.set(i, afterDeMean);
        }

        return dList;
    }

    /**
     * Count how many numbers are zero crossing on a list of numbers and update step count accordingly
     *
     * @param  filteredNum   the list of numbers that need differentiation
     */
    private void zeroCrossing(float[] filteredNum){

        float previous = 0f;

        boolean reachMinThreshold = false;
        boolean reachMaxThreshold = false;

        for (float f : filteredNum){

            if (previous > minThreshold){
                reachMinThreshold = true;
            }
            if (previous < maxThreshold){
                reachMaxThreshold = true;
            }
            if (previous > 0 && f < 0 && reachMinThreshold && reachMaxThreshold){
                stepCount ++;
                totalSteps.setText(String.valueOf(stepCount));
                reachMinThreshold = false;
                reachMaxThreshold = false;
            }

            previous = f;

        }

    }

    /**
     * Implement differentiation on a list of numbers
     *
     * @param  fList   the list of numbers that need differentiation
     * @return         the array of numbers after differentiation
     */
    private float[] diffNumbers(float[] fList){

        int length = fList.length;

        float[] result = new float[length - 1];

        for (int i = 0; i < length - 1; i ++){
            result[i] = fList[i+1] - fList[i];
        }

        return result;
    }

    /**
     * Implement deMean on a list of numbers
     * Modify C++ codes from http://toto-share.com/2011/11/cc-convolution-source-code/ to Java
     *
     * @param  fList   the list of numbers that need convolution
     * @return         the list of numbers after convolution
     */
    private float[] convolution(ArrayList<Float> fList){

        int listLength = fList.size();
        int coeffLength = coeff.length;
        int convLength = listLength +  coeffLength - 1;

        float [] result = new float[convLength];

        int i, j, k;
        float tmp;

        //convolution process
        for (i = 0; i < convLength; i++)
        {
            k = i;
            tmp = 0f;
            for (j = 0; j < coeffLength; j++)
            {
                if(k >= 0 && k < listLength)
                    tmp = tmp + (fList.get(k) * coeff[j]);

                k = k - 1;
                result[i] = tmp;
            }
        }

        //return convolution array
        return result;

    }

    // Reset the counter to 0
    private void resetCounter(){
        stepCount = 0;
        totalSteps.setText(String.valueOf(stepCount));
    }

}
