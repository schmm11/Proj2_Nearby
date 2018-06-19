package ch.bfh.students.schmm11.proj2_nearbyv5_mainapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private Button btnAdvertise, btnRecord;
    private boolean mIsAdvertising = false;
    private boolean mIsConnected = false;
    private boolean mIsRecording = false;
    private static final String TAG = "NearbyV5_Main";

    //Sensorevents
    private SensorManager mSensorManager;
    String sensorResult ="";
    private float[] mAccelGravityData = new float[3];
    private Saver saver;
    private long amount = 0;


    //Nearby
    private Strategy strategy = Strategy.P2P_POINT_TO_POINT;
    private String ownEndpointname, partnerEndpointName;
    private String serviceId = "ch.bfh.students.schmm11.proj2_nearbyv5";
    private ConnectionsClient mConnectionsClient;



    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    partnerEndpointName = endpointId;
                    Log.d(TAG, "onConnectionInit from" + endpointId);
                    mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.d(TAG, "We're sucessfully Nearby connected!");
                            mIsConnected = true;
                            btnAdvertise.setText("Disconnect");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.d(TAG, "The connection was rejected by one or both sides");
                            break;
                        default:
                            Log.d(TAG, "The connection was broken before it was accepted.");
                            break;
                    }
                }
                @Override
                public void onDisconnected(String endpointId) {
                    mIsConnected = false;
                    Log.d(TAG, "We're disconnected");
                    btnAdvertise.setText("Disconnect");
                }
            };

    //receive a Payload
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    //Log.d(TAG, "Got a Payload from " + endpointId + ", but we should receive something....");
                }
                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        //Log.d(TAG, "Got a Payload Update from " + endpointId + ", but we should receive something....");
                    }
                }
            };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConnectionsClient = Nearby.getConnectionsClient(this);
        this.saver = new Saver();
        //Sensors
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        //mSensorManager.registerListener((SensorEventListener) this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

        //Buttons
        btnAdvertise = (Button) findViewById(R.id.btnAdvertise);
        btnRecord = (Button) findViewById(R.id.btnRecord);

        btnAdvertise.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //only discover when not allready Advertise or Discovering
                if(!mIsAdvertising){
                    startAdvertising();
                }
                else if(mIsAdvertising && mIsConnected){
                    stopAdvertising();
                }
            }
        });

        btnRecord.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(!mIsRecording){
                    startRecord();
                }
                else{
                    stopRecord();
                }

            }
        });



    }
    /*
    Nearby Methods
     */
    private void startAdvertising() {

        mIsAdvertising = true;
        mConnectionsClient
                .startAdvertising(
                        /* endpointName= */ "AdvertiserDevice",
                        /* serviceId= */ serviceId,
                        mConnectionLifecycleCallback,
                        new AdvertisingOptions(strategy))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.d(TAG,"Now Advertising....");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                Log.d(TAG,"startAdvertising failed");
                            }
                        });
    }

    private void stopAdvertising() {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    private void sendData(Payload payload) {
        mConnectionsClient.sendPayload(partnerEndpointName, payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "send Payload failed; e:" + e);
                            }
                        });
    }




    /*
    Sensor Methods
 */
    public void startRecord(){
        //register the listener
        mSensorManager.registerListener((SensorEventListener) this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        mIsRecording = true;
    }
    public void stopRecord(){
        //Stop Recording
        mSensorManager.unregisterListener(this);
        mIsRecording = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mAccelGravityData[0]=(mAccelGravityData[0]*2+event.values[0])*0.33334f;
        mAccelGravityData[1]=(mAccelGravityData[1]*2+event.values[1])*0.33334f;
        mAccelGravityData[2]=(mAccelGravityData[2]*2+event.values[2])*0.33334f;
        long timestamp = System.currentTimeMillis();
        sensorResult += "TS;" + timestamp + "; x;" + mAccelGravityData[0] + "; y; "  + mAccelGravityData[1] + ";z;" + mAccelGravityData[2] + System.getProperty("line.separator");
        this.amount++;

        //hier For Schleife
        if(amount % 6 == 0) {
            saver.save(sensorResult);
            if(mIsConnected){
                sendData(Payload.fromBytes(sensorResult.getBytes()));
                Log.d(TAG, "Totally send:" + amount);
            }
            sensorResult ="";
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
