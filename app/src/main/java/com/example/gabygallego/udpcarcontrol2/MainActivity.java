package com.example.gabygallego.udpcarcontrol2;


import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;

import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.opencv.android.OpenCVLoader;

import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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
    private static DatagramSocket mSocket_data, mSocket_image;
    private static final int SERVER_PORT_IMAGE = 4579;
    private static Handler h = new Handler();
    private static Runnable runnable;
    private static Mat defaultImage;
    static {
        if (BuildConfig.DEBUG) {
            OpenCVLoader.initDebug();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView display = findViewById(R.id.imageView);
        try {
            establishSocket();
            defaultImage = Utils.loadResource(this,R.drawable.car,CV_LOAD_IMAGE_COLOR);
        } catch (IOException ex) {
            //bad
        }
        h.postDelayed(runnable = new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new ImageReader(mSocket_image, (ImageView) findViewById(R.id.imageView),defaultImage).execute();
                    }
                });
                h.postDelayed(runnable, 330);
            }
        }, 330);
        seekBarReader();
    }


    private static void establishSocket() throws IOException {
        mSocket_data = new DatagramSocket(SERVER_PORT_DATA);
        mSocket_image = new DatagramSocket(SERVER_PORT_IMAGE);
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
                    accelerationData = (accelerationData * .05) + 15.5;

                }
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(2);
                nf.setGroupingUsed(false);
                message = "PASSWORD=123456789;STEERING=" + turnData + ";";
                message = message + "ACCELERATION=" + nf.format(accelerationData);

                Log.d("TAG", message);
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
    private static final String SERVER_NAME = "10.66.22.111";
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
        } catch (IOException ex) {
            Log.d("TAG","bad");
        }
        return message[0];
    }
}

class ImageReader extends AsyncTask<Void, Void, Void> {
    DatagramSocket mSocket;
    private static final int SERVER_PORT_IMAGE = 4579;
    private static final String SERVER_NAME = "10.66.22.111";
    private static Mat mRgba;
    private static ImageView imageViewer;
    private static Mat defaultImage;

    public ImageReader(DatagramSocket mSocket_image, ImageView imageViewer, Mat defaultImage) {
        this.mSocket = mSocket_image;
        this.imageViewer = imageViewer;
        this.defaultImage = defaultImage;
    }

    protected Void doInBackground(Void... message) {
        try {
            String password = "PASSWORD=987654321";
            byte[] buffer = password.getBytes();
            InetAddress mServerAddress = InetAddress.getByName(SERVER_NAME);
            DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length, mServerAddress, SERVER_PORT_IMAGE);
            mSocket.send(messagePacket);
            byte[] imageBytes = new byte[30000];
            DatagramPacket packet = new DatagramPacket(imageBytes, imageBytes.length);

            mSocket.setSoTimeout(1000);
            try {
                mSocket.receive(packet);
                byte[] temporaryImageInMemory = packet.getData();
                mRgba = Imgcodecs.imdecode(new MatOfByte(temporaryImageInMemory), Imgcodecs.CV_LOAD_IMAGE_COLOR);
            } catch (SocketTimeoutException ex){
                Log.d("TAG","exception");
                mRgba = defaultImage;
            }
        } catch (IOException ex) {
            //bad
            Log.d("TAG", "Excpetion thrown");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        //do stuff
        try {
            loadImage();
        } catch (IOException e) {
            Log.d("Error", "IO Exception Loading Image");
        }
    }

    protected void loadImage() throws IOException {
        // convert MAT to a bitmap
        Bitmap bm = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bm);
        //setting the image
        imageViewer.setImageBitmap(bm);
    }
};

