package com.elojodelamo.feedlot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TagFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TagFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TagFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final int REQUEST_ENABLE_BT = 0;
    private static final String BT_DEVICE_NAME = "Lector";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    BluetoothAdapter btAdapter;
    BluetoothDevice btDevice;
    BluetoothSocket btSocket;
    OutputStream btOutputStream;
    InputStream btInputStream;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    ImageView bluetoothStatus;
    TextView readTxt;
    ListView listView;


    public TagFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TagFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TagFragment newInstance(String param1, String param2) {
        TagFragment fragment = new TagFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tag, container, false);

        bluetoothStatus = view.findViewById(R.id.bluetoothStatus);
        readTxt = view.findViewById(R.id.readTxt);
        listView = view.findViewById(R.id.listView);

        //adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null){
            showToast("No BT Adapter available.");
        }else{
            //check if bluetooth is available or not
            if (btAdapter == null){
                showToast("Bluetooth is not available");
            } else {
                showToast("Bluetooth is available");
            }

            //set image according to bluetooth status(on/off)
            if (btAdapter.isEnabled()){
                bluetoothStatus.setImageResource(R.drawable.ic_action_on);
            } else {
                bluetoothStatus.setImageResource(R.drawable.ic_action_off);
            }

            Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
            for (BluetoothDevice device: devices){
                //mPairedTv.append("\nDevice: " + device.getName()+ ", " + device);

                if(device.getName().equals(BT_DEVICE_NAME)) {
                    btDevice = device;
                    try {
                        openBT();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            bluetoothStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (!btAdapter.isEnabled()){
                    showToast("Turning On Bluetooth...");

                    //intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);

                } else {
                    showToast("Bluetooth is already on");

                }
                }
            });
        }

        getTags();

        // Inflate the layout for this fragment
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void showToast(String msg){
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
        btSocket.connect();
        btOutputStream = btSocket.getOutputStream();
        btInputStream = btSocket.getInputStream();

        beginListenForData();

        showToast("Bluetooth Opened");
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
            while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = btInputStream.available();
                    if(bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        btInputStream.read(packetBytes);
                        for(int i=0;i<bytesAvailable;i++) {
                            byte b = packetBytes[i];
                            if(b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                handler.post(new Runnable() {
                                    public void run() {
                                        showToast(data);
                                        readTxt.setText("Tag Readed: "+data);
                                        // DO THE MAGIC
                                    }
                                });
                            }else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                }
                catch (IOException ex) {
                    stopWorker = true;
                }
            }
            }
        });

        workerThread.start();
        showToast("Listening");
    }

    private void getTags(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://93.188.167.93:1026/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RfidAPI rfidAPI = retrofit.create(RfidAPI.class);

        Call<List<Rfid>> call = rfidAPI.getRfids();

        call.enqueue(new Callback<List<Rfid>>() {
            @Override
            public void onResponse(Call<List<Rfid>> call, Response<List<Rfid>> response) {
                if(response.isSuccessful()){
                    List<Rfid> result = response.body();
                    List<String> arr = new ArrayList<String>();

                    for(Rfid rfid : result){
                        arr.add(rfid.getRfid());
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, arr);

                    listView.setAdapter(arrayAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Rfid>> call, Throwable throwable) {

            }
        });

    }

}
