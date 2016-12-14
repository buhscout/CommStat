package com.android.commstat;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Sms implements Serializable {
    String From;
    String Text;
    Date Date;

    @Override
    public String toString() {
        String dateString = null;
        if(Date != null) {
            dateString = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date);
        }
        return String.format("%s: %s: %s", dateString, From, Text);
    }
}
