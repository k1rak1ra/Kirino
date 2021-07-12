package net.k1ra.kirino;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class fullscreen_manga_reader extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
           /* ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }*/
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public void onTap(){}

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e)
            {
                onTap();
                return true;
            }
        }
    }

    int current_page = 0;
    JSONArray pages;
    int current_chapter = 0;
    String oid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_manga_reader);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.manga_reader_image);


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        try {
            final JSONArray chapters = new JSONArray(getIntent().getStringExtra("chapters"));
            init(chapters, getIntent().getIntExtra("index",0));

            mContentView.setOnTouchListener(new OnSwipeTouchListener(fullscreen_manga_reader.this) {
                @Override
                public void onSwipeRight() {
                    if (current_page < pages.length()) {
                        current_page++;
                        load_page(pages, current_page);
                    } else {
                        go_to_next_chapter(chapters);
                    }
                }
                @Override
                public void onSwipeLeft()
                {
                    if (current_page > 0) {
                        current_page--;
                        load_page(pages, current_page);
                    }else {
                        go_to_previous_chapter(chapters);
                    }
                }
                @Override
                public void onTap(){
                    toggle();
                }
            });

            findViewById(R.id.manga_reader_left).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (current_page < pages.length()) {
                        current_page++;
                        load_page(pages, current_page);
                    } else {
                        go_to_next_chapter(chapters);
                    }
                }
            });
            findViewById(R.id.manga_reader_right).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (current_page > 0) {
                        current_page--;
                        load_page(pages, current_page);
                    }else {
                        go_to_previous_chapter(chapters);
                    }
                }
            });

        } catch (JSONException e) {}
    }

    void go_to_next_chapter(final JSONArray chapters)
    {
        String oid = "null";
        try {
            oid = chapters.getJSONObject(current_chapter+1).getString("oid");
            JSONArray pages = new JSONArray(getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(oid, "null"));

            broadcast();
            init(chapters, current_chapter+1);
        } catch (JSONException e)
        {
            if (oid.equals("null"))
            {
                Errors.toast(fullscreen_manga_reader.this, getString(R.string.no_next_chapter));
            } else {
                Errors.toast(fullscreen_manga_reader.this, getString(R.string.next_chapter_not_downloaded));
                frag_list_manga.prepare_download(null, chapters, current_chapter + 1, fullscreen_manga_reader.this);
            }
        }
    }

    void go_to_previous_chapter(final JSONArray chapters)
    {
        String oid = "null";
        try {
            oid = chapters.getJSONObject(current_chapter-1).getString("oid");
            JSONArray pages = new JSONArray(getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(oid, "null"));

            broadcast();
            init(chapters, current_chapter-1);
        } catch (JSONException e)
        {
            if (oid.equals("null"))
            {
                Errors.toast(fullscreen_manga_reader.this, getString(R.string.no_previous_chapter));
            } else {
                Errors.toast(fullscreen_manga_reader.this, getString(R.string.previous_chapter_not_downloaded));
                frag_list_manga.prepare_download(null, chapters, current_chapter - 1, fullscreen_manga_reader.this);
            }
        }
    }

    void init(final JSONArray chapters, final int chapter)
    {
        try {
            current_chapter = chapter;
            oid = chapters.getJSONObject(current_chapter).getString("oid");
            pages = new JSONArray(getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString(oid, "null"));
            current_page = getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getInt(oid + "-PROG", 0);
            load_page(pages, current_page);
        } catch (JSONException e) {}
    }


    void load_page(final JSONArray pages, final int page)
    {
        try {
            File imgFile = new File(getExternalFilesDir(null), pages.getString(page).split("\\/")[pages.getString(page).split("\\/").length-1]);
            if (imgFile.exists()) {
                FileInputStream input = new FileInputStream(imgFile);
                ((SubsamplingScaleImageView)mContentView).setImage(ImageSource.bitmap(Tools.decode_image(Tools.getBytesFromInputStream(input))));
            }
        } catch (Exception e) { e.printStackTrace();
        }
    }

    void broadcast()
    {
        Intent intent = new Intent("net.k1ra.update.manga.chap");
        intent.putExtra("page",current_page);
        intent.putExtra("oid",oid);
        sendBroadcast(intent);
    }

    @Override
    public void onBackPressed()
    {
        broadcast();
        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
