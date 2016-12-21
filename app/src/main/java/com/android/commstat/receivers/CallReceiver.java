package com.android.commstat.receivers;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.commstat.AudioRecorder;
import com.android.commstat.model.Call;
import com.android.commstat.services.BackupService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class CallReceiver extends BroadcastReceiver {
    private static final String RECORDS_FOLDER = "records";
    private static final String PHONE_STATE_ACTION = "android.intent.action.PHONE_STATE";

    private static Map<String, Call> mCalls = new ArrayMap<>(1);

    @Override
    public void onReceive(Context context, Intent intent) {
        String TAG = "CallReceiver";
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission exception", "Permission RECORD_AUDIO is not granted");
            return;
        }
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            String filePath = makeFilePath(context, phoneNumber + "_out");
            if (filePath == null) {
                return;
            }
            mCalls.put(phoneNumber, new Call(filePath));
            Log.i(TAG, "Call OUT:" + phoneNumber);
        } else if (intent.getAction().equalsIgnoreCase(PHONE_STATE_ACTION)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            switch (tm.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING:
                    String phoneNumber = intent.getStringExtra("incoming_number");
                    String filePath = makeFilePath(context, phoneNumber + "_in");
                    mCalls.put(phoneNumber, new Call(filePath));
                    Log.i(TAG, "RINGING: " + phoneNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    phoneNumber = intent.getStringExtra("incoming_number");
                    Call call = mCalls.get(phoneNumber);
                    if (call == null || call.getFilePath() == null) {
                        return;
                    }
                    AudioRecorder audioRecorder = call.getAudioRecorder();
                    if (audioRecorder != null) {
                        audioRecorder.stop();
                    }
                    audioRecorder = new AudioRecorder(MediaRecorder.AudioSource.MIC, call.getFilePath());
                    audioRecorder.start();
                    call.setAudioRecorder(audioRecorder);
                    Log.i(TAG, "Call ACCEPT: " + phoneNumber);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    phoneNumber = intent.getStringExtra("incoming_number");
                    call = mCalls.remove(phoneNumber);
                    if (call != null && call.getAudioRecorder() != null) {
                        call.getAudioRecorder().stop();

                        Intent mIntent = new Intent(context, BackupService.class);
                        mIntent.setAction(BackupService.SEND_FILES);
                        mIntent.putExtra(BackupService.FILES_FOLDER, new File(call.getFilePath()).getParentFile().getAbsolutePath());
                        context.startService(mIntent);
                    }
                    Log.i(TAG, "Call IDLE: " + phoneNumber);
                    break;
            }
        }
    }

    public String makeFilePath(Context context, String phone) {
        File recordsDir = new File(context.getFilesDir().getAbsolutePath(), RECORDS_FOLDER);
        if (!recordsDir.exists() && !recordsDir.mkdir()) {
            return null;
        }
        String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Calendar.getInstance().getTime())
                + (phone != null ? "_" + phone : "") + ".3gp";
        File file = new File(recordsDir, fileName);
        return file.getAbsolutePath();
    }
}