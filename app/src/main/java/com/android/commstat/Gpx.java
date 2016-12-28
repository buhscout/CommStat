package com.android.commstat;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.dropbox.core.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Gpx {
    private static final String TAG = Gpx.class.getName();
    private File mFile;
    private File mTempFile;
    private Context mContext;

    public Gpx(Context context, File file) throws IOException {
        mContext = context;
        mFile = file;
        mTempFile = new File(mContext.getFilesDir().getAbsolutePath(), "tmp" + file.getName());
        if (!mTempFile.exists()) {
            if (!mTempFile.getParentFile().exists()) {
                mTempFile.getParentFile().mkdirs();
            }
            if(!mTempFile.createNewFile()) {
                throw new IOException("Cant create file");
            }
        } else {
            clean();
            if(!mTempFile.createNewFile()) {
                throw new IOException("Cant create file");
            }
        }
    }

    public boolean prepareGpx() {
        if (!mTempFile.exists()) {
            return false;
        }
        try {
            copy(mTempFile, mFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!mFile.exists() || mFile.length() == 0) {
            return false;
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(mFile, true);
            writer.append("</trkseg></trk></gpx>");
        } catch (IOException e) {
            Log.e(TAG, "Error Writting Path", e);
            return false;
        } finally {
            IOUtil.closeQuietly(writer);
        }
        return true;
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            IOUtil.closeQuietly(in);
            IOUtil.closeQuietly(out);
        }
    }

    public void clean() {
        if (mTempFile.exists()) {
            mTempFile.delete();
        }
    }

    public void addPoint(Location point) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        FileWriter writer = null;
        try {
            writer = new FileWriter(mTempFile, true);
            if(mTempFile.length() == 0) {
                writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n");
                writer.append("<name>").append(df.format(Calendar.getInstance().getTime())).append("</name><trkseg>\n");
            }
            writer.append(String.format(Locale.getDefault(), "<trkpt lat=\"%s\" lon=\"%s\"><time>%s</time></trkpt>\n", point.getLatitude(), point.getLongitude(), df.format(new Date(point.getTime()))));
        } catch (IOException e) {
            Log.e(TAG, "Error Writting Path",e);
        } finally {
            IOUtil.closeQuietly(writer);
        }
    }
}