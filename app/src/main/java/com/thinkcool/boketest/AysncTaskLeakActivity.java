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
public class AysncTaskLeakActivity extends AppCompatActivity {
    AsyncTask mTask;
    private Button mNextButton;
    private TextView pageTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        pageTextView= (TextView) findViewById(R.id.tv_page);
        pageTextView.setText("AysncTaskLeakActivity");
        mNextButton= (Button) findViewById(R.id.btn_next);
        mTask=new AsyncTask<String,Void,Void>()
        {
            @Override
            protected Void doInBackground(String... params) {
                //doSomething..
                Boolean loop=true;
                while (loop) {
                    if(isCancelled()) {
                        Log.d("test","task exit");
                        return null;
                    }
                    Log.d("test","task is running");
                }
                return null;
            }
        }.execute("a task");
    }

    @Override
    protected void onDestroy() {
//        mTask.cancel(true);
        super.onDestroy();
    }
}
