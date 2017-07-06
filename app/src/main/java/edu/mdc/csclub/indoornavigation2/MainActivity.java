package edu.mdc.csclub.indoornavigation2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // app state
    private float XAcceleration;
    private float YAcceleration;
    private float ZAcceleration;
    private float XRotation;
    private float YRotation;
    private float ZRotation;
    private float XMagneticField;
    private float YMagneticField;
    private float ZMagneticField;

    //Bluetooth objects:
    //For all APIs (<21 and >=21, >=23)
    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 1;
    //For API >=23, dynamic permissions are needed
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    //For API >=21
    private BluetoothLeScanner mLEScanner;
    private ScanSettings scanSettings;
    private List<ScanFilter> filters;

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

    // Classifier
    private SVMClassifier SVMClassifier;

    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_logo);
        mTitle = "Entec Indoor Navigation";
        getSupportActionBar().setTitle(mTitle);
        getSupportActionBar().setSubtitle("by the MDC North CS Club");

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            ContactsFragment firstFragment = new ContactsFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment, "contacts").commit();
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "on resuming");


        //init sensor objects
        initSensors();

        //Init database
        db = new DatabaseHandler(this);
        db.createDataBase();
        db.openDataBase();

        // Create classifier
        SVMClassifier = new SVMClassifier(this);

        //init Bluetooth objects and permissions
        initBLESetup();
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
    public void onDestroy() {
        if (db != null)
            db.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            DebugFragment debugFragment = new DebugFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, debugFragment, "debug").commit();
            return true;


        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String tag = null;
        switch (id) {
            case R.id.nav_navigation:
                mTitle = getString(R.string.title_navigation);
                fragment = new NavigationFragment();
                tag = "navigation";
                break;
            case R.id.nav_contacts:
                mTitle = getString(R.string.title_contacts);
                fragment = new ContactsFragment();
                tag = "contacts";
                break;
            case R.id.nav_debug:
                mTitle = getString(R.string.title_debug);
                fragment = new DebugFragment();
                tag = "debug";
                break;
            case R.id.nav_share:
                mTitle = getString(R.string.title_share);
                //TODO
                break;
            case R.id.nav_send:
                mTitle = getString(R.string.title_send);
                //TODO
                break;
            default:
                mTitle = getString(R.string.title_contacts);
                fragment = new ContactsFragment();
                tag = "contacts";
        }

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(mTitle);

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);


        return true;
    }

    public void debugScan(View view) {
        DebugFragment debugFragment = (DebugFragment)
                getSupportFragmentManager().findFragmentByTag("debug");
        debugFragment.scan();

    }

    public void navigationScan(View view) {
        NavigationFragment navigationFragment = (NavigationFragment)
                getSupportFragmentManager().findFragmentByTag("navigation");
        navigationFragment.scan();

    }
    /////////////////////////////////////////////////////////////// Bluetooth methods ///////////////////////////////////////////////////////////////
    private void initBLESetup() {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //Beginning in Android 6.0 (API level 23), users grant permissions to apps while the app is running, not when they install the app.
        //Obtaining dynamic permissions from the user
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //23
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    }

    public void setupBLEScan() {
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
    /////////////////////////////////////////////////////////////// Sensor methods ///////////////////////////////////////////////////////////////
    public void initSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensors) {
            Log.i(TAG, "Found sensor: " + s.toString());
        }
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer != null) {
            Log.i(TAG, "CREATED ACCELEROMETER:" + mAccelerometer.toString());
            accelerometerPresent = true;
        }
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mMagnetometer != null) {
            Log.i(TAG, "CREATED MAGNETOMETER:" + mMagnetometer.toString());
            magnetometerPresent = true;
        }
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mGyroscope != null) {
            Log.i(TAG, "CREATED GYROSCOPE:" + mGyroscope.toString());
            gyroscopePresent = true;
        }
    }

    /////////////////////////////////utility methods////////////////////////////////////
    public iBeacon parseBLERecord(byte[] scanRecord) {
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

    /**
     * bytesToHex method
     * http://stackoverflow.com/a/9855338
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public byte[] getIdAsByte(java.util.UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /////////////////////////////////////////////////////////////// Other callback methods ///////////////////////////////////////////////////////////////
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If the user does not want to enable Bluetooth, we kill the app
        if (requestCode == getRequestEnableBt()) {
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

    /////////////////////////////////getters setters/////////////////////////////////


    public SensorManager getmSensorManager() {
        return mSensorManager;
    }

    public Sensor getmAccelerometer() {
        return mAccelerometer;
    }

    public WindowManager getmWindowManager() {
        return mWindowManager;
    }

    public Display getmDisplay() {
        return mDisplay;
    }

    public Sensor getmMagnetometer() {
        return mMagnetometer;
    }

    public Sensor getmGyroscope() {
        return mGyroscope;
    }

    public boolean isAccelerometerPresent() {
        return accelerometerPresent;
    }

    public boolean isMagnetometerPresent() {
        return magnetometerPresent;
    }

    public boolean isGyroscopePresent() {
        return gyroscopePresent;
    }

    public DatabaseHandler getDb() {
        return db;
    }

    public edu.mdc.csclub.indoornavigation2.SVMClassifier getSVMClassifier() {
        return SVMClassifier;
    }

    public float getXAcceleration() {
        return XAcceleration;
    }

    public void setXAcceleration(float XAcceleration) {
        this.XAcceleration = XAcceleration;
    }

    public float getYAcceleration() {
        return YAcceleration;
    }

    public void setYAcceleration(float YAcceleration) {
        this.YAcceleration = YAcceleration;
    }

    public float getZAcceleration() {
        return ZAcceleration;
    }

    public void setZAcceleration(float ZAcceleration) {
        this.ZAcceleration = ZAcceleration;
    }

    public float getXRotation() {
        return XRotation;
    }

    public void setXRotation(float XRotation) {
        this.XRotation = XRotation;
    }

    public float getYRotation() {
        return YRotation;
    }

    public void setYRotation(float YRotation) {
        this.YRotation = YRotation;
    }

    public float getZRotation() {
        return ZRotation;
    }

    public void setZRotation(float ZRotation) {
        this.ZRotation = ZRotation;
    }

    public float getXMagneticField() {
        return XMagneticField;
    }

    public void setXMagneticField(float XMagneticField) {
        this.XMagneticField = XMagneticField;
    }

    public float getYMagneticField() {
        return YMagneticField;
    }

    public void setYMagneticField(float YMagneticField) {
        this.YMagneticField = YMagneticField;
    }

    public float getZMagneticField() {
        return ZMagneticField;
    }

    public void setZMagneticField(float ZMagneticField) {
        this.ZMagneticField = ZMagneticField;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public int getRequestEnableBt() {
        return REQUEST_ENABLE_BT;
    }

    public BluetoothLeScanner getmLEScanner() {
        return mLEScanner;
    }

    public List<ScanFilter> getFilters() {
        return filters;
    }

    public ScanSettings getScanSettings() {
        return scanSettings;
    }
}
