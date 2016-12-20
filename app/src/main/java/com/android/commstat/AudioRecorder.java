package com.android.commstat;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mRecorder;
    private String mFilePath;
    private final int mAudioSource;

    public AudioRecorder(int audioSource, String filePath) {
        mFilePath = filePath;
        mAudioSource = audioSource;
    }

    public void start() {
        if(mRecorder != null) {
            mRecorder.stop();
        }
        File directory = new File(mFilePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Path to file could not be created.");
            return;
        }
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(mAudioSource);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(mFilePath);
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        mRecorder = recorder;
        mRecorder.start();
    }

    public void stop() {
        if(mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

}
