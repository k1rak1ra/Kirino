package net.k1ra.kirino;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public abstract class Errors {

    static void no_internet_page(final Context c)
    {
        ((Activity)c).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((Activity)c).setContentView(R.layout.error_no_internet);
                Button TryAgain = (Button) ((Activity)c).findViewById(R.id.initial_loading_no_internet_try_again);
                TryAgain.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = ((Activity)c).getIntent();
                        ((Activity)c).finish();
                        ((Activity)c).startActivity(intent);
                    }

                });
            }
        });
    }

    static void no_internet_toast(final Context c)
    {
        try {
            if (c != null) {
                ((Activity) c).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CharSequence text = ((Activity) c).getString(R.string.no_internet_toast);
                        Toast toast = Toast.makeText(c, text, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }
        } catch (Exception e) {}
    }

    static void toast(final Context c, final CharSequence text)
    {
        try {
            ((Activity) c).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(c, text, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        } catch (Exception e) {}
    }

}
