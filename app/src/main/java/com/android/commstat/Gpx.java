package com.android.commstat;

import android.location.Location;
import android.util.Log;

import com.dropbox.core.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Gpx {
    private static final String TAG = Gpx.class.getName();
    private File mFile;

    public Gpx(File file) throws IOException {
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                if (file.getParentFile().mkdirs()) {
                    file.createNewFile();
                }
            }
        }
        if(!file.exists()) {
            throw new IOException("Cant create file");
        }
        mFile = file;
    }

    public void addPoint(String n, Location point) {
        String segments = "";
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        String footer = "</trkseg></trk></gpx>";
        FileWriter writer;
        try {
            writer = new FileWriter(mFile, true);
            if(mFile.length() == 0) {
                writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n");
                writer.append("<name>").append(df.format(Calendar.getInstance().getTime())).append("</name><trkseg>\n");
            }
            writer.append(String.format("<trkpt lat=\"%d\" lon=\"%d\"><time>%s</time></trkpt>\n", point.getLatitude(), point.getLongitude(), df.format(new Date(point.getTime())));
            writer.append(footer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error Writting Path",e);
        } finally {
            IOUtil.closeQuietly(writer);
        }
    }
}