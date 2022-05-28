package com.example.sleepdetectorapp.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.sleepdetectorapp.R;
import com.example.sleepdetectorapp.event.AllEyesClosedEvent;
import com.example.sleepdetectorapp.event.AllEyesOpenedEvent;
import com.example.sleepdetectorapp.event.MouthClosedEvent;
import com.example.sleepdetectorapp.event.MouthOpenedEvent;
import com.example.sleepdetectorapp.util.CameraSourcePreview;
import com.example.sleepdetectorapp.util.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    //Declare Camera & Face-Detection related variables
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private FaceDetector mDetector;

    //Declare the MediaPlayer variables
    private MediaPlayer alarmPlayer;
    private MediaPlayer warningPlayer;
    private MediaPlayer yawnPlayer;

    //Constants for Permissions
    private static final int RC_HANDLE_GMS = 1;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    //updating lock
    private final AtomicBoolean updating = new AtomicBoolean(false);

    //Permission-grant Checking
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            //Camera Permission not requested
            Log.d(TAG, "Error in Camera-Permission: " + requestCode);

            //Call parent class menthod
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Camera Permission is granted
            Log.d(TAG, "Camera-Permission is granted");

            //Create the camera-source
            createCameraSource();
            return;
        }

        //Camera Permission is not granted
        Log.d(TAG, "Camera-Permission is not granted");

        //On "OK" application will exit
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        //Creating an AlertDialog Box for informing the client
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sleep Detector Error")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .create()
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing the Camera View
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        //Initializing the Warning Sounds
        alarmPlayer = MediaPlayer.create(this,R.raw.alarm);
        warningPlayer = MediaPlayer.create(this,R.raw.warning);
        yawnPlayer = MediaPlayer.create(this,R.raw.warning_yawn);

        //First checking if Camera permission is granted or not
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            //Camera Permission is granted. So create the camera-source
            createCameraSource();
        } else {
            //Camera Permission is not granted. So make a request for Camera Permission
            requestCameraPermission();
        }
    }
    // this will enable picture in picture mode (i.e floating window)
    @Override
    protected void onUserLeaveHint() {
        PictureInPictureParams pipp = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(2,2))
                .build();



        enterPictureInPictureMode(pipp);


        super.onUserLeaveHint();
    }
    // camera keeps running even in floating window
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if(isInPictureInPictureMode())
        {
          startCameraSource();

        }



        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    }


    //Requesting for Camera Permisiion
    private void requestCameraPermission() {
        Log.i(TAG, "Requesting for Camera-Permission");

        //Adding Camera-Permission in request queue
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            //Request has been sent to user for Camera permission.
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        //Finalizing the Activity context
        final Activity thisActivity = this;

        //On "OK" application will request for Camera-Permission
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        //Creating an Snackbar for requesting the client for Camera-Permission
        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    //Creating the Camera Source
    private void createCameraSource() {
        //Application Context is taken
        Context context = getApplicationContext();

        //Setting the Face-Detector
        mDetector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true)
                .setMode(FaceDetector.SELFIE_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        //Setting the processor for Face-Detector
        mDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        //Checking if Face-Detector is Operational or not
        if (!mDetector.isOperational()) {
            Log.i(TAG, "Face-Detector is not operational");
        }

        //Creating the Camera Source, taking the created Face-Detector
        mCameraSource = new CameraSource.Builder(context, mDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT) //Front-Camera is On
                .setAutoFocusEnabled(true)
                .setRequestedFps(14.0f)
                .build();
    }

    //Starting the Camera Source when application is in Resume State
    private void startCameraSource() {
        //Checking if Google Api/Google Play Service is available or not
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());

        //If Google Play Service is not available then show an errror message
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        //Google Play Service is available
        //Start Camera-Preview
        if (mCameraSource != null) {
            try {
                //Trying to start Camera-Preview
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                //Camera-Source is unable to start
                Log.i(TAG, "Camera-Source cannot be started.", e);

                //Release Camera-Source
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        private Context mContext = MainActivity.this;
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mContext,mGraphicOverlay);
        }
    }

    //Creating the Face-Tracker class for this Application
    private static class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;
        private Context mContext;

        GraphicFaceTracker(Context mContext,GraphicOverlay overlay) {
            this.mContext = mContext;
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        @Override
        public void onNewItem(int faceId, Face item) {
            //Nothing to implement
        }

        //Constant update on Face-Tracking
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(mContext,face);
        }

        //Remove the Face-Graphic when face cannot be tracked
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        //Remove the Face-Graphic when application is paused/closed
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

    //All eyes closed event is called
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void allEyesClosed(AllEyesClosedEvent event) {
        alarmPlayer.start();
        releaseUpdatingLock();
        Toast.makeText(this, "IF YOU ARE FEELING SLEEPY OPEN MAPS AND FIND A PLACE TO REST", Toast.LENGTH_SHORT).show();

    }

    //All eyes opened event is called
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void allEyesOpened(AllEyesOpenedEvent event) {
        warningPlayer.start();
        releaseUpdatingLock();
    }

    //Mouth opened event is called
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void mouthOpened(MouthOpenedEvent event) {
        alarmPlayer.start();

        releaseUpdatingLock();
    }

    //Mouth closed event is called
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void mouthClosed(MouthClosedEvent event) {
        yawnPlayer.start();
        releaseUpdatingLock();
    }

//    private boolean catchUpdatingLock() {
//        return !updating.getAndSet(true);
//    }

    //Relaeasing the lock
    private void releaseUpdatingLock() {
        updating.set(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register for EventBus
        EventBus.getDefault().register(this);

        //Start the Camera-Source by starting Camera-Preview
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Free-up/Unregister the EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        //Pause the Warning Sounds when other Activity comes in
        if (alarmPlayer.isPlaying()) alarmPlayer.pause();
        if (warningPlayer.isPlaying()) warningPlayer.pause();
        if (yawnPlayer.isPlaying()) yawnPlayer.pause();

        //Stop the Camera-Preview whenever other Activity comes in
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Stop the Warning Sounds when app gets closed somehow
        if (alarmPlayer.isPlaying()) alarmPlayer.stop();
        if (warningPlayer.isPlaying()) warningPlayer.stop();
        if (yawnPlayer.isPlaying()) yawnPlayer.stop();

        //Release the Face-Detector
        if(mDetector != null) {
            mDetector.release();
        }

        //Release the Camera-Source
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

}