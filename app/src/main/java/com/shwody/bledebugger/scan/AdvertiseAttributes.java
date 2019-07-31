package com.shwody.bledebugger.scan;

import java.util.UUID;

public class AdvertiseAttributes {
    public static final String NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static String SERVICE_UUID = "0000ff1f-0000-1000-8000-00805f9b34fb";
    public static String SERVICE2_UUID = "0000ff2f-0000-1000-8000-00805f9b34fb";

    public static final String CHARACTERISTIC_UUID = "0000ff11-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC2_UUID = "0000ff22-0000-1000-8000-00805f9b34fb";


    public static byte[] SERVICE_DATA = new byte[]{12, 34, 12};
    public static byte[] MANUFACTURE_DATA = new byte[]{11, 22, 33, 0x07};

}
