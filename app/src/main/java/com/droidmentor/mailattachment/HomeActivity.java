package com.droidmentor.mailattachment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HomeActivity extends AppCompatActivity {

    String TAG = "HomeActivity";

    Button btnAsset, btnStorage;

    public static String networkErrorMessage = "Network not available";
    public static boolean checkInternetConnection = true;
    public static boolean showErrorMessage = true;

    String mailID = "jaisonfdo@gmail.com";
    String mailSubject = "Attachment Sample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnStorage = (Button) findViewById(R.id.btnStorage);
        btnStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    browseDocuments();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        btnAsset = (Button) findViewById(R.id.btnAssets);
        btnAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getFilesFromAssets();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 100:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    sendMail(HomeActivity.this, mailID, mailSubject, null, uri);
                }
                break;
        }

    }


    /**
     * Select file from local storage
     *
     */
    private void browseDocuments() {

        String[] mimeTypes =
                {"application/msword", "text/plain", "application/pdf", "application/zip"};

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 0) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            String mimeTypesStr = "";
            for (String mimeType : mimeTypes) {
                mimeTypesStr += mimeType + "|";
            }
            intent.setType(mimeTypesStr.substring(0, mimeTypesStr.length() - 1));
        }
        startActivityForResult(Intent.createChooser(intent, "ChooseFile"), 100);

    }


    /**
     * Select file from Assets
     *
     * @throws IOException
     */
    public void getFilesFromAssets() throws IOException {
        AssetManager assetManager = getAssets();
        //replace the name by your file name, make sure file is inside your assets folder
        InputStream in = assetManager.open("sample.pdf");

        if (in != null) {
            File attachment = stream2file(in);
            sendMail(HomeActivity.this, mailID, mailSubject, attachment, null);
        }
        else
        {
            Log.d(TAG, "getFilesFromAssets: file not found");
        }
    }

    // Creating temp file, then only we can add this file as attachment

    public File stream2file(InputStream in) throws IOException {
        final File tempFile = File.createTempFile("sample1", ".pdf",
                HomeActivity.this.getExternalCacheDir());
        tempFile.deleteOnExit();

        FileOutputStream out = new FileOutputStream(tempFile);

        // for this you need add the following dependency in your build.gradle
        // compile 'org.apache.commons:commons-io:1.3.2'

        IOUtils.copy(in, out);
        return tempFile;
    }

    public void sendMail(Context context, String mailID, String subject, File attachment, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_EMAIL, mailID);
        // Need to grant this permission
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Attachment
        intent.setType("vnd.android.cursor.dir/email");

        if (attachment != null)
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachment));
        else if (uri != null)
            intent.putExtra(Intent.EXTRA_STREAM, uri);

        if (!TextUtils.isEmpty(subject))
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        if (isNetworkAvailable(context)) {
            if (isAppAvailable(context, "com.google.android.gm"))
                intent.setPackage("com.google.android.gm");
            startActivityForResult(intent, 101);
        }
    }

    // Check the applications presence

    public static Boolean isAppAvailable(Context context, String appName) {
        PackageManager pm = context.getPackageManager();
        boolean isInstalled;
        try {
            pm.getPackageInfo(appName,PackageManager.GET_ACTIVITIES);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            isInstalled = false;
        }
        return isInstalled;
    }
/*
     Check the Network availability
     For this you have to add the following permissions in AndroidManifest file
     <uses-permission android:name="android.permission.INTERNET" />
     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />*/

    public static boolean isNetworkAvailable(Context context) {

        if (checkInternetConnection) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting())
                return true;
            else {
                if (showErrorMessage)
                    Toast.makeText(context, networkErrorMessage, Toast.LENGTH_SHORT).show();

                return false;
            }
        } else
            return true;

    }
}
