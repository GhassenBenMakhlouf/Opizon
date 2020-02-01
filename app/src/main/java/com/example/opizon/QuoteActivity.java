package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ListMultimap;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.ArrayListMultimap;

import static com.example.opizon.BitmapUtils.blur;
import static com.example.opizon.BitmapUtils.darkenBitMap;

public class QuoteActivity extends AppCompatActivity {

    String TAG = "QuoteActivity";

    String quoteMode;

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    String emotion;

    private ListMultimap<String, String[]> dbMap = ArrayListMultimap.create();

    private Button backButton;
    private Button saveButton;
    private Button shareButton;

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    private Uri imageUri;

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        quoteMode = extras.getString("QUOTE_MODE");
        try {
            readDatabase(quoteMode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        emotion = "";

        //recover the image from saved static bitmap
        final Bitmap bitmapImage = Global.cachedBitmap;

        detectionProgressDialog = new ProgressDialog(this);

        detectAndQuote(bitmapImage);

        backButton = (Button) findViewById(R.id.btn_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        shareButton = (Button) findViewById(R.id.btn_share);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/png");
                startActivity(Intent.createChooser(intent, "Share your quoted photo"));
            }
        });

        saveButton = (Button) findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStoragePermissionGranted()) {
                    File pictureFile = getOutputMediaFile();
                    if (pictureFile == null) {
                        Log.d(TAG,
                                "Error creating media file, check storage permissions: ");
                        Toast toast = Toast.makeText(getApplicationContext(), "Error saving Image, please check storage permissions !", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);

                        Toast toast = Toast.makeText(getApplicationContext(), "photo saved !", Toast.LENGTH_SHORT);
                        toast.show();

                        fos.close();
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void readDatabase(String quoteMode) throws IOException {
        InputStream inputStream;
        switch (quoteMode) {
            case "emotion":
                inputStream = getResources().openRawResource(R.raw.db_emotion);
                break;
            case "":
                inputStream = null;
                break;
            default:
                inputStream = null;
                break;
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, Charset.forName("UTF-8"))
        );
        String line = "";
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(";");
            dbMap.put(tokens[0], new String[]{tokens[1], tokens[2]});
        }
    }

    /**
     * Saves the image as PNG to the app's cache directory.
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    private void saveBitmapInUri(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.mydomain.fileprovider", file);

        } catch (IOException e) {
            Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
        }
        setImageUri(uri);
    }

    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + "/Opizon_Pictures");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        File mediaFile;
        String mImageName="IMG_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    private boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }


    //----------------
    // FACE AZURE API
    //----------------
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

                        if (!exceptionMessage.equals("")) {
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        ImageView imageView = findViewById(R.id.quoteView);
                        //draw quote on image
                        Bitmap newBitmap = drawQuoteOnBitmap(imageBitmap, result);
                        imageView.setImageBitmap(newBitmap);

                        //update Cards Elements
                        TextView cardTitleView = findViewById(R.id.card_title);
                        TextView cardSubtitleView = findViewById(R.id.card_subtitle);

                        drawTextOnCard(cardTitleView, cardSubtitleView);

                        saveBitmapInUri(newBitmap);
                        setImageBitmap(newBitmap);
                        //clean
                        imageBitmap.recycle();
                        dbMap.clear();
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

    private Bitmap drawQuoteOnBitmap(final Bitmap originalBitmap, Face[] faces) {

        //set bitmap to mutable
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        //blur bitmap
        bitmap = blur(this, bitmap, 10.5f);

        //darken bitmap
        bitmap = darkenBitMap(bitmap);

        //prepare quote
        String[] quoteWithPerson = getQuote(faces);
        String quote = "\" "+quoteWithPerson[0]+" \""+"\n ~ "+quoteWithPerson[1];

        //get screen density
        float scale = getResources().getDisplayMetrics().density;

        Canvas canvas = new Canvas(bitmap);

        // new antialiased Paint
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        // text color
        paint.setColor(Color.WHITE);
        // text size in pixels , war 14
        paint.setTextSize((int) (14 * scale));
        //Typeface
        paint.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/OpenSans.ttf"));
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        // set text width to canvas width minus 16dp padding
        int textWidth = canvas.getWidth() - (int) (16 * scale);

        // init StaticLayout for text
        StaticLayout textLayout = new StaticLayout(
                quote, paint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        // get height of multiline text
        int textHeight = textLayout.getHeight();

        // get position of text's top left corner
        float x = (bitmap.getWidth() - textWidth)/2;
        float y = (bitmap.getHeight() - textHeight)/2;

        // draw text to the Canvas center
        canvas.save();
        canvas.translate(x, y);
        textLayout.draw(canvas);
        canvas.restore();

        return bitmap;
    }

    private String[] getQuote(Face[] faces) {
//        if (faces.length <= 1) {
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

                //sace Emotion
                setEmotion(emotion);

                //get random quote
                List<String[]> quoteList = dbMap.get(emotion);

                Random rand = new Random();
                return quoteList.get(rand.nextInt(quoteList.size()));
            }
//        } else {
//            return new String[]{"Cherish those you have in your life, but for this App mode to work you need to be alone in the picture.", "App Developers"};
//        }
        return null;
    }

    private void drawTextOnCard(TextView cardTitleView, TextView cardSubtitleView){
        switch (quoteMode) {
            case "emotion":
                cardTitleView.setText("Emotion Mode");
                switch (emotion) {
                    case "anger":
                        cardSubtitleView.setText("You look angry today !");
                        break;
                    case "contempt":
                        cardSubtitleView.setText("Your Face shows clearly contempt !");
                        break;
                    case "disgust":
                        cardSubtitleView.setText("What's disgusting here ?");
                        break;
                    case "fear":
                        cardSubtitleView.setText("What are you scared of ?");
                        break;
                    case "happiness":
                        cardSubtitleView.setText("You seem to be happy today ! Nice !");
                        break;
                    case "neutral":
                        cardSubtitleView.setText("Your face doesn't show any emotion ! ");
                        break;
                    case "sadness":
                        cardSubtitleView.setText("Stop being sad and be awesome instead !");
                        break;
                    case "surprise":
                        cardSubtitleView.setText("You seem surprised right now !");
                        break;
                    default:
                        cardSubtitleView.setText("EMOTION==NULL");
                        break;
                }
                break;
            case "":

                break;
            default:
                cardTitleView.setText("QUOTING MODE == NULL");
                break;
        }
    }


}
