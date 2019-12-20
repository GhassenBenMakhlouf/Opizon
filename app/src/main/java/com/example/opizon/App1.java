package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class App1 extends AppCompatActivity {
    private HashMap<String,String> db=new HashMap<String,String>();
    private final int REQ_CODE = 100;
    TextView textView;
    VideoView videoView;
    ArrayList<String> nWords=new ArrayList<String>();
    int incrementer=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        try {
//            readDatabase();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        setContentView(R.layout.activity_app1);
        textView = findViewById(R.id.text);
        videoView = findViewById(R.id.video);
        ImageView speak = findViewById(R.id.speak);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
                try {
                    startActivityForResult(intent, REQ_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry your device not supported",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    textView.setText((CharSequence) result.get(0));

                    nWords.add("abbreviate");
                    nWords.add("about");
                    //nWords.add("absolution");
                    //nWords.add("accessible");
                    playVideos();
                }
                break;
            }
        }
    }

//    public void readDatabase() throws IOException {
//        InputStream inputStream = getResources().openRawResource(R.raw.dataset);
//        BufferedReader reader = new BufferedReader(
//                new InputStreamReader(inputStream, Charset.forName("UTF-8"))
//        );
//        String line = "";
//        while ((line=reader.readLine()) != null){
//            String[] tokens = line.split(",");
//            db.put(tokens[0],tokens[2]);
//        }
//    }

    public void playVideos() {

        Uri videoUri = Uri.parse("android.resource://" + App1.this.getPackageName() + "/raw/" + nWords.get(0));
        videoView.setVideoURI(videoUri);
        videoView.start();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp)
            {
                if (incrementer<nWords.size()-1){
                    incrementer++;
                    Uri videoUri = Uri.parse("android.resource://" + App1.this.getPackageName() + "/raw/" + nWords.get(1));
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                }
                else{
                    incrementer=0;
                    videoView.setVisibility(GONE);
                    videoView.setVisibility(VISIBLE);
                    return;
                }

            }
        });



    }

}

