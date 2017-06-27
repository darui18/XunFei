package com.example.desktop_gql8asp.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SpeechUtility.createUtility( this, SpeechConstant.APPID +"="+ getString(R.string.app_id));
        setContentView(R.layout.activity_main);
        Button localBt = (Button) findViewById(R.id.wake_test_bt);
        localBt.setOnClickListener(this);
        Button cloudBt = (Button) findViewById(R.id.cloud_test_bt);
        cloudBt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.wake_test_bt:
                 intent = new Intent(MainActivity.this,WakeActivity.class);
                startActivity(intent);
                break;
            case R.id.cloud_test_bt:
                 intent = new Intent(MainActivity.this,CmdRecActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

    }
}
