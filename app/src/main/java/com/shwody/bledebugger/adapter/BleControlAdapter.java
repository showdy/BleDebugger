package com.shwody.bledebugger.adapter;

import android.animation.LayoutTransition;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shwody.bledebugger.R;
import com.shwody.bledebugger.helper.BleConnectionHelper;
import com.shwody.bledebugger.parser.GattAttributes;

import org.w3c.dom.Text;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.*;

public class BleControlAdapter extends RecyclerView.Adapter<BleControlAdapter.ViewHolder> {

    private static final String TAG = "BleControlAdapter";
    private Context mContext;
    private List<BluetoothGattService> mGattServiceList;

    public BleControlAdapter(Context context, List<BluetoothGattService> gattServiceList) {
        mContext = context;
        mGattServiceList = gattServiceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_layout_control, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothGattService gattService = mGattServiceList.get(position);
        holder.bindGattService(gattService);
    }


    @Override
    public int getItemCount() {
        return mGattServiceList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvServiceName;
        private TextView tvServiceUuid;
        private TextView tvServiceDeclarations;
        private LinearLayout llServiceDetails;
        private LinearLayout llCharacteristic;
        private LinearLayout llDescriptors;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            tvServiceUuid = itemView.findViewById(R.id.tv_service_uuid);
            tvServiceDeclarations = itemView.findViewById(R.id.tv_service_class);
            llServiceDetails = itemView.findViewById(R.id.ll_service_details);
            llCharacteristic = itemView.findViewById(R.id.characteristicLayout);
            llDescriptors = itemView.findViewById(R.id.secondaryServiceLayout);

        }

        public void bindGattService(BluetoothGattService gattService) {
            llServiceDetails.setVisibility(View.GONE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int visibility = llServiceDetails.getVisibility();
                    llServiceDetails.setVisibility(visibility == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });
            if (gattService.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY) {
                tvServiceDeclarations.setText("Primary Service");
            } else {
                tvServiceDeclarations.setText("Secondary Service");
            }
            UUID uuid = gattService.getUuid();
            //设置服务名
            tvServiceName.setText(GattAttributes.lookup(uuid.toString(), "Unknown Service"));
            //设置uuid
            tvServiceUuid.setText("UUID: " + uuid.toString());
            //遍历每个service下的characteristic

            llCharacteristic.removeAllViews();

            List<BluetoothGattCharacteristic> characteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                addCharacteristicLayout(characteristic);
            }
        }

        private void addCharacteristicLayout(BluetoothGattCharacteristic characteristic) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_layout_characteristic, llCharacteristic, false);
            TextView tvCharacteristic = view.findViewById(R.id.tv_characteristic);
            TextView tvCharUuid = view.findViewById(R.id.tv_char_uuid);
            TextView tvProperties = view.findViewById(R.id.tv_properties);
            ImageView ivRead = view.findViewById(R.id.iv_bot);
            ImageView ivWrite = view.findViewById(R.id.iv_top);

            LinearLayout layoutDescriptor = view.findViewById(R.id.layout_descriptor);

            ivRead.setVisibility(View.INVISIBLE);
            ivWrite.setVisibility(View.INVISIBLE);
            //设置characteristic名称
            tvCharacteristic.setText("Characteristic");
            //设置characteristic uuid
            tvCharUuid.setText("UUID: " + characteristic.getUuid().toString());
            int properties = characteristic.getProperties();
            String propertiesName = getPropertiesName(properties);
            //设置characteristic properties
            tvProperties.setText("PROPERTIES:" + propertiesName);
            if (propertiesName.contains("READ")) {
                ivRead.setVisibility(View.VISIBLE);
                ivRead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onCharacteristicRead(characteristic, v);
                        }
                    }
                });
            }
            if (propertiesName.contains("WRITE NO RESPONSE") || propertiesName.contains("WRITE")) {
                ivWrite.setVisibility(View.VISIBLE);
                ivWrite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onCharacteristicWrite(characteristic, v);
                        }
                    }
                });
            }
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            if (descriptors == null) {
                layoutDescriptor.setVisibility(View.GONE);
                return;
            }
            layoutDescriptor.setVisibility(View.VISIBLE);
            for (BluetoothGattDescriptor descriptor : descriptors) {
                View descriptorView = LayoutInflater.from(mContext).inflate(R.layout.layout_item_descriptor, layoutDescriptor, false);
                TextView tvDescriptorName = descriptorView.findViewById(R.id.tv_descriptor_name);
                TextView tvDesUuid = descriptorView.findViewById(R.id.tv_descriptor_uuid);
                ImageView ivDescRead = descriptorView.findViewById(R.id.iv_descriptor_bot);
                ImageView ivDescWrite = descriptorView.findViewById(R.id.iv_descriptor_top);
                tvDescriptorName.setText("Descriptor");
                tvDesUuid.setText("UUID:" + descriptor.getUuid().toString());
                ivDescRead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onDescriptorRead(descriptor, v);
                        }
                    }
                });
                ivDescWrite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onDescriptorWrite(descriptor, v);
                        }
                    }
                });
                layoutDescriptor.addView(descriptorView);
            }
            llCharacteristic.addView(view);
        }

        private String getPropertiesName(int properties) {
            Log.d(TAG, "getPropertiesName: " + properties);
            StringBuilder builder = new StringBuilder();
            if ((properties & PROPERTY_BROADCAST) != 0) {
                builder.append("BROADCAST,");
            }
            if ((properties & PROPERTY_READ) != 0) {
                builder.append("READ,");
            }
            if ((properties & PROPERTY_WRITE_NO_RESPONSE) != 0) {
                builder.append("WRITE NO RESPONSE,");
            }
            if ((properties & PROPERTY_WRITE) != 0) {
                builder.append("WRITE,");
            }
            if ((properties & PROPERTY_NOTIFY) != 0) {
                builder.append("NOTIFY,");
            }
            if ((properties & PROPERTY_INDICATE) != 0) {
                builder.append("INDICATE,");
            }
            if ((properties & PROPERTY_SIGNED_WRITE) != 0) {
                builder.append("SIGNED WRITE,");
            }
            if ((properties & PROPERTY_EXTENDED_PROPS) != 0) {
                builder.append("EXTENDED PROPS,");
            }
            if (builder.toString().trim().length() > 0) {
                return builder.substring(0, builder.length() - 1);
            }
            return "";
        }
    }

    public interface OnGattServiceListener {
        void onCharacteristicRead(BluetoothGattCharacteristic characteristic, View view);

        void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, View v);

        void onDescriptorRead(BluetoothGattDescriptor descriptor, View v);

        void onDescriptorWrite(BluetoothGattDescriptor descriptor, View v);
    }

    public OnGattServiceListener mListener;

    public void setListener(OnGattServiceListener listener) {
        mListener = listener;
    }
}
