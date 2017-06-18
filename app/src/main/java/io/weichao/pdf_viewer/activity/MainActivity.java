package io.weichao.pdf_viewer.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.weichao.pdf_viewer.R;
import io.weichao.pdf_viewer.widget.PdfViewPager;

public class MainActivity extends AppCompatActivity {
    public static void actionStart(Context context, String filePath) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setData(Uri.fromFile(new File(filePath)));
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RelativeLayout root = (RelativeLayout) findViewById(R.id.root);

        /*文件在 SD 卡中，可直接使用。*/
//        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "demo.pdf";

        /*文件在 assets 中，复制到 SD 中再使用。*/
        String filePath = "demo.pdf";
        if (isAnAsset(filePath)) {
            String newFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "demo.pdf";
            doFileExist(newFilePath);
            copyAssets2Sd(filePath, newFilePath);
            filePath = newFilePath;
        }

        PdfViewPager pdfViewPager = new PdfViewPager(this, filePath);
        root.addView(pdfViewPager);
    }

    private boolean isAnAsset(String path) {
        return !path.startsWith("/");
    }

    private void doFileExist(String filePath) {
        File file = new File(filePath);
        File fileDir = file.getParentFile();
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            fileDir.mkdirs();
        }
        if (!file.exists() || file.isDirectory()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyAssets2Sd(String assetFilePath, String sdFilePath) {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = getResources().getAssets().open(assetFilePath);
            fos = new FileOutputStream(sdFilePath);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
