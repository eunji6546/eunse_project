package com.example.arent.opencv_new;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
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
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    private String mDeviceName;
    private String mDeviceAddress;

    private ExpandableListView mGattServicesList;
    private ThinQBTSensorService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private  String ACCEL_MODE = "";

    // @@
    private VideoWriter mVideoWriter;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "ServiceConnection:Connected");
            //mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService =((ThinQBTSensorService.LocalBinder) service ).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            //mBluetoothLeService.initVib(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void change_fill_color(){
        double r , g, b, a, new_r;
        r = CONTOUR_COLOR.val[0];
        g= CONTOUR_COLOR.val[1];
        b =CONTOUR_COLOR.val[2];
        a = CONTOUR_COLOR.val[3];
        Log.e(TAG, "@@@@@@@@@@@@" + r);

        color_mode = color_mode %3;
        if(color_mode == 0) r = (r + 10) %255 ;
        if(color_mode == 1) g = (g + 10) %255 ;
        if(color_mode == 2) b = (b + 10) %255 ;
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


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        private boolean isSavingVibsStart = false;
        private boolean isSavingVibsEnd = false;
        private List<Float> vibArray = new ArrayList<Float>();
        private boolean mode_changed = false;
        private String mode = "VIBMODE";

        private int x_prev;
        private int y_prev;
        private int z_prev;
        private boolean first_accel = true;
        private boolean first_time = true;

        private int exception = 0;

        private float abs(float val ){
            if (val<0 ) return -val;
            else return val;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "BroadcastReceiver:onReceive");


            final String action = intent.getAction();
            Log.e("#### OnReceive (1)", action);
            //mBluetoothLeService.initVib(true);
            if (ThinQBTSensorService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Toast toast = Toast.makeText(getApplicationContext(),
                        "GATT CONNECTED", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                //mBluetoothLeService.initVib(true);

            } else if (ThinQBTSensorService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
                Toast toast = Toast.makeText(getApplicationContext(),
                        "GATT DISCONNECTED", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (ThinQBTSensorService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                //mBluetoothLeService.initVib(true);
                //mBluetoothLeService.initAccel(false);
                //mBluetoothLeService.initVib(false);
                //mBluetoothLeService.initVib(true);

            } else if (ThinQBTSensorService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(ThinQBTSensorService.EXTRA_DATA));
                Log.e("### OnReceive (3)", "들어온 DATA : " + intent.getStringExtra(ThinQBTSensorService.EXTRA_DATA));
                //Log.e("############SEYEON", "DATA AVAILABLE");
                //ThinQBTSensorService.on
                //if (mode.equals("VIBMODE")) {mBluetoothLeService.initVib(true);
                if (first_time) {
                    mBluetoothLeService.initVib(true);
                    first_time = false;
                    Log.e("### OnReceive (3)", "VIB INIT");
                }

                //    Log.e("### OnReceive (3)", "VIB INIT");}
                //else {mBluetoothLeService.initAccel(true);
                //Log.e("### OnReceive (3)", "ACC INIT"); }

            }else if (ThinQBTSensorService.ACTION_ACCEL_DATA.equals(action)){
                //수평측정핪
                //mBluetoothLeService.initAccel(true);
                byte[] packet = intent.getByteArrayExtra(ThinQBTSensorService.ACTION_ACCEL_DATA);
                Log.e("##OnReceive (7)", "ACCEL DATA ACCEPT" + action);
                //Log.e("####" + TAG + "## onReceive(7)-1", "DATA PACKE FIRST " + Integer.toString(packet[0]));
                if (packet.length != 0){

                    List<Integer> accelList = parseAccelPacket(packet);
                    int acc_X = accelList.get(0);
                    int acc_Y = accelList.get(1);
                    int acc_Z = accelList.get(2);
                    //Toast toast = Toast.makeText(getApplicationContext(),
                     //       "ACCEL_DATA"+Integer.toString(acc_X) + "\n" + Integer.toString(acc_Y) + "\n" + Integer.toString(acc_Z), Toast.LENGTH_SHORT);
                    //toast.setGravity(Gravity.CENTER, 0, 0);
                    //toast.show();
                    //displayData(Integer.toString(acc_X) + "\n" + Integer.toString(acc_Y) + "\n" + Integer.toString(acc_Z));
                    if (first_accel){
                        x_prev=  acc_X;
                        y_prev = acc_Y;
                        z_prev = acc_Z;
                        first_accel = false;
                    }else {
                        reflect_accel(acc_X -x_prev, acc_Y-y_prev, acc_Z-z_prev);
                    }

                }

            }else if (ThinQBTSensorService.ACTION_VIB_DATA.equals(action)) {
                Log.e("### OnReceive (8)", "VIB DATA ACCEPT" + action);
                //진동 측정값
                byte[] packet = intent.getByteArrayExtra(mBluetoothLeService.ACTION_VIB_DATA);
                float vib_val = parseVibPacket(packet);
                vib_val = abs(vib_val);
                Log.e("### OnReceive (9)", "VIB VAL : " + Float.toString(vib_val));

                Log.e("!!!!!!!!!!!!!!", Float.toString(vib_val));
                /*
                if (!mode_changed) {

                    //displayData(Float.toString(vib_val));
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "VIB_DATA "+Float.toString(vib_val), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    }
            */

                if (!isSavingVibsEnd) {

                    if (vib_val >= 1200) {
                        isSavingVibsStart = true;
                    }

                    if (isSavingVibsStart) {
                        if (vib_val < 1000) {
                            if (exception > 0) isSavingVibsEnd = true;
                            else exception +=1;
                        } else {
                            //Log.e("!!!!!!!!!!!!!!!", Float.toString(vib_val));
                            vibArray.add(vib_val);
                        }

                    }
                }else {
                    mode = "ACCELMODE";

                    if (ACCEL_MODE == "") {
                        // SAVING END ! LETS CALCUL AND DECIDE ACCEL_MODE!
                        Log.e("!!!!!!!", vibArray.toString());

                        boolean nothing = true;
                        for (int i = 0; i < vibArray.size(); i++) {
                            if (vibArray.get(i) >= 3000) {
                                Log.e("!!!!!!@", "Zoom");
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "CHANGE TO BIG VIB MODE :: ZOOM MODE ", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                // stop vib
                                mBluetoothLeService.initVib(false);
                                mBluetoothLeService.initAccel(true);
                                ACCEL_MODE = "ZOOM";
                                nothing = false;
                                break;
                            }
                        }
                        if (nothing) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "CHANGE TO SMALL VIB MODE :: PEN MODE ", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            // stop vib
                            mBluetoothLeService.initVib(false);
                            mBluetoothLeService.initAccel(true);
                            ACCEL_MODE = "PEN";
                        }
                    }
                    if (first_accel) {
                        mBluetoothLeService.initAccel(true);
                    }

                }


            }else if (ThinQBTSensorService.ACTION_BUTTON_EVENT.equals(action)){
                Log.e("##### OnReceive (11)", "BUTTON CLICK!");
                if (isRecording ) {
                    isRecording = false;
                    mBluetoothLeService.initAccel(false);
                    mBluetoothLeService.close();

                }

                else {

                    isRecording = true;
                }
            }
            else {
                Log.e("###### OnReceive (10)", "ELSE CASE : " + action);
            }

        }

    };

    private void reflect_accel(int x, int y, int z) {
        switch (ACCEL_MODE){
            case "PEN":
                Log.e("@@@@", x + ","+y+","+z);
                if (x >= 100 || y >= 100 || z >= 100) change_fill_color();
                break;
            case "ZOOM":
                if (y > 0 ) {
                    if ((1.0 >= zoomrate) && (zoomrate > 0.1)) {
                        zoomrate -= 0.05;
                    }else {
                        zoomin = false;
                    }

                }else {
                    if (!(zoomrate >= 1)) zoomrate += 0.05;
                    else zoomin = true;
                }



        }
    }

    // PARSE PACKET
    public List<Integer> parseAccelPacket(byte[] data){
        int id = data[0];
        if (id < 0 ){
            id = id + 256;
        }
        List<Integer> default_return = new ArrayList<Integer>();
        switch(id) {
            case 152:               // Acceleration
                int acc_x = -1;
                int acc_y = -1 ;
                int acc_z = -1 ;
                for(int i = 1; i < 19; i += 6) {
                    acc_x = (data[i] << 8) | (data[i+1] & 0xFF);
                    acc_y = (data[i+2] << 8) | (data[i+3] & 0XFF);
                    acc_z = (data[i+4] << 8) | (data[i+5] & 0xFF);

                    Log.i(TAG, "accX = " + acc_x);
                    Log.i(TAG, "accY = " + acc_y);
                    Log.i(TAG, "accZ = " + acc_z);
                }
                default_return.add(acc_x);
                default_return.add(acc_y);
                default_return.add(acc_z);
                return default_return;



            default:
                break;
        }
        return default_return;

    } // PARSE PACKET

    public float parseVibPacket(byte[] data){
        int id = data[0];
        float i = 0;
        switch(id) {
            case 104:               // Vibration
                float rms = ((data[6] << 24) | (data[5] << 16) | (data[4] << 8) | (data[3] & 0xFF)) / (float) 10.0;

                Log.i(TAG, "rms = " + rms);
                return rms;

            default:
                break;
        }
        return i;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        setTheme(R.style.no_title);
        
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Intent thinqBtServiceIntent = new Intent(this, ThinQBTSensorService.class);
        bindService(thinqBtServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        unregisterReceiver(mGattUpdateReceiver);

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
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();




        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
    private static IntentFilter makeGattUpdateIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ThinQBTSensorService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ThinQBTSensorService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ThinQBTSensorService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ThinQBTSensorService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ThinQBTSensorService.ACTION_ACCEL_DATA);
        intentFilter.addAction(ThinQBTSensorService.ACTION_VIB_DATA);
        intentFilter.addAction(ThinQBTSensorService.ACTION_BUTTON_EVENT);
        return intentFilter;
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

            if (contours.size() >0) {

                List<Point> this_mat = contours.get(0).toList();
                double sum_x = 0;
                double sum_y = 0;
                for (int i = 0; i < this_mat.size(); i++) {
                    sum_x = sum_x + this_mat.get(i).x;
                    sum_y = sum_y + this_mat.get(i).y;
                }
                Point center = new Point(sum_x / this_mat.size(), sum_y / this_mat.size());
                mCenters.add(center);

                for (int i = 0; i < mTrace.size(); i++) {
                    Imgproc.circle(mRgba, mCenters.get(i), 40, CONTOUR_COLOR, -1);
                }

                mTrace.add(contours.get(0));

                Mat colorLabel = mRgba.submat(4, 68, 4, 68);
                colorLabel.setTo(mBlobColorRgba);

                Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
                mSpectrum.copyTo(spectrumLabel);
            }
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
