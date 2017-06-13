package edu.mdc.csclub.indoornavigation2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    //UI components
    private Button scanButton;
    private TextView beacon11TextView;
    private TextView beacon12TextView;
    private TextView beacon21TextView;
    private TextView beacon22TextView;
    private TextView beacon31TextView;
    private TextView beacon32TextView;
    private TextView accelerationXTextView;
    private TextView accelerationYTextView;
    private TextView accelerationZTextView;
    private TextView rotationXTextView;
    private TextView rotationYTextView;
    private TextView rotationZTextView;
    private TextView magneticFieldXTextView;
    private TextView magneticFieldYTextView;
    private TextView magneticFieldZTextView;
    private TextView XTextView;
    private TextView YTextView;
    private TextView roomTextView;
    private ProgressBar progressBar;

    //App state
    private boolean isScanning;
    private int beacon11RSSI;
    private int beacon12RSSI;
    private int beacon21RSSI;
    private int beacon22RSSI;
    private int beacon31RSSI;
    private int beacon32RSSI;
    private float XAcceleration;
    private float YAcceleration;
    private float ZAcceleration;
    private float XRotation;
    private float YRotation;
    private float ZRotation;
    private float XMagneticField;
    private float YMagneticField;
    private float ZMagneticField;
    private Cell currentCell;
    private Room currentRoom;

    //Bluetooth objects:
    //For all APIs (<21 and >=21, >=23)
    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    //For API >=23, dynamic permissions are needed
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    //For API >=21
    private BluetoothLeScanner mLEScanner;
    private ScanSettings scanSettings;
    private List<ScanFilter> filters;
    private ScanCallback newerVersionScanCallback;
    //For API < 21
    private BluetoothAdapter.LeScanCallback olderVersionScanCallback;

    //Sensor Objects
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private Sensor mMagnetometer;
    private Sensor mGyroscope;
    private boolean accelerometerPresent = false;
    private boolean magnetometerPresent = false;
    private boolean gyroscopePresent = false;


    //SQLLite DB
    private DatabaseHandler db;

    private SVMClassifier SVMClassifier;

    ///////////////////////////////////////////////////////////////  App Lifecycle Methods ///////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up UI components
        setContentView(R.layout.activity_main);
        scanButton = (Button) findViewById(R.id.scanButton);
        XTextView = (TextView) findViewById(R.id.XTextView);
        YTextView = (TextView) findViewById(R.id.YTextView);
        roomTextView = (TextView) findViewById(R.id.roomTextView);
        beacon11TextView = (TextView) findViewById(R.id.beacon11TextView);
        beacon12TextView = (TextView) findViewById(R.id.beacon12TextView);
        beacon21TextView = (TextView) findViewById(R.id.beacon21TextView);
        beacon22TextView = (TextView) findViewById(R.id.beacon22TextView);
        beacon31TextView = (TextView) findViewById(R.id.beacon31TextView);
        beacon32TextView = (TextView) findViewById(R.id.beacon32TextView);
        accelerationXTextView = (TextView) findViewById(R.id.accelerationXTextView);
        accelerationYTextView = (TextView) findViewById(R.id.accelerationYTextView);
        accelerationZTextView = (TextView) findViewById(R.id.accelerationZTextView);
        rotationXTextView = (TextView) findViewById(R.id.rotationXTextView);
        rotationYTextView = (TextView) findViewById(R.id.rotationYTextView);
        rotationZTextView = (TextView) findViewById(R.id.rotationZTextView);
        magneticFieldXTextView = (TextView) findViewById(R.id.magneticFieldXTextView);
        magneticFieldYTextView = (TextView) findViewById(R.id.magneticFieldYTextView);
        magneticFieldZTextView = (TextView) findViewById(R.id.magneticFieldZTextView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);


        //init app state
        isScanning = false;

        //init Bluetooth objects and permissions
        initBLESetup();

        //init sensor objects
        initSensors();

        //Init database
        db = new DatabaseHandler(this);
        db.createDataBase();
        db.openDataBase();

        SVMClassifier = new SVMClassifier(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        XTextView.setText(R.string.undetermined);
        YTextView.setText(R.string.undetermined);
        roomTextView.setText(R.string.undetermined);
        beacon11TextView.setText(R.string.not_detected);
        beacon12TextView.setText(R.string.not_detected);
        beacon21TextView.setText(R.string.not_detected);
        beacon22TextView.setText(R.string.not_detected);
        beacon31TextView.setText(R.string.not_detected);
        beacon32TextView.setText(R.string.not_detected);
        accelerationXTextView.setText(R.string.not_measured);
        accelerationYTextView.setText(R.string.not_measured);
        accelerationZTextView.setText(R.string.not_measured);
        rotationXTextView.setText(R.string.not_measured);
        rotationYTextView.setText(R.string.not_measured);
        rotationZTextView.setText(R.string.not_measured);
        magneticFieldXTextView.setText(R.string.not_measured);
        magneticFieldYTextView.setText(R.string.not_measured);
        magneticFieldZTextView.setText(R.string.not_measured);
        progressBar.setVisibility(View.INVISIBLE);

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); // See onActivityResult callback method for negative behavior
        } else {
            setupBLEScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        doScan(false);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    ///////////////////////////////////////////////////////////////  UI  methods ///////////////////////////////////////////////////////////////

    /**
     * Called when the user taps the Scan button
     */
    public void scan(View view) {
        if (!isScanning) {
            doScan(true);

        } else {
            doScan(false);
        }

    }

    private void doScan(boolean enable) {
        if (enable) {
            isScanning = true;
            scanButton.setText(R.string.stop_scan_message);
            progressBar.setVisibility(View.VISIBLE);
            scanBLEDevice(true);
            if (accelerometerPresent)
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            if (magnetometerPresent)
                mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
            if (gyroscopePresent)
                mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);

        } else {
            isScanning = false;
            scanButton.setText(R.string.scan_message);
            progressBar.setVisibility(View.INVISIBLE);
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                scanBLEDevice(false);
            }
            mSensorManager.unregisterListener(this);
        }
    }

    private void updateRSSI(String uuid, int major, int minor, int rssi) {
        if (uuid.equalsIgnoreCase("B9407F30-F5F8-466E-AFF9-25556B575555")) {
            if (major == 1) {
                if (minor == 1) {
                    beacon11RSSI = rssi;
                    beacon11TextView.setText(String.valueOf(rssi));
                } else if (minor == 2) {
                    beacon12RSSI = rssi;
                    beacon12TextView.setText(String.valueOf(rssi));
                }
            } else if (major == 2) {
                if (minor == 1) {
                    beacon21RSSI = rssi;
                    beacon21TextView.setText(String.valueOf(rssi));
                } else if (minor == 2) {
                    beacon22RSSI = rssi;
                    beacon22TextView.setText(String.valueOf(rssi));
                }
            } else if (major == 3) {
                if (minor == 1) {
                    beacon31RSSI = rssi;
                    beacon31TextView.setText(String.valueOf(rssi));
                } else if (minor == 2) {
                    beacon32RSSI = rssi;
                    beacon32TextView.setText(String.valueOf(rssi));
                }
            }

        }
    }

    private void updateAcceleration(float XAcc, float YAcc, float ZAcc) {
        XAcceleration = XAcc;
        YAcceleration = YAcc;
        ZAcceleration = ZAcc;
        accelerationXTextView.setText(String.valueOf(XAcc));
        accelerationYTextView.setText(String.valueOf(YAcc));
        accelerationZTextView.setText(String.valueOf(ZAcc));
    }

    private void updateRotation(float XRot, float YRot, float ZRot) {
        XRotation = XRot;
        YRotation = YRot;
        ZRotation = ZRot;
        rotationXTextView.setText(String.valueOf(XRot));
        rotationYTextView.setText(String.valueOf(YRot));
        rotationZTextView.setText(String.valueOf(ZRot));
    }

    private void updateMagneticField(float XMF, float YMF, float ZMF) {
        XMagneticField = XMF;
        YMagneticField = YMF;
        ZMagneticField = ZMF;
        magneticFieldXTextView.setText(String.valueOf(XMF));
        magneticFieldYTextView.setText(String.valueOf(YMF));
        magneticFieldZTextView.setText(String.valueOf(ZMF));
    }

    private void updateCalculatedPosition() {
        if (currentCell != null) {
            XTextView.setText(String.valueOf(currentCell.getX()));
            YTextView.setText(String.valueOf(currentCell.getY()));
            Cell retrievedCell = db.getCell(currentCell.getX(), currentCell.getY());
            if (retrievedCell != null) {
                currentCell.setRoomID(retrievedCell.getRoomID());
                Log.i(TAG, "Room ID: " + currentCell.getRoomID());

                currentRoom = db.getRoom(currentCell.getRoomID());
                if (currentRoom != null) {
                    roomTextView.setText(String.valueOf(currentRoom.getRoomNumber()));
                } else {
                    roomTextView.setText(R.string.undetermined);
                }
            }
        } else {
            XTextView.setText(R.string.undetermined);
            YTextView.setText(R.string.undetermined);
        }

    }

    /////////////////////////////////////////////////////////////// Other callback methods ///////////////////////////////////////////////////////////////
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If the user does not want to enable Bluetooth, we kill the app
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    /////////////////////////////////////////////////////////////// Bluetooth methods ///////////////////////////////////////////////////////////////
    private void initBLESetup() {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //Beginning in Android 6.0 (API level 23), users grant permissions to apps while the app is running, not when they install the app.
        //Obtaining dynamic permissions from the user
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //23
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access.");
                builder.setMessage("Please grant location access to this app.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });// See onRequestPermissionsResult callback method for negative behavior
                builder.show();
            }
        }
        // Create callback methods
        if (Build.VERSION.SDK_INT >= 21) //LOLLIPOP
            newerVersionScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        BluetoothDevice btDevice = result.getDevice();
                        ScanRecord mScanRecord = result.getScanRecord();
                        int rssi = result.getRssi();
