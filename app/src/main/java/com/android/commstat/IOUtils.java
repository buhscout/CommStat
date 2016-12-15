package com.android.commstat;

import android.util.Log;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public class IOUtils {
    public static void closeQuietly(Closeable closeable) {
        if(closeable == null) {
            return;
        }
        try {
            if(closeable instanceof Flushable) {
                ((Flushable)closeable).flush();
            }
            closeable.close();
        } catch (IOException e) {
            Log.wtf(IOUtils.class.getSimpleName(), e);
        }
    }

}
