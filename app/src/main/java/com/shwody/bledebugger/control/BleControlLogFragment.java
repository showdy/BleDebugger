package com.shwody.bledebugger.control;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

public class BleControlLogFragment extends Fragment {
    public static BleControlLogFragment newInstance(String address, String deviceName) {
        BleControlLogFragment fragment = new BleControlLogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("Address", address);
        bundle.putString("deviceName", deviceName);
        fragment.setArguments(bundle);
        return fragment;
    }
}
