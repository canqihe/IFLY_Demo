package com.true_u.ifly_elevator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Colin
 * on 2020/5/5
 * 开机监听广播
 * E-mail: hecanqi168@gmail.com
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent thisIntent = new Intent(context, TakeElevatorActivity.class);
            thisIntent.setAction("android.intent.action.MAIN");
            thisIntent.addCategory("android.intent.category.LAUNCHER");
            thisIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(thisIntent);
        }
    }
}
