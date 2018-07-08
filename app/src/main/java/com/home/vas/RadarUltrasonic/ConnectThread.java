package com.home.vas.RadarUltrasonic;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import java.io.IOException;
import java.util.UUID;
import android.util.Log;

public class ConnectThread extends Thread {

    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  //famous Serial Port Profile UUID
    private final String TAG = "CONNECT_THREAD";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private Handler mUIHandler;

    public ConnectThread(BluetoothDevice device,Handler handler) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        mUIHandler = handler;

        try {
            tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(myUUID);  //RFCOMM: serial port emulation
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
            e.printStackTrace();
        }
        mmSocket = tmp;
    }


    public BluetoothSocket getSocket(){
        return mmSocket;
    }

    public void run() {
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();

        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.e(TAG, "Failed to connect", connectException);
            mUIHandler.sendEmptyMessage(Messages.CONNECTION_FAILURE);
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Failed to close the socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread spawned from UI main thread.
            mUIHandler.sendEmptyMessage(Messages.CONNECTION_SUCCESS);
    }

    public void close(){
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
