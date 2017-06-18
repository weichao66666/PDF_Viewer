package io.weichao.pdf_viewer.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import io.weichao.pdf_viewer.R;
import io.weichao.pdf_viewer.bean.PdfRendererParamBean;
import io.weichao.pdf_viewer.bean.PdfScaleBean;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PdfViewPager extends ViewPager {
    private Context mContext;

    private OnPageClickListener mOnPageClickListener = new OnPageClickListener() {
        @Override
        public void onPageTap(View view, float x, float y) {
            int item = getCurrentItem();
            int total = getChildCount();

            // 点击 ViewPager 左 1/3 翻到前一页，右 1/3 翻到后一页。
            if (x < 0.33f && item > 0) {
                item -= 1;
            } else if (x >= 0.67f && item < total - 1) {
                item += 1;
            }
            setCurrentItem(item);
        }
    };

    public PdfViewPager(Context context, String pdfPath) {
        super(context);

        mContext = context;

        init(pdfPath);
    }

    private void init(String pdfPath) {
        setClickable(true);
        setAdapter(new PDFPagerAdapter.Builder(mContext)
                .setPdfPath(pdfPath)
                .setOnPageClickListener(mOnPageClickListener)
                .setOffScreenSize(getOffscreenPageLimit())
                .create());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class PDFPagerAdapter extends PagerAdapter {
        private static final float DEFAULT_SCALE = 1f;
        private static final int FIRST_PAGE = 0;
        private static final float DEFAULT_RENDER_QUALITY = 2.0f;
        private static final int DEFAULT_OFF_SCREEN_SIZE = 1;

        private String mPdfPath;
        private Context mContext;

        private PdfRenderer mPdfRenderer;
        private BitmapPool mBitmapPool;
        private LayoutInflater mLayoutInflater;
        private float mRenderQuality = DEFAULT_RENDER_QUALITY;
        private int mOffScreenSize = DEFAULT_OFF_SCREEN_SIZE;
        private SparseArray<WeakReference<PhotoViewAttacher>> mPhotoViewAttachers;
        private PdfScaleBean mPdfScaleBean = new PdfScaleBean();
        private OnPageClickListener mOnPageClickListener;

        private PDFPagerAdapter(Context context, String pdfPath) {
            mPdfPath = pdfPath;
            mContext = context;

            init();
        }

        private void init() {
            mPhotoViewAttachers = new SparseArray<>();
            try {
                mLayoutInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                mPdfRenderer = new PdfRenderer(getSeekableFileDescriptor(mPdfPath));
                PdfRendererParamBean params = extractPdfParamsFromFirstPage(mPdfRenderer, mRenderQuality);
                mBitmapPool = new BitmapPool(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private ParcelFileDescriptor getSeekableFileDescriptor(String path) {
            try {
                return ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        private PdfRendererParamBean extractPdfParamsFromFirstPage(PdfRenderer renderer, float renderQuality) {
            PdfRendererParamBean params = new PdfRendererParamBean();

            params.setRenderQuality(renderQuality);
            params.setOffScreenSize(mOffScreenSize);
            PdfRenderer.Page samplePage = getPDFPage(renderer, FIRST_PAGE);
            params.setWidth((int) (samplePage.getWidth() * renderQuality));
            params.setHeight((int) (samplePage.getHeight() * renderQuality));
            samplePage.close();

            return params;
        }

        private PdfRenderer.Page getPDFPage(PdfRenderer renderer, int position) {
            return renderer.openPage(position);
        }

        @Override
        public int getCount() {
            return mPdfRenderer != null ? mPdfRenderer.getPageCount() : 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mLayoutInflater.inflate(R.layout.view_pdf_page, container, false);
            if (mPdfRenderer == null || getCount() < position) {
                return view;
            }

            ImageView iv = view.findViewById(R.id.imageView);

            PdfRenderer.Page page = getPDFPage(mPdfRenderer, position);

            Bitmap bitmap = mBitmapPool.get(position);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            PhotoViewAttacher attacher = new PhotoViewAttacher(iv);
            attacher.setScale(mPdfScaleBean.getScale(), mPdfScaleBean.getCenterX(), mPdfScaleBean.getCenterY(), true);

            mPhotoViewAttachers.put(position, new WeakReference<>(attacher));

            iv.setImageBitmap(bitmap);
            attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float x, float y) {
                    if (mOnPageClickListener != null) {
                        mOnPageClickListener.onPageTap(view, x, y);
                    }
                }

                @Override
                public void onOutsidePhotoTap() {
                }
            });
            attacher.update();
            container.addView(view, 0);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // bitmap.recycle() causes crashes if called here.
            // All bitmaps are recycled in close().
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void close() {
            if (mBitmapPool != null) {
                mBitmapPool.clear();
                mBitmapPool = null;
            }
            if (mPdfRenderer != null) {
                mPdfRenderer.close();
                mPdfRenderer = null;
            }
            if (mPhotoViewAttachers != null) {
                mPhotoViewAttachers.clear();
                mPhotoViewAttachers = null;
            }
        }

        static class Builder {
            private Context mContext;
            private String mPdfPath = "";
            private float mScale = DEFAULT_SCALE;
            private float mCenterX = 0f;
            private float mCenterY = 0f;
            private int mOffScreenSize = DEFAULT_OFF_SCREEN_SIZE;
            private float mRenderQuality = DEFAULT_RENDER_QUALITY;
            private OnPageClickListener mOnPageClickListener;

            private Builder(Context context) {
                mContext = context;
            }

            private Builder setScale(float scale) {
                mScale = scale;
                return this;
            }

            private Builder setScale(PdfScaleBean scale) {
                mScale = scale.getScale();
                mCenterX = scale.getCenterX();
                mCenterY = scale.getCenterY();
                return this;
            }

            private Builder setCenterX(float centerX) {
                mCenterX = centerX;
                return this;
            }

            private Builder setCenterY(float centerY) {
                mCenterY = centerY;
                return this;
            }

            private Builder setRenderQuality(float renderQuality) {
                mRenderQuality = renderQuality;
                return this;
            }

            private Builder setOffScreenSize(int offScreenSize) {
                mOffScreenSize = offScreenSize;
                return this;
            }

            private Builder setPdfPath(String path) {
                mPdfPath = path;
                return this;
            }

            private Builder setOnPageClickListener(OnPageClickListener listener) {
                mOnPageClickListener = listener;
                return this;
            }

            private PDFPagerAdapter create() {
                PDFPagerAdapter adapter = new PDFPagerAdapter(mContext, mPdfPath);
                adapter.mPdfScaleBean.setScale(mScale);
                adapter.mPdfScaleBean.setCenterX(mCenterX);
                adapter.mPdfScaleBean.setCenterY(mCenterY);
                adapter.mOffScreenSize = mOffScreenSize;
                adapter.mRenderQuality = mRenderQuality;
                adapter.mOnPageClickListener = mOnPageClickListener;
                return adapter;
            }
        }
    }

    interface OnPageClickListener {
        void onPageTap(View view, float x, float y);
    }
}
