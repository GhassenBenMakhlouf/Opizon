package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ViewSwitcher;

import com.google.android.material.button.MaterialButton;

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
    TextView wordOverlay;
    TextView containerUserHelper;
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

        wordOverlay = findViewById(R.id.word_overlay);
        wordOverlay.setVisibility(View.INVISIBLE);

        containerUserHelper = findViewById(R.id.container_userhelper);
        containerUserHelper.setVisibility(View.INVISIBLE);

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
                Log.i("app1", tokens[0]);
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
                    if (db.containsKey(String.valueOf(words.get(i).charAt(j)).toUpperCase())){
                        String s;
                        s=db.get(String.valueOf(words.get(i).charAt(j)).toUpperCase())[1];
                        s=s.substring(0,s.length()-4);
                        videosToPlay.add(s);
                        seperatedSentence.add(String.valueOf(words.get(i).charAt(j)));
                    }
                }
            }
        }
    }

    public void playVideos() {

        Uri videoUri = Uri.parse("android.resource://" + App1.this.getPackageName() + "/raw/" + videosToPlay.get(0));
        videoView.setVideoURI(videoUri);
        videoView.start();

        //overlay manager
        wordOverlay.setText(videosToPlay.get(0));
        wordOverlay.setVisibility(VISIBLE);

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

                    //overlay manager
                    wordOverlay.setText(videosToPlay.get(incrementer));
                    wordOverlay.setVisibility(VISIBLE);
                    Log.i("app1", "this video has been played: "+videosToPlay.get(incrementer));
                }
                else{
                    incrementer=0;
                    videoView.setVisibility(GONE);
                    videoView.setVisibility(VISIBLE);

                    //overlay manager
                    wordOverlay.setVisibility(View.INVISIBLE);
                    return;
                }

            }
        });



    }

    public void createButtons(){

        //show container helper
        containerUserHelper.setVisibility(VISIBLE);

        //Get ScrollView and Layout
        HorizontalScrollView hsv1 = (HorizontalScrollView) findViewById( R.id.hsv);
        LinearLayout layout = (LinearLayout) hsv1.findViewById( R.id.wordsContainer );
        layout.removeAllViews();

        //configure layout parameters for buttons
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT );
        lp.setMargins( 20, 0, 20, 0 );

        for (int i=0;i<seperatedSentence.size();i++){
            Log.i("app1", "part of seperatedSentence: "+seperatedSentence.get(i));


//            Button wordButton = new Button(this);
            MaterialButton wordButton = new MaterialButton(this, null, R.attr.materialButtonOutlinedStyle);
            wordButton.setText(seperatedSentence.get(i));
            final String actualWord=seperatedSentence.get(i);
            final ArrayList<String> wordsToShow=new ArrayList<String>();
            wordsToShow.clear();
            final int finalI = i;
            wordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView txtClose;
                    TextView txtSwipeRight;
                    TextView txtSwipeLeft;
                    final TextSwitcher SwitcherWord;
                    final TextSwitcher SwitcherMeaning;
                    final TextView[] txtWord = new TextView[1];
                    final TextView[] txtMeaning = new TextView[1];
                    Button btnConfirm;
                    wordsDialog.setContentView(R.layout.app1_popup);
                    txtClose =(TextView) wordsDialog.findViewById(R.id.txtclose);
                    txtSwipeRight = (TextView) wordsDialog.findViewById(R.id.txtswiperight);
                    txtSwipeLeft = (TextView) wordsDialog.findViewById(R.id.txtswipeleft);
                    SwitcherWord= wordsDialog.findViewById(R.id.textswitcher1);
                    SwitcherMeaning= wordsDialog.findViewById(R.id.textswitcher2);
                    btnConfirm = (Button) wordsDialog.findViewById(R.id.btnconfirm);

                    String word=actualWord;
                    final int[] wordPosition = {0};
                    if (Character.isDigit(word.charAt(word.length()-1))) {
                        word = word.substring(0,word.length()-1);
                    }
                    int j=2;
                    wordsToShow.add(word);
                    String testKey=word+j;
                    while (db.containsKey(testKey.toUpperCase())){
                        wordsToShow.add(testKey);
                        j += 1;
                        testKey=word+j;
                    }
                    Log.i("app1", "wordsToShow Size: "+wordsToShow.size());

                    SwitcherWord.setFactory(new ViewSwitcher.ViewFactory() {
                        @Override
                        public View makeView() {
                            txtWord[0] = new TextView(App1.this);
                            txtWord[0].setTextColor(Color.WHITE);
                            txtWord[0].setGravity(Gravity.CENTER_HORIZONTAL);
                            txtWord[0].setTypeface(Typeface.DEFAULT_BOLD);
                            return txtWord[0];
                        }
                    });

                    SwitcherMeaning.setFactory(new ViewSwitcher.ViewFactory() {
                        @Override
                        public View makeView() {
                            txtMeaning[0] = new TextView(App1.this);
                            txtMeaning[0].setTextColor(Color.WHITE);
                            txtMeaning[0].setGravity(Gravity.CENTER_HORIZONTAL);
                            return txtMeaning[0];
                        }
                    });

                    SwitcherWord.setText(wordsToShow.get(0));
                    SwitcherMeaning.setText(db.get(wordsToShow.get(0).toUpperCase())[0]);

                    txtClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            wordsDialog.dismiss();
                        }
                    });


                    txtSwipeLeft.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (wordPosition[0] >0){
                                wordPosition[0]--;
                                Animation in = AnimationUtils.loadAnimation(App1.this,R.anim.slide_in_left);
                                Animation out = AnimationUtils.loadAnimation(App1.this,R.anim.slide_out_right);
                                SwitcherWord.setInAnimation(in);
                                SwitcherWord.setOutAnimation(out);
                                SwitcherMeaning.setInAnimation(in);
                                SwitcherMeaning.setOutAnimation(out);
                                SwitcherWord.setText(wordsToShow.get(wordPosition[0]));
                                SwitcherMeaning.setText(db.get(wordsToShow.get(wordPosition[0]).toUpperCase())[0]);
                                Log.i("app1", "swipe left: "+"wordPosition: "+wordPosition[0]);

                            }

                        }
                    });

                    txtSwipeRight.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (wordPosition[0] <wordsToShow.size()-1){
                                wordPosition[0]++;
                                Animation in = AnimationUtils.loadAnimation(App1.this,R.anim.slide_in_right);
                                Animation out = AnimationUtils.loadAnimation(App1.this,R.anim.slide_out_left);
                                SwitcherWord.setInAnimation(in);
                                SwitcherWord.setOutAnimation(out);
                                SwitcherMeaning.setInAnimation(in);
                                SwitcherMeaning.setOutAnimation(out);
                                SwitcherWord.setText(wordsToShow.get(wordPosition[0]));
                                SwitcherMeaning.setText(db.get(wordsToShow.get(wordPosition[0]).toUpperCase())[0]);
                                Log.i("app1", "swipe right: "+"wordPosition: "+wordPosition[0]);
                            }

                        }
                    });


                    btnConfirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String s;
                            s=db.get(wordsToShow.get(wordPosition[0]).toUpperCase())[1];
                            s=s.substring(0,s.length()-4);
                            videosToPlay.set(finalI,s);
                            seperatedSentence.set(finalI,wordsToShow.get(wordPosition[0]));
                            wordsDialog.dismiss();
                            playVideos();
                            wordsContainer.removeAllViews();
                            createButtons();
                        }
                    });


                    wordsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    wordsDialog.show();
                }
            });

            //set up the button dynamically
            wordButton.setLayoutParams(lp);
            wordButton.setGravity( Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL );

            wordsContainer.addView(wordButton);

        }
    }

}

