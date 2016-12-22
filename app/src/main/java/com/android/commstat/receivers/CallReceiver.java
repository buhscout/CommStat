package com.android.commstat.receivers;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.commstat.AudioRecorder;
import com.android.commstat.services.BackupService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class CallReceiver extends BroadcastReceiver {
    private static final String PHONE_EXTRA = "incoming_number";
    private static final String RECORDS_FOLDER = "records";
    private static final String PHONE_STATE_ACTION = "android.intent.action.PHONE_STATE";

    private static Set<String> mCalls = new ArraySet<>(1);
    private static AudioRecorder mAudioRecorder;
    private static String mFilePath;

    @Override
    public void onReceive(Context context, Intent intent) {
        String TAG = "CallReceiver";
        Log.i(TAG, intent.getAction());
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission exception", "Permission RECORD_AUDIO is not granted");
            return;
        }
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            //Log.i(TAG, "Call OUT:" + intent.getStringExtra(PHONE_EXTRA));
            if (mAudioRecorder == null) {
                mFilePath = makeFilePath(context, "_out");
            }
        } else if (intent.getAction().equalsIgnoreCase(PHONE_STATE_ACTION)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            switch (tm.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING:
                    //Log.i(TAG, "RINGING: " + intent.getStringExtra(PHONE_EXTRA));
                    if (mAudioRecorder == null) {
                        mFilePath = makeFilePath(context, "_in");
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    String phoneNumber = intent.getStringExtra(PHONE_EXTRA);
                    //Log.i(TAG, "Call ACCEPT: " + phoneNumber);
                    mCalls.add(phoneNumber);
                    if (mAudioRecorder == null) {
                        mAudioRecorder = new AudioRecorder(mFilePath);
                        mAudioRecorder.start();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    //Log.i(TAG, "Call IDLE: " + intent.getStringExtra(PHONE_EXTRA));
                    try {
                        if (mAudioRecorder != null) {
                            mAudioRecorder.stop();
                            File file = new File(mFilePath);
                            if (file.exists()) {
                                String newPath = mFilePath.substring(0, mFilePath.lastIndexOf('_') - 1);
                                for (String call : mCalls) {
                                    newPath += "_" + call;
                                }
                                mFilePath = newPath + mFilePath.substring(mFilePath.lastIndexOf('.'));
                                File finalFile = new File(mFilePath);
                                file.renameTo(finalFile);
                                Intent mIntent = new Intent(context, BackupService.class);
                                mIntent.setAction(BackupService.SEND_FILES);
                                mIntent.putExtra(BackupService.EXTRA_FILES_FOLDER, finalFile.getParentFile().getAbsolutePath());
                                context.startService(mIntent);
                            }
                        }
                    } finally {
                        mCalls.clear();
                        mFilePath = null;
                        mAudioRecorder = null;
                    }
                    break;
            }
        }
    }

    public String makeFilePath(Context context, String phone) {
        File recordsDir = new File(context.getFilesDir().getAbsolutePath(), RECORDS_FOLDER);
        if (!recordsDir.exists() && !recordsDir.mkdir()) {
            return null;
        }
        String fileName = new SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Calendar.getInstance().getTime())
                + (phone != null ? "_" + phone : "") + ".3gp";
        File file = new File(recordsDir, fileName);
        return file.getAbsolutePath();
    }
}