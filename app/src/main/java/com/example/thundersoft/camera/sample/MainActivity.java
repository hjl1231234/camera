package com.example.thundersoft.camera.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.example.thundersoft.camera.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Button mShutter;
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private byte mdate[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surface);
        mShutter = findViewById(R.id.shutter);


        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        mShutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictute();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        checkIfPermission();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void initCamera(){
        try {
            mCamera = Camera.open(0);
            mParameters = mCamera.getParameters();
//        mParameters.setFlashMode();
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setParameters(mParameters);
//            mCamera.setPreviewCallback();
            mCamera.startPreview();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void takePictute(){
        mCamera.takePicture(null,null,pictureCallback);
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mdate = data;
        }
    };

    private void checkIfPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA}, 1);
        } else {
            initCamera();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCamera();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
    public void onPreviewFrame( byte[] data, Camera camera)
    {
        data = mdate;
        if (data == null) {
            Log.i("Save error", "no data available");
            return;
        }
        try{
          File savefile = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + "/cameratest.jpeg");
          FileOutputStream fos = new FileOutputStream(savefile);
          fos.write(data);
          fos.close();
        } catch( FileNotFoundException e ) {
               e.printStackTrace();
        } catch( IOException e) {
               e.printStackTrace();
        }
    }

}
