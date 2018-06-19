package ch.bfh.students.schmm11.proj2_nearbyv5_receiverapp;

import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private Button btnDiscover, btnReceived;
    private boolean mIsDiscovering = false;
    private boolean mIsConnected = false;
    private boolean mIsRecording = false;
    private static final String TAG = "NearbyV5_Receiver";

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
                            btnDiscover.setText("Disconnect");
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
                    btnDiscover.setText("Disconnect");
                }
            };

    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    //
                    saveReceivedDate(payload);

                    //Log.d(TAG, "Received some Data");
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

        //Buttons
        btnDiscover = (Button) findViewById(R.id.btnDiscover);
        btnReceived = (Button) findViewById(R.id.btnReceive);

        btnDiscover.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //only discover when not allready Advertise or Discovering
                if(!mIsDiscovering){
                    startDiscovering();
                }
            }
        });

        //this Button has no sense at the moment
        btnReceived.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            }
        });
    }



    private void startDiscovering() {
        mIsDiscovering = true;

        mConnectionsClient
                .startDiscovery(
                        serviceId,
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                Log.d(TAG, "Endpoint Found, Name is: "+ endpointId);
                                if (serviceId.equals(info.getServiceId())) {
                                    partnerEndpointName = endpointId;

                                    // Ask for a Connection
                                    mConnectionsClient
                                            .requestConnection("DiscovererName", partnerEndpointName, mConnectionLifecycleCallback)
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d(TAG, "onEndpointDiscovered failed");
                                                        }
                                                    });
                                }
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                Log.d(TAG, "onEndpointLost failed");
                            }
                        },
                        new DiscoveryOptions(strategy))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.d(TAG, "Discovery started");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                Log.d(TAG, "Discovery failed");
                            }
                        });
    }

    //On Receive
    public void saveReceivedDate(Payload payload){
        String receivedPayload = "";
        try {
            receivedPayload = new String(payload.asBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        saver.save(receivedPayload);
        amount += 6;
        Log.d(TAG, "received Data; amount Total: " + amount);
    }


}
