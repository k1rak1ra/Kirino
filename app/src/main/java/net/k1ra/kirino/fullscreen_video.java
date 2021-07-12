package net.k1ra.kirino;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


public class fullscreen_video extends AppCompatActivity {

    ExoPlayer player;

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_video);
        mContentView = getWindow().getDecorView();

        PlayerView playerView = findViewById(R.id.video_view);
        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(fullscreen_video.this),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(fullscreen_video.this, Util.getUserAgent(fullscreen_video.this, "Kirino"));

        // This is the MediaSource representing the media to be played.
        try {
            Uri videoUri = Uri.parse(getIntent().getStringExtra("stream"));
            MediaSource videoSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri);

            player.prepare(videoSource);
            player.seekTo(getIntent().getIntExtra("playhead", 0)*1000);
        } catch (Exception e) {
            Errors.toast(fullscreen_video.this, getString(R.string.fsv_error));
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (player != null) {
            long playhead = player.getCurrentPosition()/1000;

            Intent intent = new Intent("net.k1ra.CR_video_closed");
            intent.putExtra("playhead",playhead);
            intent.putExtra("index",getIntent().getIntExtra("index",0));
            sendBroadcast(intent);

            player.release();
            player = null;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        delayedHide(100);
    }

    @Override
    public void onResume(){
        super.onResume();
        hide();
    }


    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
