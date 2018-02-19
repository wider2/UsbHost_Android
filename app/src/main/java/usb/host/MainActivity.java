package usb.host;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.text.ParseException;
import java.util.HashMap;

import static usb.host.utils.Utility.writeFile;

public class MainActivity extends Activity {

    private static final String TAG = "UsbHost";
    private UsbManager mUsbManager;
    private TextView mStatusView, mResultView;
    public static Thread.UncaughtExceptionHandler androidDefaultUEH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusView = (TextView) findViewById(R.id.text_status);
        mResultView = (TextView) findViewById(R.id.text_result);

        mUsbManager = getSystemService(UsbManager.class);

        // Detach events are sent as a system-wide broadcast
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        handleIntent(getIntent());


        androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (isSDPresent) {
        } else {
        }
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            //@Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                String report = "";
                StackTraceElement[] arr = paramThrowable.getStackTrace();
                report = paramThrowable.toString() + "\r\n";
                report += "--------- Stack trace ---------\r\n" + paramThread.toString();
                for (int i = 0; i < arr.length; i++) {
                    report += "    " + arr[i].toString() + "\r\n";
                }

                Throwable cause = paramThrowable.getCause();
                if (cause != null) {
                    report += "\n------------ Cause ------------\r\n";
                    report += cause.toString() + "\r\n";
                    arr = cause.getStackTrace();
                    for (int i = 0; i < arr.length; i++) {
                        report += "    " + arr[i].toString() + "\r\n";
                    }
                }

                String rep = "";
                //rep += "Device: " + getDeviceType() + "\r\n";
                rep += "OS version: " + android.os.Build.VERSION.RELEASE + "\r\n";
                rep += "Message: " + paramThrowable.getMessage() + "\r\n";
                writeFile("CrashReport.txt", rep + report + "\r\n", false, getApplicationContext(), "");

                androidDefaultUEH.uncaughtException(paramThread, paramThrowable);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        //handleIntent(getIntent());
    }

    /**
     * Broadcast receiver to handle USB disconnect events.
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (device != null) {
                    printStatus(getString(R.string.status_removed));
                    printDeviceDescription(device);
                    //setDevice(device);
                }
            }
        }
    };


    private void handleIntent(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            printStatus(getString(R.string.status_added));
            printDeviceDetails(device);
            setDevice(device);
        } else {
            // List all devices connected to USB host on startup
            printStatus(getString(R.string.status_list));
            printDeviceList();
        }
    }


    private void printDeviceList() {
        HashMap<String, UsbDevice> connectedDevices = mUsbManager.getDeviceList();

        if (connectedDevices.isEmpty()) {
            printResult("No Devices Currently Connected");
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Connected Device Count: ");
            builder.append(connectedDevices.size());
            builder.append("\n\n");
            for (UsbDevice device : connectedDevices.values()) {
                //Use the last device detected (if multiple) to open
                builder.append(UsbHelper.readDevice(device));
                builder.append("\n\n");
            }
            printResult(builder.toString());
        }
    }


    private void printDeviceDescription(UsbDevice device) {
        String result = UsbHelper.readDevice(device) + "\n\n";
        printResult(result);
    }


    private void printDeviceDetails(UsbDevice device) {
        UsbDeviceConnection connection = mUsbManager.openDevice(device);

        String deviceString = "";
        try {
            //Parse the raw device descriptor
            deviceString = DeviceDescriptor.fromDeviceConnection(connection)
                    .toString();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid device descriptor", e);
        }

        String configString = "";
        try {
            //Parse the raw configuration descriptor
            configString = ConfigurationDescriptor.fromDeviceConnection(connection)
                    .toString();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid config descriptor", e);
        } catch (ParseException e) {
            Log.w(TAG, "Unable to parse config descriptor", e);
        }

        printResult(deviceString + "\n\n" + configString);
        connection.close();
    }


    /* Helpers to display user content */
    private void printStatus(String status) {
        mStatusView.setText(status);
        Log.i(TAG, status);
    }

    private void printResult(String result) {
        mResultView.append(result);
        Log.i(TAG, result);
    }


    private void setDevice(UsbDevice usbDevice) {

        UsbInterface intf = null;
        UsbEndpoint epIN = null;
        UsbEndpoint epOUT = null;

        if (usbDevice != null) {
            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                intf = usbDevice.getInterface(i);
                mResultView.append("\nUsb Interface " + i + ": " + intf.getName());

                for (int j = 0; j < intf.getEndpointCount(); j++) {
                    if (intf.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (intf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT) {
                            // from android to device
                            epOUT = intf.getEndpoint(j);
                            mResultView.append("\nfound OUT endpoint: " + intf.getEndpoint(j) + "; type: " + intf.getEndpoint(j).getType());
                        }
                        if (intf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN) {
                            // from device to android
                            epIN = intf.getEndpoint(j);
                            mResultView.append("\nfound IN endpoint: " + intf.getEndpoint(j) + "; type: " + intf.getEndpoint(j).getType());
                            readData(intf, usbDevice, epIN);
                        }
                    }
                }
            }
        } else {
            mResultView.append("\nDevice not opened.");
        }
    }

    private void readData(UsbInterface intf, UsbDevice usbDevice, UsbEndpoint epIN) {

        UsbDeviceConnection connection = mUsbManager.openDevice(usbDevice);
        if (connection != null && connection.claimInterface(intf, true)) {

            int readByte = readResponse(connection, epIN);
            mResultView.append("\nResponse: " + readByte);

        }
        connection.close();
    }

    public int readResponse(UsbDeviceConnection connection, UsbEndpoint epIN) {
        StringBuilder result = new StringBuilder();
        final byte[] buffer = new byte[epIN.getMaxPacketSize()];
        byte[] bufferRaw = new byte[epIN.getMaxPacketSize()];
        int byteCount = 0;
        byteCount = connection.bulkTransfer(epIN, buffer, buffer.length, 10000);

        result.setLength(0);
        for (byte cc : bufferRaw) {
            result.append(String.format("%02x", cc & 0xff));
        }
        mResultView.append("\nRaw buffer: " + result.toString());

        return byteCount;
    }

}