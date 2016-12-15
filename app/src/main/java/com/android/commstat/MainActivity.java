package com.android.commstat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 111;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        OutgoingMessageObserver.register(this);
    }

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new LinkedList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            Map<String, Integer> perms = new HashMap<>(2);
            perms.put(Manifest.permission.READ_SMS, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }
                if (perms.get(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)
                            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.permissions_required_message))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        checkAndRequestPermissions();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        showErrorPermissionsMessage();
                                    }
                                })
                                .create()
                                .show();
                    } else {
                        showErrorPermissionsMessage();
                    }
                }
            }
        }
    }

    private void showErrorPermissionsMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Warning!")
                .setMessage(getString(R.string.permissions_required_message) + getString(R.string.permissions_go_to_settings))
                .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.runFinalization();
                        System.exit(0);
                    }
                })
                .create()
                .show();
    }
}
