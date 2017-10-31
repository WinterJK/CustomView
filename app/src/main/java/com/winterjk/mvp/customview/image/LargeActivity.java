package com.winterjk.mvp.customview.image;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.winterjk.mvp.customview.R;

import java.io.File;

/**
 * create by WinterJK
 * 高清图浏览控件展示
 */
public class LargeActivity extends Activity {
    private LargeImageView mLargeImageView;
    public final int UP = 19, DOWN = 20, LEFT = 21, RIGHT = 22;
    private String mPath;
    private int mDegree;
    private LargeImageViewDegree mLargeImageViewDegree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.large_layout);
        Intent intent = getIntent();
        if (null != intent) {
            mPath = intent.getStringExtra("path");
            if (!"".equals(mPath) && null != mPath) {
                mDegree = BitmapUtils.readPictureDegree(mPath);
                if (mDegree == 0 || mDegree == 180) {
                    mLargeImageView = (LargeImageView) findViewById(R.id.iv_photo);
                    mLargeImageView.setVisibility(View.VISIBLE);
                    mLargeImageView.setInputStream(new File(mPath).getPath(), this);
                } else {
                    mLargeImageViewDegree = (LargeImageViewDegree) findViewById(R.id.iv_photo_degree);
                    mLargeImageViewDegree.setVisibility(View.VISIBLE);
                    mLargeImageViewDegree.setInputStream(new File(mPath).getPath(), this);
                }
            } else {
                Toast.makeText(this, R.string.view_image_none, Toast.LENGTH_SHORT).show();
                this.finish();
            }
        } else {
            Toast.makeText(this, R.string.view_image_none, Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (mDegree == 0 || mDegree == 180) {
            switch (keyCode) {
                case UP:
                    mLargeImageView.moveEvent(EKeyEvent.UP);
                    break;
                case DOWN:
                    mLargeImageView.moveEvent(EKeyEvent.DOWN);
                    break;
                case LEFT:
                    mLargeImageView.moveEvent(EKeyEvent.LEFT);
                    break;
                case RIGHT:
                    mLargeImageView.moveEvent(EKeyEvent.RIGHT);
                    break;
                default:
                    break;
            }
        } else {
            switch (keyCode) {
                case UP:
                    mLargeImageViewDegree.moveEvent(EKeyEvent.LEFT);
                    break;
                case DOWN:
                    mLargeImageViewDegree.moveEvent(EKeyEvent.RIGHT);
                    break;
                case LEFT:
                    mLargeImageViewDegree.moveEvent(EKeyEvent.DOWN);
                    break;
                case RIGHT:
                    mLargeImageViewDegree.moveEvent(EKeyEvent.UP);
                    break;
                default:
                    break;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (null != mLargeImageView) {
            mLargeImageView.recycleBitmap();
            mLargeImageView = null;
        }
        if (null != mLargeImageViewDegree) {
            mLargeImageViewDegree.recycleBitmap();
            mLargeImageViewDegree = null;
        }
        super.onDestroy();
    }

}
