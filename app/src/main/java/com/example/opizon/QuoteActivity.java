package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextPaint;
import android.widget.ImageView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuoteActivity extends AppCompatActivity {

//    String quoteMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote);

//        Intent intent = getIntent();
//        Bundle extras = intent.getExtras();

//        quoteMode = extras.getString("QUOTE_MODE");
//        byte[] b = extras.getByteArray("IMAGE");
//        Bitmap bitmapImage = BitmapFactory.decodeByteArray(b, 0, b.length);

        Bitmap bitmapImage = Global.cachedBitmap;
//        try {
//            bitmapImage = BitmapFactory.decodeStream(this.openFileInput("myImage"));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        detectionProgressDialog = new ProgressDialog(this);

        detectAndQuote(bitmapImage);
    }

    private final String subscriptionKey = BuildConfig.FACE_SUBSCRIPTION_KEY;

    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("https://westeurope.api.cognitive.microsoft.com/face/v1.0", subscriptionKey);

    private ProgressDialog detectionProgressDialog;

    private void detectAndQuote(final Bitmap imageBitmap) {
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
                                    new FaceServiceClient.FaceAttributeType[] {
                                            FaceServiceClient.FaceAttributeType.Emotion}
                            );
                            if (result == null){
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
                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        detectionProgressDialog.dismiss();

                        if(!exceptionMessage.equals("")){
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        ImageView imageView = findViewById(R.id.quoteView);

                        Bitmap newBitmap =  drawFaceRectanglesOnBitmap( imageBitmap, result);
                        imageView.setImageBitmap(newBitmap);
                        imageBitmap.recycle();
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
                    }})
                .create().show();
    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
//        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);

                Map<String, Double> emoMap = new HashMap<>();
                emoMap.put("anger", face.faceAttributes.emotion.anger);
                emoMap.put("contempt", face.faceAttributes.emotion.contempt);
                emoMap.put("disgust", face.faceAttributes.emotion.disgust);
                emoMap.put("fear", face.faceAttributes.emotion.fear);
                emoMap.put("happiness", face.faceAttributes.emotion.happiness);
                emoMap.put("neutral", face.faceAttributes.emotion.neutral);
                emoMap.put("sadness", face.faceAttributes.emotion.sadness);
                emoMap.put("surprise", face.faceAttributes.emotion.surprise);

                Double maxValueInMap= Collections.max(emoMap.values());
                String emotion = new String();
                for (Map.Entry<String, Double> entry : emoMap.entrySet()) {
                    if (entry.getValue()==maxValueInMap) {
                        emotion = entry.getKey();
                        break;
                    }
                }

                TextPaint textPaint = new TextPaint();
                textPaint.setTypeface(Typeface.SANS_SERIF);
                textPaint.setColor(Color.GREEN);

                canvas.drawText(emotion, faceRectangle.left,faceRectangle.top+20,textPaint);

            }
//        }

        return bitmap;
    }
}
