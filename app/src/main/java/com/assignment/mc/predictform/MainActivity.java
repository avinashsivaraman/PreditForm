package com.assignment.mc.predictform;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.aditya.filebrowser.Constants;
import com.aditya.filebrowser.FileChooser;
import com.opencsv.CSVReader;

import net.alhazmy13.gota.Gota;
import net.alhazmy13.gota.GotaResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Gota.OnRequestPermissionsBack{


    public static final String TAG = "Decision-tree";
    public static final String processId = Integer.toString(android.os.Process.myPid());

    public static int PICK_TESTFILE = 3000;
    public static int PICK_MODELFILE = 3001;

    ProgressDialog progressDialog;
    Button predictButton;
    Button testFilePicker;

    public static String appFolderPath;
    public static String systemPath;
    private Uri uri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        progressDialog = new ProgressDialog(this);
        testFilePicker = (Button) findViewById(R.id.testfilepicker);
        predictButton = (Button) findViewById(R.id.predict_btn);
        final boolean readStorage = canReadExternalStorage();
        final boolean writeStorage = canWriteExternalStorage();
        // request app permissions
        if (!readStorage || !writeStorage){
            new Gota.Builder(this)
                    .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_LOGS)
                    .requestId(1)
                    .setListener(this)
                    .check();
        }
        systemPath = Environment.getExternalStorageDirectory() + "/";
        appFolderPath = systemPath+"assignment2/";

        testFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i2 = new Intent(MainActivity.this, FileChooser.class);
                i2.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
                startActivityForResult(i2, PICK_TESTFILE);
            }
        });

        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new  AsyncPredictTask().execute();
            }
        });

        // create assets folder if it doesn't exist
        createAssetsFolder();

        // copy all data files from assets to external storage
        try {
            String[] list = getAssets().list("data");
            for (String file: list) {
                copyToExternalStorage(file, "data");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TESTFILE && data != null) {
            if (resultCode == Activity.RESULT_OK) {
                uri = data.getData();
                testFilePicker.setText(uri.getPath());
            }
        }
    }


    @Override
    public void onRequestBack(int requestId, @NonNull GotaResponse gotaResponse) {
        if (gotaResponse.isDenied(Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(this, "READ_EXTERNAL_STORAGE permission is required for the app to function properly.", Toast.LENGTH_LONG).show();
        }
        if (gotaResponse.isDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission is required for the app to function properly.", Toast.LENGTH_LONG).show();
        }
        if (gotaResponse.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) || gotaResponse.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(this, "Restart the app to able to use the newly granted permissions.", Toast.LENGTH_LONG).show();
        }
    }

    public boolean canReadExternalStorage(){
        int permissionStatus = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    public boolean canWriteExternalStorage(){
        int permissionStatus = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    private void createAssetsFolder(){
        // create app assets folder if not created
        File folder = new File(appFolderPath);

        if (!folder.exists()) {
            Log.d(TAG,"Assignment folder not found");
            folder.mkdirs();
        } else {
            Log.w(TAG,"INFO: Assign folder already exists.");
        }
    }

    private void copyToExternalStorage(String assetName, String assetsDirectory){
        String from = assetName;
        String to = appFolderPath+from;

        // check if the file exists
        File file = new File(to);
        if(file.exists()){
            Log.d(TAG, "copyToExternalStorage: file already exist, no need to copy: "+from);
        } else {
            // do copy
            boolean copyResult = copyAsset(getAssets(), from, assetsDirectory, to);
            Log.d(TAG, "copyToExternalStorage: isCopied -> "+copyResult);
        }
    }

    private boolean copyAsset(AssetManager assetManager, String fromAssetPath, String assetsDirectory, String toPath) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = assetManager.open(assetsDirectory+"/"+fromAssetPath);
            new File(toPath).createNewFile();
            outputStream = new FileOutputStream(toPath);
            copyFile(inputStream, outputStream);
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "copyAsset: unable to copy file: "+fromAssetPath);
            return false;
        }
    }

    private void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = inputStream.read(buffer)) != -1){
            outputStream.write(buffer, 0, read);
        }
    }
    private class AsyncPredictTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle("Decision Tree Predict");
            progressDialog.setMessage("Executing Decision-predict, please wait...");
            progressDialog.show();
            Log.d(TAG, "==================\nStart of Decision PREDICT\n==================");
        }

        @Override
        protected Void doInBackground(String... params) {
//            LibSVM.getInstance().predict(TextUtils.join(" ", params));
                try (CSVReader csvReader = new CSVReader(new FileReader(uri.getPath()))) {
                    String[] nextLine;
                    csvReader.readNext();
                    while ((nextLine = csvReader.readNext()) != null) {
                        float x1 = Float.valueOf(nextLine[3]);
                        float x2 = Float.valueOf(nextLine[4]);
                        float x3 = Float.valueOf(nextLine[6]);
                        float x4 = Float.valueOf(nextLine[7]);
                        float x5 = Float.valueOf(nextLine[9]);
                        float x6 = Float.valueOf(nextLine[10]);
                        float x7 = Float.valueOf(nextLine[12]);
                        float x8 = Float.valueOf(nextLine[13]);
                        float x9 = Float.valueOf(nextLine[15]);
                        float x10 = Float.valueOf(nextLine[16]);
                        float x11 = Float.valueOf(nextLine[18]);
                        float x12 = Float.valueOf(nextLine[19]);
                        float x13 = Float.valueOf(nextLine[21]);
                        float x14 = Float.valueOf(nextLine[22]);
                        float x15 = Float.valueOf(nextLine[24]);
                        float x16 = Float.valueOf(nextLine[25]);
                        float x17 = Float.valueOf(nextLine[27]);
                        float x18 = Float.valueOf(nextLine[28]);
                        float x19 = Float.valueOf(nextLine[30]);
                        float x20 = Float.valueOf(nextLine[31]);
                        float x21 = Float.valueOf(nextLine[33]);
                        float x22 = Float.valueOf(nextLine[34]);
//                        Decision Tree Algorithm here
                        Log.d(TAG, "doInBackground: "+x7);
                        System.out.println(nextLine[0] + nextLine[1] + "etc...");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
//            Toast.makeText(MainActivity.this, "Decision has executed successfully!", Toast.LENGTH_LONG).show();
//            Log.d(TAG, "==================\nDescison PREDICT\n==================");
//            Utility.readLogcat(MainActivity.this, "Decision Results");
        }
    }
}


