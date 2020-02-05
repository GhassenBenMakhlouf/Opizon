package com.example.opizon;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;

public class MyVideoView extends VideoView {
    private static final int SCALE_TYPE_NORMAL = 0;
    private static final int SCALE_TYPE_CENTER_CROP = 1;
    private static final int SCALE_TYPE_FILL = 2;

    private int mScaleType;
    private int mHorizontalAspectRatioThreshold;
    private int mVerticalAspectRatioThreshold;

    public MyVideoView(Context context) {
        this(context, null, 0);
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MyVideoView, 0, 0);
        mScaleType = attributes.getInt(R.styleable.MyVideoView_scaleType, SCALE_TYPE_NORMAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mScaleType == SCALE_TYPE_CENTER_CROP) {
            applyCenterCropMeasure(widthMeasureSpec, heightMeasureSpec);
        } else if (mScaleType == SCALE_TYPE_FILL) {
            applyFillMeasure(widthMeasureSpec, heightMeasureSpec);
        } // else default/no-op
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        if (mScaleType == SCALE_TYPE_CENTER_CROP) {
            applyCenterCropLayout(l, t, r, b);
        } else {
            super.layout(l, t, r, b);
        }
    }

    private void applyCenterCropLayout(int left, int top, int right, int bottom) {
        super.layout(left + mHorizontalAspectRatioThreshold, top + mVerticalAspectRatioThreshold, right
                + mHorizontalAspectRatioThreshold, bottom + mVerticalAspectRatioThreshold);
    }

    private void applyCenterCropMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int videoWidth = getMeasuredWidth();
        int videoHeight = getMeasuredHeight();

        int viewWidth = getDefaultSize(0, widthMeasureSpec);
        int viewHeight = getDefaultSize(0, heightMeasureSpec);

        mHorizontalAspectRatioThreshold = 0;
        mVerticalAspectRatioThreshold = 0;
        if (videoWidth == viewWidth) {
            int newWidth = (int) ((float) videoWidth / videoHeight * viewHeight);
            setMeasuredDimension(newWidth, viewHeight);
            mHorizontalAspectRatioThreshold = -(newWidth - viewWidth) / 2;
        } else {
            int newHeight = (int) ((float) videoHeight / videoWidth * viewWidth);
            setMeasuredDimension(viewWidth, newHeight);
            mVerticalAspectRatioThreshold = -(newHeight - viewHeight) / 2;

        }
    }

    private void applyFillMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}