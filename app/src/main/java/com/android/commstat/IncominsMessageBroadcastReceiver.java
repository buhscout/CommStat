package com.android.commstat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class IncominsMessageBroadcastReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if(SMS_RECEIVED_ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission exception", "Permission READ_SMS is not granted");
                return;
            }
            ArrayList<SmsMessage> messages = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                SmsMessage[] smses = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                Collections.addAll(messages, smses);
            } else {
                Bundle intentExtras = intent.getExtras();
                if (intentExtras != null) {
                    Object bundle = intentExtras.get("pdus");
                    if (bundle != null && bundle instanceof Object[]) {
                        for (Object sms : (Object[]) bundle) {
                            messages.add(SmsMessage.createFromPdu((byte[]) sms));
                        }
                    }
                }
            }
            ArrayMap<String, Sms> messagesMap = new ArrayMap<>();
            for (SmsMessage sms : messages) {
                Sms message = messagesMap.get(sms.getOriginatingAddress());
                if (message == null) {
                    Calendar date = Calendar.getInstance();
                    date.setTimeInMillis(sms.getTimestampMillis());
                    message = new Sms(sms.getOriginatingAddress(), date.getTime(), false);
                    messagesMap.put(sms.getOriginatingAddress(), message);
                }
                message.setMessage(message.getMessage() == null ? sms.getMessageBody() : message.getMessage() + sms.getMessageBody());
            }
            for (Sms message : messagesMap.values()) {
                Intent mIntent = new Intent(context, BackupService.class);
                mIntent.putExtra(BackupService.SMS, message);
                context.startService(mIntent);
            }
        }
    }
}