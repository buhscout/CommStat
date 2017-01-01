package com.android.commstat;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mRecorder;
    private String mFilePath;

    public AudioRecorder(String filePath) {
        mFilePath = filePath;
    }

    public void start() throws IOException {
        if (mRecorder != null) {
            mRecorder.stop();
        }
        File directory = new File(mFilePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Path to file could not be created.");
            return;
        }
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(mFilePath);
        try {
            recorder.prepare();
            mRecorder = recorder;
            recorder.start();
        } catch (Exception e) {
            if(mRecorder != null) {
                mRecorder.release();
                mRecorder = null;
            }
            throw e;
        }
    }

    public void stop() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.reset();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(mRecorder != null) {
                    mRecorder.release();
                }
                mRecorder = null;
            }
        }
    }

}
