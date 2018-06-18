package ch.bfh.students.schmm11.proj2_nearbyv3;



import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Saver {
    private static final String TAG = "NearbyV3Saver";
    private File file;
    public Saver(){
        if(isExternalStorageWritable()){
            //nice go On
            Log.e("SUCESS", " Writable");
        }
        else{
            //fucked up
            Log.e("Error", "Not Writable");
        }
        //file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AccelerationTest.txt");
        //File filepath = new File(Environment.getExternalStorageDirectory().getPath() + "/Proj2Acceleration");
        File filepath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Proj2Acceleration");

        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        file = new File( filepath.getPath(), "AccelerationTest.txt");
        if (!file.exists()) {
            try {

                file.createNewFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    public void save(String str){
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);

            BufferedWriter fbw = new BufferedWriter(writer);

            fbw.append(str);
            fbw.newLine();

            fbw.flush();
            fbw.close();


            fileOutputStream.flush();
            fileOutputStream.close();

            Log.d(TAG, "Data saved");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}