package com.keiko.nativecamera;

/**
 * Created by Petr on 31.10.2017.
 */

public class DetectMarker implements Runnable {
    private final String DetectorParams;
    private final String saveVideo;
    private final String CameraParams;
    private final long inPutAddr;

    public native void detectmarker(String DetectorParams, String saveVideo, String CameraParams, long inPutAddr);

    DetectMarker(String DetectorParams,String saveVideo,String CameraParams,long inPutAddr){
        this.DetectorParams=DetectorParams;
        this.saveVideo = saveVideo;
        this.CameraParams = CameraParams;
        this.inPutAddr = inPutAddr;
    }
    @Override
    public void run() {
        detectmarker( DetectorParams, saveVideo, CameraParams, inPutAddr);
    }
}
