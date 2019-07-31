package com.shwody.bledebugger.scan;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.shwody.bledebugger.R;
import com.shwody.bledebugger.parser.BluetoothLeUtils;

import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.*;
import static com.shwody.bledebugger.scan.AdvertiseAttributes.*;

public class BleAdvertiseFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "BleAdvertiseFragment";
    private static final int REQUEST_BLE_ENABLE = 0xFF;
    //广播时间(设置为0则持续广播)
    private int mBroadcastTime = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mLeAdvertiser;
    private BluetoothGattServer mGattServer;
    private TextView mTvDisplay;
    private AppCompatActivity mActivity;
    private BluetoothManager mBluetoothManager;


    public static BleAdvertiseFragment newInstance() {
        return new BleAdvertiseFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_advertise, container, false);
        mTvDisplay = view.findViewById(R.id.tv_display);
        view.findViewById(R.id.btn_send).setOnClickListener(this);
        view.findViewById(R.id.btn_stop).setOnClickListener(this);
        view.findViewById(R.id.btn_add_service).setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }


    @Override
    public void onClick(View v) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Snackbar.make(mTvDisplay, "must be Android5 above", Snackbar.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.btn_send:
                startAdvertising();
                break;
            case R.id.btn_stop:
                stopAdvertising();
                break;
            case R.id.btn_add_service:
                addGattServer();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLE_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            mActivity.finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startAdvertising() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLE_ENABLE);
        }
        mBluetoothAdapter.setName("showdy_00ff");
        //初始化广播设置
        //广播模式：高功率低延迟
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                //广播模式：高功率低延迟
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setTimeout(mBroadcastTime)
                .setConnectable(true)
                .build();

        //初始化广播报文
        //设置广播中是否包含设备名
        //设置广播包是否包含发射功率
        //设置uuid
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                //设置广播中是否包含设备名
                .setIncludeDeviceName(true)
                //设置广播包是否包含发射功率
                .setIncludeTxPowerLevel(true)
                //设置uuid
                .addServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                .addServiceUuid(ParcelUuid.fromString(SERVICE2_UUID))
                .build();

        //设置广播响应报文（可选）
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceData(ParcelUuid.fromString(SERVICE2_UUID), SERVICE_DATA)
                .addManufacturerData(0x06, MANUFACTURE_DATA)
                .build();
        //获取BLE广播操作对象
        //官网建议获取mBluetoothLeAdvertiser时，先做mBluetoothAdapter.isMultipleAdvertisementSupported判断，
        //但部分华为手机支持Ble广播却还是返回false,所以最后以mBluetoothLeAdvertiser是否不为空且蓝牙打开为准
        mLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mLeAdvertiser == null || !mBluetoothAdapter.isEnabled()) {
            Snackbar.make(mTvDisplay, "phone not support ble broadcast", Snackbar.LENGTH_SHORT).show();
            return;
        }
        boolean supported = mBluetoothAdapter.isMultipleAdvertisementSupported();
        mLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, scanResponseData, mAdvertiseCallback);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAdvertising() {
        if (mLeAdvertiser == null || !mBluetoothAdapter.isEnabled()) {
            return;
        }
        mLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        disPlay("Stop Broadcast");
    }


    private void addGattServer() {
        //初始化服务
        BluetoothGattService gattService = new BluetoothGattService(UUID.fromString(SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //初始化Characteristic
        BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC_UUID),
                PROPERTY_READ | PROPERTY_WRITE | PROPERTY_NOTIFY,
                PERMISSION_READ | PERMISSION_WRITE);
        gattCharacteristic.setValue(new byte[]{0x11, 0x12, 0x22, 0x01});
        //设置只读Characteristic
        BluetoothGattCharacteristic readGattCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC2_UUID),
                PROPERTY_READ, PERMISSION_READ);
        //初始化Descriptor
        BluetoothGattDescriptor gattDescriptor = new BluetoothGattDescriptor(UUID.fromString(NOTIFICATION_DESCRIPTOR_UUID),
                BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        gattDescriptor.setValue(new byte[]{0x01, 0x22, 0x11, 0x02});

        gattService.addCharacteristic(gattCharacteristic);
        gattService.addCharacteristic(readGattCharacteristic);
        gattCharacteristic.addDescriptor(gattDescriptor);
        mGattServer = mBluetoothManager.openGattServer(mActivity, mServerCallback);
        mGattServer.addService(gattService);
    }

    private void disPlay(String log) {
        Log.d(TAG, "disPlay: " + Thread.currentThread().getName());
        mTvDisplay.post(new Runnable() {
            @Override
            public void run() {
                mTvDisplay.setText(String.format("%s\r\n%s", mTvDisplay.getText().toString(), log));
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (mBroadcastTime != 0) {
                mTvDisplay.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disPlay("Ble broadcast over!");
                    }
                }, mBroadcastTime);
            }
            StringBuilder builder = new StringBuilder("Ble start broadcast success!");
            if (settingsInEffect.isConnectable()) {
                builder.append(" Ble can be connectable!");
            } else {
                builder.append(" Ble can't be connectable!");
            }
            if (settingsInEffect.getTimeout() == 0) {
                builder.append(" Continue to broadcast!");
            } else {
                builder.append(" Broadcast period:" + settingsInEffect.getTimeout());
            }
            disPlay(builder.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            switch (errorCode) {
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    disPlay("start broadcast failed, advertise data excel 31 bits!");
                    break;
                case ADVERTISE_FAILED_ALREADY_STARTED:
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    disPlay("start broadcsat failed, " + errorCode);
                    break;
                default:
                    break;
            }

        }
    };
    private BluetoothGattServerCallback mServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            disPlay("onConnectionStateChange: \r\n");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    disPlay(device.getAddress() + " : connect success!");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    disPlay(device.getAddress() + " disconnect success");
                }
            } else {
                disPlay("connection state changed: " + status);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            disPlay("onServiceAdded: \r\n");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                disPlay("add service success, UUID=" + service.getUuid().toString());
            } else {
                disPlay("add service failed.");
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            disPlay("onCharacteristicReadRequest: \r\n");
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            disPlay(device.getAddress() + ",read characteristic, UUID: " + characteristic.getUuid().toString()
                    + ", read value: " + BluetoothLeUtils.bytesToHexString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            disPlay("onCharacteristicWriteRequest:\r\n");
            //更新特征值
            characteristic.setValue(value);
            //响应客户端
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            disPlay(device.getAddress() + ", write Characteristic, UUID:" + characteristic.getUuid().toString()
                    + ",write value: " + BluetoothLeUtils.bytesToHexString(value));
            //模拟数据处理
            mTvDisplay.postDelayed(new Runnable() {
                @Override
                public void run() {
                    characteristic.setValue(value);
                    //通知客户端，让客户端读取新的特征值
                    mGattServer.notifyCharacteristicChanged(device, characteristic, false);
                }
            }, 100);

        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            disPlay("onDescriptorReadRequest:\r\n");
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
            disPlay(device.getAddress() + ",read descriptor, uuid:" + descriptor.getUuid().toString()
                    + ", read value: " + BluetoothLeUtils.bytesToHexString(descriptor.getValue()));
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite,
                                             boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            disPlay("onDescriptorWriteRequest:\r\n");
            descriptor.setValue(value);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            disPlay(device.getAddress() + ",write descriptor, uuid:" + descriptor.getUuid().toString()
                    + ",write value:" + BluetoothLeUtils.bytesToHexString(value));

        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            disPlay("onNotificationSent:\r\n");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                disPlay(device.getAddress() + " send notification success!");
            } else {
                disPlay(device.getAddress() + "send notification failed!");
            }
        }
    };
}
