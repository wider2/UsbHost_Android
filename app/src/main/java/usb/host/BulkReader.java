package usb.host;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;

public class BulkReader extends Reader {

    private static final int TIMEOUT = 1000;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpoint;

    public BulkReader(UsbDeviceConnection connection, UsbEndpoint endpoint) {
        this.connection = connection;
        this.endpoint = endpoint;
    }

    @Override
    public int read(char[] buf, int offset, int count) throws IOException {
        byte[] buffer = new byte[count];
        int byteCount = connection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT);

        if (byteCount < 0) {
          throw new IOException();
        }
        char[] charBuffer = new String(buffer, Charset.forName("US-ASCII")).toCharArray();
        System.arraycopy(charBuffer, 0, buf, offset, byteCount);
        return byteCount;
    }

    @Override
    public void close() throws IOException {
    }

}