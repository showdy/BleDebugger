package com.shwody.bledebugger.helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.shwody.bledebugger.parser.BluetoothLeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class BleConnectionHelper {

    private static final String TAG = "BleConnectionHelper";
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothGattCallback mBluetoothGattCallback;
    public BluetoothDevice mBluetoothDevice;
    public boolean isConnected;

    public BluetoothGatt mBluetoothGatt;

    private Handler mMainHandler;
    private Handler mThreadHandler;
    private List<BluetoothGattService> mGattServiceList;
    private Context mContext;
    private String mAddress;
    private int retryCount;

    public BleConnectionHelper(Context context) {
        mContext = context.getApplicationContext();

        mMainHandler = new Handler(Looper.getMainLooper());
        HandlerThread thread = new HandlerThread("connection");
        thread.start();
        mThreadHandler = new Handler(thread.getLooper());

        mGattServiceList = new ArrayList<>();

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            throw new IllegalArgumentException("BluetoothAdapter is null!");
        }
        mBluetoothGattCallback = new BluetoothGattCallbackPlus();
    }


    //connect ble
    public void connect(String address) {
        if (mBluetoothAdapter == null || address == null || isConnected) {
            return;
        }
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        //重置重连次数
        retryCount = 0;
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false,
                            mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false,
                            mBluetoothGattCallback);
                }
            }
        });
        mAddress = address;
    }

    //发现服务
    public void discoverService() {
        if (mBluetoothGatt == null || !isConnected) {
            return;
        }
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.discoverServices();
            }
        });
    }

    public List<BluetoothGattService> getGattServiceList() {
        if (mBluetoothGatt == null || !isConnected) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }


    public void reconnect() {
        if (mBluetoothAdapter == null || isConnected) {
            return;
        }
        retryCount++;
        close();
        mThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mAddress);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false,
                            mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false,
                            mBluetoothGattCallback);
                }
            }
        }, 500);
    }


    //断开连接
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        isConnected = false;
        mBluetoothGatt.disconnect();
    }

    //关闭服务
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        if (isConnected) {
            disconnect();
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    //读取特征值
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    //写入特征值
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    //设置特征通知
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enable) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
    }

    //读取描述符
    public void readDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readDescriptor(descriptor);
    }

    //写入描述符
    public void writeDescriptor(BluetoothGattDescriptor descriptor, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        descriptor.setValue(value);
        mBluetoothGatt.writeDescriptor(descriptor);
    }


    public void clear() {
        mThreadHandler.removeCallbacksAndMessages(null);
        mMainHandler.removeCallbacksAndMessages(null);
        mThreadHandler.getLooper().quit();
        if (isConnected) {
            disconnect();
            close();
        }
        mGattServiceList.clear();
    }

    //反射调用: 发现服务时，可能存在发现不了特定的服务，或者发现的服务列表为空，使用该方式刷新Gatt服务列表
    public boolean refreshService() {
        try {
            Class<? extends Class> clazz = BluetoothGatt.class.getClass();
            Method method = clazz.getMethod("refresh", (Class<?>) null);
            return (boolean) method.invoke(mBluetoothGatt, (Object) null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }


    private class BluetoothGattCallbackPlus extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected = true;
                    if (mListener != null) {
                        mListener.onConnectionSuccess();
                    }
                    mBluetoothGatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false;
                    if (mListener != null) {
                        mListener.disConnection();
                    }
                    mBluetoothGatt.close();
                }
            } else {
                if (retryCount < 1 && !isConnected) {
                    reconnect();
                } else {
                    if (isConnected) {
                        if (mListener != null) {
                            mListener.disConnection();
                        }
                    } else {
                        if (mListener != null) {
                            mListener.onConnectionFail();
                        }
                    }
                    close();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGattServiceList.clear();
                //重新获取新列表
                List<BluetoothGattService> services = mBluetoothGatt.getServices();
                if (services == null) {
                    refreshService();
                    services = mBluetoothGatt.getServices();
                }
                if (mListener != null) {
                    mListener.discoveredServices();
                }
            } else {
                Log.w(TAG, "onServicesDiscovered: failed");
                //尝试刷新列表
                refreshService();
                //再次发现服务
                discoverService();
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                String data = BluetoothLeUtils.bytesToHexString(value);
                if (mListener != null) {
                    mListener.readCharacteristic(data);
                }
            } else if (BluetoothGatt.GATT_READ_NOT_PERMITTED == status) {
                if (mListener != null) {
                    mListener.readCharacteristic("have no read permission");
                }
            } else {
                if (mListener != null) {
                    mListener.readCharacteristic("read characteristic failed,status=" + status);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                String data = BluetoothLeUtils.bytesToHexString(value);
                if (mListener != null) {
                    mListener.writeCharacteristic(data);
                }
            } else {
                if (mListener != null) {
                    mListener.writeCharacteristic("write characteristic failed: status=" + status);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
            String data = BluetoothLeUtils.bytesToHexString(value);
            if (mListener != null) {
                mListener.characteristicChange(data);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = descriptor.getValue();
                String data = BluetoothLeUtils.bytesToHexString(value);
                if (mListener != null) {
                    mListener.readDescriptor(data);
                }
            } else {
                if (mListener != null) {
                    mListener.characteristicChange("read descriptor failed,status=" + status);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = descriptor.getValue();
                String data = BluetoothLeUtils.bytesToHexString(value);
                if (mListener != null) {
                    mListener.writeDescriptor(data);
                }
            } else {
                if (mListener != null) {
                    mListener.characteristicChange("write descriptor failed,status=" + status);
                }
            }
        }
    }

    private void connectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null) {
                            mListener.onConnectionSuccess();
                        }
                    }
                });
            }
        }
    }

    public interface BleConnectListener {
        void onConnectionSuccess();

        void onConnectionFail();

        void disConnection();

        void discoveredServices();

        void readCharacteristic(String value);

        void writeCharacteristic(String value);

        void readDescriptor(String value);

        void writeDescriptor(String value);

        void characteristicChange(String value);
    }

    public BleConnectListener mListener;

    public void setOnBleConnectListener(BleConnectListener listener) {
        mListener = listener;
    }
}
