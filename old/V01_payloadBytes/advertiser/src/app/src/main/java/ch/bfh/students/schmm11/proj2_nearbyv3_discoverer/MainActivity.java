package ch.bfh.students.schmm11.proj2_nearbyv3_discoverer;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
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

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private TextView textBox1;
    private TextView textBox2;
    private Boolean mIsDiscovering = false;
    private String partnerEndpointName;
    private String serviceId = "ch.bfh.students.schmm11.proj2_nearbyv3_advertiser";
    /** Our handler to Nearby Connections. */
    private ConnectionsClient mConnectionsClient;

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    partnerEndpointName = endpointId;
                    textBox2.setText("onConnection Init from" + endpointId );
                    mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            textBox1.setText("We're connected! Can now start sending and receiving data.");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            textBox1.setText("The connection was rejected by one or both sides");
                            break;
                        default:
                            textBox1.setText("The connection was broken before it was accepted.");
                            break;
                    }
                }
                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    //receive a Payload
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    try {
                        String payloadString = new String(payload.asBytes(), "UTF-8");
                        textBox2.setText("Payload from"+ partnerEndpointName+ "is:" + payloadString );
                    } catch (UnsupportedEncodingException e) {
                        textBox2.setText("Payload received but no support Encoding" );
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    textBox1.setText("TPayload progress");
                }
            };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectionsClient = Nearby.getConnectionsClient(this);

        textBox1 = (TextView) findViewById(R.id.textBox1);
        textBox2 = (TextView) findViewById(R.id.textBox2);

        startDiscovering();


    }

    private void startDiscovering() {
        mIsDiscovering = true;

        mConnectionsClient
                .startDiscovery(
                        serviceId,
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                textBox1.setText("endpoint discovered" + endpointId);
                                if (getServiceId().equals(info.getServiceId())) {
                                    /*Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    mDiscoveredEndpoints.put(endpointId, endpoint);*/
                                    partnerEndpointName = endpointId;
                                    onEndpointDiscovered(partnerEndpointName, info);

                                }
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                textBox1.setText("Endpoiint Lost");
                            }
                        },
                        new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                textBox1.setText("Discovery started");
                                //onDiscoveryStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                textBox1.setText("startDiscovering() failed."+ e);
                                //onDiscoveryFailed();
                            }
                        });






    }

    private void onEndpointDiscovered(String endPointId, DiscoveredEndpointInfo info) {
        textBox2.setText("Try do connect with " + endPointId);
        mConnectionsClient
                .requestConnection("DiscovererName", endPointId, mConnectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                textBox1.setText("Connection failed");
                            }
                        });
    }

    private void stopDiscovering() {
        mIsDiscovering = false;

    }

    public String getServiceId(){
        return serviceId;
    }
}
