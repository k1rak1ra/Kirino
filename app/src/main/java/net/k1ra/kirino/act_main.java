package net.k1ra.kirino;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.File;

public class act_main extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    BroadcastReceiver rec;
    BroadcastReceiver bg_rec;

    @Override
    public void onDestroy()
    {
        //dispose of DB reciever
        unregisterReceiver(rec);
        unregisterReceiver(bg_rec);

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_main);

        Tools.picasso = Picasso.get();

        //try to load background image if exists
        final ImageView background = findViewById(R.id.bg_img);
        bg_rec = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                try {
                    File imgFile = new File(getExternalFilesDir(null), "background.png");
                    if (imgFile.exists()) {
                        background.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                    }
                } catch (Exception e) {
                }
            }
        };
        IntentFilter bg_filter = new IntentFilter();
        bg_filter.addAction("net.k1ra.background");
        registerReceiver(bg_rec, bg_filter);
        sendBroadcast(new Intent("net.k1ra.background"));

        final SharedPreferences sharedPref = getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);

        //if n00b show intro dialog
        if (sharedPref.getBoolean("n00b", true)) {
            final Dialog settings = new Dialog(act_main.this);
            settings.setCancelable(true);
            settings.setContentView(R.layout.dialog_n00b_into);
            settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            settings.findViewById(R.id.n00b_dismiss).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("n00b", false).commit();
                    settings.dismiss();
                }
            });
            settings.show();
        }

        //show update notes dialog
        if (sharedPref.getBoolean("1.2.1.0", true)) {
            final Dialog settings = new Dialog(act_main.this);
            settings.setCancelable(true);
            settings.setContentView(R.layout.dialog_update_notes);
            settings.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            settings.findViewById(R.id.updated_dismiss).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sharedPref.edit().putBoolean("1.2.1.0", false).commit();
                    settings.dismiss();
                }
            });
            settings.show();
        }

        //update overrides
        Tools.update_overrides(act_main.this);

        //reciever for adding to feed
        rec = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                mViewPager.setCurrentItem(2);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("net.k1ra.update.feed_add");
        filter.addAction("net.k1ra.relay");
        registerReceiver(rec, filter);

        //auto-dismiss notification if exists
        if (getIntent().getIntExtra("notification_id", -1) != -1) {
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(getIntent().getIntExtra("notification_id", 0));
        }

        //send broadcast to start sync just in case
        final ComponentName serviceComponent = new ComponentName(this, sync_service.class);
        final JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
            builder.setMinimumLatency(1 * 1000); // wait at least
            builder.setOverrideDeadline(3 * 1000); // maximum delay
        final JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
        //make a new crunchyroll session
        if (sharedPref.getBoolean("CR_logged_in", false)) {
            final NetworkResponse out_cr = new NetworkResponse();
            NetworkRequest.make(out_cr, NetworkRequest.CR_login, true, false, new Pair[]{Pair.create("account", sharedPref.getString("CR_email", "")), Pair.create("password", Tools.fetch_CR_password(act_main.this))}, act_main.this, new Runnable() {
                @Override
                public void run() {
                    try {
                        //make sure user is premium
                        if (out_cr.obj.getJSONObject("data").getJSONObject("user").getString("premium").equals("anime|drama|manga")) {
                            //start ze session
                            final NetworkResponse ss_out = new NetworkResponse();
                            NetworkRequest.make(ss_out, NetworkRequest.CR_start_session, true, false, new Pair[]{Pair.create("auth", out_cr.obj.getJSONObject("data").getString("auth"))}, act_main.this, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //we're logged in!
                                        sharedPref.edit().putBoolean("CR_logged_in", true).putString("CR_session_id", ss_out.obj.getJSONObject("data").getString("session_id")).commit();
                                    } catch (JSONException e) {
                                    }
                                }
                            }, null);
                        } else {
                            Errors.toast(act_main.this, getString(R.string.CR_premium_needed));
                        }
                    } catch (JSONException e) {
                        Errors.toast(act_main.this, getString(R.string.login_fail));
                    }
                }
            }, null);
        }


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        //item 0 = manga list
        //item 1 = anime list
        //item 2 = main view
        //item 3 = waifu list
        //item 4 = settings

        System.out.println("LINK: "+getIntent().getDataString());

        //if we have a dymanic link
        if (getIntent().getDataString() != null) {
            try {
                //check if it's an anilist token. Make sure it's no an anime/manga
                if (!getIntent().getDataString().split("=")[1].split("&")[0].equals("access_denied") &&
                        !getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[1].equals("media") &&
                        !getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[1].equals("chara")) {
                    //save in sharedpref
                    SharedPreferences.Editor editor = getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit();
                    editor.putString("AL_token", getIntent().getDataString().split("=")[1].split("&")[0]);
                    editor.commit();

                    //get AL UID
                    final NetworkResponse out = new NetworkResponse();
                    NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, true, NetworkRequest.make_AL_query(4, "", 0), act_main.this, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //save in sharedpref
                                SharedPreferences.Editor editor = getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit();
                                editor.putString("AL_uid", out.obj.getJSONObject("data").getJSONObject("Viewer").getString("id"));
                                editor.commit();

                                //fetch lists from AL
                                jobScheduler.schedule(builder.build());

                            } catch (JSONException e) {
                            }
                        }
                    }, null);


                    //and go right to settings screen
                    mViewPager.setCurrentItem(4);
                } else {
                    mViewPager.setCurrentItem(getIntent().getIntExtra("sec", 2));
                }
            } catch (ArrayIndexOutOfBoundsException e) //because some idiot will definitely try this
            {
                mViewPager.setCurrentItem(getIntent().getIntExtra("sec", 2));
            }
        } else {
            mViewPager.setCurrentItem(getIntent().getIntExtra("sec", 2));
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position)
            {
                case 0: return frag_list_manga.newInstance();
                case 1: return frag_list_anime.newInstance();
                case 3: return frag_list_waifu.newInstance();
                case 4: return frag_settings.newInstance();
                default: return frag_home.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    }
}
