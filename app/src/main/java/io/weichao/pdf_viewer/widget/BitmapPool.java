package io.weichao.pdf_viewer.widget;

import android.graphics.Bitmap;
import android.graphics.Color;

import io.weichao.pdf_viewer.bean.PdfRendererParamBean;

public class BitmapPool {
    private Bitmap[] mBitmaps;
    private int mBitmapPoolSize;
    private int mWidth;
    private int mHeight;
    private Bitmap.Config mConfig;

    public BitmapPool(PdfRendererParamBean params) {
        mBitmapPoolSize = initBitmapPoolSize(params.getOffScreenSize());
        mWidth = params.getWidth();
        mHeight = params.getHeight();
        mConfig = params.getConfig();
        mBitmaps = new Bitmap[mBitmapPoolSize];
    }

    private int initBitmapPoolSize(int offScreenSize) {
        return (offScreenSize) * 2 + 1;
    }

    public Bitmap get(int position) {
        int index = position % mBitmapPoolSize;
        if (mBitmaps[index] == null) {
            Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, mConfig);
            mBitmaps[index] = bitmap;
        }
        mBitmaps[index].eraseColor(Color.TRANSPARENT);
        return mBitmaps[index];
    }

    public void remove(int position) {
        if (mBitmaps[position] != null) {
            mBitmaps[position].recycle();
            mBitmaps[position] = null;
        }
    }

    public void clear() {
        for (int i = 0; i < mBitmapPoolSize; i++) {
            if (mBitmaps[i] != null) {
                mBitmaps[i].recycle();
                mBitmaps[i] = null;
            }
        }
    }
}