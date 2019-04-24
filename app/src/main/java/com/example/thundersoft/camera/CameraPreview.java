package com.example.thundersoft.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mholder;
    private Camera mcamera;

    public CameraPreview(Context context,Camera camera) {
        super(context);
        mcamera = camera;
        mholder = getHolder();
        mholder.addCallback(this);
        mholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mcamera.setPreviewDisplay(holder);
            mcamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview:"+e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mholder.getSurface()==null){
            return;
        }
        try {
            mcamera.stopPreview();
        }catch (Exception e){

        }
        try {
            mcamera.setPreviewDisplay(mholder);
            mcamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview:"+e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
