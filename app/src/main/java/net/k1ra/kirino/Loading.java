package net.k1ra.kirino;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public abstract class Loading {

    static Dialog dialog = null;

    static void show(final Context c)
    {
        try {
            if (dialog == null && c != null) {
                dialog = new Dialog(c);
                dialog.setContentView(R.layout.dialog_loading_spinner);
                dialog.setCancelable(false);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        } catch (Exception e) {}
    }

    static void dismiss()
    {
        try {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {}
    }
}
