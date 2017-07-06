package edu.mdc.csclub.indoornavigation2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by transflorida on 6/15/17.
 */

public class NavigationFragment extends android.support.v4.app.Fragment implements SensorEventListener {

    private static final String TAG = NavigationFragment.class.getSimpleName();

    //UI components
    private Button scanButton;
    private TextView XTextView;
    private TextView YTextView;
    private TextView roomTextView;
    private ProgressBar progressBar;
    private ImageView mapImageView;
    private Drawable marker;


    //App state
    private boolean isScanning;
    private int beacon11RSSI;
    private int beacon12RSSI;
    private int beacon21RSSI;
    private int beacon22RSSI;
    private int beacon31RSSI;
    private int beacon32RSSI;
    private Cell currentCell;
    private Room currentRoom;
    private static final int numCellsX = 50;
    private static final int numCellsY = 30;
    private static final int mapWidth = 652;
    private static final int mapHeight = 266;
    private static final int mapOffsetX = 26;
    private static final int mapOffsetY = 90;

    //Bluetooth objects:
    //For API >=21
    private ScanCallback newerVersionScanCallback;
    //For API < 21
    private BluetoothAdapter.LeScanCallback olderVersionScanCallback;

    private MainActivity mOwner;


    ///////////////////////////////////////////////////////////////  App Lifecycle Methods ///////////////////////////////////////////////////////////////

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mOwner = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //init app state
        isScanning = false;

