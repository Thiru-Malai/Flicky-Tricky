package com.example.flickytricky;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ComponentActivity;

import static com.example.flickytricky.GameActivity.*;

public class GameView extends SurfaceView implements Runnable {
    //public static GameActivity ga = new GameActivity();
    public int res_count = 0;
    public String filename = "score.txt";
    String score1 = null;
    public Timer time;
    public static Boolean eye = false;
    public static Thread thread;

    private static boolean isPlaying;
    private boolean isGameOver = false;
    public int screenX=0, screenY=0, score = 0;
    int count = 0;
    public static float screenRatioX=0, screenRatioY=0;
    private Paint paint;
    private Bird[] birds;
    private SharedPreferences prefs;
    private Random random;
    private SoundPool soundPool;
    private List<Bullet> bullets;
    private int sound;
    public static Flight flight;
    public GameActivity activity;
    private Background background1, background2;
    public  Boolean log = false;
    public static Boolean if_pause = false;
    public GameActivity gameac;
    List<Thread> threadList = Collections.synchronizedList(new ArrayList<Thread>());
    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);

        this.activity = activity;

        prefs = activity.getSharedPreferences("game", Context.MODE_PRIVATE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(audioAttributes)
                    .build();

        }else
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        sound = soundPool.load(activity,R.raw.shoot,1);

        this.screenX = screenX;
        this.screenY = screenY;
        screenRatioX = 1920f / screenX;
        screenRatioY = 1080f / screenY;

        background1 = new Background(screenX, screenY, getResources());
        background2 = new Background(screenX, screenY, getResources());

        flight = new Flight(this, screenY, getResources());

        bullets = new ArrayList<>();

        background2.x = screenX;

        paint = new Paint();
        paint.setTextSize(128);
        paint.setColor(Color.BLACK);

        birds = new Bird[4];

        for (int i = 0;i < 4;i++) {

            Bird bird = new Bird(getResources());
            birds[i] = bird;

        }

        random = new Random();

    }

    @Override
    public synchronized void run() {

        while (isPlaying) {
            update ();
            draw ();
            sleep ();
        }

    }
    private void update(){

        background1.x -= 10 * screenRatioX;
        background2.x -= 10 * screenRatioX;

        if (background1.x + background1.background.getWidth() < 0) {
            background1.x = screenX;
        }

        if (background2.x + background2.background.getWidth() < 0) {
            background2.x = screenX;
        }

        if (flight.isGoingUp)
            flight.y -= 30 * screenRatioY;
        else
            flight.y += 30 * screenRatioY;

        if (flight.y < 0)
            flight.y = 0;

        if (flight.y >= screenY - flight.height)
            flight.y = screenY - flight.height;

        List<Bullet> trash = new ArrayList<>();

        for (Bullet bullet : bullets) {

            if (bullet.x > screenX)
                trash.add(bullet);

            bullet.x += 50 * screenRatioX;

            for (Bird bird : birds) {

                if (Rect.intersects(bird.getCollisionShape(),
                        bullet.getCollisionShape())) {

                    score++;
                    bird.x = -500;
                    bullet.x = screenX + 500;
                    bird.wasShot = true;

                }

            }

        }

        for (Bullet bullet : trash)
            bullets.remove(bullet);

        for (Bird bird : birds) {

            bird.x -= bird.speed;

            if (bird.x + bird.width < 0) {

                if (!bird.wasShot) {
                    isGameOver = true;
                    return;
                }

                int bound = (int) (30 * screenRatioX);
                bird.speed = random.nextInt(bound);

                if (bird.speed < 10 * screenRatioX)
                    bird.speed = (int) (10 * screenRatioX);

                bird.x = screenX;
                bird.y = random.nextInt(screenY - bird.height);

                bird.wasShot = false;
            }

            if (Rect.intersects(bird.getCollisionShape(), flight.getCollisionShape())) {
                isGameOver = true;
                return;
            }

        }

    }

    private void draw () {

        if (getHolder().getSurface().isValid()) {

            Canvas canvas = getHolder().lockCanvas();
            canvas.drawBitmap(background1.background, background1.x, background1.y, paint);
            canvas.drawBitmap(background2.background, background2.x, background2.y, paint);

            for (Bird bird : birds)
                canvas.drawBitmap(bird.getBird(), bird.x, bird.y, paint);

            canvas.drawText(score + "", screenX / 2f, 164, paint);

            if (isGameOver) {

                isPlaying = false;
                canvas.drawBitmap(flight.getDead(), flight.x, flight.y, paint);
                getHolder().unlockCanvasAndPost(canvas);
                saveIfHighScore();
                waitBeforeExiting ();
                return;
            }

            canvas.drawBitmap(flight.getFlight(), flight.x, flight.y, paint);

            for (Bullet bullet : bullets)
                canvas.drawBitmap(bullet.bullet, bullet.x, bullet.y, paint);

            getHolder().unlockCanvasAndPost(canvas);

        }

    }

    private void waitBeforeExiting() {
        try {
            Thread.sleep(1000);
          //  thread.notifyAll();

            synchronized(threadList) {
                Iterator<Thread> iterator = threadList.iterator();
                while (iterator.hasNext()){
                    Thread startThread = iterator.next();
                    startThread.interrupt();
                }
            }
            //Toast.makeText(getContext(),"Refreshing Score..",Toast.LENGTH_SHORT).show();
            activity.startActivity(new Intent(activity, MainActivity.class));

            activity.finish();

            //activity.startActivity(new Intent(activity,GameActivity.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveIfHighScore() {
        if (prefs.getInt("highscore", 0) < score) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highscore", score);
            editor.apply();
        }
    }

    private void sleep () {
        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        //activity.startActivity(activity.getIntent());
        //pause();
        isPlaying = true;

        res_count++;
        log = false;
        thread = new Thread(this);
        thread.start();
        threadList.add(thread);

    }

    public void pause() {
        try {
            res_count--;
            log = true;
            if_pause = true;
            isPlaying = false;
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Override
    //public boolean onTouchEvent(MotionEvent event) {

    //switch (event.getAction()) {
    //case MotionEvent.ACTION_DOWN:
    //if (event.getX() < screenX / 2) {
    //   flight.isGoingUp = true;
    // }
    //  break;
    //case MotionEvent.ACTION_UP:
    //flight.isGoingUp = false;
    //  if (event.getX() > screenX / 2)
    //        flight.toShoot++;
    //      break;
    //}

    //  return true;
    //}
    public void updateMainView(Condition condition){
        switch (condition){
            case USER_EYES_OPEN:
                flight.isGoingUp = true;
                //resume();
                if(!isPlaying){
                    resume();
                    //activity.startActivity(new Intent(activity, GameActivity.class));
                }
                break;
            case USER_EYES_CLOSED:
                flight.isGoingUp = false;
                flight.toShoot++;
                break;
            case FACE_NOT_FOUND:
                pause();
                break;
            default:

        }
    }

    public void newBullet() {

        if (!prefs.getBoolean("isMute", false))
            soundPool.play(sound, 1, 1, 0, 0, 1);

        Bullet bullet = new Bullet(getResources());
        bullet.x = flight.x + flight.width;
        bullet.y = flight.y + (flight.height / 2);
        bullets.add(bullet);

    }
    private void writeData()
    {
        try
        {
            FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
            String data = score1;
            fos.write(data.getBytes());
            fos.flush();
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //Toast.makeText(getApplicationContext(),"writing to file " + filename + "completed...",Toast.LENGTH_SHORT).show();
    }

}