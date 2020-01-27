package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private HashMap<String,String[]> db=new HashMap<String,String[]>();
    private final int REQ_CODE = 100;
    TextView textView;
    VideoView videoView;
    LinearLayout wordsContainer;
    Dialog wordsDialog;
    ArrayList<String> videosToPlay=new ArrayList<String>();
    ArrayList<String> seperatedSentence=new ArrayList<String>();
    int incrementer=0;
    String sentence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            readDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_app1);
        textView = findViewById(R.id.text);
        videoView = findViewById(R.id.video);
        wordsContainer = (LinearLayout) findViewById(R.id.wordsContainer);
        wordsDialog = new Dialog(this);
        ImageView speak = findViewById(R.id.speak);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
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
                    sentence = (String) result.get(0);
                    wordsContainer.removeAllViews();
                    textView.setText(sentence);
                    //sentence=sentence.toLowerCase();

                    setVideos();
                    playVideos();
                    createButtons();


                }
                break;
            }
        }
    }

    public void readDatabase() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.dataset_all_modified);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, Charset.forName("UTF-8"))
        );
        String line = "";
        while ((line=reader.readLine()) != null){
            String[] tokens = line.split(",");

            int i=1;
            String testKey=tokens[0];
            while (db.containsKey(testKey)){
                i += 1;
                testKey=tokens[0]+i;
            }

            if (i==1){
                db.put(tokens[0],new String[]{tokens[1],tokens[2]});
            }
            else{
                db.put(testKey,new String[]{tokens[1],tokens[2]});
            }
        }
    }

    public void setVideos(){
        Log.i("app1", "setVideos: "+sentence);
        ArrayList<String> words=new ArrayList<String>();
        String word="";
        for (int i=0; i<sentence.length(); i++ ){
            if (sentence.charAt(i)==' '){
                words.add(word);
                word="";
            }
            else{
                word+= String.valueOf(sentence.charAt(i));
            }
            if (i == sentence.length()-1 && !word.isEmpty()){
                words.add(word);
            }
        }
        videosToPlay.clear();
        seperatedSentence.clear();

        for (int i=0; i<words.size(); i++ ){
            Boolean inDatabase=false;
            ArrayList<String> combinations=new ArrayList<String>();
            Log.i("app1", "combinations starting with "+words.get(i));
            combinations.add(words.get(i));
            Log.i("app1", "combination 0 : "+combinations.get(0));
            for (int j=i+1; j<words.size(); j++){
                combinations.add(combinations.get(j-i-1)+' '+words.get(j));
                Log.i("app1", "combination "+(j-i)+" : "+combinations.get(j-i));
            }

            for (int j=combinations.size()-1; j>=0; j--){
                Log.i("app1", "checking combination "+(j)+" : "+combinations.get(j));
                if (db.containsKey(combinations.get(j).toUpperCase())){
                    Log.i("app1", "combination "+j+" starting from word "+i+" exist in the db: "+combinations.get(j));
                    String s;
                    s=db.get(combinations.get(j).toUpperCase())[1];
                    s=s.substring(0,s.length()-4);
                    videosToPlay.add(s);
                    Log.i("app1", "Video added: "+s);
                    seperatedSentence.add(combinations.get(j));
                    int wordsNumber=0;
                    wordsNumber=combinations.get(j).length() - combinations.get(j).replaceAll(" ", "").length() + 1;
                    i=i+wordsNumber-1;
                    Log.i("app1", "wordsNumber: "+wordsNumber+" ; new i: "+i);
                    inDatabase=true;
                    break;
                }
            }
            if (!inDatabase){
                Log.i("app1", "word "+i+" doesn't exist in the db: "+words.get(i));
                for(int j=0; j<words.get(i).length(); j++ ){
                    if (db.containsKey(String.valueOf(words.get(i).charAt(j)))){
                        String s;
                        s=db.get(String.valueOf(words.get(i).charAt(j)))[1];
                        s=s.substring(0,s.length()-5);
                        videosToPlay.add(s);
                    }
                }
                seperatedSentence.add(words.get(i));
            }
        }
    }

    public void playVideos() {

        Uri videoUri = Uri.parse("android.resource://" + App1.this.getPackageName() + "/raw/" + videosToPlay.get(0));
        videoView.setVideoURI(videoUri);
        videoView.start();
        Log.i("app1", "this video has been played: "+videosToPlay.get(0));
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp)
            {
                if (incrementer<videosToPlay.size()-1){
                    incrementer++;
                    Uri videoUri = Uri.parse("android.resource://" + App1.this.getPackageName() + "/raw/" + videosToPlay.get(incrementer));
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                    Log.i("app1", "this video has been played: "+videosToPlay.get(incrementer));
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

    public void createButtons(){

        for (int i=0;i<seperatedSentence.size();i++){
            Log.i("app1", "part of seperatedSentence: "+seperatedSentence.get(i));
            Button wordButton = new Button(this);
            wordButton.setText(seperatedSentence.get(i));
            final String word=seperatedSentence.get(i);
            final ArrayList<String> wordsToShow=new ArrayList<String>();
            wordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView txtClose;
                    TextView txtSwipeRight;
                    TextView txtSwipeLeft;
                    TextView txtWord;
                    TextView txtMeaning;
                    Button btnConfirm;
                    wordsDialog.setContentView(R.layout.app1_popup);
                    txtClose =(TextView) wordsDialog.findViewById(R.id.txtclose);
                    txtSwipeRight = (TextView) wordsDialog.findViewById(R.id.txtswiperight);
                    txtSwipeLeft = (TextView) wordsDialog.findViewById(R.id.txtswipeleft);
                    txtWord = (TextView) wordsDialog.findViewById(R.id.txtword);
                    txtMeaning = (TextView) wordsDialog.findViewById(R.id.txtmeaning);
                    btnConfirm = (Button) wordsDialog.findViewById(R.id.btnconfirm);

                    int i=1;
                    wordsToShow.add(word);
                    String testKey=word;
                    while (db.containsKey(testKey)){
                        wordsToShow.add(testKey);
                        i += 1;
                        testKey=word+i;
                    }

                    txtWord.setText(wordsToShow.get(0));
                    txtMeaning.setText(db.get(wordsToShow.get(0).toUpperCase())[0]);

                    txtClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            wordsDialog.dismiss();
                        }
                    });

                    txtSwipeLeft.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });

                    txtSwipeRight.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });

                    btnConfirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });


                    wordsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    wordsDialog.show();
                }
            });
            wordsContainer.addView(wordButton);

        }
    }

}

