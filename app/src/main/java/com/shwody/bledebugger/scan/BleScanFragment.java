package com.shwody.bledebugger.scan;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.shwody.bledebugger.control.BleControlActivity;
import com.shwody.bledebugger.helper.BleScanHelper;
import com.shwody.bledebugger.R;
import com.shwody.bledebugger.adapter.BleScanAdapter;
import com.shwody.bledebugger.bean.BleDevice;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BleScanFragment extends Fragment {

    private static final int REQUEST_BLE_ENABLE = 0x10;
    private static final int REQUEST_FINE_LOCATION = 0x1f;
    public static final int REQUEST_SETTING = 0x2f;
    private RecyclerView mRvScanList;
    private SwipeRefreshLayout mRefreshLayout;
    private Activity mActivity;
    private BleScanAdapter mScanAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private static final String TAG = "BleScanFragment";
    private BleScanHelper mScanHelper;
    public static final int TIME_OUT = 10 * 1000;
    private List<BleDevice> mBleDeviceList = new ArrayList<>();


    public static BleScanFragment newInstance() {
        return new BleScanFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        mRvScanList = view.findViewById(R.id.rv_ble_scan);
        mRefreshLayout = view.findViewById(R.id.swl_ble_scan);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initRecyclerView();
        initSwipeRefreshLayout();
        //android6.0以上需要定位权限
        initBluetooth();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestFineLocationPermission();
            } else {
                mScanHelper.startScan(TIME_OUT);
            }
        } else {
            mScanHelper.startScan(TIME_OUT);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actions_filter:
                //TODO:设置过滤条件
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestFineLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(mRefreshLayout, "Scan need ACCESS_FINE_LOCATION permission", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(mActivity,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_LOCATION);
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        mRvScanList.setLayoutManager(layoutManager);
        mRvScanList.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration itemDecoration = new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(getResources().getDrawable(R.drawable.rv_divider));
        mRvScanList.addItemDecoration(itemDecoration);
        mScanAdapter = new BleScanAdapter(mActivity, mBleDeviceList);
        mRvScanList.setAdapter(mScanAdapter);
        mScanAdapter.setOnItemConnectClickListener(new BleScanAdapter.OnItemConnectClickListener() {
            @Override
            public void OnItemConnectClick(int position, View view) {
                BleDevice bleDevice = mBleDeviceList.get(position);
                BleControlActivity.switchToBleControlActivity(mActivity,
                        bleDevice.device.getName(), bleDevice.device.getAddress());
            }
        });
        mScanAdapter.setOnItemRawClickListener(new BleScanAdapter.OnItemRawClickListener() {
            @Override
            public void onRawClick(View view, int position, byte[] raw) {

            }
        });
    }

    private void initSwipeRefreshLayout() {
        mRefreshLayout.setColorSchemeColors(ContextCompat.getColor(mActivity, R.color.colorPrimary));
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //清空列表
                mBleDeviceList.clear();
                mScanAdapter.notifyDataSetChanged();
                //重新扫描
                mScanHelper.startScan(TIME_OUT);
            }
        });
    }

    private void initBluetooth() {
        //1.检查手机是否支持BLE
        if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Snackbar.make(mRvScanList, "BLE is not supported", Snackbar.LENGTH_SHORT).show();
            mActivity.finish();
        }
        //2. mActivity
        BluetoothManager bluetoothManager = (BluetoothManager) mActivity.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Snackbar.make(mRvScanList, "BLE is not supported", Snackbar.LENGTH_SHORT).show();
            mActivity.finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLE_ENABLE);
        }

        mScanHelper = new BleScanHelper(mActivity);
        mScanHelper.setOnScanListener(new BleScanHelper.OnScanListener() {
            @Override
            public void onStart() {
                mRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onNext(BleDevice device) {
                refreshScanDeviceList(device);
            }

            @Override
            public void onFinish() {
                mRefreshLayout.setRefreshing(false);
            }
        });
    }


    private void refreshScanDeviceList(BleDevice device) {
        for (int i = 0; i < mBleDeviceList.size(); i++) {
            BleDevice bleDevice = mBleDeviceList.get(i);
            if (bleDevice.device.getAddress().equals(device.device.getAddress())) {
                Log.d(TAG, "update rssi: " + device.rssi);
                bleDevice.rssi = device.rssi;
                mScanAdapter.notifyItemChanged(i);
                return;
            }
        }
        mBleDeviceList.add(device);
        mScanAdapter.notifyItemInserted(mBleDeviceList.size() - 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_BLE_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            mActivity.finish();
            return;
        } else if (requestCode == REQUEST_SETTING) {
            Snackbar.make(mRefreshLayout, R.string.setting_come_back, Snackbar.LENGTH_SHORT).show();
            requestFineLocationPermission();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mScanHelper.startScan(TIME_OUT);
            } else {
                //提示去设置界面
                Snackbar.make(mRefreshLayout, R.string.setting_hint, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + mActivity.getPackageName()));
                                startActivityForResult(intent, REQUEST_SETTING);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScanHelper.clear();
    }
}
