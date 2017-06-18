package io.weichao.pdf_viewer.bean;

import android.graphics.Bitmap;

public class PdfRendererParamBean {
    private static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.ARGB_8888;

    private int width;
    private int height;
    private float renderQuality;
    private int offScreenSize;
    private Bitmap.Config config = DEFAULT_CONFIG;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public float getRenderQuality() {
        return renderQuality;
    }

    public void setRenderQuality(float renderQuality) {
        this.renderQuality = renderQuality;
    }

    public int getOffScreenSize() {
        return offScreenSize;
    }

    public void setOffScreenSize(int offScreenSize) {
        this.offScreenSize = offScreenSize;
    }

    public Bitmap.Config getConfig() {
        return config;
    }

    public void setConfig(Bitmap.Config config) {
        this.config = config;
    }
}
