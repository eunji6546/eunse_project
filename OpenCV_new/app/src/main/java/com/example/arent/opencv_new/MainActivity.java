package com.example.arent.opencv_new;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.opencv.android.JavaCameraView;

import java.io.File;
import java.io.IOException;
import java.nio.file.ProviderNotFoundException;
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
import org.opencv.core.Point;
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

public class MainActivity extends AppCompatActivity implements OnTouchListener, CvCameraViewListener2{


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

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
    private ArrayList<Point> mCenters = new ArrayList<>();

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button mButton;
    private Button mZoomButton;
    private Button mColorChanger;
    private float zoomrate = 1;
    boolean zoomin = true;
    public MediaRecorder mMediaRecorder;

    private boolean isRecording = false;
    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath();
    String folder_name = "Face Detection Signal";
    String folder_pathforfile = null;
    String showTimefile = null;
    String showTime = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH_mm_ss");
    SimpleDateFormat sdf_fileintxt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


    private int color_mode=0;
    private int radius;

    // @@
    private VideoWriter mVideoWriter;

    public void change_fill_color(){
        double r , g, b, a, new_r;
        r = CONTOUR_COLOR.val[0];
        g= CONTOUR_COLOR.val[1];
        b =CONTOUR_COLOR.val[2];
        a = CONTOUR_COLOR.val[3];
        Log.e(TAG, "@@@@@@@@@@@@" + r);

        color_mode = color_mode %3;
        if(color_mode == 0) r = (r + 50) %255 ;
        if(color_mode == 1) g = (g + 50) %255 ;
        if(color_mode == 2) b = (b + 50) %255 ;
        color_mode += 1;
        CONTOUR_COLOR = new Scalar(r, g, b, a);

    }
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

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
        mColorChanger = (Button) findViewById(R.id.button4);
        mColorChanger.setVisibility(SurfaceView.VISIBLE);
        mColorChanger.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View view) {
                                                 //CONTOUR_COLOR = new Scalar(255,255,0,255);
                                                 change_fill_color();
                                             }
                                         }

        );

        mZoomButton = (Button) findViewById(R.id.zoom_button);
        mZoomButton.setVisibility(SurfaceView.VISIBLE);
        mZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.e("zoom clicked", "zoomrate"+zoomrate +"zoom"+zoomin);
                if (zoomin) {
                    if ((1.0 >= zoomrate) && (zoomrate > 0.1)) {
                        zoomrate -= 0.1;
                    }else {
                        zoomin = false;
                    }

                }else {
                    if (!(zoomrate >= 1)) zoomrate += 0.1;
                    else zoomin = true;
                }
            }
        });

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

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();


        Size orig = mRgba.size();
        Log.e("zoomOnFrame", "FROM "+orig);
        int offx = (int)( 0.5 * (1.0-zoomrate) * orig.width);
        int offy = (int) (0.5 * (1.0-zoomrate) * orig.height);

        // crop the part, you want to zoom into:
        Mat cropped = mRgba.submat(offy,(int) orig.height-offy, offx, (int)orig.width-offx);
        // resize to original:
        Imgproc.resize(cropped, cropped, orig);
        Log.e("zoomOnFrame","To "+cropped.size() );

        mRgba = cropped;

        if (mIsColorSelected) {

            //for (int i = 0; i < mTrace.size(); i++)
            //   Imgproc.fillConvexPoly(mRgba, mTrace.get(i), CONTOUR_COLOR);
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            //Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR, 3);

            List<Point> this_mat =contours.get(0).toList();
            double sum_x=0;
            double sum_y = 0;
            for(int i=0; i<this_mat.size(); i++){
                sum_x = sum_x + this_mat.get(i).x;
                sum_y = sum_y + this_mat.get(i).y;
            }
            Point center = new Point(sum_x/this_mat.size(), sum_y/this_mat.size());
            mCenters.add(center);

            for (int i=0; i<mTrace.size(); i++){
                Imgproc.circle(mRgba, mCenters.get(i)  ,40,CONTOUR_COLOR, -1);
            }

            mTrace.add(contours.get(0));

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
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    private void ongetTime() {
        Date dt=new Date();
        showTime=sdf_fileintxt.format(dt);
        showTimefile =sdf.format(dt);
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
}
