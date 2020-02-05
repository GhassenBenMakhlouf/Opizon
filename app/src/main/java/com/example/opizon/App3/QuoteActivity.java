package com.example.opizon.App3;

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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.opizon.BuildConfig;
import com.example.opizon.R;
import com.google.common.collect.ListMultimap;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Accessory;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.ArrayListMultimap;
import com.microsoft.projectoxford.face.contract.FacialHair;
import com.microsoft.projectoxford.face.contract.Glasses;
import com.microsoft.projectoxford.face.contract.Hair;
import com.microsoft.projectoxford.face.contract.HeadPose;
import com.microsoft.projectoxford.face.contract.Makeup;

import static com.example.opizon.Utils.BitmapUtils.blur;
import static com.example.opizon.Utils.BitmapUtils.darkenBitMap;

public class QuoteActivity extends AppCompatActivity {

    String TAG = "QuoteActivity";

    String quoteMode;

    public void setAttributeDetected(String attributeDetected) {
        this.attributeDetected = attributeDetected;
    }

    String attributeDetected;

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
            readDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }

        attributeDetected = "";

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

    private void readDatabase() throws IOException {
        InputStream inputStream;
        switch (quoteMode) {
            case "emotion":
                inputStream = getResources().openRawResource(R.raw.db_emotion);
                break;
            case "faceage":
                inputStream = getResources().openRawResource(R.raw.db_faceage);
                break;
            case "hair":
                inputStream = getResources().openRawResource(R.raw.db_hair);
                break;
            case "makeup":
                inputStream = getResources().openRawResource(R.raw.db_makeup);
                break;
            case "headpose":
                inputStream = getResources().openRawResource(R.raw.db_headpose);
                break;
            case "accessories":
                inputStream = getResources().openRawResource(R.raw.db_accessories);
                break;
            default:
                throw new UnsupportedOperationException();
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

                            FaceServiceClient.FaceAttributeType[] fatArray = getNeededAttributes();
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    false,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    // returnFaceAttributes:
                                    fatArray
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
                        ImageView cardImageView = findViewById(R.id.card_image);

                        drawTextOnCard(cardTitleView, cardSubtitleView, cardImageView);

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

    private FaceServiceClient.FaceAttributeType[] getNeededAttributes() {
        FaceServiceClient.FaceAttributeType[] fatArray;

        switch (quoteMode) {
            case "emotion":
                fatArray = new FaceServiceClient.FaceAttributeType[]{
                        FaceServiceClient.FaceAttributeType.Emotion};
                break;
            case "faceage":
                fatArray = new FaceServiceClient.FaceAttributeType[]{
                        FaceServiceClient.FaceAttributeType.Age};
                break;
            case "hair":
                fatArray = new FaceServiceClient.FaceAttributeType[]{
                        FaceServiceClient.FaceAttributeType.Hair,
                        FaceServiceClient.FaceAttributeType.FacialHair,
                        FaceServiceClient.FaceAttributeType.Gender};
                break;
            case "makeup":
                fatArray = new FaceServiceClient.FaceAttributeType[]{
                        FaceServiceClient.FaceAttributeType.Makeup,
                        FaceServiceClient.FaceAttributeType.Gender};
                break;
            case "headpose":
                fatArray = new FaceServiceClient.FaceAttributeType[]{
                        FaceServiceClient.FaceAttributeType.HeadPose};
                break;
            case "accessories":
                fatArray = new FaceServiceClient.FaceAttributeType[]{
                        FaceServiceClient.FaceAttributeType.Accessories,
                        FaceServiceClient.FaceAttributeType.Glasses};
                break;
            default:
                fatArray = new FaceServiceClient.FaceAttributeType[]{};
                break;
        }
        return fatArray;
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

                switch (quoteMode) {

                    //FIRST MODE
                    case "emotion":
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
                        String emotionDetected = new String();
                        for (Map.Entry<String, Double> entry : emoMap.entrySet()) {
                            if (entry.getValue() == maxValueInMap) {
                                emotionDetected = entry.getKey();
                                break;
                            }
                        }

                        //save Emotion
                        setAttributeDetected(emotionDetected);
                        break;
                        //SECOND MODE
                    case "faceage":

                        double age = face.faceAttributes.age;
                        if (age <= 12) {
                            setAttributeDetected("kid");
                        } else if (age <= 18) {
                            setAttributeDetected("teenager");
                        } else if (age <= 35) {
                            setAttributeDetected("youngadult");
                        } else if (age <= 64) {
                            setAttributeDetected("adult");
                        } else {setAttributeDetected("elderly");}
                        break;
                    case "hair":
                        String gender = face.faceAttributes.gender;
                        FacialHair facialHair = face.faceAttributes.facialHair;
                        Hair hair = face.faceAttributes.hair;

                        List<String> possibleAttributes = new ArrayList<>();
                        if (gender=="male") {
                            if (facialHair.beard >= 0.75) {possibleAttributes.add("beard");}
                            if (facialHair.moustache >= 0.75) {possibleAttributes.add("moustache");}
                        }

                        if (hair.invisible) {
                            possibleAttributes.add("invisible");
                        } else if (hair.bald >= 0.75) {
                            possibleAttributes.add("bald");
                        } else {
                            Map<String, Double> hairColourMap = new HashMap<>();
                            for (int i=1; i<=hair.hairColor.length;i++) {
                                hairColourMap.put(hair.hairColor[i-1].color.name().toLowerCase(), hair.hairColor[i-1].confidence);
//                                Log.i("COLORS", hair.hairColor[i-1].color.name());
                            }

                            //get the hair color
                            Double maxValueInMap2 = Collections.max(hairColourMap.values());
                            String colourDetected = new String();
                            for (Map.Entry<String, Double> entry : hairColourMap.entrySet()) {
                                if (entry.getValue() == maxValueInMap2) {
                                    colourDetected = entry.getKey();
                                    break;
                                }
                            }
                            possibleAttributes.add(colourDetected);
                        }

                        //select one from the possible attributes
                        Random randHair = new Random();
                        setAttributeDetected(possibleAttributes.get(randHair.nextInt(possibleAttributes.size())));
                        break;

                    case "makeup":

                        String genderMakeUp = face.faceAttributes.gender;
                        Makeup makeUp = face.faceAttributes.makeup;


                        List<String> possibleAttributesMakeUp = new ArrayList<>();
                        if (genderMakeUp=="male") {
                            if (makeUp.eyeMakeup && makeUp.lipMakeup) {
                                possibleAttributesMakeUp.add("male_both");}
                            else if (makeUp.eyeMakeup) {
                                possibleAttributesMakeUp.add("male_eye");}
                            else if (makeUp.lipMakeup) {
                                possibleAttributesMakeUp.add("male_lip");}
                            else {possibleAttributesMakeUp.add("male_no");}
                        } else if (makeUp.eyeMakeup && makeUp.lipMakeup) {
                            possibleAttributesMakeUp.add("female_both");}
                        else if (makeUp.eyeMakeup) {
                            possibleAttributesMakeUp.add("female_eye");}
                        else if (makeUp.lipMakeup) {
                            possibleAttributesMakeUp.add("female_lip");}
                        else {possibleAttributesMakeUp.add("female_no");}

                        setAttributeDetected(possibleAttributesMakeUp.get(0));
                        break;

                    case "headpose":

                        HeadPose headPose = face.faceAttributes.headPose;

                        List<String> possibleAttributesHeadpose = new ArrayList<>();
                        if (headPose.yaw>=10) {
                            possibleAttributesHeadpose.add("look_left");
                        } else if (headPose.yaw <= -10) {
                            possibleAttributesHeadpose.add("look_right");
                        }

                        if (headPose.pitch>=8) {
                            possibleAttributesHeadpose.add("look_up");
                        } else if (headPose.pitch <= -8) {
                            possibleAttributesHeadpose.add("look_down");
                        }

                        if (headPose.roll>=15) {
                            possibleAttributesHeadpose.add("lean_left");
                        } else if (headPose.roll <= -15) {
                            possibleAttributesHeadpose.add("lean_right");
                        }

                        if (possibleAttributesHeadpose.size()==0) {
                            possibleAttributesHeadpose.add("straight");
                        }

                        //select one from the possible attributes
                        Random randHeadpose = new Random();
                        setAttributeDetected(possibleAttributesHeadpose.get(randHeadpose.nextInt(possibleAttributesHeadpose.size())));

                        break;
                    case "accessories":

                        Accessory[] accessories = face.faceAttributes.accessories;
                        Glasses glasses = face.faceAttributes.glasses;

                        List<String> possibleAttributesAccessories = new ArrayList<>();



                        if (accessories.length==0) {
                            possibleAttributesAccessories.add("no_acc");
                        } else {
                            for (int i=1; i<=accessories.length;i++) {
                                if (accessories[i-1].confidence>=0.8) {
                                    if (accessories[i-1].type==Accessory.AccessoryType.Glasses){
                                        possibleAttributesAccessories.add(glasses.name().toLowerCase());
                                    } else {
                                        possibleAttributesAccessories.add(accessories[i-1].type.name().toLowerCase());
                                    }
                                }
                            }
                        }
                        //select one from the possible attributes
                        Random randAccessory = new Random();
                        setAttributeDetected(possibleAttributesAccessories.get(randAccessory.nextInt(possibleAttributesAccessories.size())));

                        break;
                    default:

                        break;
                }


                //get random quote
                List<String[]> quoteList = dbMap.get(attributeDetected);
                if (quoteList.size()==0) {
                    return new String[]{"Detection failed","Confused Devs"}; }

                Random rand = new Random();
                return quoteList.get(rand.nextInt(quoteList.size()));
            }
//        } else {
//            return new String[]{"Cherish those you have in your life, but for this App mode to work you need to be alone in the picture.", "App Developers"};
//        }
        return new String[]{"Your face is like John Cena, we can't see it","Confused Devs"};
    }

    private void drawTextOnCard(TextView cardTitleView, TextView cardSubtitleView, ImageView cardImageView){
        switch (quoteMode) {
            case "emotion":
                cardTitleView.setText("Emotion Mode");
                switch (attributeDetected) {
                    case "anger":
                        cardSubtitleView.setText("You look angry today !");
                        cardImageView.setImageResource(R.drawable.emotion_anger);
                        break;
                    case "contempt":
                        cardSubtitleView.setText("Your Face shows clearly contempt !");
                        cardImageView.setImageResource(R.drawable.emotion_contempt);
                        break;
                    case "disgust":
                        cardSubtitleView.setText("What's disgusting here ?");
                        cardImageView.setImageResource(R.drawable.emotion_disgust);
                        break;
                    case "fear":
                        cardSubtitleView.setText("What are you scared of ?");
                        cardImageView.setImageResource(R.drawable.emotion_fear);
                        break;
                    case "happiness":
                        cardSubtitleView.setText("You seem to be happy today ! Nice !");
                        cardImageView.setImageResource(R.drawable.emotion_happiness);
                        break;
                    case "neutral":
                        cardSubtitleView.setText("Your face doesn't show any emotion ! ");
                        cardImageView.setImageResource(R.drawable.emotion_neutral);
                        break;
                    case "sadness":
                        cardSubtitleView.setText("Stop being sad and be awesome instead !");
                        cardImageView.setImageResource(R.drawable.emotion_sadness);
                        break;
                    case "surprise":
                        cardSubtitleView.setText("You seem surprised right now !");
                        cardImageView.setImageResource(R.drawable.emotion_surprise);
                        break;
                    default:
                        cardSubtitleView.setText("Your Emotion is like John Cena, we can't see it");
                        cardImageView.setImageResource(R.drawable.ic_emotion_mode);
                        break;
                }
                break;

            case "faceage":
                cardTitleView.setText("Face Age Mode");
                switch (attributeDetected) {
                    case "kid":
                        cardSubtitleView.setText("You look like you're a kid (age lower than or equal to 12)");
                        cardImageView.setImageResource(R.drawable.faceage_kid);
                        break;
                    case "teenager":
                        cardSubtitleView.setText("You look like you're a teenager (age between 13 and 18)");
                        cardImageView.setImageResource(R.drawable.faceage_teenager);
                        break;
                    case "youngadult":
                        cardSubtitleView.setText("You look like you're a young adult (age between 19 and 35)");
                        cardImageView.setImageResource(R.drawable.faceage_youngadult);
                        break;
                    case "adult":
                        cardSubtitleView.setText("You look like you're an adult (age between 36 and 64)");
                        cardImageView.setImageResource(R.drawable.faceage_adult);
                        break;
                    case "elderly":
                        cardSubtitleView.setText("You look like you're a teenager (age higher than or equal to 65)");
                        cardImageView.setImageResource(R.drawable.faceage_elderly);
                        break;
                    default:
                        cardSubtitleView.setText("Your Face is like John Cena, we can't see it");
                        cardImageView.setImageResource(R.drawable.ic_faceage_mode);
                        break;}

                break;
            case "hair":
                cardTitleView.setText("Hair Mode");
                switch (attributeDetected) {
                    case "beard":
                        cardSubtitleView.setText("You really have a nice beard !");
                        cardImageView.setImageResource(R.drawable.hair_beard);
                        break;
                    case "moustache":
                        cardSubtitleView.setText("You really have a nice moustache !");
                        cardImageView.setImageResource(R.drawable.hair_moustache);
                        break;
                    case "invisible":
                        cardSubtitleView.setText("Don't hide your hair !");
                        cardImageView.setImageResource(R.drawable.hair_invisible);
                        break;
                    case "bald":
                        cardSubtitleView.setText("Your baldness is really special !");
                        cardImageView.setImageResource(R.drawable.hair_bald);
                        break;
                    case "other":
                    case "unknown":
                        cardSubtitleView.setText("What colour is your hair ?");
                        cardImageView.setImageResource(R.drawable.hair_unknown);
                        break;
                    case "black":
                        cardSubtitleView.setText("You have a really nice black hair !");
                        cardImageView.setImageResource(R.drawable.hair_black);
                        break;
                    case "blond":
                        cardSubtitleView.setText("You have a really nice blond hair !");
                        cardImageView.setImageResource(R.drawable.hair_blond);
                        break;
                    case "brown":
                        cardSubtitleView.setText("You have a really nice brown hair !");
                        cardImageView.setImageResource(R.drawable.hair_brown);
                        break;
                    case "gray":
                        cardSubtitleView.setText("You have a really nice gray hair !");
                        cardImageView.setImageResource(R.drawable.hair_gray);
                        break;
                    case "red":
                        cardSubtitleView.setText("You have a really nice red hair !");
                        cardImageView.setImageResource(R.drawable.hair_red);
                        break;
                    case "white":
                        cardSubtitleView.setText("You have a really nice white hair !");
                        cardImageView.setImageResource(R.drawable.hair_gray);
                        break;
                    default:
                        cardSubtitleView.setText("Your Hair is like John Cena, we can't see it");
                        cardImageView.setImageResource(R.drawable.ic_hair_mode);
                        break;}
                break;
            case "makeup":
                cardTitleView.setText("Makeup Mode");
                switch (attributeDetected) {
                    case "female_both":
                    case "male_both":
                        cardSubtitleView.setText("Nice Makeup you have on !");
                        cardImageView.setImageResource(R.drawable.makeup_full);
                        break;
                    case "female_eye":
                    case "male_eye":
                        cardSubtitleView.setText("You really have a nice eye makeup on !");
                        cardImageView.setImageResource(R.drawable.makeup_eye);
                        break;
                    case "female_lip":
                    case "male_lip":
                        cardSubtitleView.setText("You really have a nice lip makeup on !");
                        cardImageView.setImageResource(R.drawable.makeup_lip);
                        break;
                    case "female_no":
                    case "male_no":
                        cardSubtitleView.setText("Who needs makeup anyway");
                        cardImageView.setImageResource(R.drawable.makeup_no);
                        break;
                    default:
                        cardSubtitleView.setText("FYour Face Age is like John Cena, we can't see it");
                        cardImageView.setImageResource(R.drawable.ic_makeup_mode);
                        break;}
                break;
            case "headpose":
                cardTitleView.setText("Head Pose Mode");
                switch (attributeDetected) {
                    case "look_right":
                        cardSubtitleView.setText("You were looking at your right !");
                        cardImageView.setImageResource(R.drawable.headpose_right);
                        break;
                    case "look_left":
                        cardSubtitleView.setText("You were looking at your left !");
                        cardImageView.setImageResource(R.drawable.headpose_left);
                        break;
                    case "look_up":
                        cardSubtitleView.setText("You were looking up !");
                        cardImageView.setImageResource(R.drawable.headpose_up);
                        break;
                    case "look_down":
                        cardSubtitleView.setText("You were looking down !");
                        cardImageView.setImageResource(R.drawable.headpose_down);
                        break;
                    case "lean_right":
                        cardSubtitleView.setText("You were leaning to your right !");
                        cardImageView.setImageResource(R.drawable.headpose_lean_right);
                        break;
                    case "lean_left":
                        cardSubtitleView.setText("You were leaning to your left !");
                        cardImageView.setImageResource(R.drawable.headpose_lean_left);
                        break;
                    case "straight":
                        cardSubtitleView.setText("You were looking ahead ! ");
                        cardImageView.setImageResource(R.drawable.headpose_straight);
                        break;
                    default:
                        cardSubtitleView.setText("Your Head Pose is like John Cena, we can't see it");
                        cardImageView.setImageResource(R.drawable.ic_headpose_mode);
                        break;}

                break;
            case "accessories":

                cardTitleView.setText("Accessories Mode");
                cardImageView.setImageResource(R.drawable.ic_accessories_mode);
                switch (attributeDetected) {
                    case "no_acc":
                        cardSubtitleView.setText("You're wearing no accessories !");
                        cardImageView.setImageResource(R.drawable.makeup_no);
                        break;
                    case "swimminggoggles":
                        cardSubtitleView.setText("You have swimming goggles on !");
                        cardImageView.setImageResource(R.drawable.accessories_goggles);
                        break;
                    case "sunglasses":
                        cardSubtitleView.setText("Nice sunglasses you have there !");
                        cardImageView.setImageResource(R.drawable.accessories_sunglasses);
                        break;
                    case "readingglasses":
                        cardSubtitleView.setText("You're wearing glasses !");
                        cardImageView.setImageResource(R.drawable.accessories_glasses);
                        break;
                    case "noglasses":
                        cardSubtitleView.setText("You're not wearing glasses!");
                        cardImageView.setImageResource(R.drawable.makeup_no);
                        break;
                    case "headwear":
                        cardSubtitleView.setText("You have something on your head !");
                        cardImageView.setImageResource(R.drawable.accessories_headwear);
                        break;
                    case "mask":
                        cardSubtitleView.setText("You're hiding behind a mask ! ");
                        cardImageView.setImageResource(R.drawable.accessories_mask);
                        break;
                    default:
                        cardSubtitleView.setText("Your Head Pose is like John Cena, we can't see it");
                        cardImageView.setImageResource(R.drawable.ic_accessories_mode);
                        break;}

                break;
            default:
                cardTitleView.setText("Your QuotingMode is like John Cena, we can't see it");
                cardImageView.setImageResource(R.drawable.app3);
                break;
        }
    }


}
