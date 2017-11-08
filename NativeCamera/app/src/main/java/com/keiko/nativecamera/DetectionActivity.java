package com.keiko.nativecamera;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class DetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "DetectionActivity";
    /*Creating Mat fields*/
    Mat mRgba, mFlip, bgr;

    /*Path to files*/
    String CameraParams = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Calib/CameraParams.yml";
    String DetectorParams = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Calib/DetectorParams.yml";
    String saveVideo =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/Video/test.avi";

    /*Creating camerabridgeview */
    private CameraBridgeViewBase mOpenCvCameraView;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);

        mOpenCvCameraView.setMaxFrameSize(1280,720);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
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


        Core.flip(mRgba,mFlip,1);
        Core.flip(mFlip,mRgba,0);

            DetectMarker detect = new DetectMarker(DetectorParams,saveVideo,CameraParams,bgr.getNativeObjAddr());

            Imgproc.cvtColor(mRgba,bgr,Imgproc.COLOR_RGBA2BGR);
            detect.run();
            ///    detect.detectmarker(DetectorParams,saveVideo,CameraParams,bgr.getNativeObjAddr());
            Imgproc.cvtColor(bgr,mRgba,Imgproc.COLOR_BGR2RGBA);



                /*//if(recording){
                   new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
                            VideoWriter videoWriter = new VideoWriter(saveVideo,fourcc,30.0,mRgba.size());
                            videoWriter.open(saveVideo,fourcc,30.0,mRgba.size());
                            while(videoWriter.isOpened()){

                                videoWriter.write(mRgba);
                            }
                        }
                    }).start();
                //}
                //videoWriter.release();*/

        return mRgba;
    }
}
