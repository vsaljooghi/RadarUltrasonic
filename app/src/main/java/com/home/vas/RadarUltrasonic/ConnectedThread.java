package com.home.vas.RadarUltrasonic;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ConnectedThread extends Thread {

    private final String TAG = "CONNECTED_THREAD";
    private boolean stop = false;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private BufferedReader mBufferedReader;
    private BufferedWriter mBufferedWriter;
    private Handler mUIHandler;
    private String[] split = new String[3];

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        mUIHandler = handler;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mmSocket.getInputStream();
            tmpOut = mmSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {

        mBufferedReader = new BufferedReader(new InputStreamReader(mmInStream));
        mBufferedWriter = new BufferedWriter(new OutputStreamWriter(mmOutStream));

        String line = null;
        while(!stop) {
            try {
                if (mBufferedReader.ready()) {
                    line = mBufferedReader.readLine();
                    Log.i(TAG, "Angle, Dist1, Dist2: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (line != null) {
                Message msg = new Message();
                msg.what = Messages.RECEIVED_MESSAGE;
                split = line.split(",");
                if(split[0] != null && split[1] != null && split[2] != null) {
                    msg.arg1 = Integer.parseInt(split[0]);
                    msg.arg2 = Integer.parseInt(split[1]);
                    msg.obj = Integer.parseInt(split[2]);
                }

                mUIHandler.sendMessage(msg);
                line = null;
            }
        }
    }

    public void close(){
        //finish up by closing the streams and the socket
        stop = true;
        try {
            mmInStream.close();
            mmOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(int msg){

        try {
            mBufferedWriter.write(Integer.toString(msg));
            mBufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



