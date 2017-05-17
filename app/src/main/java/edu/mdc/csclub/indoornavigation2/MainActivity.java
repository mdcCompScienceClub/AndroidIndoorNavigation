package edu.mdc.csclub.indoornavigation2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
//https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
//https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner.html#startScan(java.util.List<android.bluetooth.le.ScanFilter>, android.bluetooth.le.ScanSettings, android.bluetooth.le.ScanCallback)
///http://www.truiton.com/2015/04/android-bluetooth-low-energy-ble-example/
//http://kittensandcode.blogspot.com/2014/08/ibeacons-and-android-parsing-uuid-major.html

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    private ScanCallback mScanCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =  (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (Build.VERSION.SDK_INT >= 21)
         mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i("callbackType", String.valueOf(callbackType));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.i("result", result.toString());
                    BluetoothDevice btDevice = result.getDevice();


                    Log.i(TAG, "Address: "+ btDevice.getAddress());
                    Log.i(TAG, "TX Power Level: " + result.getScanRecord().getTxPowerLevel());
                    Log.i(TAG, "RSSI in DBm: " + result.getRssi());
                    ScanRecord mScanRecord = result.getScanRecord();
                    Log.i(TAG, "Manufacturer data: "+ mScanRecord.getManufacturerSpecificData());
                    Log.i(TAG, "device name: "+ mScanRecord.getDeviceName());
                    Log.i(TAG, "ADvertise flag: "+ mScanRecord.getAdvertiseFlags());
                    Log.i(TAG, "service uuids: "+ mScanRecord.getServiceUuids());
                    Log.i(TAG, "Service data: "+ mScanRecord.getServiceData());
                    byte[] recordBytes = mScanRecord.getBytes();
                    String record = recordBytes.toString();



                    Log.i(TAG, "record: "+ record);
                    int startByte = 2;
                    boolean patternFound = false;
                    while (startByte <= 5) {
                        if (    ((int) recordBytes[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                                ((int) recordBytes[startByte + 3] & 0xff) == 0x15) { //Identifies correct data length
                            patternFound = true;
                            break;
                        }
                        startByte++;
                    }

                    if (patternFound) {
                        //Convert to hex String
                        byte[] uuidBytes = new byte[16];
                        System.arraycopy(recordBytes, startByte+4, uuidBytes, 0, 16);
                        String hexString = bytesToHex(uuidBytes);

                        //Here is your UUID
                        String uuid =  hexString.substring(0,8) + "-" +
                                hexString.substring(8,12) + "-" +
                                hexString.substring(12,16) + "-" +
                                hexString.substring(16,20) + "-" +
                                hexString.substring(20,32);

                        //Here is your Major value
                        int major = (recordBytes[startByte+20] & 0xff) * 0x100 + (recordBytes[startByte+21] & 0xff);

                        //Here is your Minor value
                        int minor = (recordBytes[startByte+22] & 0xff) * 0x100 + (recordBytes[startByte+23] & 0xff);
                        Log.i(TAG, "uuid: " + hexString + ", major: " + major + ", minor: " + minor);


                        //ADD CODE HERE
                    }


                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult sr : results) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    Log.i("ScanResult - Results", sr.toString());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan Failed", "Error Code: " + errorCode);
            }
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access.");
                builder.setMessage("Please grant location access to this app.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){
                   public void onDismiss(DialogInterface dialog){
                       if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                           requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }

    }
    /**
     * bytesToHex method
     * Found on the internet
     * http://stackoverflow.com/a/9855338
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
                //
                //0CF052C2-97CA-407C-84F8-B62AAC4E9020
                ScanFilter.Builder mBuilder = new ScanFilter.Builder();
                ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
                ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);
                byte[] uuid = getIdAsByte(UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B575555"));
                mManufacturerData.put(0, (byte) 0xBE);
                mManufacturerData.put(1, (byte) 0xAC);
                for (int i=2; i<=17; i++) {
                    mManufacturerData.put(i, uuid[i-2]);
                }
                for (int i=0; i<=17; i++) {
                    mManufacturerDataMask.put((byte)0x01);
                }
                mBuilder.setManufacturerData(76, mManufacturerData.array(), mManufacturerDataMask.array());
                ScanFilter mScanFilter = mBuilder.build();
                //filters.add(mScanFilter);

            }
            scanLeDevice(true);
        }
    }

    public static byte[] getIdAsByte(java.util.UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }




    //if (Build.VERSION.SDK_INT < 21)
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            //connectToDevice(device);
                        }
                    });
                }
            };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
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


}
