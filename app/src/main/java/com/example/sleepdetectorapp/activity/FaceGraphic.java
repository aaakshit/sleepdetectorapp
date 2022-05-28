package com.example.sleepdetectorapp.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;

import com.example.sleepdetectorapp.event.AllEyesClosedEvent;
import com.example.sleepdetectorapp.event.AllEyesOpenedEvent;
import com.example.sleepdetectorapp.event.MouthClosedEvent;
import com.example.sleepdetectorapp.event.MouthOpenedEvent;
import com.example.sleepdetectorapp.util.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class FaceGraphic extends GraphicOverlay.Graphic {
    //Threshold values initialized later
    private float EAR_THRESHOLD;
    private float MAR_THRESHOLD;

    //Landmark Request Contant
    private static final int LANDMARK_CONST = 99;

    //Frames per second considered for detection
    private static final int REQ_EYE_FPS = 20;
    private static final int REQ_MOUTH_FPS = 15;

    //Initial frame-counts for both Eye & Yawn Detection
    private int eyeFrameCount = 0;
    private int mouthFrameCount = 0;

    private static final String TAG = "FaceGraphic";

    //Default Text-Size Constants
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_ALERT_SIZE = 65.0f;

    //Default X,Y Offset Constants
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;

    //Default Width of Bounding-Box
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private Paint mIdPaint;
    private Paint mBoxPaint;
    private Paint mIdAlert;

    private volatile Face mFace;
    private Context mContext;

    //Detection events are by-default False
    private boolean leftClosed, rightClosed, mouthOpened,isDrowsyAlertOn,isYawnAlertOn;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        Log.i(TAG,"Variables are to be Initialized");

        mIdPaint = new Paint();
        mIdPaint.setColor(Color.GREEN);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mIdAlert = new Paint();
        mIdAlert.setColor(Color.RED);
        mIdAlert.setTextSize(ID_ALERT_SIZE);
        mIdAlert.setStrokeWidth(6.0f);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(Color.GREEN);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void draw(Canvas canvas) {
        Log.i(TAG,"Face Detection begins");

        //Handler is created to hold the Alert Signal for few milli-seconds
        final Handler handler = new Handler();


        Face face = mFace;
        if (face == null) {
            return;
        }

        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            EAR_THRESHOLD = 0.75f;
            MAR_THRESHOLD = 110.0f;
        } else if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            EAR_THRESHOLD = 0.65f;
            MAR_THRESHOLD = 119.5f;
        }

        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);
        //Draw Texts for Driver
        canvas.drawText("DRIVER", left + 10.0f, top - 10.f, mIdPaint);

        if(isDrowsyAlertOn) {
            canvas.drawText("ALERT: SLEEPY", left - 4 * ID_X_OFFSET, top - ID_Y_OFFSET, mIdAlert);
        }

        if (isYawnAlertOn) {
            canvas.drawText("ALERT: YAWNING", left - 4 * ID_X_OFFSET, top - ID_Y_OFFSET, mIdAlert);
        }

        //Measuring Eyes Opened Probs
        float leftEyesOpenProb = face.getIsLeftEyeOpenProbability();
        float rightEyesOpenProb = face.getIsRightEyeOpenProbability();

        //Checking for Eyes open & Close
        if (contains(face.getLandmarks(),4) != LANDMARK_CONST && contains(face.getLandmarks(),10) != LANDMARK_CONST) {
            canvas.drawText("right eye: " + String.format("%.2f", rightEyesOpenProb), left + 10.0f, bottom + ID_Y_OFFSET, mIdPaint);
            canvas.drawText("left eye: " + String.format("%.2f", leftEyesOpenProb), right + 4*ID_X_OFFSET, bottom + ID_Y_OFFSET, mIdPaint);

            if (leftClosed && face.getIsLeftEyeOpenProbability() > EAR_THRESHOLD) {
                leftClosed = false;
            } else if (!leftClosed && face.getIsLeftEyeOpenProbability() < EAR_THRESHOLD) {
                leftClosed = true;
            }
            if (rightClosed && face.getIsRightEyeOpenProbability() > EAR_THRESHOLD) {
                rightClosed = false;
            } else if (!rightClosed && face.getIsRightEyeOpenProbability() < EAR_THRESHOLD) {
                rightClosed = true;
            }
            if (leftClosed && rightClosed) {
                eyeFrameCount += 1;
                if (eyeFrameCount >= REQ_EYE_FPS) {
                    if (!isYawnAlertOn) isDrowsyAlertOn = true;
                    EventBus.getDefault().post(new AllEyesClosedEvent());
                }
            } else if (!leftClosed && !rightClosed) {
                if (eyeFrameCount >= REQ_EYE_FPS && isDrowsyAlertOn) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isDrowsyAlertOn = false;
                        }
                    },1500);
                    EventBus.getDefault().post(new AllEyesOpenedEvent());
                }
                eyeFrameCount = 0;
            }
        }

        //Checking for Landmarks - Right, Left & Bottom Mouth
        if ((contains(face.getLandmarks(), 11) != LANDMARK_CONST)
                && (contains(face.getLandmarks(), 5) != LANDMARK_CONST)
                && (contains(face.getLandmarks(), 0) != LANDMARK_CONST)
                && (contains(face.getLandmarks(),6) != LANDMARK_CONST)) {
            Log.i("FaceGraphic","Landmarks Present");
            //Coordinate : Bottom Mouth
            int cBottomMouthX = (int) translateX(face.getLandmarks().get(contains(face.getLandmarks(), 0)).getPosition().x);
            int cBottomMouthY = (int) translateY(face.getLandmarks().get(contains(face.getLandmarks(), 0)).getPosition().y);
            canvas.drawCircle(cBottomMouthX, cBottomMouthY, 10, mIdPaint);
            //Coordinate : Left Mouth
            int cLeftMouthX = (int) translateX(face.getLandmarks().get(contains(face.getLandmarks(), 5)).getPosition().x);
            int cLeftMouthY = (int) translateY(face.getLandmarks().get(contains(face.getLandmarks(), 5)).getPosition().y);
            //canvas.drawCircle(cLeftMouthX, cLeftMouthY, 10, mIdPaint);
            //Coordinate : Right Mouth
            int cRightMouthX = (int) translateX(face.getLandmarks().get(contains(face.getLandmarks(), 11)).getPosition().x);
            int cRightMouthY = (int) translateY(face.getLandmarks().get(contains(face.getLandmarks(), 11)).getPosition().y);
            //canvas.drawCircle(cRightMouthX, cRightMouthY, 10, mIdPaint);
            //Coordinate : Nose Base
            int cNoseBaseX = (int) translateX(face.getLandmarks().get(contains(face.getLandmarks(), 6)).getPosition().x);
            int cNoseBaseY = (int) translateY(face.getLandmarks().get(contains(face.getLandmarks(), 6)).getPosition().y);
            //canvas.drawCircle(cNoseBaseX, cNoseBaseY, 10, mIdPaint);


            double sqrt = Math.sqrt(Math.pow(cRightMouthX - cLeftMouthX, 2) + Math.pow(cRightMouthY - cLeftMouthY, 2));

            float distanceY = (float) Math.sqrt(Math.pow(cBottomMouthX - cNoseBaseX,2) + Math.pow(cBottomMouthY - cNoseBaseY,2));
            float distanceX = (float) sqrt;
            float ratioF = (distanceY / distanceX);

            int leftRight = (int) sqrt;
            int leftBottom = (int) Math.sqrt(Math.pow(cBottomMouthX - cLeftMouthX,2) + Math.pow(cBottomMouthY - cLeftMouthY,2));
            int rightBottom = (int) Math.sqrt(Math.pow(cBottomMouthX - cRightMouthX,2) + Math.pow(cBottomMouthY - cRightMouthY,2));

            double ratio = (Math.pow(rightBottom,2) + Math.pow(leftBottom,2) - Math.pow(leftRight,2)) / (2 * leftBottom * rightBottom);
            float degree = (float) (Math.acos(ratio) * (180 / Math.PI));

            canvas.drawText("mouth open: " + String.format("%.2f",degree) /*+ ": " + String.format("%.2f",ratioF)*/, cBottomMouthX + ID_X_OFFSET, cBottomMouthY + ID_Y_OFFSET, mIdPaint);

            if (!mouthOpened && degree < MAR_THRESHOLD) {
                mouthOpened = true;
            } else if(mouthOpened && degree >= MAR_THRESHOLD) {
                mouthOpened = false;
            }
            if (mouthOpened && face.getIsSmilingProbability() <= 0.475f && ratioF >= 1.1f) {

                mouthFrameCount += 1;
                if(mouthFrameCount >= REQ_MOUTH_FPS) {
                    if (!isDrowsyAlertOn) isYawnAlertOn = true;
                    EventBus.getDefault().post(new MouthOpenedEvent());
                }
            } else {
                if (mouthFrameCount >= REQ_MOUTH_FPS && isYawnAlertOn) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isYawnAlertOn = false;
                        }
                    },1500);
                    EventBus.getDefault().post(new MouthClosedEvent());
                }
                mouthFrameCount = 0;
            }

        } else  {
            Log.i("FaceGraphic","Landmarks Not Present");
        }
    }

    //Check if required landmarks are found or not
    int contains(List<Landmark> list, int name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getType() == name) {
                return i;
            }
        }
        return LANDMARK_CONST;
    }

    //Detection of frontal-face is being updated according to consecutive frames & positions
    void updateFace(Context context,Face face) {
        mContext = context;
        mFace = face;
        postInvalidate();
    }

}
