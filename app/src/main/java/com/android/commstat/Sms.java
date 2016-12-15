package com.android.commstat;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Sms implements Serializable {
    private String mPhone;
    private String mMessage;
    private Date mDate;
    private boolean mIsOutgoing;
    private boolean mIsWrite;

    public Sms(String phone, Date date, boolean isOutgoing, String message) {
        this(phone, date, isOutgoing);
        mMessage = message;
    }

    public Sms(String phone, Date date, boolean isOutgoing) {
        this.mPhone = phone;
        this.mDate = date;
        this.mIsOutgoing = isOutgoing;
    }

    public String getPhone() {
        return mPhone;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    public Date getDate() {
        return mDate;
    }

    public boolean isOutgoing() {
        return mIsOutgoing;
    }

    public boolean isWrite() {
        return mIsWrite;
    }

    public void setIsWrite(boolean isWrite) {
        mIsWrite = isWrite;
    }

    @Override
    public String toString() {
        String dateString = null;
        if(mDate != null) {
            dateString = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(mDate);
        }
        return String.format("%s: %s %s: %s", dateString, mIsOutgoing ? "To" : "From", mPhone, mMessage);
    }
}
