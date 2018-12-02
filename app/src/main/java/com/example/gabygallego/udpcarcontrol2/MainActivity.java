package com.example.gabygallego.udpcarcontrol2;


import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;

import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.NumberFormat;
import org.opencv.*;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_COLOR;


public class MainActivity extends AppCompatActivity {
    private static double turnData = 5.0;
    private static double accelerationData = 0.0;
    private static final int SERVER_PORT_DATA = 4597;
    private static DatagramSocket mSocket_data,mSocket_image;
    private static final int SERVER_PORT_IMAGE = 4579;
    private static final String SERVER_NAME = "10.67.155.142";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView display = findViewById(R.id.imageView);

        try {
            establishSocket();
            sendFake();

        } catch (IOException ex){
            //bad
        }
        //seekBarReader();
        new ImageReader(display,mSocket_image).execute("sup");
    }

    private static void establishSocket() throws IOException{
        mSocket_data = new DatagramSocket(SERVER_PORT_DATA);
        mSocket_image = new DatagramSocket(SERVER_PORT_IMAGE);
    }

    private static void sendFake() throws IOException {
        byte[] buffer = "Hello".getBytes();
        InetAddress mServerAddress = InetAddress.getByName(SERVER_NAME);
        DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length, mServerAddress, SERVER_PORT_DATA);
        mSocket_data.send(messagePacket);
    }

    void seekBarReader() {
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String message;
                if (seekBar.getId() == R.id.turningSeekBar) {
                    turnData = progress;
                    turnData = turnData + 10;

                } else {
                    accelerationData = progress;
                    accelerationData = (accelerationData*.05) + 15.5;

                }
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(2);
                nf.setGroupingUsed(false);
                message = "STEERING=" + turnData + ";";
                message = message + "ACCELERATION=" + nf.format(accelerationData) + ";";

                Log.d("TAG",message);
                new DataSender(mSocket_data).execute(message);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // not really needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // not really needed
            }
        };
        final SeekBar turningSeekBar = findViewById(R.id.turningSeekBar);
        turnData = turningSeekBar.getProgress();
        final SeekBar accelerationSeekBar = findViewById(R.id.accelerationSeekBar);
        accelerationData = accelerationSeekBar.getProgress();
        accelerationSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        turningSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
    }
}

class DataSender extends AsyncTask<String, Void, String> {
    private static final int SERVER_PORT_DATA = 4597;
    private static final String SERVER_NAME = "10.67.155.142";
    private DatagramSocket mSocket_data;

    public DataSender(DatagramSocket mSocket_data) {
        this.mSocket_data = mSocket_data;
    }

    protected String doInBackground(String... message) {
        byte[] buffer = message[0].getBytes();
        try {
            InetAddress mServerAddress = InetAddress.getByName(SERVER_NAME);
            DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length, mServerAddress, SERVER_PORT_DATA);
            mSocket_data.send(messagePacket);
        } catch(IOException ex) {
            //bad
        }
        return message[0];
    }
}

class ImageReader extends AsyncTask<String,Void,Bitmap> {
    private static final int BUFFER_SIZE = 256;
    private DatagramSocket mSocket_image;
    private ImageView display;

    public ImageReader(ImageView display, DatagramSocket mSocket_image) {
        this.display = display;
        this.mSocket_image = mSocket_image;
    }

    protected Bitmap doInBackground(String... message) {
        Bitmap bm = null;
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            mSocket_image.receive(packet);
            Log.d("TAG","got image");
        } catch (IOException ex){
            //bad
            Log.d("TAG","Excpetion thrown");
        }
        byte[] data = packet.getData();
        Mat mat = Imgcodecs.imdecode(new MatOfByte(data),Imgcodecs.CV_LOAD_IMAGE_COLOR);
        try {
            bm = Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat,bm);
        } catch (CvException ex) {
            //bad
        }
        return bm;
    }

    protected void onPostExecute(Bitmap bm ) {
        display.setImageBitmap(bm);
    }
}