        initBLECallbackMethods();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.navigation_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set up UI components
        scanButton = (Button) getView().findViewById(R.id.scanButton);
        XTextView = (TextView) getView().findViewById(R.id.XTextView);
        YTextView = (TextView) getView().findViewById(R.id.YTextView);
        roomTextView = (TextView) getView().findViewById(R.id.roomTextView);
        progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);
        mapImageView = (ImageView) getView().findViewById(R.id.mapImageView);

        //Make a new marker drawable
        marker = ContextCompat.getDrawable(mOwner, R.drawable.pin);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "on resuming");

        XTextView.setText(R.string.undetermined);
        YTextView.setText(R.string.undetermined);
        roomTextView.setText(R.string.undetermined);
        progressBar.setVisibility(View.INVISIBLE);
    }

    /*@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            Log.i(TAG, "width=" + mapImageView.getMeasuredWidth() + "height=" + mapImageView.getMeasuredHeight());
            //addMarker(0, 30);
        }
    }*/

    @Override
    public void onPause() {
        super.onPause();

        doScan(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    ///////////////////////////////////////////////////////////////  UI  methods ///////////////////////////////////////////////////////////////

    /**
     * Called when the user taps the Scan button
     */
    public void scan() {
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
            if (mOwner.isAccelerometerPresent())
                mOwner.getmSensorManager().registerListener(this, mOwner.getmAccelerometer(), SensorManager.SENSOR_DELAY_GAME);
            if (mOwner.isMagnetometerPresent())
                mOwner.getmSensorManager().registerListener(this, mOwner.getmMagnetometer(), SensorManager.SENSOR_DELAY_GAME);
            if (mOwner.isGyroscopePresent())
                mOwner.getmSensorManager().registerListener(this, mOwner.getmGyroscope(), SensorManager.SENSOR_DELAY_GAME);

        } else {
            isScanning = false;
            scanButton.setText(R.string.scan_message);
            progressBar.setVisibility(View.INVISIBLE);
            if (mOwner.getmBluetoothAdapter() != null && mOwner.getmBluetoothAdapter().isEnabled()) {
                scanBLEDevice(false);
            }
            mOwner.getmSensorManager().unregisterListener(this);
        }
    }

    private void updateRSSI(String uuid, int major, int minor, int rssi) {
        if (uuid.equalsIgnoreCase("B9407F30-F5F8-466E-AFF9-25556B575555")) {
            if (major == 1) {
                if (minor == 1) {
                    beacon11RSSI = rssi;
                } else if (minor == 2) {
                    beacon12RSSI = rssi;
                }
            } else if (major == 2) {
                if (minor == 1) {
                    beacon21RSSI = rssi;
                } else if (minor == 2) {
                    beacon22RSSI = rssi;
                }
            } else if (major == 3) {
                if (minor == 1) {
                    beacon31RSSI = rssi;
                } else if (minor == 2) {
                    beacon32RSSI = rssi;
                }
            }

        }
    }

    private void updateAcceleration(float XAcc, float YAcc, float ZAcc) {
        mOwner.setXAcceleration(XAcc);
        mOwner.setYAcceleration(YAcc);
        mOwner.setZAcceleration(ZAcc);
    }

    private void updateRotation(float XRot, float YRot, float ZRot) {
        mOwner.setXRotation(XRot);
        mOwner.setYRotation(YRot);
        mOwner.setZRotation(YRot);
    }

    private void updateMagneticField(float XMF, float YMF, float ZMF) {
        mOwner.setXMagneticField(XMF);
        mOwner.setYMagneticField(YMF);
        mOwner.setZMagneticField(ZMF);
    }

    private void updateCalculatedPosition() {
        if (currentCell != null) {
            XTextView.setText(String.valueOf(currentCell.getX()));
            YTextView.setText(String.valueOf(currentCell.getY()));
            Cell retrievedCell = mOwner.getDb().getCell(currentCell.getX(), currentCell.getY());
            if (retrievedCell != null) {
                currentCell.setRoomID(retrievedCell.getRoomID());
                Log.i(TAG, "Room ID: " + currentCell.getRoomID());

                currentRoom = mOwner.getDb().getRoom(currentCell.getRoomID());
                if (currentRoom != null) {
                    roomTextView.setText(String.valueOf(currentRoom.getRoomNumber()));
                } else {
                    roomTextView.setText(R.string.undetermined);
                }
            }
            addMarker(currentCell.getX(), currentCell.getY());
        } else {
            XTextView.setText(R.string.undetermined);
            YTextView.setText(R.string.undetermined);
        }

    }

    private void addMarker(int X, int Y) {
        mapImageView.getOverlay().remove(marker);
        Rect markerBounds = new Rect(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        double cellWidth = mapWidth / (double) numCellsX;
        markerBounds.offset(X * mapWidth / numCellsX + mapOffsetX - (markerBounds.width() / 2),
                Y * mapHeight / numCellsY + mapOffsetY - markerBounds.height());//translate
        //markerBounds.offset( X + mapOffsetX - (markerBounds.width() /2),
        // Y + mapOffsetY - markerBounds.height());//translate
        marker.setBounds(markerBounds);
        mapImageView.getOverlay().add(marker);
    }

    /////////////////////////////////////////////////////////////// Bluetooth methods ///////////////////////////////////////////////////////////////

    private void initBLECallbackMethods() {
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

                        iBeacon ib = mOwner.parseBLERecord(recordBytes);

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
            olderVersionScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     final byte[] scanRecord) {
                    mOwner.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());

                            iBeacon ib = mOwner.parseBLERecord(scanRecord);

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

    private void scanBLEDevice(final boolean enable) {
        if (enable) {
            if (Build.VERSION.SDK_INT < 21) {
                mOwner.getmBluetoothAdapter().startLeScan(olderVersionScanCallback);
            } else {
                mOwner.getmLEScanner().startScan(mOwner.getFilters(), mOwner.getScanSettings(), newerVersionScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mOwner.getmBluetoothAdapter().stopLeScan(olderVersionScanCallback);
            } else {
                mOwner.getmLEScanner().stopScan(newerVersionScanCallback);
            }
        }
    }

    /////////////////////////////////////////////////////////////// Sensor methods ///////////////////////////////////////////////////////////////

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
            switch (mOwner.getmDisplay().getRotation()) {
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
        return mOwner.getSVMClassifier().predict(new Measurement(-1, -1, beacon11RSSI, beacon12RSSI, beacon21RSSI, beacon22RSSI, beacon31RSSI, beacon32RSSI));
    }


}
