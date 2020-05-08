package com.true_u.ifly_elevator.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by Colin
 * on 2020/4/26
 * E-mail: hecanqi168@gmail.com
 * Copyright (C) 2018 SSZB, Inc.
 */
public class ShowUtils {

    public static void showToast(Context context, String stringTx) {
        popToast(context, stringTx, Toast.LENGTH_SHORT);
    }

    public static void popToast(Context context, String toastText, int during) {
        if (context == null) {
            return;
        }
        if (context instanceof Activity) {
            if (((Activity) context).isFinishing())
                return;
        }
        Toast sToast = Toast.makeText(context, toastText, during);
        sToast.show();
    }
}
