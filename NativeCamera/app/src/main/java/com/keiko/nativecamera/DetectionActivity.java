package com.keiko.nativecamera;

import android.content.res.AssetManager;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import static org.opencv.core.Core.BORDER_CONSTANT;
import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.Core.BORDER_ISOLATED;
import static org.opencv.core.Core.BORDER_REFLECT;
import static org.opencv.photo.Photo.fastNlMeansDenoisingColored;
import static org.opencv.photo.Photo.fastNlMeansDenoisingColoredMulti;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FPS;

public class DetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "DetectionActivity";
    /*Creating Mat fields*/
    Mat mRgba, mFlip, bgr;
    Chronometer focus;
    /*Path to files*/
    String CameraParams = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Calib/CameraParams.yml";
    String DetectorParams = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Calib/DetectorParams.yml";
    String saveVideo =  Environment.getExternalStorageDirectory().getAbsolutePath();



    Button videoRecord;
    /*Creating camerabridgeview */
    private CameraBridgeViewBase mOpenCvCameraView;
    VideoWriter mVideoWriter;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }
    boolean recording = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detection);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);

        videoRecord = (Button)findViewById(R.id.bt_record);
        focus = (Chronometer)findViewById(R.id.simpleChronometer);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);

        mOpenCvCameraView.setCvCameraViewListener(this);


        videoRecord.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0){
                if (recording) {
                    recording = false;
                    focus.stop();
                    mVideoWriter.release();
                    Toast.makeText(DetectionActivity.this,"Stop recording",Toast.LENGTH_SHORT).show();
                } else {
                    recording = true;
                    focus.setBase(SystemClock.elapsedRealtime()); // remet le compteur à 0
                    focus.start();
                    recordfilepath();
                    java.util.Date date= new java.util.Date();
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date.getTime());
                    File file = new File(recordfilepath(), "VID_" + timeStamp + ".avi");
                    Log.d(TAG, "file : " + file);

                    try {
                        if(!file.exists()){
                            file.createNewFile();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    saveVideo = file.getAbsolutePath();
                    Toast.makeText(DetectionActivity.this,"Recording",Toast.LENGTH_SHORT).show();
                }
            }
        });

        mOpenCvCameraView.setMaxFrameSize(720,480);//1280x720 720x480

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);





    }
    private String recordfilepath() {

        File sddir = Environment.getExternalStorageDirectory();
        File vrdir = new File(sddir, "ArCameraVideo");
        if(!vrdir.exists())
        {
            vrdir.mkdir();
        }
        String filepath = vrdir.getAbsolutePath();
        return filepath;
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width, CvType.CV_8UC4);
        mFlip = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        bgr = new Mat();

            /*Funkce která otčí obraz kamery*/
            Flip(mRgba,bgr);

            /*Vytvoření konstruktoru ze třídy DetectMarker*/
            DetectMarker detect = new DetectMarker(DetectorParams,CameraParams,bgr.getNativeObjAddr());
            Imgproc.cvtColor(mRgba,bgr,Imgproc.COLOR_RGBA2BGR);
            detect.run();
            /*Funkce pro natáčení videa*/
            recording(recording,bgr);
            Imgproc.cvtColor(bgr,mRgba,Imgproc.COLOR_BGR2RGBA);
        return mRgba;
    }
    public void Flip(Mat mrgba, Mat bgr){
        Core.flip(mRgba,mFlip,1);
        Core.flip(mFlip,mRgba,0);
    }

    public void recording(boolean recording, Mat bgr){
        if(recording){
            // Log.d(TAG, "filepath : " + filepath);
            if (mVideoWriter == null) {
                mVideoWriter = new VideoWriter(saveVideo, VideoWriter.fourcc('M', 'J', 'P', 'G'), CV_CAP_PROP_FPS, bgr.size());
                Log.d(TAG,"mVideoWriter good : " + mVideoWriter);
                mVideoWriter.open(saveVideo, VideoWriter.fourcc('M', 'J', 'P', 'G'), CV_CAP_PROP_FPS, bgr.size());
                Log.i(TAG, "onCameraFrame: recordFilePath" + saveVideo);

            }
            if (!mVideoWriter.isOpened()) {
                Log.w(TAG, "onCameraFrame: open");
                mVideoWriter.open(saveVideo, VideoWriter.fourcc('M', 'J', 'P', 'G'), CV_CAP_PROP_FPS, bgr.size());
            }
            mVideoWriter.write(bgr);
        } else {
            if (mVideoWriter != null) {
                mVideoWriter.release();
                Log.d(TAG,"mVideoWriter release");
            }
        }
    }


}
