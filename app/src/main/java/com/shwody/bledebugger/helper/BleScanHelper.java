package com.shwody.bledebugger.helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.shwody.bledebugger.bean.BleDevice;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class BleScanHelper {

    private boolean mScanning = false;

    private Handler mThreadHandler;

    private Handler mMainHandler;

    private BluetoothLeScanner mLeScanner;

    private BluetoothAdapter mBluetoothAdapter;

    private static final String TAG = "BleScanHelper";


    public interface OnScanListener {

        void onStart();

        void onNext(BleDevice device);

        void onFinish();
    }

    public OnScanListener mOnScanListener;

    public void setOnScanListener(OnScanListener listener) {
        mOnScanListener = listener;
    }

    public BleScanHelper(Context context) {

        initBluetoothAdapter(context);

        mMainHandler = new Handler(Looper.getMainLooper());
        HandlerThread thread = new HandlerThread("ScanThread");
        thread.start();
        mThreadHandler = new Handler(thread.getLooper());

    }

    private void initBluetoothAdapter(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new IllegalArgumentException("BLE is not supported");
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        if (mBluetoothAdapter == null) {
            throw new IllegalArgumentException("BLE is not supported");
        }
        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
    }

    public void startScan(int timeOut) {
        if (!mBluetoothAdapter.isEnabled() || mScanning) {
            return;
        }
        if (mOnScanListener != null) {
            mOnScanListener.onStart();
        }
        //先清除延迟任务
        mThreadHandler.removeCallbacks(null);
        //子线程中扫描
        mThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: stop scam for timeout");
                stopScan();
            }
        }, timeOut);
        mScanning = true;
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start scan");
                if (Build.VERSION.SDK_INT >= LOLLIPOP) {
                    mLeScanner.startScan(buildScanFilters(), buildScanSetting(), mScanCallback);
                } else {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            }
        });
    }


    public void stopScan() {
        if (!mBluetoothAdapter.isEnabled() || !mScanning) {
            return;
        }
        mScanning = false;
        //移除延迟任务
        mThreadHandler.removeCallbacks(null);
        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            mLeScanner.stopScan(mScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnScanListener != null) {
                    mOnScanListener.onFinish();
                }
            }
        });
    }

    @RequiresApi(api = LOLLIPOP)
    private ScanSettings buildScanSetting() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        }
        if (mBluetoothAdapter.isOffloadedScanBatchingSupported()) {
            builder.setReportDelay(0);
        }
        return builder.build();
    }

    @RequiresApi(api = LOLLIPOP)
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
//                .setServiceUuid(ParcelUuid.fromString("0000fff1-0000-1000-8000-00805f9b34fb"))
                .build();
        scanFilters.add(filter);
        return scanFilters;
    }

    public void clear() {
        mMainHandler.removeCallbacksAndMessages(null);
        mThreadHandler.removeCallbacksAndMessages(null);
        mThreadHandler.getLooper().quit();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            final BleDevice bleDevice = new BleDevice(device, rssi, scanRecord);
            Log.d(TAG, "onLeScan: " + device.toString());
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mOnScanListener != null) {
                        mOnScanListener.onNext(bleDevice);
                    }
                }
            });
        }
    };

    @RequiresApi(api = LOLLIPOP)
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult: " + result.getDevice().getAddress());
            boolean isConnectable = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || result.isConnectable();
            final BleDevice device = new BleDevice(result.getDevice(), result.getRssi(),
                    result.getScanRecord().getBytes(), isConnectable);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mOnScanListener != null) {
                        mOnScanListener.onNext(device);
                    }
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed: " + errorCode);
        }
    };
}
