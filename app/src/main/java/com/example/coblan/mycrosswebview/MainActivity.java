package com.example.coblan.mycrosswebview;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;

import org.xwalk.core.XWalkDownloadListener;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private XWalkView xWalkWebView;

    HttpDownloader downloader ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xWalkWebView = (XWalkView) findViewById(R.id.xwalkWebView);
        xWalkWebView.setUIClient(new UIClient(xWalkWebView));
        xWalkWebView.load("http://10.0.18.6:8080/home", null);

        // turn on debugging
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);

        Log.i("init","init windows");
        xWalkWebView.setDownloadListener(new XWalkDownloadListener(getApplicationContext()) {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String
                                                mimetype,
                                        long contentLength) {
                Log.i("download_mmm",url);
//                downloader = new HttpDownloader();
                final String urlStr = url;
//                downloader.download(urlStr);


                DownloadManager downloadManager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
//                downloadManager = (DownloadManager)getSystemService(serviceString);

                String file_name = url.substring(url.lastIndexOf("/") + 1, url.length());

                Uri uri = Uri.parse(url);
                DownloadManager.Request request = new DownloadManager.Request(uri);
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir() ;
                String applicationName = getApplicationName();

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS , "/"+applicationName+"/"+file_name);
                long downloadId = downloadManager.enqueue(request);
//                long reference = downloadManager.enqueue(request);



//                Runnable networkTask = new Runnable() {
//
//                    @Override
//                    public void run() {
//                        // TODO
//                        // 在这里进行 http request.网络请求相关操作
//                        String path="file";
//                        String fileName="2.jpg";
//                        HttpDownloader httpDownloader = new HttpDownloader();
//                        httpDownloader.downlaodFile(urlStr,path,fileName);
//                    }
//                };
//                new Thread(networkTask).start();


//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse(url));
//                startActivity(i);
            }
        });
    }

    public String getApplicationName() {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName =
                (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (xWalkWebView != null) {
            xWalkWebView.pauseTimers();
            xWalkWebView.onHide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (xWalkWebView != null) {
            xWalkWebView.resumeTimers();
            xWalkWebView.onShow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (xWalkWebView != null) {
            xWalkWebView.onDestroy();
        }
    }

    private ValueCallback<Uri> mFilePathCallback;
    private String mCameraPhotoPath;
    public static final int INPUT_FILE_REQUEST_CODE = 1;

    class UIClient extends XWalkUIClient {

        public UIClient(XWalkView view) {
            super(view);
        }
        public void openFileChooser(XWalkView view, ValueCallback<Uri> uploadFile,
                                    String acceptType, String capture) {

            if(!(acceptType.toLowerCase().equals("image/*") && capture.equals("true"))){
                super.openFileChooser(view, uploadFile, acceptType, capture);
                return;
            }


            if(mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = uploadFile;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
            } catch (IOException ex) {
//                Log.e(TAG, "Unable to create Image File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }


            Intent contentSelectionIntent =new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            xWalkWebView.onActivityResult(requestCode, resultCode, data);
            return;
        }


        Uri results = null;

        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data == null) {
                // If there is not data, then we may have taken a photo
                if(mCameraPhotoPath != null) {
                    results = Uri.parse(mCameraPhotoPath);
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = Uri.parse(dataString);
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
        return;

    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }
}
