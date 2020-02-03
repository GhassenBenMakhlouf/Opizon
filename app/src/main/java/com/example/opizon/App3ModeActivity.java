package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class App3ModeActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView Mode1Card, Mode2Card, Mode3Card, Mode4Card, Mode5Card, Mode6Card;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app3_mode);
        Mode1Card = (CardView) findViewById(R.id.emotion_mode_card);
        Mode2Card = (CardView) findViewById(R.id.faceage_mode_card);
        Mode3Card = (CardView) findViewById(R.id.hair_mode_card);
        Mode4Card = (CardView) findViewById(R.id.makeup_mode_card);
        Mode5Card = (CardView) findViewById(R.id.headpose_mode_card);
        Mode6Card = (CardView) findViewById(R.id.accessories_mode_card);

        Mode1Card.setOnClickListener(this);
        Mode2Card.setOnClickListener(this);
        Mode3Card.setOnClickListener(this);
        Mode4Card.setOnClickListener(this);
        Mode5Card.setOnClickListener(this);
        Mode6Card.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i = new Intent(this, App3.class);

        switch (v.getId()){
            case R.id.emotion_mode_card :
                i.putExtra("QUOTE_MODE", "emotion");
                startActivity(i); break;
            case R.id.faceage_mode_card :
                i.putExtra("QUOTE_MODE", "faceage" );
                startActivity(i); break;
            case R.id.hair_mode_card :
                i.putExtra("QUOTE_MODE", "hair" );
                startActivity(i); break;
            case R.id.makeup_mode_card :
                i.putExtra("QUOTE_MODE", "makeup");
                startActivity(i); break;
            case R.id.headpose_mode_card :
                i.putExtra("QUOTE_MODE", "headpose");
                startActivity(i); break;
            case R.id.accessories_mode_card :
                i.putExtra("QUOTE_MODE", "accessories");
                startActivity(i); break;
            default : break;
        }

    }
}
