package com.example.myapplication4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Button buttonScan;
    TextView textViewBeacon1, textViewBeacon2, textViewBeacon3, textViewDegree,textViewDegree2, textViewDistanceB1, textViewDistanceB2, textViewDistanceB3, textViewUserX, textViewUserY, textViewZone;

    //BLUETOOTH
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Set<BluetoothDevice> bluetoothDevice;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 123;

    //SENSOR
    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    Sensor orientation;
    private float[] accelerometerValues = new float[3];
    private float[] magnetometerValues = new float[3];

    // Coordonnées des balises
    double[] coordBeacon1 = {0, 0};
    double[] coordBeacon2 = {0, 5.58};
    double[] coordBeacon3 = {8.56, 0};

    // Distances entre les balises et le téléphone
    double distanceBeacon1 = 6;
    double distanceBeacon2 = 5;
    double distanceBeacon3 = 3;

    double arrondiDistanceB1;
    double arrondiDistanceB2;
    double arrondiDistanceB3;

    double rssi0 = -53.57;
    double n = 2;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonScan = findViewById(R.id.idButtonScan);
        textViewBeacon1 = findViewById(R.id.idTextViewBeacon1);
        textViewBeacon2 = findViewById(R.id.idTextViewBeacon2);
        textViewBeacon3 = findViewById(R.id.idTextViewBeacon3);
        textViewDegree = findViewById(R.id.idTextViewDegree);
        textViewDegree2 = findViewById(R.id.idTextViewDegree2);
        textViewDistanceB1 = findViewById(R.id.idTextViewDistance1);
        textViewDistanceB2 = findViewById(R.id.idTextViewDistance2);
        textViewDistanceB3 = findViewById(R.id.idTextViewDistance3);
        textViewUserX = findViewById(R.id.idTextViewUserX);
        textViewUserY = findViewById(R.id.idTextViewUserY);
        textViewZone = findViewById(R.id.idTextViewZone);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        double arrondiDistanceB1 = 0;
        double arrondiDistanceB2 = 0;
        double arrondiDistanceB3 = 0;

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("myTag", "onClick");
                double[] coordUser = trilaterate(coordBeacon1, coordBeacon2, coordBeacon3, distanceBeacon1, distanceBeacon2, distanceBeacon3);
                Log.d("coordonateTag", Arrays.toString(coordUser));
                double[] coordUser2 = {8.56,0};
                double[] coordDest = {0,2.5};
                calculateAngleToDestination(coordUser, coordDest);
                Log.d("AngleTag", "Angle : " + calculateAngleToDestination(coordUser2, coordDest));
                //double direction = calculerDirectionRelative(15,calculateAngleToDestination(coordUser, coordDest));
                //String instruction = obtenirInstructionsDirection(direction);
                //Log.d("AngleTag", String.valueOf(direction) + " " + instruction);
                Log.d("myTagInstruction", String.valueOf(obtenirInstructionsDirection(350,3)));
                
                startBluetoothScan();
            }
        });


        // Initialisez le gestionnaire de capteurs
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Obtenez les capteurs d'accéléromètre et de magnétomètre
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);


        // Enregistrez les écouteurs de capteurs
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);

    }

    private void startBluetoothScan() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "Bluetooth LE Scanner not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Filtre sur la balise Bluettooth
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setDeviceAddress("80:EC:CC:CD:CC:12")
                .build();

        ScanFilter scanFilter2 = new ScanFilter.Builder()
                .setDeviceAddress("80:EC:CC:CD:CE:C1")
                .build();

        ScanFilter scanFilter3 = new ScanFilter.Builder()
                .setDeviceAddress("D0:2C:70:C3:E5:D3")
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);
        filters.add(scanFilter2);
        filters.add(scanFilter3);

        //Demande de permission si besoin
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        bluetoothLeScanner.startScan(filters, settings, scanCallback);

    }

    public double distanceFromRSSI(int txPower, int rssi, double pathLossExponent) {
        double distance = Math.pow(10, ((txPower - rssi) / (10 * pathLossExponent)));
        return distance;
    }


    private ScanCallback scanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    //récupération des données des balises
                    BluetoothDevice device = result.getDevice();
                    String deviceAddress = device.getAddress();
                    int rssi = result.getRssi();
                    int txpower = result.getTxPower();
                    String distance = String.valueOf(distanceFromRSSI(txpower, rssi,2.0));



                    if(deviceAddress.equals("80:EC:CC:CD:CC:12")){
                        textViewBeacon1.setText("Beacon 1 : " + deviceAddress + " :" + rssi + " dbm");
                        arrondiDistanceB1 = Math.round(estimateDistance(rssi , -52.9, n) * 1000.0) / 1000.0;

                        /*
                        double FILTER_ALPHA = 0.2;
                        double filteredDistance;
                        if (!(filteredDistance == null))
                        {
                            filteredDistance = FILTER_ALPHA * arrondiDistance + (1 - FILTER_ALPHA) * filteredDistance;
                        }

                         */
                        if(arrondiDistanceB1<10.2)
                            textViewDistanceB1.setText("Distance B1 : " + arrondiDistanceB1 + " m");
                        Log.d("rssiTagB1", String.valueOf(rssi));
                    }

                    if(deviceAddress.equals("80:EC:CC:CD:CE:C1")) {
                        textViewBeacon2.setText("Beacon 2 : " + deviceAddress + " :" + rssi + " dbm");
                        arrondiDistanceB2 = Math.round(estimateDistance(rssi , -58, n) * 1000.0) / 1000.0;
                        if(arrondiDistanceB2<10.2)
                            textViewDistanceB2.setText("Distance B2 : " + arrondiDistanceB2 + " m");
                        Log.d("rssiTagB2", String.valueOf(rssi));
                    }

                    if(deviceAddress.equals("D0:2C:70:C3:E5:D3")){
                        textViewBeacon3.setText("Beacon 3 : " + deviceAddress + " :" + rssi + " dbm " );
                        arrondiDistanceB3 = Math.round(estimateDistance(rssi ,-54.3 , n) * 1000.0) / 1000.0;
                        if(arrondiDistanceB3<10.2)
                            textViewDistanceB3.setText("Distance B3 : " + arrondiDistanceB3 + " m");
                        Log.d("rssiTagB3", String.valueOf(rssi));
                    }

                    double[] coordUser = trilaterate(coordBeacon1, coordBeacon2, coordBeacon3, arrondiDistanceB1, arrondiDistanceB2, arrondiDistanceB3);

                    if(coordUser[0]<8.56 && coordUser[0]>-2 && coordUser[1]<5.58 && coordUser[1]>-2) {
                        textViewUserX.setText("User X : " + coordUser[0]);
                        textViewUserY.setText("User Y : " + coordUser[1]);
                        estimateZone(coordUser);
                    }



                }
            };


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerValues = event.values;
        }
        else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            Log.d("degreeTag", Arrays.toString(event.values));
            textViewDegree2.setText("Degree V2 : " + Arrays.toString(event.values));
        }
        float[] rotationMatrix = new float[9];
        float[] I = new float[9];
        float[] orientationValues = new float[3];


        if (SensorManager.getRotationMatrix(rotationMatrix, I, accelerometerValues, magnetometerValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationValues);

            float azimuthDegrees = (float) Math.toDegrees(orientationValues[0]);
            //float pitchDegrees = (float) Math.toDegrees(orientationValues[1]);
            //float rollDegrees = (float) Math.toDegrees(orientationValues[2]);
            //calculateAngleToDestination(azimuthDegrees);

            Log.d("OrientationTag", String.valueOf(azimuthDegrees));

            textViewDegree.setText("Degree : " +((azimuthDegrees + 360) % 360));
            //textViewDegree.setText("Radian : " + orientationValues[0]);
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onResume() {
        super.onResume();
        //SensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        //SensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause() {
        super.onPause();
        //SensorManager.unregisterListener(this);
    }

    //calcul l'angle d'orientation entre l'utilisateur et la destinantion
    private double calculateAngleToDestination(double[] coordUser, double[] coordDest) {
        double xU = coordUser[0];
        double yU = coordUser[1];

        // Coordonnées du point d'arrivée
        double xD = coordDest[0];
        double yD = coordDest[1];

        // Calcul du vecteur de déplacement
        double vx = xD - xU;
        double vy = yD - yU;

        // Calcul de l'angle d'orientation en degrés
        double angleDegrees = Math.toDegrees(Math.atan2(vy, vx));

        // Affichage de l'angle d'orientation
        //System.out.println("Angle d'orientation : " + angleDegrees + " degrés");

        return angleDegrees;
    }

    // Fonction de trilatération
    public static double[] trilaterate(double[] coordA, double[] coordB, double[] coordC, double d1, double d2, double d3) {
        double[] estimatedCoordinates = new double[2];

        // Calcul des coefficients pour les équations de trilatération
        double A1 = 2 * (coordB[0] - coordA[0]);
        double B1 = 2 * (coordB[1] - coordA[1]);
        double C1 = (Math.pow(d1, 2) - Math.pow(d2, 2) - Math.pow(coordA[0], 2) + Math.pow(coordB[0], 2) - Math.pow(coordA[1], 2) + Math.pow(coordB[1], 2));

        double A2 = 2 * (coordC[0] - coordA[0]);
        double B2 = 2 * (coordC[1] - coordA[1]);
        double C2 = (Math.pow(d1, 2) - Math.pow(d3, 2) - Math.pow(coordA[0], 2) + Math.pow(coordC[0], 2) - Math.pow(coordA[1], 2) + Math.pow(coordC[1], 2));

        // Calcul des coordonnées estimées
        double denominator = A1 * B2 - A2 * B1;

        estimatedCoordinates[0] = (C1 * B2 - C2 * B1) / denominator;
        estimatedCoordinates[1] = (A1 * C2 - A2 * C1) / denominator;

        return estimatedCoordinates;
    }

    private static double calculerDirectionRelative(double orientationUtilisateur, double angleDirectionCible) {
        // Calculez la différence d'angle entre l'orientation de l'utilisateur et la direction cible
        double directionRelative = angleDirectionCible - orientationUtilisateur;

        directionRelative = (directionRelative + 180) % 360 - 180;

        return directionRelative;
    }

    private static double obtenirAngleDirection(double[] coordUser) {
        double xUser = coordUser[0];
        double yUser = coordUser[1];

        if(xUser>7 && yUser<2){
            return 3;
        }
        else{
            return 183;
        }
    }

    private static double obtenirInstructionsDirection(double angleUser, double angleDirection) {
        double diff = angleDirection - angleUser;
        diff = (diff + 360) % 360;

/*
        if ((angleUser >= (diff - 15 + 360) % 360) && (angleUser <= (diff + 15 + 360) % 360)) {
            System.out.println("Allez tout droit");
        } else if (angleUser >= 18 && angleUser > 348) {
            System.out.println("Allez tout droit");
        }*/
        return diff;
    }





    public static double estimateDistance(double rssi, double rssi0, double n) {
        return Math.pow(10, ((rssi - rssi0) / (-10 * n)));
    }
    public  void estimateZone (double[] coordUser) {
        double xUser = coordUser[0];
        double yUser = coordUser[1];

        if(xUser>6){
            textViewZone.setText("Rayon : Poisson");
        }
        else if(xUser>2){
            textViewZone.setText("Rayon : Fruits et Légumes");
        }
        else{
            textViewZone.setText("Rayon : Alcool");
        }

    }


}