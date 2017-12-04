package com.example.arent.opencv_new;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import android.app.Activity;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Button;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    //@@
    private ArrayList<MatOfPoint>    mTrace = new ArrayList<MatOfPoint>();

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button mButton;
    public MediaRecorder mMediaRecorder;



    private boolean isRecording = false;
    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath();
    String folder_name = "Face Detection Signal";
    String folder_pathforfile = null;
    String showTimefile = null;
    String showTime = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH_mm_ss");
    SimpleDateFormat sdf_fileintxt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    // @@
    private VideoWriter mVideoWriter;




    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);



        folder_pathforfile = folder_path + File.separator + folder_name
                + File.separator + "hi" + "_.mp4";
        Log.e("@@@2 filename ", folder_pathforfile);
        CreateSDfolder();
        ongetTime();

        //@@

        Log.e("@@@@@@", "~~~~~");

        Log.e("@@@@@@", "*****");

        mButton = (Button) findViewById(R.id.record_button);
        mButton.setVisibility(SurfaceView.VISIBLE);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isRecording ) {
                    isRecording = false;
                }

                else {

                    isRecording = true;
                }

            }
        });

    }
    private String path() {
        //        ongetTime();
        File sddir = Environment.getExternalStorageDirectory();
        File vrdir = new File(sddir, folder_name);
        String mTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(vrdir, "KLI_" + mTimeStamp + ".avi");
        String filepath = file.getAbsolutePath();
        Log.e("debug mediarecorder", filepath);
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
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

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        // mVideoWriter = new VideoWriter(path(), VideoWriter.fourcc('M','J','P','G'), 25.0D,
        //        new Size(width, height)); //(folder_pathforfile, Videoio.CAP_FFMPEG, )


    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        // @@ TODO : set mBlobColorHsv to target color

        mDetector.setHsvColor(mBlobColorHsv);

        //@@ TODO : Can we not just ge Spectrum but make sure that is circle shape? or using signal from blutooth

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            //Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR, 3);

            mTrace.add(contours.get(0));

            for (int i = 0; i < mTrace.size(); i++)
                Imgproc.fillConvexPoly(mRgba, mTrace.get(i), CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }


        //size = new Size(inputFrame.getWidth(), inputFrame.getHeight());


        if ( isRecording) {
            int fourcc = VideoWriter.fourcc('M','J','P','G');
            if (mVideoWriter == null){
                mVideoWriter = new VideoWriter(recordfilepath(), fourcc, 25.0D,mRgba.size() );
                mVideoWriter.open(recordfilepath(),fourcc, 25.0D,mRgba.size()  );
                Log.i(TAG, "onCameraFrame: path "+ path());


            }
            if (! mVideoWriter.isOpened()){
                mVideoWriter.open(recordfilepath(),fourcc, 25.0D,mRgba.size()  );
            }

            Mat newRgba = mRgba.clone();
            mVideoWriter.write(newRgba);

        }else {
            if (mVideoWriter != null ){
                mVideoWriter.release();
                //mVideoWriter = null;
            }
        }

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    /* methods for recorder */
    private  void releaseMediaRecorder(){
        if (mMediaRecorder != null){
            //clear recorder configuration
            mMediaRecorder.reset();
            //release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            JavaCameraView.mCamera.lock();
            mOpenCvCameraView.releaseRecord();


            // @@ Dont know why camera be locked. ..

        }
    }
    private boolean prepareVideoRecorder() {
        // Deleted Camera part...
        Log.e("debug mediarecorder", "in prepareMediaRecorder");


        mMediaRecorder = new MediaRecorder();

        try{
            JavaCameraView.mCamera.lock();
            JavaCameraView.mCamera.unlock();
        }catch (RuntimeException e){
            Log.e("debug mediarecorder","JavaCameraView.mCamera.unlock() fail");

        }
        mMediaRecorder.setPreviewDisplay(mMediaRecorder.getSurface());

        mMediaRecorder.reset();

        // Step 1: Unlock and set camera to MediaRecorder
        //mCamera.unlock();
        //mMediaRecorder.setCamera() 얘도 depreciated 인듯
        // Step 2: Set sources

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);//@@TODO : previously SURFACE

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        //profile.videoFrameWidth = mOpenCvCameraView.mFrameWidth;
        //profile.videoFrameHeight = mOpenCvCameraView.mFrameHeight;

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);
        // Step 4: Set output file
        //File path = Environment.getExternalStoragePublicDirectory(
        //        Environment.DIRECTORY_MOVIES);
        //File newfile = new File(path+"/out.mp4");
        //Log.e(TAG,"file path : "+newfile);
        mMediaRecorder.setOutputFile(recordfilepath());///getFilesDir().getAbsolutePath()+"out.mp4");//newfile);
        mMediaRecorder.setVideoSize(mOpenCvCameraView.mFrameWidth, mOpenCvCameraView.mFrameHeight);

        mMediaRecorder.setAudioSamplingRate(16000);
        // Step 5: Prepare configured MediaRecorder
        try {
            Log.e(TAG,"********Start prepare");
            mMediaRecorder.prepare();

        } catch (IllegalStateException e) {
            Log.e(TAG,"********FAIL prepare1");

            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.e(TAG,"********FAIL prepare2");

            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private String recordfilepath() {
        // TODO Auto-generated method stub
        ongetTime();
        File sddir =  Environment.getExternalStorageDirectory();
        File vrdir = new File(sddir, folder_name);
        File file = new File(vrdir, showTimefile+"_.avi");
        String filepath = file.getAbsolutePath();
        Log.e("debug mediarecorder", filepath);
        return filepath;
    }
    private void CreateSDfolder() {
        String filefolderpath = folder_path + File.separator +  folder_name;
        File dir = new File(filefolderpath);
        if (!dir.exists()){
            Log.e("folder", "not exist");
            try{
                //dir.createNewFile(true);
                dir.mkdir();
                Log.e("folder", "creat exist");
            }catch(Exception e){
                Log.e("folder", "creat not exist");
                e.printStackTrace();
            }
        }
        else{
            Log.e("folder", "exist");
        }
    }

    private void ongetTime() {
        Date dt=new Date();
        showTime=sdf_fileintxt.format(dt);
        showTimefile =sdf.format(dt);
    }

}