//                        Log.i(TAG, "Address: "+ btDevice.getAddress());
//                        Log.i(TAG, "TX Power Level: " + result.getScanRecord().getTxPowerLevel());
//                        Log.i(TAG, "RSSI in DBm: " + rssi);
//                        Log.i(TAG, "Manufacturer data: "+ mScanRecord.getManufacturerSpecificData());
//                        Log.i(TAG, "device name: "+ mScanRecord.getDeviceName());
//                        Log.i(TAG, "Advertise flag: "+ mScanRecord.getAdvertiseFlags());
//                        Log.i(TAG, "service uuids: "+ mScanRecord.getServiceUuids());
//                        Log.i(TAG, "Service data: "+ mScanRecord.getServiceData());
                        byte[] recordBytes = mScanRecord.getBytes();

                        iBeacon ib = parseBLERecord(recordBytes);

                        if (ib != null) {
                            updateRSSI(ib.getUuid(), ib.getMajor(), ib.getMinor(), rssi);
                            currentCell = calculatePosition();
                            updateCalculatedPosition();
                        }
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e("Scan Failed", "Error Code: " + errorCode);
                }
            };
        else
            olderVersionScanCallback =
                    new BluetoothAdapter.LeScanCallback() {
                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i("onLeScan", device.toString());

                                    iBeacon ib = parseBLERecord(scanRecord);

                                    if (ib != null) {
                                        updateRSSI(ib.getUuid(), ib.getMajor(), ib.getMinor(), rssi);
                                        currentCell = calculatePosition();
                                        updateCalculatedPosition();
                                    }
                                }
                            });
                        }
                    };
    }

    private void setupBLEScan() {
        //Android 4.3 (JELLY_BEAN_MR2) introduced platform support for Bluetooth Low Energy (Bluetooth LE) in the central role.
        // In Android 5.0 (LOLLIPOP, 21), an Android device can now act as a Bluetooth LE peripheral device. Apps can use this capability to make their presence known to nearby devices.
        // There was a new android.bluetooth.le API!!!
        if (Build.VERSION.SDK_INT >= 21) {//LOLLIPOP
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
            ScanFilter.Builder mBuilder = new ScanFilter.Builder();
            ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
            ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);
            byte[] uuid = getIdAsByte(UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B575555"));
            mManufacturerData.put(0, (byte) 0xBE);
            mManufacturerData.put(1, (byte) 0xAC);
            for (int i = 2; i <= 17; i++) {
                mManufacturerData.put(i, uuid[i - 2]);
            }
            for (int i = 0; i <= 17; i++) {
                mManufacturerDataMask.put((byte) 0x01);
            }
            mBuilder.setManufacturerData(76, mManufacturerData.array(), mManufacturerDataMask.array());
            ScanFilter mScanFilter = mBuilder.build();
            //TODO
            //filters.add(mScanFilter);

        }
    }

    private void scanBLEDevice(final boolean enable) {
        if (enable) {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(olderVersionScanCallback);
            } else {
                mLEScanner.startScan(filters, scanSettings, newerVersionScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(olderVersionScanCallback);
            } else {
                mLEScanner.stopScan(newerVersionScanCallback);
            }
        }
    }

    /////////////////////////////////////////////////////////////// Sensor methods ///////////////////////////////////////////////////////////////
    private void initSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensors) {
            Log.i(TAG, "Found sensor: " + s.toString());
        }
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer != null) {
            Log.i(TAG, "CREATED ACCELEROMETER:" + mAccelerometer.toString());
            accelerometerPresent = true;
        } else {
            accelerationXTextView.setText(R.string.no_accelerometer);
            accelerationYTextView.setText(R.string.no_accelerometer);
            accelerationZTextView.setText(R.string.no_accelerometer);
        }
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mMagnetometer != null) {
            Log.i(TAG, "CREATED MAGNETOMETER:" + mMagnetometer.toString());
            magnetometerPresent = true;
        } else {
            magneticFieldXTextView.setText(R.string.no_magnetometer);
            magneticFieldYTextView.setText(R.string.no_magnetometer);
            magneticFieldZTextView.setText(R.string.no_magnetometer);
        }
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mGyroscope != null) {
            Log.i(TAG, "CREATED GYROSCOPE:" + mGyroscope.toString());
            gyroscopePresent = true;
        } else {
            rotationXTextView.setText(R.string.no_gyroscope);
            rotationYTextView.setText(R.string.no_gyroscope);
            rotationZTextView.setText(R.string.no_gyroscope);
        }
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed");
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /*
             * We need to
             * take into account how the screen is rotated with respect to the
             * sensors (which always return data in a coordinate space aligned
             * to with the screen in its native orientation).
             */
            float mSensorX = 0;
            float mSensorY = 0;
            float mSensorZ = 0;
            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    mSensorX = event.values[0];
                    mSensorY = event.values[1];
                    mSensorZ = event.values[2];
                    break;
                case Surface.ROTATION_90:
                    mSensorX = -event.values[1];
                    mSensorY = event.values[0];
                    mSensorZ = event.values[2];
                    break;
                case Surface.ROTATION_180:
                    mSensorX = -event.values[0];
                    mSensorY = -event.values[1];
                    mSensorZ = event.values[2];
                    break;
                case Surface.ROTATION_270:
                    mSensorX = event.values[1];
                    mSensorY = -event.values[0];
                    mSensorZ = event.values[2];
                    break;
            }
            updateAcceleration(mSensorX, mSensorY, mSensorZ);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float magneticFieldX = event.values[0];
            float magneticFieldY = event.values[1];
            float magneticFieldZ = event.values[2];
            updateMagneticField(magneticFieldX, magneticFieldY, magneticFieldZ);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float rotationX = event.values[0];
            float rotationY = event.values[1];
            float rotationZ = event.values[2];
            updateRotation(rotationX, rotationY, rotationZ);
        }
    }

    /////////////////////////////////////////////////////////////// Utility methods   ///////////////////////////////////////////////////////////////

    private Cell calculatePosition() {
        return SVMClassifier.predict(new Measurement(-1, -1, beacon11RSSI, beacon12RSSI, beacon21RSSI, beacon22RSSI, beacon31RSSI, beacon32RSSI));
    }

    /**
     * bytesToHex method
     * http://stackoverflow.com/a/9855338
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static byte[] getIdAsByte(java.util.UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private iBeacon parseBLERecord(byte[] scanRecord) {
        iBeacon ib = null;
        String record = scanRecord.toString();
        Log.i(TAG, "record: " + record);
        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                    ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { //Identifies correct data length
                patternFound = true;
                break;
            }
            startByte++;
        }
        if (patternFound) {
            //Convert to hex String
            byte[] uuidBytes = new byte[16];
            System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
            String hexString = bytesToHex(uuidBytes);

            //Here is your UUID
            String uuid = hexString.substring(0, 8) + "-" +
                    hexString.substring(8, 12) + "-" +
                    hexString.substring(12, 16) + "-" +
                    hexString.substring(16, 20) + "-" +
                    hexString.substring(20, 32);

            //Here is your Major value
            int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

            //Here is your Minor value
            int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);
            Log.i(TAG, "uuid: " + hexString + ", major: " + major + ", minor: " + minor);

            ib = new iBeacon(uuid, major, minor);

        }
        return ib;

    }

}
