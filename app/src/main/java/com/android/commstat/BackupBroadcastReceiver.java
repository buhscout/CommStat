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

public class BackupBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if("android.provider.Telephony.SMS_RECEIVED".compareToIgnoreCase(intent.getAction()) == 0) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission exception", "Permission READ_SMS is not granted");
                return;
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission exception", "Permission READ_PHONE_STATE is not granted");
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
                    message = new Sms();
                    Calendar date = Calendar.getInstance();
                    date.setTimeInMillis(sms.getTimestampMillis());
                    message.Date = date.getTime();
                    message.From = sms.getOriginatingAddress();
                    messagesMap.put(sms.getOriginatingAddress(), message);
                }
                message.Text = message.Text == null ? sms.getMessageBody() : message.Text + sms.getMessageBody();
            }
            for (Sms message : messagesMap.values()) {
                Intent mIntent = new Intent(context, BackupService.class);
                mIntent.putExtra(BackupService.SMS, message);
                context.startService(mIntent);
            }
        }
    }
}
