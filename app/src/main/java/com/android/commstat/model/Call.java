package com.android.commstat.model;

import com.android.commstat.AudioRecorder;

public class Call {
    private String mFilePath;
    private AudioRecorder mAudioRecorder;

    public Call(String filePath) {
        this.mFilePath = filePath;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public AudioRecorder getAudioRecorder() {
        return mAudioRecorder;
    }

    public void setAudioRecorder(AudioRecorder audioRecorder) {
        mAudioRecorder = audioRecorder;
    }
}
