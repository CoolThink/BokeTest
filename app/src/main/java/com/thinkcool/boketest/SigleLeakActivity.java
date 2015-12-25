package com.thinkcool.boketest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by thinkcool on 2015/12/25.
 */
public class SigleLeakActivity extends AppCompatActivity{

    private MyListener mMyListener=new MyListener() {
        @Override
        public void onSomeThingHappen() {
        }
    };
    private TestManager testManager=TestManager.getInstance();
    private Button mNextButton;
    private TextView pageTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        pageTextView= (TextView) findViewById(R.id.tv_page);
        pageTextView.setText("SigleLeakActivity");
        mNextButton= (Button) findViewById(R.id.btn_next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SigleLeakActivity.this,AysncTaskLeakActivity.class);
                startActivity(intent);
            }
        });
        testManager.registerListener(mMyListener);
    }

    @Override
    protected void onDestroy() {
        testManager.unregisterListener(mMyListener);
        super.onDestroy();
    }
}
