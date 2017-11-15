package com.keiko.nativecamera;

/**
 * Created by Petr on 31.10.2017.
 */

public class DetectMarker implements Runnable {
    private final String DetectorParams;
    private final String CameraParams;
    private final long inPutAddr;


    public native void detectmarker(String DetectorParams, String CameraParams, long inPutAddr);


    DetectMarker(String DetectorParams, String CameraParams, long inPutAddr){
        this.DetectorParams=DetectorParams;
        this.CameraParams = CameraParams;
        this.inPutAddr = inPutAddr;
    }

    @Override
    public void run() {
        detectmarker( DetectorParams, CameraParams, inPutAddr);

    }
}
