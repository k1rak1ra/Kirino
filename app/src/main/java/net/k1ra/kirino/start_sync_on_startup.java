package net.k1ra.kirino;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class start_sync_on_startup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName serviceComponent = new ComponentName(context, sync_service.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(1 * 1000); // wait at least
        builder.setOverrideDeadline(3 * 1000); // maximum delay
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
