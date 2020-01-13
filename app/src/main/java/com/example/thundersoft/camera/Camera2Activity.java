package com.example.thundersoft.camera;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.aware.Characteristics;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.thundersoft.camera.MainActivity.TAG;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;


//    private Button mPicture;
//    private Button mRecord;
//    private Button mFlash;
//    private Button mSwitch;

    private ImageView mPicture;
    private ImageView mRecord;
    private ImageView mFlash;
    private ImageView mSwitch;
    private ImageView mPhoto;


    private CameraManager mCameraManager;
    private CameraDevice mDevice;
    private CaptureRequest mRequest;
    private CaptureRequest.Builder mBuilder;
    private CameraCaptureSession mSession;


    private ImageReader mImageReader;


    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;


    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;
    private String mNextVideoAbsolutePath;
    private Size mPreviewSize;
    private Integer mSensorOrientation;


    private final int PICTURE = 0;
    private final int RECORD = 1;
    private int mState = PICTURE;
    private boolean mIsRecordingVideo = false;


    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();


    private boolean mFlashSupported = false;

    private String mCameraId = "0";

    File mOutputImage = null;


    private Uri mImageUri;

    private final int REQUEST_ALBUM_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mTextureView = findViewById(R.id.texture);
        mPicture = findViewById(R.id.picture);
        mRecord = findViewById(R.id.record);
        mFlash = findViewById(R.id.bt_flash);
        mSwitch = findViewById(R.id.bt_switch);
        mPhoto = findViewById(R.id.bt_photo);


        mTextureView.setSurfaceTextureListener(surfaceTextureListener);


        startBackgroundThread();

        mVideoSize = new Size(1920, 1080);

        mOutputImage = new File(getExternalFilesDir("/"), "pic.jpg");


        mPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mState = PICTURE;
                takePicture();
            }
        });
        mRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mState = RECORD;
                if (mIsRecordingVideo) {
                    Log.d(TAG, "stoprecord:     vedio");
                    stopRecordingVideo();
                } else {
                    Log.d(TAG, "startrecord:     vedio");
                    takeRecord();
                }
            }
        });

        mFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFlashSupported == false) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
                            mFlash.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_on_black_24dp));
                            mFlashSupported = true;

                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
                            mFlash.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_off_black_24dp));

                            mFlashSupported = false;

                        }
                    });
                }

            }
        });


        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("0".equals(mCameraId)) {
                    mCameraId = "1";
                    closeCamera();
                    checkIfPermission();

                } else if ("1".equals(mCameraId)) {
                    mCameraId = "0";
                    closeCamera();
                    checkIfPermission();
                }
            }

        });

        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_ALBUM_CODE);

