#include <jni.h>
#include <string>
#include <iostream>
#include <opencv2/opencv.hpp>
#include <opencv2/aruco.hpp>

#include <unistd.h>
#include <pthread.h>
#include <android/native_window.h> // requires ndk r5 or newer



#include <thread>

#include <android/log.h>
#include <opencv2/highgui.hpp>




using namespace std;
using namespace cv;
using namespace aruco;





extern "C" {



bool estimatePose = true;
bool showRejected = false;
bool detectorParametrs = true;


/*Camera Calib*/
static bool saveCameraParams(const string &filename, Size imageSize, float aspectRatio, int flags,
                             const Mat &cameraMatrix, const Mat &distCoeffs, double totalAvgErr) {
    FileStorage fs(filename, FileStorage::WRITE);
    if (!fs.isOpened()) {
        __android_log_print(ANDROID_LOG_ERROR, "SaveCameraParams", "Not Opened");
        return false;
    }


    time_t tt;
    time(&tt);
    struct tm *t2 = localtime(&tt);
    char buf[1024];
    strftime(buf, sizeof(buf) - 1, "%c", t2);

    fs << "calibration_time" << buf;

    fs << "image_width" << imageSize.width;
    fs << "image_height" << imageSize.height;

    if (flags & CALIB_FIX_ASPECT_RATIO) fs << "aspectRatio" << aspectRatio;

    if (flags != 0) {
        sprintf(buf, "flags: %s%s%s%s",
                flags & CALIB_USE_INTRINSIC_GUESS ? "+use_intrinsic_guess" : "",
                flags & CALIB_FIX_ASPECT_RATIO ? "+fix_aspectRatio" : "",
                flags & CALIB_FIX_PRINCIPAL_POINT ? "+fix_principal_point" : "",
                flags & CALIB_ZERO_TANGENT_DIST ? "+zero_tangent_dist" : "");
    }

    fs << "flags" << flags;

    fs << "camera_matrix" << cameraMatrix;
    fs << "distortion_coefficients" << distCoeffs;

    fs << "avg_reprojection_error" << totalAvgErr;

    __android_log_print(ANDROID_LOG_INFO, "avg_reprojection_error", "%f", totalAvgErr);
    return true;
}

/*Read camera calib*/
static bool readCameraParameters(string filename, Mat &camMatrix, Mat &distCoeffs) {
    FileStorage fs(filename, FileStorage::READ);
    if (!fs.isOpened()) {
        __android_log_print(ANDROID_LOG_ERROR, "File", "FIle Not Opened");
        return false;
    }

    fs["camera_matrix"] >> camMatrix;
    fs["distortion_coefficients"] >> distCoeffs;
    return true;
}


void drawCube(InputOutputArray _image, InputArray _cameraMatrix, InputArray _distCoeffs,
              InputArray _rvec, InputArray _tvec, float length) {

    CV_Assert(_image.getMat().total() != 0 &&
              (_image.getMat().channels() == 1 || _image.getMat().channels() == 3));
    CV_Assert(length > 0);

    vector<Point3f> objectPoints;
    double halfSize = 0.04 / 2;
    vector<Point3f> points;
    points.push_back(Point3f(-halfSize, -halfSize, 0));
    points.push_back(Point3f(-halfSize, halfSize, 0));
    points.push_back(Point3f(halfSize, halfSize, 0));
    points.push_back(Point3f(halfSize, -halfSize, 0));
    points.push_back(Point3f(-halfSize, -halfSize, 0.04));
    points.push_back(Point3f(-halfSize, halfSize, 0.04));
    points.push_back(Point3f(halfSize, halfSize, 0.04));
    points.push_back(Point3f(halfSize, -halfSize, 0.04));

    vector<Point2f> imagePoints;
    projectPoints(points, _rvec, _tvec, _cameraMatrix, _distCoeffs, imagePoints);

    // draw axis lines
    for (int i = 0; i < 4; i++) {
        line(_image, imagePoints[i], imagePoints[(i + 1) % 4], Scalar(0, 0, 255), 2);
        line(_image, imagePoints[i + 4], imagePoints[4 + (i + 1) % 4], Scalar(0, 255, 0), 2);
        line(_image, imagePoints[i], imagePoints[i + 4], Scalar(255, 0, 0), 2);

    }

}



void draw3D(InputOutputArray _image, InputArray _cameraMatrix, InputArray _distCoeffs,
            InputArray _rvec, InputArray _tvec, float length) {


}


static bool readDetectorParameters(string filename, Ptr<aruco::DetectorParameters> &params) {
    FileStorage fs(filename, FileStorage::READ);
    if (!fs.isOpened())
        return false;
    fs["adaptiveThreshWinSizeMin"] >> params->adaptiveThreshWinSizeMin;
    fs["adaptiveThreshWinSizeMax"] >> params->adaptiveThreshWinSizeMax;
    fs["adaptiveThreshWinSizeStep"] >> params->adaptiveThreshWinSizeStep;
    fs["adaptiveThreshConstant"] >> params->adaptiveThreshConstant;
    fs["minMarkerPerimeterRate"] >> params->minMarkerPerimeterRate;
    fs["maxMarkerPerimeterRate"] >> params->maxMarkerPerimeterRate;
    fs["polygonalApproxAccuracyRate"] >> params->polygonalApproxAccuracyRate;
    fs["minCornerDistanceRate"] >> params->minCornerDistanceRate;
    fs["minDistanceToBorder"] >> params->minDistanceToBorder;
    fs["minMarkerDistanceRate"] >> params->minMarkerDistanceRate;
    fs["cornerRefinementMethod"] >> params->cornerRefinementMethod;
    fs["cornerRefinementWinSize"] >> params->cornerRefinementWinSize;
    fs["cornerRefinementMaxIterations"] >> params->cornerRefinementMaxIterations;
    fs["cornerRefinementMinAccuracy"] >> params->cornerRefinementMinAccuracy;
    fs["markerBorderBits"] >> params->markerBorderBits;
    fs["perspectiveRemovePixelPerCell"] >> params->perspectiveRemovePixelPerCell;
    fs["perspectiveRemoveIgnoredMarginPerCell"] >> params->perspectiveRemoveIgnoredMarginPerCell;
    fs["maxErroneousBitsInBorderRate"] >> params->maxErroneousBitsInBorderRate;
    fs["minOtsuStdDev"] >> params->minOtsuStdDev;
    fs["errorCorrectionRate"] >> params->errorCorrectionRate;
    return true;
}


JNIEXPORT void JNICALL
Java_com_keiko_nativecamera_CameraCalib_calibCamera(JNIEnv *env, jobject instance,
                                                    jstring outputFile_, jlong inputRgbaAddr) {
    const char *outPutFile = env->GetStringUTFChars(outputFile_, NULL);

    Mat &inputImage = *(Mat *) inputRgbaAddr;
    Mat imageCopy;


    int markersX = 5;
    int markersY = 7;
    float markerLength = 0.04;
    float markerSeparation = 0.01;

    //int dictionaryId=11; //DICT_6X6_1000

    int calibrationFlags = 0;
    float aspectRatio = 1;


    Ptr<Dictionary> dictionary = getPredefinedDictionary(PREDEFINED_DICTIONARY_NAME::DICT_6X6_250);

    Ptr<aruco::DetectorParameters> detectorParams = aruco::DetectorParameters::create();

    Ptr<aruco::GridBoard> gridboard = GridBoard::create(markersX, markersY, markerLength,
                                                        markerSeparation, dictionary);
    Ptr<aruco::Board> board = gridboard.staticCast<aruco::Board>();

    // collected frames for calibration
    vector<vector<vector<Point2f> > > allCorners;
    vector<vector<int> > allIds;
    Size imgSize;

    vector<int> ids;
    vector<vector<Point2f> > corners, rejected;

    // detect markers
    aruco::detectMarkers(inputImage, dictionary, corners, ids, detectorParams, rejected);

    // draw results
    inputImage.copyTo(imageCopy);
    if (ids.size() > 0) aruco::drawDetectedMarkers(imageCopy, corners, ids);

    // putText(imageCopy, "Nevim co tady dat",Point(10, 20), FONT_HERSHEY_SIMPLEX, 0.5, Scalar(255, 0, 0), 2);
    if (ids.size() > 0) {
        //cout << "Frame captured" << endl;
        __android_log_print(ANDROID_LOG_INFO, "FrameCap", "Frame captured");
        __android_log_print(ANDROID_LOG_INFO, "ids SIze)", "IDS SIze is %lu", ids.size());

        allCorners.push_back(corners);
        allIds.push_back(ids);
        imgSize = inputImage.size();


        putText(imageCopy, "Frame captured", Point(10, 20), FONT_HERSHEY_SIMPLEX, 0.5,
                Scalar(255, 0, 128), 2);
    }
    if (allIds.size() < 1) {
        cerr << "Not enough captures for calibration" << endl;
        __android_log_print(ANDROID_LOG_ERROR, "allIds.size() < 1",
                            "Not enough captures for calibration");
        return;
    }

    Mat cameraMatrix, distCoeffs;
    vector<Mat> rvecs, tvecs;
    double repError;

    if (calibrationFlags & CALIB_FIX_ASPECT_RATIO) {
        cameraMatrix = Mat::eye(3, 3, CV_64F);
        cameraMatrix.at<double>(0, 0) = aspectRatio;
    }
    // prepare data for calibration
    vector<vector<Point2f> > allCornersConcatenated;
    vector<int> allIdsConcatenated;
    vector<int> markerCounterPerFrame;
    markerCounterPerFrame.reserve(allCorners.size());
    for (unsigned int i = 0; i < allCorners.size(); i++) {
        markerCounterPerFrame.push_back((int) allCorners[i].size());
        for (unsigned int j = 0; j < allCorners[i].size(); j++) {
            allCornersConcatenated.push_back(allCorners[i][j]);
            allIdsConcatenated.push_back(allIds[i][j]);
        }
    }
    // calibrate camera
    repError = aruco::calibrateCameraAruco(allCornersConcatenated, allIdsConcatenated,
                                           markerCounterPerFrame, board, imgSize, cameraMatrix,
                                           distCoeffs, rvecs, tvecs, calibrationFlags);

    bool saveOk = saveCameraParams(outPutFile, imgSize, aspectRatio, calibrationFlags, cameraMatrix,
                                   distCoeffs, repError);
    if (!saveOk) {
        cerr << "Cannot save output file" << endl;
        __android_log_print(ANDROID_LOG_ERROR, "Cannot save output file",
                            "Cannot save output file");

        return;
    }
    imageCopy.copyTo(inputImage);
    //cout << "Rep Error: " << repError << endl;
    __android_log_print(ANDROID_LOG_INFO, "Rep Error:", "%f", repError);
    //cout << "Calibration saved to " << outPutFile << endl;
    __android_log_print(ANDROID_LOG_INFO, "Calibration saved to", "%s", outPutFile);


    env->ReleaseStringUTFChars(outputFile_, outPutFile);
}

JNIEXPORT void JNICALL
Java_com_keiko_nativecamera_DetectMarker_detectmarker(JNIEnv *env, jobject instance,
                                                      jstring DetecorParams_, jstring CameraParams_,
                                                      jlong inPutAddr) {

    const char *CameraParamsFile = env->GetStringUTFChars(CameraParams_, NULL);
    const char *detectParamsFile = env->GetStringUTFChars(DetecorParams_, NULL);

    Mat &inputImage = *(Mat *) inPutAddr;

    Mat imageCopy, Blure;
    Mat camMatrix, distCoeffs;

    vector<int> ids;
    vector<vector<Point2f> > corners, rejected;
    vector<Vec3d> rvecs, tvecs;


    float markerLength = 0.04;

    Ptr<aruco::DetectorParameters> detectorParams = aruco::DetectorParameters::create();
    if (detectorParametrs) {
        bool readOk = readDetectorParameters(detectParamsFile, detectorParams);
        if (!readOk) {
            cerr << "Invalid detector parameters file" << endl;
            __android_log_print(ANDROID_LOG_ERROR, "File: ", "Invalid detector parameters file");
            return;
        }
    }
    detectorParams->cornerRefinementMethod = aruco::CORNER_REFINE_CONTOUR; // do corner refinement in markers

    Ptr<aruco::Dictionary> dictionary = aruco::getPredefinedDictionary(
            aruco::PREDEFINED_DICTIONARY_NAME::DICT_6X6_250);


    if (estimatePose) {
        bool readOk = readCameraParameters(CameraParamsFile, camMatrix, distCoeffs);
        if (!readOk) {
            cerr << "Invalid camera file" << endl;
            __android_log_print(ANDROID_LOG_ERROR, "File: ", "Invalid camera file");
            return;
        }
    }


    // detect markers and estimate pose
    aruco::detectMarkers(inputImage, dictionary, corners, ids, detectorParams, rejected);

    if (estimatePose && ids.size() > 0) {
        estimatePoseSingleMarkers(corners, markerLength, camMatrix, distCoeffs, rvecs, tvecs);
        __android_log_print(ANDROID_LOG_INFO, "Marker Size id ", "Marker Size id = %lu",
                            ids.size());
    }

    // draw results
    inputImage.copyTo(imageCopy);


    if (ids.size() > 0) {
        aruco::drawDetectedMarkers(imageCopy, corners, ids, Scalar(0, 255, 0));
        if (estimatePose) {
            for (unsigned int i = 0; i < ids.size(); i++) {
                //aruco::drawAxis(imageCopy, camMatrix, distCoeffs, rvecs[i], tvecs[i], markerLength * 0.5f);
                /*Draw a cube*/
                drawCube(imageCopy, camMatrix, distCoeffs, rvecs[i], tvecs[i], markerLength * 0.5f);

            }
        }
        if (showRejected && rejected.size() > 0) {
            aruco::drawDetectedMarkers(imageCopy, rejected, noArray(), Scalar(100, 0, 255));
        }
        //medianBlur(imageCopy,imageCopy,3);

        // write the flipped frame
        imageCopy.copyTo(inputImage);


        env->ReleaseStringUTFChars(CameraParams_, CameraParamsFile);
        env->ReleaseStringUTFChars(DetecorParams_, detectParamsFile);
    }


}
}

