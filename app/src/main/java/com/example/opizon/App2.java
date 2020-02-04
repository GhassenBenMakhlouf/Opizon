package com.example.opizon;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class App2 extends AppCompatActivity {

    //<------------------------------------>
    //|           CAMERA API 2             |
    //<------------------------------------>

    private static final String TAG = "AndroidCameraApi2_App2";
    private Button startButton;
    private TextureView textureView;
    private TextView circleTv;
    private TextView[] taskTxtViews = new TextView[5];
    private TextView[] resultTxtViews = new TextView[5];
    private int actualCol;
    private int resultPourcentage;
    private Dialog resultDialog;
    Map<String, String> equivalentEmotions = new HashMap<String, String>();
    private String[] possibleEmotions = {"happiness","neutral","disgust","sadness","surprise","fear","anger"};
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    //to rotate the bitmap afterwards
    private int jpegOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        detectionProgressDialog = new ProgressDialog(this);

        setContentView(R.layout.activity_app2);
        circleTv = (TextView) findViewById(R.id.circle_tv);
        taskTxtViews[0] = (TextView) findViewById(R.id.tasktxtview1);
        taskTxtViews[1] = (TextView) findViewById(R.id.tasktxtview2);
        taskTxtViews[2] = (TextView) findViewById(R.id.tasktxtview3);
        taskTxtViews[3] = (TextView) findViewById(R.id.tasktxtview4);
        taskTxtViews[4] = (TextView) findViewById(R.id.tasktxtview5);
        resultTxtViews[0] = (TextView) findViewById(R.id.resulttxtview1);
        resultTxtViews[1] = (TextView) findViewById(R.id.resulttxtview2);
        resultTxtViews[2] = (TextView) findViewById(R.id.resulttxtview3);
        resultTxtViews[3] = (TextView) findViewById(R.id.resulttxtview4);
        resultTxtViews[4] = (TextView) findViewById(R.id.resulttxtview5);
        resultDialog = new Dialog(this);
        equivalentEmotions.put("happiness",getResources().getString(R.string.happiness));
        equivalentEmotions.put("neutral",getResources().getString(R.string.neutral));
        equivalentEmotions.put("disgust",getResources().getString(R.string.disgust));
        equivalentEmotions.put("sadness",getResources().getString(R.string.sadness));
        equivalentEmotions.put("surprise",getResources().getString(R.string.surprise));
        equivalentEmotions.put("fear",getResources().getString(R.string.fear));
        equivalentEmotions.put("anger",getResources().getString(R.string.anger));
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        startButton = (Button) findViewById(R.id.btn_start);
        assert startButton != null;
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.INVISIBLE);
                taskNumber=0;
                actualCol=0;
                updateTable();
                initializeEmotionsArrays();
                taskTxtViews[0].setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                resultTxtViews[0].setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                new CountDownTimer(25000, 1000){
                    @Override
                    public void onTick(long millisUntilFinished){
                        circleTv.setText(String.valueOf((millisUntilFinished/1000)%5));
                        Log.i("app2", "counter: "+millisUntilFinished/1000);
                        updateTable();
                        if ((millisUntilFinished/1000)%5==0){
                            takePicture();
                            actualCol++;
                            if (actualCol<5){
                                taskTxtViews[actualCol-1].setBackgroundColor(getResources().getColor(R.color.white));
                                resultTxtViews[actualCol-1].setBackgroundColor(getResources().getColor(R.color.white));

                                taskTxtViews[actualCol].setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                                resultTxtViews[actualCol].setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                            }

                            Log.i("app2", "picture took!");
                        }
                    }
                    @Override
                    public void onFinish() {
                        taskTxtViews[4].setBackgroundColor(getResources().getColor(R.color.white));
                        resultTxtViews[4].setBackgroundColor(getResources().getColor(R.color.white));
                    }
                }.start();
            }
        });
    }

    private void initializeEmotionsArrays() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i=0; i<7; i++) {
            list.add(new Integer(i));
        }
        Collections.shuffle(list);
        for (int i=0; i<5; i++) {
            expectedEmotion[i]=possibleEmotions[list.get(i)];
            taskTxtViews[i].setText(equivalentEmotions.get(expectedEmotion[i]));
            detectedEmotion[i]="empty";
        }
    }

    private void updateTable() {
        for (int i=0; i<5;i++){
            if (expectedEmotion[i]==detectedEmotion[i]){
                resultTxtViews[i].setText(getResources().getString(R.string.check));
            }
            else if(taskNumber>i) {
                resultTxtViews[i].setText(getResources().getString(R.string.cross));
            }
            else{
                resultTxtViews[i].setText(getResources().getString(R.string.wait));
            }
        }
    }

    private void showResults(){
        startButton.setVisibility(View.VISIBLE);
        resultPourcentage=0;
        for (int i=0; i<5;i++) {
            if (expectedEmotion[i] == detectedEmotion[i]) {
                resultPourcentage = resultPourcentage + 20;
            }
        }
        TextView txtClose;
        TextView txtResult;
        Button btnReplay;
        resultDialog.setContentView(R.layout.app2_popup);
        txtClose =(TextView) resultDialog.findViewById(R.id.txtclose);
        txtResult =(TextView) resultDialog.findViewById(R.id.txtresult);
        btnReplay = (Button) resultDialog.findViewById(R.id.btnreplay);

        switch (resultPourcentage) {
            case 0:     txtResult.setText("You got 0%. Don't worry, practice makes perfect!");
                break;
            case 20:    txtResult.setText("You got 20%. Don't worry, practice makes perfect!");
                break;
            case 40:    txtResult.setText("You got 40%. Not bad, but you can do better!");
                break;
            case 60:    txtResult.setText("You got 60%. Good, keep practicing!");
                break;
            case 80:    txtResult.setText("You got 80%. Great, you start mastering it!");
                break;
            case 100:   txtResult.setText("You got 100%. Wow, you are just perfect!");
                break;
            default:    txtResult.setText("Result can't be shown. Try one more time or contact the Admin!");
                break;
        }
        txtClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultDialog.dismiss();
            }
        });
        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultDialog.dismiss();
                startButton.performClick();
            }
        });
        resultDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        resultDialog.show();
    }



    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Toast.makeText(App2.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createImageFromBitmap(Bitmap bitmap) {
        String fileName = "myImage";//no .png or .jpg needed
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            FileOutputStream fo = openFileOutput(fileName, Context.MODE_PRIVATE);
            fo.write(bytes.toByteArray());

            fo.close();
        } catch (Exception e) {
            e.printStackTrace();
            fileName = null;
        }
    }


    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
            //CALCULATE THE JPEG ORIENTATION
            int sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int surfaceRotation = ORIENTATIONS.get(deviceRotation);
            jpegOrientation = (surfaceRotation + sensorOrientation + 270) % 360;

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);


            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    image = reader.acquireLatestImage();

                    Bitmap bitmapImage = ImageHelper.loadSizeLimitedBitmapFromImage(image, jpegOrientation);

                    detectEmotionFromBitmap(bitmapImage);


                    if (image != null) {
                        image.close();
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(App2.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(App2.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            //1 for frontcamera , 0 for back camera
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(App2.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(App2.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    //<------------------------------------>
    //|         AZURE FACE API             |
    //<------------------------------------>

    private String[] detectedEmotion= new String[5];
    private String[] expectedEmotion= new String[5];
    private int taskNumber;

    private final String subscriptionKey = BuildConfig.FACE_SUBSCRIPTION_KEY;

    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("https://westeurope.api.cognitive.microsoft.com/face/v1.0", subscriptionKey);

    private ProgressDialog detectionProgressDialog;

    private void detectEmotionFromBitmap(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Creating your Quote...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    false,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    // returnFaceAttributes:
                                    new FaceServiceClient.FaceAttributeType[]{
                                            FaceServiceClient.FaceAttributeType.Emotion}
                            );
                            if (result == null) {
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                        //detectionProgressDialog.show();
                    }

                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        //detectionProgressDialog.setMessage(progress[0]);
                    }

                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        //detectionProgressDialog.dismiss();

                        if (!exceptionMessage.equals("")) {
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        imageBitmap.recycle();

                        detectedEmotion[taskNumber] = getEmotionFromFace(result);
                        Log.i("app2", "expected "+taskNumber+":  "+expectedEmotion[taskNumber]);
                        Log.i("app2", "result "+taskNumber+":  "+detectedEmotion[taskNumber]);
                        taskNumber++;
                        if (taskNumber==5){
                            updateTable();
                            showResults();
                        }
                    }
                };

        detectTask.execute(inputStream);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .create().show();
    }

    private String getEmotionFromFace(Face[] faces) {

        for (Face face : faces) {
            Map<String, Double> emoMap = new HashMap<>();
            emoMap.put("anger", face.faceAttributes.emotion.anger);
            emoMap.put("contempt", face.faceAttributes.emotion.contempt);
            emoMap.put("disgust", face.faceAttributes.emotion.disgust);
            emoMap.put("fear", face.faceAttributes.emotion.fear);
            emoMap.put("happiness", face.faceAttributes.emotion.happiness);
            emoMap.put("neutral", face.faceAttributes.emotion.neutral);
            emoMap.put("sadness", face.faceAttributes.emotion.sadness);
            emoMap.put("surprise", face.faceAttributes.emotion.surprise);

            Double maxValueInMap = Collections.max(emoMap.values());
            String emotion = new String();
            for (Map.Entry<String, Double> entry : emoMap.entrySet()) {
                if (entry.getValue() == maxValueInMap) {
                    emotion = entry.getKey();
                    break;
                }
            }
            return emotion;
        }
        return null;
    }



}
