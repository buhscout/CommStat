package com.android.commstat;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;

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
        if (mRecorder != null) {
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
            recorder.start();
            mRecorder = recorder;
        } catch (Exception e) {
            mRecorder.release();
            e.printStackTrace();
        }
    }

    public void stop() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mRecorder = null;
            }
        }
    }

}
