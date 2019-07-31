package com.shwody.bledebugger.parser;

import java.util.HashMap;

public class GattAttributes {
    public static HashMap<String,String> sAttributes= new HashMap<>();

    static {
        //Gatt Service
        sAttributes.put("00001800-0000-1000-8000-00805f9b34fb","GenericAccess");
        sAttributes.put("00001801-0000-1000-8000-00805f9b34fb","GenericAttribute");
        sAttributes.put("00001802-0000-1000-8000-00805f9b34fb","Immediate Alert");
        sAttributes.put("00001803-0000-1000-8000-00805f9b34fb","Link Loss");
        sAttributes.put("00001804-0000-1000-8000-00805f9b34fb","Tx Power");
        sAttributes.put("00001805-0000-1000-8000-00805f9b34fb","Current Time Service");
        sAttributes.put("00001806-0000-1000-8000-00805f9b34fb","Reference Time Update Service");
        sAttributes.put("00001807-0000-1000-8000-00805f9b34fb","Next DST Change Service");

        sAttributes.put("00001808-0000-1000-8000-00805f9b34fb","Glucose");
        sAttributes.put("00001809-0000-1000-8000-00805f9b34fb","Health Thermometer");
        sAttributes.put("0000180a-0000-1000-8000-00805f9b34fb","Device Information");
        sAttributes.put("0000180b-0000-1000-8000-00805f9b34fb","Network Availability");
        sAttributes.put("0000180d-0000-1000-8000-00805f9b34fb","Heart Rate");
        sAttributes.put("0000180e-0000-1000-8000-00805f9b34fb","Phone Alert Status Service");

        sAttributes.put("0000180f-0000-1000-8000-00805f9b34fb","Battery Service");
        sAttributes.put("00001810-0000-1000-8000-00805f9b34fb","Blood Pressure");
        sAttributes.put("00001811-0000-1000-8000-00805f9b34fb","Alert Notification Service");
        sAttributes.put("00001812-0000-1000-8000-00805f9b34fb","Human Interface Device");
        sAttributes.put("00001813-0000-1000-8000-00805f9b34fb","Scan Parameters");
        sAttributes.put("00001814-0000-1000-8000-00805f9b34fb","Running Speed and Cadence");
        sAttributes.put("00001816-0000-1000-8000-00805f9b34fb","Cycling Speed and Cadence");
        sAttributes.put("00001818-0000-1000-8000-00805f9b34fb","Cycling Power");
        sAttributes.put("00001819-0000-1000-8000-00805f9b34fb","Location and Navigation");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = sAttributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
