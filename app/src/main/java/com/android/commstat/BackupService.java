package com.android.commstat;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Scout on 12.12.2016.
 */
public class BackupService extends IntentService {

    public static final String SMS = "sms";

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
        final String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(message.Date.getTime());

        Map<String, String> params = new ArrayMap<>();
        params.put("from_phone", message.From);
        params.put("text", message.Text);
        params.put("date", dateStr);
        String response = executeQuery(params);
        JSONObject jsonObject = new JSONObject(response);
        if(jsonObject.getInt("success") != 1) {
            throw new Exception(jsonObject.getString("message"));
        }
    }

    private void executeQuery(Map<String, String> params) throws Exception {
        InputStream in = null;
        try {
            HttpURLConnection connection;
            String paramsStr = "";
            for(Map.Entry<String, String> entry : params.entrySet()) {
                if(!TextUtils.equals(paramsStr, "")) {
                    paramsStr += "&";
                }
                paramsStr += String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            Log.d("Send sms", paramsStr);
            connection = (HttpURLConnection) new URL(mUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDefaultUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(paramsStr.getBytes());
            os.flush();
            os.close();
            in = new BufferedInputStream(connection.getInputStream());
        } finally {
            in.close();
        }
    }
}
