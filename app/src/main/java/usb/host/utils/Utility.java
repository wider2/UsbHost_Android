package usb.host.utils;


import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Utility {

    public static void writeFile(String filename, String output, boolean b, Context fcontext, String catalog) {
        filename = filename.replace(" ", "-");
        File root = new File(Environment.getExternalStorageDirectory(), "UsbSimple");
        if (!root.exists()) root.mkdirs();

        try {
            Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
            if (isSDPresent) {
                if (!catalog.matches("")) catalog = "/" + catalog;
                root = new File(Environment.getExternalStorageDirectory(), "UsbSimple" + catalog);//Add folder
                if (!root.exists()) root.mkdirs();

                //sdCardMemoryFree
                if (root.canWrite()) {
                    File gpxfile = new File(root, filename);//File
                    FileWriter writer = new FileWriter(gpxfile, b);//,true);//file exists - add to it
                    writer.append(output);
                    writer.flush();
                    writer.close();
                    //Toast.makeText(this, sFileName+" saved", Toast.LENGTH_SHORT).show();
                }
            } else {
                //FileOutputStream fos = this.openFileOutput(filename, Context.MODE_PRIVATE);
                FileOutputStream fos = fcontext.openFileOutput(filename, Context.MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(output);
                osw.flush();

                fos.flush();
                fos.getFD().sync();
                osw.close();
                fos.close();
            }
        } catch (IOException e) {
            //Toast.makeText(this, "" + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}