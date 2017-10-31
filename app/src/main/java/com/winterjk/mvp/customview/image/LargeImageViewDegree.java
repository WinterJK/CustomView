package com.winterjk.mvp.customview.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import com.winterjk.mvp.customview.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LargeImageViewDegree extends View {
    private BitmapRegionDecoder mDecoder;

    private volatile Rect mRectLarge = new Rect();

    private MoveGestureDetector mDetector;

    private int moveX;
    private int moveY;

    private RectF mRect;//外边框
    private RectF mThumbRect;//缩略图位置
    private RectF mRectSmall;//移动小框

    private Paint mPaint;
    private Paint mPaintSmall;

    private Bitmap mBitmap;
    private Bitmap mBitmapThumb;

    //缩略图
    private int mThumbW, mThumbH;
    //大图尺寸
    private int mImageWidth, mImageHeight;
    //缩略图与屏幕比例
    private float thumbScreenScale = 0.15f;
    //移动步长
    private float MOVE_SCALE = 0.38f;
    //大图移动量
    private int mMoveX, mMoveY;
    //局部小框移动量
    private float mZoomMoveX, mZoomMoveY;
    //缩略图位置
    private float mThumbX = 100, mThumbY = 100;
    //局部区域大小
    private float mSmallRangeX, mSmallRangeY;
    //局部区域表框圆角
    private final int ZOOM_ROUND = 6;

    private float[] zoomScaleWH = new float[2];

    private static final BitmapFactory.Options options = new BitmapFactory.Options();

    private String mImagePath;

    private int onMeasureCount = 0;

    static {
        if (BitmapUtils.getDeviceTotalMemory() > 1800) {
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        } else {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
    }

    private int mPictureDegree;
    private LargeActivity mActivity;

    public LargeImageViewDegree(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInputStream(String path, Context context) {
        mImagePath = path;
        mPictureDegree = BitmapUtils.readPictureDegree(mImagePath);
        this.mActivity = (LargeActivity) context;
    }

    private Bitmap decodeThumb() {
        BitmapFactory.Options thumbOptions = new BitmapFactory.Options();
        thumbOptions.inSampleSize = calculateInSampleSize(options, mThumbW, mThumbH);
        thumbOptions.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(mImagePath, thumbOptions);
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public float[] scaleWH(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        float[] inSampleSize = new float[2];

        if (height > reqHeight || width > reqWidth) {
            inSampleSize[0] = (float) width / (float) reqWidth;
            inSampleSize[1] = (float) height / (float) reqHeight;
        } else {
            inSampleSize[0] = 1;
            inSampleSize[1] = 1;
        }
        return inSampleSize;
    }

    public void init() {
        //缩略图位置
        mThumbRect = new RectF(0, 0, mThumbW, mThumbH);
        //外边框
        mRect = new RectF(0, 0, mThumbW, mThumbH);
        //小边框
        mRectSmall = new RectF(0, 0, mSmallRangeX, mSmallRangeY);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(getResources().getColor(R.color.colorWithe));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);

        mPaintSmall = new Paint();
        mPaintSmall.setAntiAlias(true);
        mPaintSmall.setColor(getResources().getColor(R.color.per80_blue_00b7ee));
        mPaintSmall.setStyle(Paint.Style.STROKE);
        mPaintSmall.setStrokeWidth(2);

        mDetector = new MoveGestureDetector(getContext(), new MoveGestureDetector.SimpleMoveGestureDetector() {
            @Override
            public boolean onMove(MoveGestureDetector detector) {
                moveX = (int) detector.getMoveX();
                moveY = (int) detector.getMoveY();

                if (mImageWidth > getWidth()) {
                    mRectLarge.offset(-moveX, 0);
                    mRectSmall.offset(-moveX / zoomScaleWH[0], 0);
                    checkWidth();
                    invalidate();
                }
                if (mImageHeight > getHeight()) {
                    mRectLarge.offset(0, -moveY);
                    mRectSmall.offset(0, -moveY / zoomScaleWH[1]);
                    checkHeight();
                    invalidate();
                }

                return true;
            }
        });
    }

    private void checkWidth() {
        Rect rect = mRectLarge;
        RectF rectSmall = mRectSmall;
        int imageWidth = mImageWidth;

        if (rect.right >= imageWidth) {
            rect.right = imageWidth;
            rect.left = imageWidth - getHeight();
        }

        if (rect.left < 0) {
            rect.left = 0;
            rect.right = getHeight();
        }

        if (rectSmall.top <= mThumbRect.top) {
            rectSmall.top = mThumbRect.top;
            rectSmall.bottom = mThumbRect.top + mSmallRangeX;
        }

        if (rectSmall.bottom >= mThumbRect.bottom) {
            rectSmall.bottom = mThumbRect.bottom;
            rectSmall.top = mThumbRect.bottom - mSmallRangeX;
        }
    }

    private void checkHeight() {
        Rect rect = mRectLarge;
        RectF rectSmall = mRectSmall;
        //修复CANAPK-102用实际尺寸查看色彩渐变图片时，向下移动焦点后屏幕变黑
        int imageHeight = mImageHeight - (int) (mPaintSmall.getStrokeWidth() + mPaint.getStrokeWidth());

        if (rect.top <= 0) {
            rect.top = 0;
            rect.bottom = getWidth();
        }

        if (rect.bottom >= imageHeight) {
            rect.bottom = imageHeight;
            rect.top = imageHeight - getWidth();
        }

        if (rectSmall.right > mThumbRect.right) {
            rectSmall.right = mThumbRect.right;
            rectSmall.left = mThumbRect.right - mSmallRangeY;
        }

        if (rectSmall.left < mThumbRect.left) {
            rectSmall.left = mThumbRect.left;
            rectSmall.right = mThumbRect.left + mSmallRangeY;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onToucEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //显示局部的大图
        try {
            if (null != mDecoder) {
                mBitmap = mDecoder.decodeRegion(mRectLarge, options);
                if (null == mBitmap) {
                    mActivity.finish();
                    Toast.makeText(mActivity, R.string.view_image_none, Toast.LENGTH_SHORT).show();
                }
                Bitmap bitmap = BitmapUtils.rotateBitmapByDegree(mBitmap, mPictureDegree);
                canvas.drawBitmap(bitmap, 0, 0, null);
            } else {
                Toast.makeText(mActivity, R.string.view_image_none, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //缩略图
        Bitmap bitmap = BitmapUtils.rotateBitmapByDegree(ThumbnailUtils.extractThumbnail(mBitmapThumb, mThumbW, mThumbH), mPictureDegree);
        canvas.drawBitmap(bitmap, null, mThumbRect, null);
        //外边框
        canvas.drawRect(mRect, mPaint);
        //局部边框
        canvas.drawRect(mRectSmall, mPaintSmall);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (onMeasureCount == 0) {
            try {
                mDecoder = BitmapRegionDecoder.newInstance(mImagePath, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mImagePath, options);

            mImageWidth = options.outWidth;
            mImageHeight = options.outHeight;

            requestLayout();
            init();
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            int imageWidth = mImageWidth;
            int imageHeight = mImageHeight;

            float widthMax = (float) width / (float) mImageWidth;
            float heightMax = (float) height / (float) mImageHeight;
            float thumbScaleMax;
            if (widthMax > heightMax) {
                thumbScaleMax = widthMax;
            } else {
                thumbScaleMax = heightMax;
            }

            //缩略图大小
            mThumbW = (int) ((float) imageWidth * thumbScaleMax * thumbScreenScale);
            mThumbH = (int) ((float) imageHeight * thumbScaleMax * thumbScreenScale);

            mBitmapThumb = decodeThumb();

            if (mPictureDegree == 90 || mPictureDegree == 270) {
                width = getMeasuredHeight();
                height = getMeasuredWidth();
            }

            //小框大小
            zoomScaleWH = scaleWH(options, width, height);
            mSmallRangeX = zoomScaleWH[0] >= 1 ? mThumbW / zoomScaleWH[0] : mThumbW;
            mSmallRangeY = zoomScaleWH[1] >= 1 ? mThumbH / zoomScaleWH[1] : mThumbH;

            //大图移动步长
            mMoveX = (int) (width * MOVE_SCALE);
            mMoveY = (int) (height * MOVE_SCALE);

            //局部小框移动步长
            mZoomMoveX = (int) (mSmallRangeX * MOVE_SCALE);
            mZoomMoveY = (int) (mSmallRangeY * MOVE_SCALE);

            //默认直接显示图片的中心区域
            mRectLarge.left = imageWidth / 2 - width / 2;
            mRectLarge.top = imageHeight / 2 - height / 2;
            mRectLarge.right = mRectLarge.left + width;
            mRectLarge.bottom = mRectLarge.top + height;

            mThumbX = getMeasuredWidth() - mThumbX - mThumbH;

            if (mPictureDegree == 90 || mPictureDegree == 270) {
                mThumbRect.left = mThumbX;
                mThumbRect.top = mThumbY;
                mThumbRect.right = mThumbX + mThumbH;
                mThumbRect.bottom = mThumbRect.top + mThumbW;

                mRect.left = mThumbX;
                mRect.top = mThumbY;
                mRect.right = mThumbX + mThumbH;
                mRect.bottom = mRect.top + mThumbW;

                mRectSmall.left = (mThumbH / 2) - (mSmallRangeY / 2) + mThumbX;
                mRectSmall.top = (mThumbW / 2) - (mSmallRangeX / 2) + mThumbY;
                mRectSmall.right = (mThumbH / 2) + mThumbX + (mSmallRangeY / 2);
                mRectSmall.bottom = (mThumbW / 2) + mThumbY + (mSmallRangeX / 2);
            }
        }
        onMeasureCount++;
    }

    public void moveEvent(EKeyEvent keyEvent) {

        switch (keyEvent) {
            case UP:
                if (zoomScaleWH[1] <= 1) {
                    break;
                }
                mRectLarge.offset(0, mPictureDegree == 270 ? (moveY + mMoveY) : (moveY - mMoveY));
                mRectSmall.offset(mZoomMoveY, 0);
                checkHeight();
                invalidate();
                break;
            case DOWN:
                if (zoomScaleWH[1] <= 1) {
                    break;
                }
                mRectLarge.offset(0, mPictureDegree == 270 ? (moveY - mMoveY) : (moveY + mMoveY));
                mRectSmall.offset(-mZoomMoveY, 0);
                checkHeight();
                invalidate();
                break;
            case LEFT:
                if (zoomScaleWH[0] <= 1) {
                    break;
                }
                mRectLarge.offset(mPictureDegree == 270 ? (moveX + mMoveX) : (moveX - mMoveX), 0);
                mRectSmall.offset(0, -mZoomMoveX);
                checkWidth();
                invalidate();
                break;
            case RIGHT:
                if (zoomScaleWH[0] <= 1) {
                    break;
                }
                mRectLarge.offset(mPictureDegree == 270 ? (moveX - mMoveX) : (moveX + mMoveX), 0);
                mRectSmall.offset(0, mZoomMoveX);
                checkWidth();
                invalidate();
                break;
            default:
                break;
        }
        checkHeight();
        invalidate();
    }

    public void recycleBitmap() {
        if (null != mDecoder && !mDecoder.isRecycled()) {
            mDecoder.recycle();
            mDecoder = null;
        }
        if (null != mBitmap && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }

        if (null != mBitmapThumb && !mBitmapThumb.isRecycled()) {
            mBitmapThumb.recycle();
            mBitmapThumb = null;
        }
    }

}