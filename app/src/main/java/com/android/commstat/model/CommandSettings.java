package com.android.commstat.model;

import android.text.TextUtils;

import com.android.commstat.enums.CommandType;

import java.util.ArrayList;
import java.util.List;

public class CommandSettings {
    private int mDuration = 0;
    private int mDelay = 0;
    private int mTotalDuration = 60;
    private CommandType mCommandType;

    public CommandSettings(String command) {
        if (command == null || command.length() < 2) {
            return;
        }
        switch (Character.toUpperCase(command.charAt(0))) {
            case 'M':
                mCommandType = CommandType.RecordAudio;
                break;
            case 'L':
                mCommandType = CommandType.RecordLocation;
                break;
            default:
                return;
        }
        if(command.length() < 3) {
            return;
        }
        List<Integer> args = parseCommandArgs(command.substring(2));
        if(args.size() > 0) {
            mTotalDuration = args.get(0);
            if(mTotalDuration < 0) {
                mTotalDuration = 0;
            }
        }
        if(args.size() > 1) {
            mDuration = args.get(1);
            if(mDuration < 0) {
                mDuration = 0;
            }
        }
        if(args.size() > 2) {
            mDelay = args.get(2);
            if(mDelay < 0) {
                mDelay = 0;
            }
        }
        if(mDelay == 0 && mDuration > 0) {
            mDelay = mDuration;
        }
    }

    public int getDuration() {
        return mDuration;
    }

    public int getDelay() {
        return mDelay;
    }

    public int getTotalDuration() {
        return mTotalDuration;
    }

    public CommandType getCommandType() {
        return mCommandType;
    }

    private List<Integer> parseCommandArgs(String command) {
        List<Integer> args = new ArrayList<>(1);
        if(command == null || command.length() == 0) {
            return args;
        }
        if (command.length() > 1) {
            String[] arguments = TextUtils.split(command, ":");
            for (String argument : arguments) {
                String value = "";
                for (int j = 0; j < argument.length(); j++) {
                    Character c = argument.charAt(j);
                    if (Character.isDigit(c)) {
                        value += c;
                    }
                    if (j == argument.length() - 1) {
                        try {
                            int iVal = Integer.decode(value);
                            if (Character.toUpperCase(c) == 'M') {
                                iVal *= 60;
                            } else if (Character.toUpperCase(c) == 'H') {
                                iVal *= 3600;
                            }
                            args.add(iVal * 1000);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
        return args;
    }
}