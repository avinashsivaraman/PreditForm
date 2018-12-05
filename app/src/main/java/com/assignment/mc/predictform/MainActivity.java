package com.assignment.mc.predictform;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Gota.OnRequestPermissionsBack {


    public static final String TAG = "Decision-tree";
    public static final String processId = Integer.toString(android.os.Process.myPid());

    public static int PICK_TESTFILE = 3000;
    public static int PICK_MODELFILE = 3001;

    ProgressDialog progressDialog;
    Button predictButton;
    Button testFilePicker;
    TextView Result;

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
        Result = (TextView) findViewById(R.id.text_result);
        final boolean readStorage = canReadExternalStorage();
        final boolean writeStorage = canWriteExternalStorage();
        // request app permissions
        if (!readStorage || !writeStorage) {
            new Gota.Builder(this)
                    .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_LOGS)
                    .requestId(1)
                    .setListener(this)
                    .check();
        }
        systemPath = Environment.getExternalStorageDirectory() + "/";
        appFolderPath = systemPath + "assignment2/";

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
                Result.setText(readCSV());
            }
        });
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
        if (gotaResponse.isDenied(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "READ_EXTERNAL_STORAGE permission is required for the app to function properly.", Toast.LENGTH_LONG).show();
        }
        if (gotaResponse.isDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission is required for the app to function properly.", Toast.LENGTH_LONG).show();
        }
        if (gotaResponse.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) || gotaResponse.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Restart the app to able to use the newly granted permissions.", Toast.LENGTH_LONG).show();
        }
    }

    public boolean canReadExternalStorage() {
        int permissionStatus = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    public boolean canWriteExternalStorage() {
        int permissionStatus = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    public String getClassifierClass(Map<String, Float> x) {
        int var = -1;
        if (x.get("x22") < 163.439) {
            if (x.get("x16") < 186.201) {
                if (x.get("x20") < 192.356) {
                    var = 1;
                } else {
                    var = 0;
                }
            } else {
                if (x.get("x16") < 207.941) {
                    if (x.get("x19") < 173.03) {
                        var = 1;
                    } else {
                        var = 0;
                    }
                } else {
                    var = 0;
                }
            }
        } else {
            if (x.get("x20") < 282.414) {
                if (x.get("x19") < 123.807) {
                    var = 0;
                } else {
                    if (x.get("x15") < 173.808) {
                        if (x.get("x19") < 154.963) {
                            var = 1;
                        } else {
                            var = 0;
                        }
                    } else {
                        if (x.get("x19") < 137.23) {
                            var = 1;
                        } else {
                            if (x.get("x16") < 300.542) {
                                var = 1;
                            } else {
                                if (x.get("x14") < 186.843) {
                                    var = 0;
                                } else {
                                    var = 1;
                                }
                            }
                        }
                    }
                }
            } else {
                if (x.get("x13") < 66.906) {
                    var = 1;
                } else {
                    if (x.get("x3") < 422.245) {
                        if (x.get("x17") < 10.5122) {
                            var = 1;
                        } else {
                            var = 0;
                        }
                    } else {
                        var = 1;
                    }
                }
            }
        }

        String class_ = "";

        if (var == 0) {
            class_ = "Father";
        } else {
            class_ = "About";
        }
        return class_;
    }

    private String readCSV() {
        Map<String, Float> decisionValue = new HashMap<>();
        String result = "";
        try (CSVReader csvReader = new CSVReader(new FileReader(uri.getPath()))) {
            String[] nextLine;
            csvReader.readNext();
            int count = 1;

            while ((nextLine = csvReader.readNext()) != null) {
                decisionValue.put("x1", Float.valueOf(nextLine[3]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x2", Float.valueOf(nextLine[4]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x3", Float.valueOf(nextLine[6]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x4", Float.valueOf(nextLine[7]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x5", Float.valueOf(nextLine[9]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x6", Float.valueOf(nextLine[10]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x7", Float.valueOf(nextLine[12]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x8", Float.valueOf(nextLine[13]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x9", Float.valueOf(nextLine[15]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x10", Float.valueOf(nextLine[16]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x11", Float.valueOf(nextLine[18]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x12", Float.valueOf(nextLine[19]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x13", Float.valueOf(nextLine[21]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x14", Float.valueOf(nextLine[22]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x15", Float.valueOf(nextLine[24]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x16", Float.valueOf(nextLine[25]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x17", Float.valueOf(nextLine[27]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x18", Float.valueOf(nextLine[28]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x19", Float.valueOf(nextLine[30]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x20", Float.valueOf(nextLine[31]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x21", Float.valueOf(nextLine[33]) + decisionValue.getOrDefault("x1", 0.0f));
                decisionValue.put("x22", Float.valueOf(nextLine[34]) + decisionValue.getOrDefault("x1", 0.0f));
                count += 1;
            }
            for (String key : decisionValue.keySet()) {
                decisionValue.put(key, decisionValue.get(key) / count);
            }
            result = getClassifierClass(decisionValue);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Log.d(TAG, "ReadCSV: " + result);
        return result;
    }
}


