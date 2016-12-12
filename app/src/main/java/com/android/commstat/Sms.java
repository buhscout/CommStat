package com.android.commstat;

import java.io.Serializable;
import java.util.Date;

class Sms implements Serializable {
    String From;
    String Text;
    Date Date;
}
