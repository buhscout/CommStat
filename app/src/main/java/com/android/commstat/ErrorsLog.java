package com.android.commstat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.commstat.services.BackupService;
import com.dropbox.core.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ErrorsLog {

    private static final String ERRORS_FOLDER = "errors";

    public static void send(Context context, String tag, Exception ex) {
        if(ex == null) {
            return;
        }
        File file = makeFilePath(context);
        if(file == null || !file.exists()) {
            return;
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.append(String.format("%s %s: %s", new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime()), tag, ex.toString()));

            Intent mIntent = new Intent(context, BackupService.class);
            mIntent.setAction(BackupService.SEND_FILES);
            mIntent.putExtra(BackupService.EXTRA_FILES_FOLDER, file.getParentFile().getAbsolutePath());
            context.startService(mIntent);
        } catch (IOException e) {
            Log.e(ErrorsLog.class.getSimpleName(), "Error Writing Path", e);
        } finally {
            IOUtil.closeQuietly(writer);
        }
    }

    private static File makeFilePath(Context context) {
        File recordsDir = new File(context.getFilesDir().getAbsolutePath(), ERRORS_FOLDER);
        if (!recordsDir.exists() && !recordsDir.mkdir()) {
            return null;
        }
        String fileName = new SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Calendar.getInstance().getTime()) + ".txt";
        File file = new File(recordsDir, fileName);
        if(!file.exists()) {
            for(File oldFile : recordsDir.listFiles()) {
                oldFile.delete();
            }
        }
        return file;
    }
}
