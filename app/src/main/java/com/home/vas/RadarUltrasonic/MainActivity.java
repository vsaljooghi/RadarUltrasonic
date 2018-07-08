package com.home.vas.RadarUltrasonic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";
    int REQUEST_ENABLE_BT = 5;
    private Context mContext;
    private MediaPlayer MPSubmarineSound;
    private MediaPlayer MPSonarSound;
    private Intent LogFileIntent;
    StringBuilder mStringBuilder;
    public static final String EXTRA_MESSAGE = "COM.NEDSA.MainActivity.MSG";
    private BluetoothAdapter mBluetoothAdapter = null;
    private Handler mUIHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread = null;
    private ArrayAdapter<String> mArrayAdapter;
    private final BroadcastReceiver mBTFoundReceiver = new BroadcastReceiver() {     // Create a BroadcastReceiver for ACTION_FOUND.
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };
    private boolean connected = false;
    private static final ScheduledExecutorService execSocketClose = Executors.newSingleThreadScheduledExecutor();
    private BluetoothSocket mBTSocket = null;
    private BluetoothDevice mBTDevice = null;
    private RadarView mRadarView;
    private TextView mCoordinatesView;
    private Spinner mBluetoothSpinner;
    private Button mConnectButton;
    private Button mLogFileButton;
    private RadioGroup mRadioGroup;
    private CheckBox mOnOffCheckBox;
    private EditText mEditTextAngle;
    private View mMainActivityLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainActivityLayout = findViewById(R.id.MainActivityLayout);
        mRadarView = (RadarView) findViewById(R.id.radarView);
        mCoordinatesView = (TextView) findViewById(R.id.textViewCoordinates);
        mBluetoothSpinner = (Spinner) findViewById(R.id.pairedDevicesSpinner);
        mConnectButton = (Button) findViewById(R.id.connectButton);
        mLogFileButton = (Button) findViewById(R.id.logFileButton);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mOnOffCheckBox = (CheckBox) findViewById(R.id.On_OffCheckBox);
        mEditTextAngle = (EditText) findViewById(R.id.editTextAngle);

        mContext = getApplicationContext();
        mOnOffCheckBox.setEnabled(false);
        mEditTextAngle.setEnabled(false);

        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            mRadioGroup.getChildAt(i).setEnabled(false);
        }

        MPSubmarineSound = MediaPlayer.create(mContext, R.raw.submarine);
        MPSubmarineSound.setScreenOnWhilePlaying(true);
        MPSubmarineSound.setLooping(true);
        MPSonarSound = MediaPlayer.create(mContext, R.raw.sonar);

        LogFileIntent = new Intent(this, LogActivity.class);
        mStringBuilder = new StringBuilder();

        Drawable background = mMainActivityLayout.getBackground();
        background.setAlpha(160);  //alpha: 255(fully opaque) , 0(fully transparent)

        initBlueTooth();

        //Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBTFoundReceiver, filter);
    }

    public void initBlueTooth() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            Toast.makeText(mContext, R.string.WarnNotSupportBT, Toast.LENGTH_LONG).show();
            finish();
        }

        bluetoothOn();  // Turn bluetooth on
    }

    private void bluetoothOn() {

        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Allow to enable Bluetooth");
            Toast.makeText(mContext, R.string.WarnEnBT, Toast.LENGTH_LONG).show();

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(mContext, R.string.InfoBTEnabled, Toast.LENGTH_SHORT).show();
            AfterBTEnabled();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Log.i(TAG, "Bluetooth successfully enabled");
            Toast.makeText(mContext, R.string.InfoBTSuccesEn, Toast.LENGTH_LONG).show();
            AfterBTEnabled();

        } else if (resultCode == RESULT_CANCELED) {
            Log.e(TAG, "Bluetooth enable request wasn't accepted by the user");
            Toast.makeText(mContext, R.string.WarnBTMustEn, Toast.LENGTH_LONG).show();
            bluetoothOn();
        }
    }

    private void AfterBTEnabled() {
        pairedDevicesList();
        mBluetoothAdapter.startDiscovery();
        initListeners();
        initHandler();
    }

    private void pairedDevicesList() {

        Set<BluetoothDevice> pairedDevices;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();

        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        mBluetoothSpinner.setAdapter(mArrayAdapter);
    }

    private void initListeners() {

        mConnectButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) {
                    getBTDevice();
                    mConnectThread = new ConnectThread(mBTDevice, mUIHandler);
                    mBTSocket = mConnectThread.getSocket();
                    // Cancel discovery because it otherwise slows down the connection.
                    mBluetoothAdapter.cancelDiscovery();
                    mConnectThread.start();
                }else{
                    BTDisconnect();
                }

            }
        });

        mLogFileButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogFileIntent.putExtra(EXTRA_MESSAGE, mStringBuilder.toString());
                startActivity(LogFileIntent);
            }
        });

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                switch (checkedId){
                    case R.id.radio_auto:
                        mOnOffCheckBox.setEnabled(true);
                        mEditTextAngle.setEnabled(false);
                        break;
                    case R.id.radio_manual:
                        if (mOnOffCheckBox.isChecked())
                            mOnOffCheckBox.performClick();
                        mOnOffCheckBox.setEnabled(false);
                        mEditTextAngle.setEnabled(true);
                        break;
                }
            }
        });

        mOnOffCheckBox.setOnClickListener(new CheckBox.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mConnectedThread != null) {
                    if (mOnOffCheckBox.isChecked()) {
                        MPSubmarineSound.start();
                        mConnectedThread.sendMsg(-2);  // To turn on servo motor

                    } else {
                        MPSubmarineSound.pause();
                        mConnectedThread.sendMsg(-1); // To turn off servo motor
                    }
                }
            }
        });

        mEditTextAngle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    int tmpAngle = -1;
                    String tmpEditTextString = mEditTextAngle.getText().toString();
                    if( tmpEditTextString != null)
                        tmpAngle = !tmpEditTextString.equals("")?Integer.parseInt(tmpEditTextString):0;

                    if ((mConnectedThread != null) && (tmpAngle >= 0) && (tmpAngle <= 360)) {
                        if(tmpAngle > 180 )
                            tmpAngle -= 180;
                        mConnectedThread.sendMsg(tmpAngle);
                    }else{
                        Toast.makeText(mContext, R.string.Warn0_360, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
                return false;
            }
        });
    }

    private void getBTDevice() {
        String selectedDevice = mArrayAdapter.getItem(mBluetoothSpinner.getSelectedItemPosition());
        String MAC = (selectedDevice.split("\n"))[1];
        mBTDevice = mBluetoothAdapter.getRemoteDevice(MAC);
    }

    private void BTDisconnect() {
        if (mConnectedThread != null) {
            if (mOnOffCheckBox.isChecked())
                mOnOffCheckBox.performClick();
        }

        mRadioGroup.clearCheck();
        mOnOffCheckBox.setEnabled(false);
        mEditTextAngle.setEnabled(false);
        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            mRadioGroup.getChildAt(i).setEnabled(false);
        }

      execSocketClose.schedule(new Runnable(){
            @Override
            public void run(){
                mConnectedThread.close();
                mConnectThread.close();
            }
        }, 6, TimeUnit.SECONDS);

        connected = false;
        mConnectButton.setText(R.string.connect);
        Toast.makeText(mContext, R.string.Disconnected, Toast.LENGTH_SHORT).show();
    }

    private void initHandler() {

        mUIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int distance2;
                String StringLDist;
                String StringLAngle;
                String StringRDist;
                String StringRAngle;
                String StringFirstLine;
                String StringSecLine;

                switch (msg.what) {
                    case Messages.CONNECTION_SUCCESS:
                        connected = true;
                        mConnectButton.setText(R.string.disconnect);
                        Toast.makeText(mContext, R.string.InfoSuccessConnected, Toast.LENGTH_SHORT).show();
                        mConnectedThread = new ConnectedThread(mBTSocket, mUIHandler);
                        mConnectedThread.start();
                        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
                            mRadioGroup.getChildAt(i).setEnabled(true);
                        }
                        break;
                    case Messages.CONNECTION_FAILURE:
                        Toast.makeText(mContext, R.string.WarnFailedConnection, Toast.LENGTH_LONG).show();
                        break;
                    case Messages.RECEIVED_MESSAGE:
                        distance2 = (Integer) msg.obj;

                        if(msg.arg1 != -1) {
                            StringLDist = String.format("%10s%3d%2s", getString(R.string.Distance1), msg.arg2, "cm");
                            StringLAngle = String.format("%7s%3d%s", getString(R.string.Angle1), msg.arg1, (char) 0x00B0);
                            StringRDist = String.format("%10s%3d%2s", getString(R.string.Distance2), distance2, "cm");
                            StringRAngle = String.format("%7s%3d%s", getString(R.string.Angle2), (180 + msg.arg1), (char) 0x00B0);
                            StringFirstLine = String.format("%-17s%-15s", StringLDist, StringRDist);
                            StringSecLine = String.format("%-17s%-11s", StringLAngle, StringRAngle);
                            mCoordinatesView.setText(StringFirstLine + "\n" + StringSecLine);
                            mStringBuilder.append(StringFirstLine + "\n" + StringSecLine + "\n");

                            if (msg.arg2 > 0 || distance2 > 0)
                                MPSonarSound.start();
                        }else {
                            mStringBuilder.setLength(0);
                        }

                        mRadarView.drawMeasurement(msg.arg1, msg.arg2, distance2);

                        break;
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister the ACTION_FOUND receiver.
        unregisterReceiver(mBTFoundReceiver);
        BTDisconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.about_item:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.help_item:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            case R.id.license_item:
                startActivity(new Intent(this, LicenseActivity.class));
                return true;
            case R.id.exit_item:
                finish();  //execute onDestroy()
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}