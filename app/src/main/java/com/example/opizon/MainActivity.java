package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CardView app1Card, app2Card;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app1Card = (CardView) findViewById(R.id.app1_card);
        app2Card = (CardView) findViewById(R.id.app2_card);
        app1Card.setOnClickListener(this);
        app2Card.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;

        switch (v.getId()){
            case R.id.app1_card : i = new Intent(this,App1.class); startActivity(i); break;
            case R.id.app2_card : i = new Intent(this,App2.class); startActivity(i); break;
            default : break;
        }

    }
}
