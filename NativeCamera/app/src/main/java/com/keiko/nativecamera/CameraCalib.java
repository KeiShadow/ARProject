package com.keiko.nativecamera;

import android.graphics.Camera;

import org.opencv.core.Mat;
import org.opencv.core.Size;

/**
 * Created by Petr on 25.10.2017.
 */

public class CameraCalib implements  Runnable {

    private final String outputFile;
    private final long inputRgbaAddr;

    public native void calibCamera(String outputFile, long inputRgbaAddr);

    public CameraCalib(String outputFile, long inputRgbaAddr){
        this.outputFile = outputFile;
        this.inputRgbaAddr=inputRgbaAddr;
    }
    @Override
    public void run() {
        calibCamera(outputFile,inputRgbaAddr);
    }
}
