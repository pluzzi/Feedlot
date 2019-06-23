package com.elojodelamo.feedlot;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
 * {@link ObservationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ObservationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ObservationFragment extends Fragment {
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
    Button newBtn;

    Retrofit retrofit;
    RfidAPI rfidAPI;
    ObservationsAPI observationsAPI;
    Integer idRfid;

    Dialog dialog;


    public ObservationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ObservationFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ObservationFragment newInstance(String param1, String param2) {
        ObservationFragment fragment = new ObservationFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_observation, container, false);

        bluetoothStatus = view.findViewById(R.id.bluetoothStatus);
        readTxt = view.findViewById(R.id.readTxt);
        listView = view.findViewById(R.id.listView);
        newBtn = view.findViewById(R.id.newBtn);
        retrofit = new Retrofit.Builder()
                .baseUrl("http://93.188.167.93:1026/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        rfidAPI = retrofit.create(RfidAPI.class);
        observationsAPI = retrofit.create(ObservationsAPI.class);
        dialog = new Dialog(getActivity());


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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                adb.setTitle("Delete");
                adb.setMessage("Seguro que desea borrar la observación?");
                adb.setNegativeButton("Cancelar", null);
                adb.setPositiveButton("Borrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String code = listView.getItemAtPosition(position).toString();
                        deleteObservation(code);
                        showToast(code);
                    }
                });
                adb.show();

            }
        });

        newBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup();
            }
        });


        // Inflate the layout for this fragment
        return view;
    }

    public void showPopup(){
        final EditText editText;
        Button cancelBtn;
        Button saveBtn;
        dialog.setContentView(R.layout.new_observation_layout);
        editText = (EditText) dialog.findViewById(R.id.obsevationTxt);
        cancelBtn = (Button) dialog.findViewById(R.id.cancelBtn);
        saveBtn = (Button) dialog.findViewById(R.id.saveBtn);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createObservation(editText.getText().toString());
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void  createObservation(String description){
        Observation observation = new Observation(idRfid, description);

        Call<Observation> call = observationsAPI.createObservation(observation);

        call.enqueue(new Callback<Observation>() {
            @Override
            public void onResponse(Call<Observation> call, Response<Observation> response) {
                if(response.isSuccessful()){
                    getObservationsByCode();
                }
            }

            @Override
            public void onFailure(Call<Observation> call, Throwable throwable) {

            }
        });

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
        try {
            if(btSocket!=null){
                btSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
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
                                            String code = data.replaceAll("[^a-zA-Z0-9]", "");
                                            showToast(code);
                                            readTxt.setText("Tag Readed: "+code);
                                            // DO THE MAGIC
                                            //showToast(data.toString());
                                            getTagByCode(code);
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

    private void getObservationsByCode(){
        //ObservationsAPI observationsAPI = retrofit.create(ObservationsAPI.class);

        Call<List<Observation>> call = observationsAPI.getObservationByTagId(idRfid);

        call.enqueue(new Callback<List<Observation>>() {
            @Override
            public void onResponse(Call<List<Observation>> call, Response<List<Observation>> response) {
                if(response.isSuccessful()){
                    List<Observation> result = response.body();
                    List<String> arr = new ArrayList<String>();

                    for(Observation observation : result){
                        arr.add(observation.getId()+", "+observation.getObservation());
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, arr);

                    listView.setAdapter(arrayAdapter);
                    showToast("Successful");
                }else{
                    showToast("unSuccessful");
                }
            }

            @Override
            public void onFailure(Call<List<Observation>> call, Throwable throwable) {
                showToast("fail");
            }
        });

    }

    private void getTagByCode(String code){
        Call<Rfid> call = rfidAPI.getRfidByCode(code);

        call.enqueue(new Callback<Rfid>() {
            @Override
            public void onResponse(Call<Rfid> call, Response<Rfid> response) {
                if(response.isSuccessful()){
                    Rfid result = response.body();

                    if(result != null){
                        idRfid = result.getId();
                        getObservationsByCode();
                    }
                }else{
                    showToast("unSuccessful");
                }
            }

            @Override
            public void onFailure(Call<Rfid> call, Throwable throwable) {
                showToast("faile");
            }
        });

    }

    private void  deleteObservation(String code){
        final int id = Integer.parseInt(code.split(",")[0].trim());

        Call<Void> call = observationsAPI.deleteObservation(id);

        call.enqueue(new Callback<Void>() {
             @Override
             public void onResponse(Call<Void> call, Response<Void> response) {
                 if(response.isSuccessful()){
                     getObservationsByCode();
                 }
             }

             @Override
             public void onFailure(Call<Void> call, Throwable throwable) {

             }
         }
        );

    }


}
