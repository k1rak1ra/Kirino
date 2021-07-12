package net.k1ra.kirino;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class sync_service extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        final SharedPreferences sharedPref = getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);

        //reschedule immediately so it runs again even if it fails
        ComponentName serviceComponent = new ComponentName(this, sync_service.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(sharedPref.getInt("sync_wait", 15)*60000); // wait at least
        builder.setOverrideDeadline(sharedPref.getInt("sync_wait", 15)*60000+60000); // maximum delay
        JobScheduler jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());

        if (!sharedPref.getString("AL_token", "").equals("")) {
            Tools.fetch_AL_lists(sync_service.this);
        } else {
            Tools.update_lists_no_AL(sync_service.this);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
    }
