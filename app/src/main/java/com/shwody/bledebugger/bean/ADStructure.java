package com.shwody.bledebugger.bean;

import android.bluetooth.le.ScanResult;

/**
 * 广播数据单元：len（1bit）+type（1bit）+data（len-1bit）
 */
public class ADStructure {
    public int len; //1bit

    public int type;//1bit

    public byte[] data; //len-1 bit

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
