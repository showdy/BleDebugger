package com.shwody.bledebugger.adapter;

import android.animation.LayoutTransition;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
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
            tvServiceName.setText("ServiceName: " + GattAttributes.lookup(uuid.toString(), "Unknown service"));
            //设置uuid
            tvServiceUuid.setText("UUID: " + uuid.toString());
            //遍历每个service下的characteristic
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


            //设置characteristic名称
            tvCharacteristic.setText("Characteristic");
            //设置characteristic uuid
            tvCharUuid.setText("UUID: " + characteristic.getUuid().toString());
            int properties = characteristic.getProperties();
            String propertiesName = getPropertiesName(properties);
            //设置characteristic properties
            tvProperties.setText(propertiesName);
            if (propertiesName.contains("Read")) {
                ivRead.setVisibility(View.VISIBLE);
                ivRead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onCharacterisitcRead(characteristic, v);
                        }
                    }
                });
            } else if (propertiesName.contains("Write no response") || propertiesName.contains("Write")) {
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
                tvDescriptorName.setText("Characteristic");
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


        }

        private String getPropertiesName(int properties) {

            StringBuilder builder = new StringBuilder();

            if ((properties & PROPERTY_BROADCAST) != 0) {
                builder.append("Broadcast,");
            } else if ((properties & PROPERTY_READ) != 0) {
                builder.append("Read,");
            } else if ((properties & PROPERTY_WRITE_NO_RESPONSE) != 0) {
                builder.append("Write no response,");
            } else if ((properties & PROPERTY_WRITE) != 0) {
                builder.append("Write,");
            } else if ((properties & PROPERTY_NOTIFY) != 0) {
                builder.append("Notify,");
            } else if ((properties & PROPERTY_INDICATE) != 0) {
                builder.append("Indicate,");
            } else if ((properties & PROPERTY_SIGNED_WRITE) != 0) {
                builder.append("Signed Write,");
            } else if ((properties & PROPERTY_EXTENDED_PROPS) != 0) {
                builder.append("Extend Props,");
            }
            if (builder.toString().trim().length() > 0) {
                return builder.substring(0, builder.length() - 1);
            }
            return "";
        }
    }

    public interface OnGattServiceListener {
        void onCharacterisitcRead(BluetoothGattCharacteristic characteristic, View view);

        void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, View v);

        void onDescriptorRead(BluetoothGattDescriptor descriptor, View v);

        void onDescriptorWrite(BluetoothGattDescriptor descriptor, View v);
    }

    public OnGattServiceListener mListener;

    public void setListener(OnGattServiceListener listener) {
        mListener = listener;
    }
}
