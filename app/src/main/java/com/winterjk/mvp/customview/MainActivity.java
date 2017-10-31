package com.winterjk.mvp.customview;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mLargeImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initListener() {
        mLargeImageView.setOnClickListener(this);
    }

    private void initView() {
        mLargeImageView = (Button) findViewById(R.id.bt_image);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_image:
                Intent intent = new Intent();
                String path = Environment.getExternalStorageDirectory().getPath() + "/image.jpg";
                intent.putExtra("path", path);
                intent.setAction("com.winterjk.mvp.customview.action.IMAGE");
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
