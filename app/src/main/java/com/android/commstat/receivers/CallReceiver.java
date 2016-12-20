package com.android.commstat.receivers;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.commstat.AudioRecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CallReceiver extends BroadcastReceiver {
    private static final String RECORDS_FOLDER = "records";
    private static boolean mIsIncomingCall;
    private static AudioRecorder mAudioRecorder;
    private static final String PHONE_STATE_ACTION = "android.intent.action.PHONE_STATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String TAG = "CallReceiver";
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission exception", "Permission RECORD_AUDIO is not granted");
            return;
        }
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            mIsIncomingCall = false;
            String mPhoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            String filePath = makeFilePath(context, mPhoneNumber + "_out");
            if(filePath == null) {
                return;
            }
            mAudioRecorder = new AudioRecorder(MediaRecorder.AudioSource.VOICE_CALL, filePath);
            Log.i(TAG, "call OUT:" + mPhoneNumber);
        } else if (intent.getAction().equalsIgnoreCase(PHONE_STATE_ACTION)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            switch (tm.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING:
                    mIsIncomingCall = true;
                    String phoneNumber = intent.getStringExtra("incoming_number");
                    Log.i(TAG, "RINGING :" + phoneNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (mIsIncomingCall) {
                        phoneNumber = intent.getStringExtra("incoming_number");
                        String filePath = makeFilePath(context, phoneNumber + "_in");
                        if(filePath == null) {
                            return;
                        }
                        if(mAudioRecorder != null) {
                            mAudioRecorder.stop();
                        }
                        mAudioRecorder = new AudioRecorder(MediaRecorder.AudioSource.VOICE_CALL, filePath);
                        mAudioRecorder.start();
                        Log.i(TAG, "incoming ACCEPT :" + phoneNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mIsIncomingCall) {
                        if(mAudioRecorder != null) {
                            mAudioRecorder.stop();
                        }
                        mIsIncomingCall = false;
                        Log.i(TAG, "incoming IDLE");
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
        String fileName = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime())
                + (phone != null ? "_" + phone : "") + ".3gp";
        File file = new File(recordsDir, fileName);
        return file.getAbsolutePath();
    }
}