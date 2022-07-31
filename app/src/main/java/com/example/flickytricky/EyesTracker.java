package com.example.flickytricky;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.content.Context;
import android.util.Log;
import com.example.flickytricky.GameActivity.*;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import static com.example.flickytricky.GameActivity.gameView;
public class EyesTracker extends Tracker<Face> {
   // public static GameView gameView;
    private final float THRESHOLD = 0.75f;
    public Context context;

    public EyesTracker(Context context) {
        this.context = context;
    }

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
        if (face.getIsLeftEyeOpenProbability() > THRESHOLD || face.getIsRightEyeOpenProbability() > THRESHOLD) {
            Log.i(TAG, "onUpdate: Open Eyes Detected");
            gameView.updateMainView(Condition.USER_EYES_OPEN);
        }
        else {
            Log.i(TAG, "onUpdate: Close Eyes Detected");
            gameView.updateMainView(Condition.USER_EYES_CLOSED);
        }
    }

    @Override
    public void onMissing(Detector.Detections<Face> detections) {
        super.onMissing(detections);

        Log.i(TAG, "onUpdate: Face Not Detected!");
        //GameView.pause();
        gameView.updateMainView(Condition.FACE_NOT_FOUND);
    }

    @Override
    public void onDone() {
        super.onDone();
    }
}
