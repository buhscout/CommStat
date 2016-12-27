package com.android.commstat.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.android.commstat.services.BackupService;

public class CheckCommandReceiver extends WakefulBroadcastReceiver {
    private static boolean mIsActiveAlarm;
    private static final String CHECK_COMMAND_ACTION = "commstat.permission.CHECK_COMMAND";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(CHECK_COMMAND_ACTION.equalsIgnoreCase(intent.getAction())) {
            Log.d("CheckCommandReceiver", "CHECK_COMMAND_ACTION");
            Intent serviceIntent = new Intent(context, BackupService.class);
            serviceIntent.setAction(CHECK_COMMAND_ACTION);
            startWakefulService(context, serviceIntent);
        } else if(!mIsActiveAlarm) {
            setAlarm(context);
        }
        mIsActiveAlarm = true;
    }

    private void setAlarm(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CheckCommandReceiver.class);
        intent.setAction(CHECK_COMMAND_ACTION);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgr.cancel(alarmIntent);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 36000000, alarmIntent);
    }

}
