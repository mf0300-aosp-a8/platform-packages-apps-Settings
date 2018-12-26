/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Build;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

import android.net.LocalSocketAddress;
import android.net.LocalSocket;
import android.util.Log;

public class SerialNumberPreferenceController extends PreferenceController {
    private static final String TAG = "Settings_Status";

    private static final String KEY_SERIAL_NUMBER = "serial_number";

    private String mSerialNumber;

    public SerialNumberPreferenceController(Context context) {
        super(context);
        // //////////////////////////////////////////////
        // MF0300
        mSerialNumber = readSerialNumber();
        Log.i(TAG, "*** serial:" + mSerialNumber);
    }

    @VisibleForTesting
    SerialNumberPreferenceController(Context context, String serialNumber) {
        super(context);
        mSerialNumber = serialNumber;
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(mSerialNumber);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_SERIAL_NUMBER);
        if (pref != null) {
            pref.setSummary(mSerialNumber);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SERIAL_NUMBER;
    }

    private static String readSerialNumber() {
        String serial = null;
        try {
            Log.i(TAG, "*** HWSER LocalSocket");
            LocalSocketAddress localsocketaddr = new LocalSocketAddress("serialnumber", LocalSocketAddress.Namespace.RESERVED);
            LocalSocket socket = new LocalSocket();
            socket.connect(localsocketaddr);

            PrintStream out = new PrintStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Log.i(TAG, "*** HWSER getBytes");
            String sernum = "cmd::get::";
            out.write(sernum.getBytes());

            if (sernum.endsWith("get::")) {
                StringBuffer strBuffer = new StringBuffer();
                char c = (char) in.read();
                while (c != 0xffff) {
                    strBuffer.append(c);
                    c = (char) in.read();
                }
                serial = strBuffer.toString();
            }

            Log.i(TAG, "*** HWSER getBytes DONE serial: " + serial);

            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "*** ENABLE GET HW:SERIAL FROMSOCKET EXCEPTION " + e.getMessage());
            e.printStackTrace();
        }
        return serial;
    }
}
