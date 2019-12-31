package com.example.thundersoft.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.thundersoft.camera.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Button mClick;

    private CameraManager mCameraManager;
    private CameraDevice mDevice;
    private CaptureRequest mRequest;
    private CaptureRequest.Builder mBuilder;
    private CameraCaptureSession mSession;

    private ImageReader mImageReader;
//    private File mFile;
//    private Image mImage;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mTextureView = findViewById(R.id.texture);
        mClick = findViewById(R.id.click);
        mTextureView.setSurfaceTextureListener(surfaceTextureListener);

//        mImageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 2);

        startBackgroundThread();

        mClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = mTextureView.getSurfaceTexture();
            mSurfaceTexture.setDefaultBufferSize(1280, 720);
            mSurface = new Surface(mSurfaceTexture);
            checkIfPermission();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        try {
            mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            mCameraManager.openCamera("0", mStateCallback, null);

            //设置ImageReader,将大小，图片格式
            StreamConfigurationMap map = null;
            try {
                map = mCameraManager.getCameraCharacteristics("0").get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            try {
                mDevice = camera;
                startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };
    private CameraCaptureSession.StateCallback previewCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onClosed(CameraCaptureSession session) {
            super.onClosed(session);
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            super.onSurfacePrepared(session, surface);
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                mSession = session;
                mSession.setRepeatingRequest(mRequest, null, null);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private void startPreview() {
        try {
            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mBuilder.addTarget(mSurface);
            mRequest = mBuilder.build();

            mDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), previewCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        try {
            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mBuilder.addTarget(mImageReader.getSurface());
            mRequest = mBuilder.build();
            mSession.capture(mRequest, null, null);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkIfPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA}, 1);
        } else {
//            initCamera();
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    initCamera();
                    openCamera();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            Log.d(MainActivity.TAG, "compare: ");
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            Log.d(MainActivity.TAG, "run: ");
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void startBackgroundThread() {
        Log.d(MainActivity.TAG, "startBackgroundThread: ");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(MainActivity.TAG, "onImageAvailable: ");
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), new File(getExternalFilesDir(null), "pic.jpg")));
        }

    };
}
