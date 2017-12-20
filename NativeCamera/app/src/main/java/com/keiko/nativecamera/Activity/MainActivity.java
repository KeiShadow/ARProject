package com.keiko.nativecamera.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.keiko.nativecamera.CreateMarker;
import com.keiko.nativecamera.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity{
    Button detekce,calibrate,createMarker,techAct;
    Button about;
    String saveMarker = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Calib/";


    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        detekce=(Button)findViewById(R.id.bt_Det);
        calibrate=(Button)findViewById(R.id.bt_Calib);
        createMarker=(Button)findViewById(R.id.bt_Create);
        about= (Button)findViewById(R.id.bt_about);

        detekce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent detekce = new Intent(MainActivity.this,DetectionActivity.class);
                startActivity(detekce);
            }
        });

        calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent calib = new Intent(MainActivity.this,CalibrationActivity.class);
                startActivity(calib);
            }
        });
        createMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateMarker cm = new CreateMarker();
               cm.createMarker(saveMarker);
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,AboutActivity.class);
                startActivity(intent);
            }
        });


        MakeFiles();
        copyAssets();

    }

    public void MakeFiles(){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()+"/", "Calib");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("App", "failed to create directory");
            }
        }else{
            Toast.makeText(getApplicationContext(),"Soubor Vytvořen, nebo již existuje", Toast.LENGTH_LONG).show();
        }

    }
    public  void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                String outDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Calib" ;
                File outFile = new File(outDir, filename);
                if(!outFile.exists()){
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                }else{
                    // Toast.makeText(getApplicationContext(),"Soubor Vytvořen, nebo již existuje", Toast.LENGTH_LONG).show();
                }

            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }



}
