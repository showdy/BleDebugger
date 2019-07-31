package com.shwody.bledebugger.adapter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.shwody.bledebugger.R;
import com.shwody.bledebugger.bean.ADStructure;
import com.shwody.bledebugger.bean.BleDevice;
import com.shwody.bledebugger.parser.BluetoothLeUtils;
import com.shwody.bledebugger.parser.BluetoothScanRecord;

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class BleScanAdapter extends RecyclerView.Adapter<BleScanAdapter.ViewHolder> {

    private final Context mContext;
    private List<BleDevice> mBleDeviceList;

    public BleScanAdapter(Context context, List<BleDevice> bleDevices) {
        this.mContext = context;
        this.mBleDeviceList = bleDevices;
    }

    @NonNull
    @Override
    public BleScanAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item_scan_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BleScanAdapter.ViewHolder holder, int position) {
        BleDevice bleDevice = mBleDeviceList.get(position);
        BluetoothDevice device = bleDevice.device;
        //设置默认值
        holder.ivIcon.setImageResource(R.drawable.bluetoothon);
        holder.tvName.setText(!TextUtils.isEmpty(device.getName())
                ? device.getName() : "N/A");
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                holder.tvBond.setText("Bonded");
                break;
            case BluetoothDevice.BOND_BONDING:
            case BluetoothDevice.BOND_NONE:
            default:
                holder.tvBond.setText("Not Bond");
                break;
        }
        holder.tvAddress.setText(device.getAddress());
        holder.tvRssi.setText(String.format(Locale.getDefault(), "%1$d dBm", bleDevice.rssi));
        holder.cvConnect.setVisibility(bleDevice.isConnectable ? View.VISIBLE : View.INVISIBLE);

        //解析广播数据
        StringBuilder builder = new StringBuilder();
        //1.设备类型
        int deviceType = bleDevice.device.getType();
        //可以使用 {@link BluetoothClass.Device}区别设备
        switch (deviceType) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                builder.append("Device Type: Classic").append("\r\n");
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                builder.append("Device Type: Dual").append("\r\n");
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                builder.append("Device Type: Le").append("\r\n");
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
            default:
                builder.append("Device Type: Unknown").append("\r\n");
                break;
        }
        BluetoothScanRecord record = BluetoothScanRecord.parseFromBytes(bleDevice.raw);
        StringBuilder sb = getAdvertiseInfo(record, holder);
        holder.tvAdvertise.setText(builder.append(sb).toString());

        //设置广播raw data
        List<ADStructure> adStructureList = record.getTypeFieldData();
        holder.tvRaw.setText(BluetoothLeUtils.bytesToHexString(record.getBytes()));
        StringBuilder rawBuilder = new StringBuilder();
        rawBuilder.append("Len(1b)--AD Type(1b)--AD Data(Len-1 b)").append("\r\n");
        for (ADStructure adStructure : adStructureList) {
            rawBuilder.append(adStructure.len)
                    .append("--")
                    .append(String.format(Locale.getDefault(), "0x%02X", adStructure.type))
                    .append("--")
                    .append("0x" + BluetoothLeUtils.bytesToHexString(adStructure.data))
                    .append("\r\n");
        }
        holder.tvAD.setText(rawBuilder.toString());
        holder.tvRaw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemRawClickListener != null) {
                    mOnItemRawClickListener.onRawClick(v, position, bleDevice.raw);
                }
            }
        });
        holder.tvConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemConnectClickListener != null) {
                    mOnItemConnectClickListener.OnItemConnectClick(position, v);
                }
            }
        });

        //动画
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //简单的隐藏或者显示
                int visibility = holder.layoutCompat.getVisibility();
                holder.layoutCompat.setVisibility(visibility == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
    }

    private StringBuilder getAdvertiseInfo(BluetoothScanRecord scanRecord, ViewHolder holder) {
        StringBuilder builder = new StringBuilder();
        //2. flags
        int flags = scanRecord.getAdvertiseFlags();
        if (flags != -1) {
            builder.append("AD flags:")
                    .append("<")
                    .append(String.format(Locale.getDefault(), "0x%02X", flags))
                    .append(">")
                    .append(BluetoothLeUtils.parserFlags(flags)).append("\r\n");
        }
        //3.UUID
        List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (serviceUuids != null && serviceUuids.size() != 0) {
            //无法拿到ad type，无法区别uuid的类型
            builder.append("Service uuid:");
            for (int i = 0; i < serviceUuids.size(); i++) {
                builder.append(serviceUuids.get(i).getUuid().toString()).append(";");
            }
            builder.append("\r\n");
        }

        //4.设备名
        String deviceName = scanRecord.getDeviceName();
        if (!TextUtils.isEmpty(deviceName)) {
            builder.append("Complete Local Name：" + deviceName)
                    .append("<")
                    .append(BluetoothLeUtils.bytesToHexString(deviceName.getBytes()))
                    .append(">\r\n");
        }
        //5.厂商信息
        SparseArray<byte[]> manuDatas = scanRecord.getManufacturerSpecificData();
        if (manuDatas != null && manuDatas.size() != 0) {
            builder.append("Manufacturer data: \r\n");
            for (int i = 0; i < manuDatas.size(); i++) {
                int key = manuDatas.keyAt(i);
                builder.append("Company:");
                switch (key) {
                    case 0x4C:
                        holder.ivIcon.setImageResource(R.drawable.apple);
                        builder.append("Apple,Inc.").append("\r\n");
                        break;
                    case 0x59:
                        holder.ivIcon.setImageResource(R.drawable.ic_device_nordic);
                        builder.append("Nordic Semiconductor ASC").append("\r\n");
                        break;
                    case 0x06:
                        holder.ivIcon.setImageResource(R.drawable.windows);
                        builder.append("Microsoft.").append("\r\n");
                        break;
                    default:
                        holder.ivIcon.setImageResource(R.drawable.bluetoothon);
                        break;

                }
                builder.append("<")
                        .append(String.format(Locale.getDefault(), "0x%02X", key))
                        .append(">")
                        .append("0x")
                        .append(BluetoothLeUtils.bytesToHexString(manuDatas.get(key)));
            }
            builder.append("\r\n");
        }
        return builder;
    }

    @NonNull
    @Override
    public int getItemCount() {
        return mBleDeviceList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView ivIcon;
        AppCompatTextView tvName;
        AppCompatTextView tvAddress;
        AppCompatTextView tvBond;
        AppCompatTextView tvConnect;
        CardView cvConnect;
        AppCompatTextView tvRssi;
        AppCompatTextView tvAD;
        AppCompatTextView tvAdvertise;
        AppCompatTextView tvRaw;
        LinearLayoutCompat layoutCompat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvAddress = itemView.findViewById(R.id.tv_device_address);
            tvBond = itemView.findViewById(R.id.tv_device_bond);
            tvConnect = itemView.findViewById(R.id.tv_connect);
            cvConnect = itemView.findViewById(R.id.cv_connect);
            tvRssi = itemView.findViewById(R.id.tv_rssi);
            tvAdvertise = itemView.findViewById(R.id.tv_advertise);
            tvRaw = itemView.findViewById(R.id.tv_raw_data_value);
            tvAD = itemView.findViewById(R.id.tv_raw_details_value);
            layoutCompat = itemView.findViewById(R.id.ll_raw_info);
        }
    }

    public interface OnItemRawClickListener {
        void onRawClick(View view, int position, byte[] raw);
    }

    public OnItemRawClickListener mOnItemRawClickListener;

    public void setOnItemRawClickListener(OnItemRawClickListener onItemRawClickListener) {
        mOnItemRawClickListener = onItemRawClickListener;
    }

    public interface OnItemConnectClickListener {
        void OnItemConnectClick(int position, View view);
    }

    public OnItemConnectClickListener mOnItemConnectClickListener;

    public void setOnItemConnectClickListener(OnItemConnectClickListener onItemClickListener) {
        mOnItemConnectClickListener = onItemClickListener;
    }
}
