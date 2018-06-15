package ch.bfh.students.schmm11.proj2_nearbyv3;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    // Init Layout
    private TextView messageBox;
    private Button btnDiscover, btnAdvertise, btnSend;
    private Strategy strategy = Strategy.P2P_POINT_TO_POINT;
    private Boolean mIsDiscovering = false;
    private Boolean mIsAdvertising = false;
    private Boolean mIsConnected = false;

    private String ownEndpointname, partnerEndpointName;
    private String serviceId = "ch.bfh.students.schmm11.proj2_nearbyv3";

    private ConnectionsClient mConnectionsClient;

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    partnerEndpointName = endpointId;
                    messageBox.append(writeTime() + "onConnectionInit from" + endpointId + System.getProperty("line.separator"));
                    mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            messageBox.append(writeTime() + "We're connected! Can now start sending and receiving data."+ System.getProperty("line.separator"));
                            btnSend.setText("Send");
                            mIsConnected = true;
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            messageBox.append(writeTime() + "The connection was rejected by one or both sides"+ System.getProperty("line.separator"));
                            break;
                        default:
                            messageBox.append(writeTime() + "The connection was broken before it was accepted."+ System.getProperty("line.separator"));
                            break;
                    }
                }
                @Override
                public void onDisconnected(String endpointId) {
                    mIsConnected = false;
                    btnSend.setText("...");
                    messageBox.append(writeTime() + "onDisconnected"+ System.getProperty("line.separator"));
                }
            };

    //receive a Payload
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    receiveData(payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    messageBox.append(writeTime() + "onPayloadTransferUpdate (No Clue what this means, Payload finishehs is above ....");
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConnectionsClient = Nearby.getConnectionsClient(this);
        this.messageBox = (TextView) findViewById(R.id.messageBox);
        this.btnAdvertise = (Button) findViewById(R.id.btnAdvertise);
        this.btnDiscover = (Button) findViewById(R.id.btnDiscover);
        this.btnSend = (Button) findViewById(R.id.btnSend);
        btnAdvertise.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                messageBox.append( writeTime() + "Clicked Advertise" + System.getProperty("line.separator"));
                //only advertise when not allready Advertise or Discovering
                if(!mIsAdvertising && !mIsDiscovering) {
                    startAdvertising();
                }
            }
        });
        btnDiscover.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                messageBox.append(writeTime()+"Clicked Discover" + System.getProperty("line.separator"));
                //only discover when not allready Advertise or Discovering
                if(!mIsAdvertising && !mIsDiscovering){
                    startDiscovering();
                }
                else if(!mIsAdvertising && mIsDiscovering){
                    stopDiscovering();
                }
                else if(mIsAdvertising && !mIsDiscovering){
                    messageBox.append(" ... but I am allready Advertising..."+ System.getProperty("line.separator"));
                }

            }
        });

        btnSend.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                messageBox.append( writeTime() + "Clicked Send" + System.getProperty("line.separator"));
                //only advertise when not allready Advertise or Discovering
                if(mIsConnected) {
                    messageBox.append( writeTime() + "Started to Send." + System.getProperty("line.separator"));
                    sendData(getTestPayload());
                }
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
                                messageBox.append( writeTime() + "endpoint found" + endpointId + System.getProperty("line.separator"));
                                if (serviceId.equals(info.getServiceId())) {
                                    /*Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    mDiscoveredEndpoints.put(endpointId, endpoint);*/
                                    partnerEndpointName = endpointId;
                                    onEndpointDiscovered(partnerEndpointName, info);

                                }
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                messageBox.append( writeTime() + "Endpoiint Lost"+ System.getProperty("line.separator"));
                            }
                        },
                        new DiscoveryOptions(strategy))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                messageBox.append( writeTime() + "Discovery started"+ System.getProperty("line.separator"));
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                messageBox.append( writeTime() + "startDiscovering() failed."+ e+ System.getProperty("line.separator"));
                                //onDiscoveryFailed();
                            }
                        });
    }

    private void onEndpointDiscovered(String endPointId, DiscoveredEndpointInfo info) {
        messageBox.append( writeTime() + "EndpointDiscovered: " + endPointId+ System.getProperty("line.separator"));
        mConnectionsClient
                .requestConnection("DiscovererName", endPointId, mConnectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                messageBox.append( writeTime() + "onEndpointDiscovered failed"+ System.getProperty("line.separator"));
                            }
                        });
    }


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
                                messageBox.append( writeTime() + "Now advertising; Name: AdvertiserDevice "+ System.getProperty("line.separator"));
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                messageBox.append( writeTime() + "startAdvertising() failed: "+ e.toString()+ System.getProperty("line.separator"));
                            }
                        });
    }


    private void sendData(Payload payload) {
        messageBox.append( writeTime() + "SendData gestartet");
                mConnectionsClient.sendPayload(partnerEndpointName, payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                messageBox.append( writeTime() + "sendPayload() failed."+ System.getProperty("line.separator"));
                            }
                        });
    }

    private void receiveData(Payload payload){
        //Was tun wir mit den empfangen Daten?

        messageBox.append(writeTime() + "Payload finished: It is :" + bytePayloadToString(payload) + System.getProperty("line.separator"));

    }



    public Payload getTestPayload() {

        //File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);


        // get File Name: NearbyTest100Mb.pdf im Download Dir



        //just a test String
        byte[] bytes = "hallo 123".getBytes();
        return Payload.fromBytes(bytes);
    }

    public String bytePayloadToString(Payload payload)  {
        String payloadString = "Unsupported Encoding";
        try {
            payloadString = new String(payload.asBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {

        }
        return payloadString;
    }

    private void stopDiscovering() {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }
    private void stopAdvertising() {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    public String writeTime(){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        return DateFormat.format("hh:mm:ss", cal).toString() + ": ";
    }
}