//                Intent intent = new Intent("android.intent.action.GET_CONTENT");
//                intent.setType("image/*");
//                startActivityForResult(intent, REQUEST_ALBUM_CODE);    //	打开相册



            }
        });
    }


    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        Log.d(TAG, "setAutoFlash: ");
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        }
    }


    private void takeRecord() {
        mMediaRecorder = new MediaRecorder();


        try {
            closePreviewSession();
            setUpMediaRecorder();


            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<Surface>();

            surfaces.add(mSurface);
            mBuilder.addTarget(mSurface);


            Surface recordSurface = mMediaRecorder.getSurface();
            surfaces.add(recordSurface);
            mBuilder.addTarget(recordSurface);
            //addtarget
            //为什么更新之后也不行?
            mBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            setAutoFlash(mBuilder);


            mRequest = mBuilder.build();


            mDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured:  takerecord");
                    mSession = session;

                    try {
                        mSession.setRepeatingRequest(mRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                    updatePreview();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
//                            mRecord.setText("停止");
                            mRecord.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));

                            mIsRecordingVideo = true;

                            // Start recording
                            mMediaRecorder.start();
                        }
                    });

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = mTextureView.getSurfaceTexture();
            //为什么这么大？ 这个会影响预览界面大小，但不影响拍摄照片真实大小。
            //将opencamera中的东西移动到这里也没有影响。可能是已经取得了权限。
            mSurfaceTexture.setDefaultBufferSize(1920, 1080);


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
            mCameraManager.openCamera(mCameraId, mStateCallback, null);

            //设置ImageReader,将大小，图片格式
            //这里和保存相关
            StreamConfigurationMap map = null;
            try {
                map = mCameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }


            Log.d(TAG, "openCamera:        " + mCameraId);
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
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
                mRequest = mBuilder.build();

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
            //录像 拍照预览时候会不断执行
            Log.d(TAG, "onConfigured: ");
            mSession = session;

            switch (mState) {
                case PICTURE:
                    try {
                        //这里在预览中可以被使用到吗？
                        mSession.setRepeatingRequest(mRequest, null, null);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case RECORD:
                    updatePreview();

                    break;

            }

            //录像 拍照共用一个setrepeating 不行


        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private void updatePreview() {
        //这里在预览中可以被使用到吗？
        try {
//            mBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mRequest = mBuilder.build();
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
            Log.d(TAG, "updatePreview: " + mRequest + "  " + mBuilder.build());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void startPreview() {

        try {

            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //为啥这也要getsurface
            mDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), previewCallback, null);

            mBuilder.addTarget(mSurface);


            //这imagereader有用么？去掉？
//            mDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), previewCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        try {
            mBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //这也要getsurface
            mBuilder.addTarget(mImageReader.getSurface());
            //下发旋转90
            mBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            setAutoFlash(mBuilder);


            mRequest = mBuilder.build();

            mSession.capture(mRequest, null, null);

//缩略图
            if (Build.VERSION.SDK_INT >= 24) {
                mImageUri = FileProvider.getUriForFile(Camera2Activity.this,
                        "com.example.thundersoft.camera.fileprovider", mOutputImage);
            } else {
                mImageUri = Uri.fromFile(mOutputImage);
            }
            try {
                //	将拍摄的照片显示出来
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mImageUri));
                mPhoto.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
//            Intent	intent	=	new	Intent("android.media.action.IMAGE_CAPTURE");
//            intent.putExtra(MediaStore.EXTRA_OUTPUT,	imageUri);
//            startActivityForResult(intent,	TAKE_PHOTO);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ALBUM_CODE:
                if (resultCode == RESULT_OK) {
                    //	判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        //	4.4及以上系统使用这个方法处理图片
                        handleImageOnKitKat(data);

                    } else {
                        //	4.4以下系统使用这个方法处理图片
//                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }


    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            Log.d(TAG, "handleImageOnKitKat: isdocumenturi");
            //	如果是document类型的Uri,则通过document	id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                Log.d(TAG, "handleImageOnKitKat: media antuority");

                String id = docId.split(":")[1];    //	解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);

            } else if ("com.android.providers.downloads.documents".equals(uri.
                    getAuthority())) {
                Log.d(TAG, "handleImageOnKitKat: downloads antuority");
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
//                imagePath=getImagePath(mImageUri,null);

            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "handleImageOnKitKat:   content antuority");
            //	如果是content类型的Uri,则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "handleImageOnKitKat: file antuority");
            //	如果是file类型的Uri,直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath);    //	根据图片路径显示图片
    }

    private String getImagePath(Uri uri, String selection) {
        Log.d(TAG, "getImagePath: ");

        String path = null;
        //	通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.
                        Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        Log.d(TAG, "displayImage: " + imagePath);

        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            mPicture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed	to	get	image", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
                    , Manifest.permission.RECORD_AUDIO
                    , Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            Log.d(TAG, "compare: ");
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
            Log.d(TAG, "run: ");
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
        Log.d(TAG, "startBackgroundThread: ");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: ");
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), new File(getExternalFilesDir(null), "pic.jpg")));
        }

    };


    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        Log.d(TAG, "takeRecord: " + mBuilder + "    " + mDevice + "    " + CameraDevice.TEMPLATE_RECORD);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(this);
        }


        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);


//        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
//        switch (mSensorOrientation) {
//            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
//                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
//                break;
//            case SENSOR_ORIENTATION_INVERSE_DEGREES:
//                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
//                break;
//        }


        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }


    private void stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false;
//        mRecord.setText("录像");
        mRecord.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));


        mMediaRecorder.setOnErrorListener(null);
        mMediaRecorder.setOnInfoListener(null);
        mMediaRecorder.setPreviewDisplay(null);

        // Stop recording
        try {
            mMediaRecorder.stop();
        } catch (RuntimeException stopException) {

        }

        mMediaRecorder.reset();
        mMediaRecorder.release();


        Toast.makeText(this, "Video saved: " + mNextVideoAbsolutePath,
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);
        startPreview();
    }


    private void closePreviewSession() {
        Log.d(TAG, "closePreviewSession:     vedio");
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }
    }


    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {

        if (null != mSession) {
            mSession.close();
            mSession = null;
        }
        if (null != mDevice) {
            mDevice.close();
            mDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }
}
