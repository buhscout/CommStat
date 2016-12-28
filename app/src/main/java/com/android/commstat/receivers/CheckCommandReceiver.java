package com.android.commstat.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.android.commstat.R;
import com.android.commstat.services.BackupService;

public class CheckCommandReceiver extends WakefulBroadcastReceiver {
    private static boolean mIsActiveAlarm;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = context.getString(R.string.CheckCommandAction);
        if(action.equals(intent.getAction())) {
            Log.d("CheckCommandReceiver", action);
            Intent serviceIntent = new Intent(context, BackupService.class);
            serviceIntent.setAction(action);
            startWakefulService(context, serviceIntent);
        } else if(!mIsActiveAlarm) {
            setAlarm(context);
        }
        mIsActiveAlarm = true;
    }

    private void setAlarm(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CheckCommandReceiver.class);
        intent.setAction(context.getString(R.string.CheckCommandAction));
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgr.cancel(alarmIntent);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 36000000, alarmIntent);
    }

}
