package com.example.danyal.bluetoothhc05;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ubidots.ApiClient;
import com.ubidots.Variable;

import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ledControl extends AppCompatActivity {

    Button btn1, btn2, btn3, btnDis;
    String address = null;
    TextView lumn,display_data;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Map<String,String> variableLabels;
    boolean sending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);
        Log.i("address",address);

        sending=false;

        setContentView(R.layout.activity_led_control);

        btn1 = (Button) findViewById(R.id.button1);
        btn2 = (Button) findViewById(R.id.button2);
        btn3 = (Button) findViewById(R.id.button3);
        btnDis = (Button) findViewById(R.id.button4);
        lumn = (TextView) findViewById(R.id.textView2);
        display_data = (TextView) findViewById(R.id.textView4);

        new ConnectBT().execute();

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                sending=true;
//                Toast.makeText(getApplicationContext(),"get",Toast.LENGTH_LONG).show();
                Log.i("xxxbtnget","clicked");
                sendSignal("1");
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Log.i("xxxbtnstop","clicked");
//                Toast.makeText(getApplicationContext(),"stop",Toast.LENGTH_LONG).show();
                sending=false;

                //sendSignal("2");
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
//                sending=false;
                sendSignal("3");
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Disconnect();
            }
        });
        variableLabels=new HashMap<>();
        variableLabels.put("a","https:-industrial.ubidots.com-app-devices-5e3d46c01d8472537b2d6106-undefinednew-variable");
        variableLabels.put("b","new-variable");
        variableLabels.put("c","c");
        variableLabels.put("d","d");


    }

    private void sendSignal ( String number ) {
        Log.i("xxxxxsendSignalCalled",number);
        new streamBTData().execute();

    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

        finish();
    }

    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    private  class streamBTData extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            if ( btSocket != null ) {
                try{

                    InputStream inputStream=btSocket.getInputStream();
                    DataInputStream dataInputStream=new DataInputStream(inputStream);
                    byte[] buffer = new byte[256];
                    int bytes;


                    while (sending) {

                        Log.i("xxxsending", String.valueOf(sending));
                        bytes = dataInputStream.read(buffer);
                        final String receivedMessage = new String(buffer, 0, bytes);
                        Log.i("xxxxdata", receivedMessage);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                display_data.setText(receivedMessage);
                            }
                        });
                        String[] updates;
                        updates = receivedMessage.split("\n");

                        for (String s : updates) {
                            String data = "";
                            data += variableLabels.get(String.valueOf(s.charAt(0)));
                            data += "_";
                            data += s.charAt(1);
                            final String finalData = data;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i("xxxuithread","run");
                                    new ApiUbiDots().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,finalData);
                                }
                            });
                        }


                    }


                } catch (Exception e) {
                    // ADD THIS TO SEE ANY ERROR
                    e.printStackTrace();
                    msg("something wrong in recieving part");
//                break;
                }
            }
            return null;
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected");
                isBtConnected = true;
            }

            progress.dismiss();
        }
    }



    public class ApiUbiDots extends AsyncTask<String,Void,Void>{

        private String deviceLabel = "demo-1";
        private String token = "BBFF-zRLZtutpx4ZGdEO9oSsMNIppHNdZyf";
        private String endpoint = "https://industrial.api.ubidots.com/api/v1.6/devices";
        private String userAgent = "Java/0.1";


        @Override
        protected Void doInBackground(String... params) {
            Log.i("xxxasync","run");
            String ubiEndpoint= endpoint + "/" + deviceLabel;
            String[] data=params[0].split("_");
            String variableLabel= data[0];
            String testValue = data[1];
            URL ubiUrl = null;
            try {
                ubiUrl = new URL(ubiEndpoint);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            String json = "{\"" + variableLabel + "\":" + testValue + "}";

            System.out.println("payload: " + json);

            try{
                HttpURLConnection con = (HttpURLConnection) ubiUrl.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", userAgent);
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("X-AUTH-TOKEN", token);
                con.setDoOutput(true);
                con.setDoInput(true);

                con.setRequestMethod("POST");
                OutputStream os = con.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                int responseCode = con.getResponseCode();
                System.out.println("response code: " + responseCode);
            } catch (IOException ex) {
                Logger.getLogger(ApiUbiDots.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        }
    }
}

