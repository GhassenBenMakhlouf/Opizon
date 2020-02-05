package com.example.opizon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.opizon.App1.App1;
import com.example.opizon.App2.App2;
import com.example.opizon.App3.App3ModeActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CardView app1Card, app2Card, app3Card;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app1Card = (CardView) findViewById(R.id.app1_card);
        app2Card = (CardView) findViewById(R.id.app2_card);
        app3Card = (CardView) findViewById(R.id.app3_card);
        app1Card.setOnClickListener(this);
        app2Card.setOnClickListener(this);
        app3Card.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;

        switch (v.getId()){
            case R.id.app1_card : i = new Intent(this, App1.class); startActivity(i); break;
            case R.id.app2_card : i = new Intent(this, App2.class); startActivity(i); break;
            case R.id.app3_card : i = new Intent(this, App3ModeActivity.class); startActivity(i); break;
            default : break;
        }

    }
}
