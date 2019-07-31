package com.shwody.bledebugger.control;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.shwody.bledebugger.R;
import com.shwody.bledebugger.adapter.TabPageAdapter;
import com.shwody.bledebugger.helper.BleConnectionHelper;

import java.util.ArrayList;
import java.util.List;

public class BleControlActivity extends AppCompatActivity {

    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    private String mAddress;
    private String mDeviceName;
    private BleConnectionHelper mConnectionHelper;
    private static final String TAG = "BleControlActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        TabLayout tabLayout = findViewById(R.id.tablayout);
        ViewPager viewPager = findViewById(R.id.viewpager);
        Intent intent = getIntent();
        if (getIntent() != null) {
            mAddress = intent.getStringExtra(DEVICE_ADDRESS);
            mDeviceName = intent.getStringExtra(DEVICE_NAME);
        }
        //设置toolbar
        setSupportActionBar(toolbar);
        //给左上角图标的左边加上一个返回的图标 。对应ActionBar.DISPLAY_HOME_AS_UP
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //使自定义的普通View能在title栏显示，即actionBar.setCustomView能起作用，对应ActionBar.DISPLAY_SHOW_CUSTOM
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        //这个小于4.0版本的默认值为true的。但是在4.0及其以上是false,决定左上角的图标是否可以点击。。
        getSupportActionBar().setHomeButtonEnabled(true);
        //使左上角图标是否显示，如果设成false，则没有程序图标，仅仅就个标题，否则，显示应用程序图标，
        // 对应id为android.R.id.home，对应ActionBar.DISPLAY_SHOW_HOME
        //其中setHomeButtonEnabled和setDisplayShowHomeEnabled共同起作用，
        //如果setHomeButtonEnabled设成false，即使setDisplayShowHomeEnabled设成true，图标也不能点击
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        //对应ActionBar.DISPLAY_SHOW_TITLE。
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(mDeviceName + "\r\n" + mAddress);
        //android.R.id.home导航
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //设置viewpager+tablayout

        List<Fragment> fragments = new ArrayList<>();
        BleControlFragment bleControlFragment = BleControlFragment.newInstance(mAddress, mDeviceName);
        BleControlLogFragment bleControlLogFragment = BleControlLogFragment.newInstance(mAddress, mDeviceName);
        fragments.add(bleControlFragment);
        fragments.add(bleControlLogFragment);
        String[] titles = getResources().getStringArray(R.array.fragment_control_title);
        TabPageAdapter adapter = new TabPageAdapter(getSupportFragmentManager(), fragments, titles);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    public static void switchToBleControlActivity(Context context, String deviceName, String address) {
        Intent intent = new Intent(context, BleControlActivity.class);
        intent.putExtra(DEVICE_NAME, deviceName);
        intent.putExtra(DEVICE_ADDRESS, address);
        context.startActivity(intent);
    }
}
