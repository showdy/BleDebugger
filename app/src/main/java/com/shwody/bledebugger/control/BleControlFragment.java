package com.shwody.bledebugger.control;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.shwody.bledebugger.R;
import com.shwody.bledebugger.adapter.BleControlAdapter;
import com.shwody.bledebugger.helper.BleConnectionHelper;
import com.shwody.bledebugger.scan.BleScanFragment;

import java.time.chrono.HijrahEra;
import java.util.ArrayList;
import java.util.List;

public class BleControlFragment extends Fragment {

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private String mAddress;
    private String mDeviceName;
    private Context mContext;
    private BleConnectionHelper mConnectionHelper;
    private BleControlAdapter mControlAdapter;
    private List<BluetoothGattService> mGattServiceList = new ArrayList<>();


    public static BleControlFragment newInstance(String address, String deviceName) {
        BleControlFragment fragment = new BleControlFragment();
        Bundle bundle = new Bundle();
        bundle.putString("Address", address);
        bundle.putString("deviceName", deviceName);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_control_fragment, container, false);
        mRefreshLayout = view.findViewById(R.id.swl_control_scan);
        mRecyclerView = view.findViewById(R.id.rv_control_scan);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout.setColorSchemeColors(getContext().getResources().getColor(R.color.colorPrimary));
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mConnectionHelper.discoverService();
            }
        });
        mRefreshLayout.setEnabled(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration itemDecoration = new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(mContext.getResources().getDrawable(R.drawable.rv_divider));
        mRecyclerView.addItemDecoration(itemDecoration);
        mControlAdapter = new BleControlAdapter(mContext, mGattServiceList);
        mRecyclerView.setAdapter(mControlAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = getArguments();
        mAddress = bundle.getString("Address");
        mDeviceName = bundle.getString("deviceName");
        mConnectionHelper = new BleConnectionHelper(mContext);
        mConnectionHelper.setOnBleConnectListener(mOnBleConnectListener);
        mConnectionHelper.connect(mAddress);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnectionHelper.disconnect();
        mConnectionHelper.close();
    }

    private BleConnectionHelper.BleConnectListener mOnBleConnectListener = new BleConnectionHelper.BleConnectListener() {
        @Override
        public void onConnectionSuccess() {
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setEnabled(true);
                }
            });

        }
        @Override
        public void onConnectionFail() {
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setEnabled(false);
                }
            });

        }

        @Override
        public void disConnection() {
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setEnabled(false);
                }
            });

        }

        @Override
        public void discoveredServices() {
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (mRefreshLayout.isRefreshing()) {
                        mRefreshLayout.setRefreshing(false);
                    }
                    mGattServiceList.clear();
                    mGattServiceList.addAll(mConnectionHelper.getGattServiceList());
                    mControlAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void readCharacteristic(String value) {

        }

        @Override
        public void writeCharacteristic(String value) {

        }

        @Override
        public void readDescriptor(String value) {

        }

        @Override
        public void writeDescriptor(String value) {

        }

        @Override
        public void characteristicChange(String value) {

        }
    };
}
