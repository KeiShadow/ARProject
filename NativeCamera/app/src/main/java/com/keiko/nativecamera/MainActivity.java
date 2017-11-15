package com.keiko.nativecamera;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity{
    Button detekce,calibrate,createMarker,techAct;

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

    }



}
