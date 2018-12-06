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
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.concurrent.Semaphore;
import java.net.InetSocketAddress;
import android.net.LocalSocketAddress;
import android.net.LocalSocket;
import java.net.Socket;
import android.util.Log;

public class SerialNumberPreferenceController extends PreferenceController {
    private String TAG = "Settings_Status";

    private static final String KEY_SERIAL_NUMBER = "serial_number";

    private String mSerialNumber;

    public SerialNumberPreferenceController(Context context) {
        super(context);
        // //////////////////////////////////////////////
        // MF0300

        Log.i(TAG, "*** TcpClientThread BEFORE");
        tcpClientThread = new TcpClientThread();
        tcpClientThread.start();
        Log.i(TAG, "*** TcpClientThread AFTER");

        try {
                sema_sync.acquire();
        } catch(Exception e) {
                Log.e(TAG, "*** UNABLE GET HW:SERIALNO EXCEPTION" + e.getMessage());
                e.printStackTrace();
        }
        // //////////////////////////////////////////////

        //String serial = Build.SERIAL;
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

    class TcpClientThread extends Thread {
        TcpClientThread() {
        }

        public void run() {
                try {
                        String sernum="cmd::get::";

                        LocalSocket socket;
                        LocalSocketAddress localsocketaddr;
                        InputStream is;
                        OutputStream os;
                        DataInputStream dis;
                        PrintStream ps;
                        BufferedReader br;
                        Log.i(TAG, "*** HWSER LocalSocket");
                        socket = new LocalSocket();
                        localsocketaddr = new LocalSocketAddress("serialnumber",LocalSocketAddress.Namespace.RESERVED);
                        socket.connect(localsocketaddr);
                        is = socket.getInputStream();
                        os = socket.getOutputStream();
                        dis = new DataInputStream(is);
                        ps = new PrintStream(os);

                        Log.i(TAG, "*** HWSER getBytes");

                        byte[] msg1 = sernum.getBytes();
                        ps.write(msg1);
                        InputStream in = socket.getInputStream();
                        br = new BufferedReader(new InputStreamReader(in));
                        if (sernum.endsWith("get::")) {
                                StringBuffer strBuffer = new StringBuffer();
                                char c = (char) br.read();
                                while (c != 0xffff) {
                                        strBuffer.append(c);
                                        c=(char) br.read();
                                }
                                mSerialNumber=strBuffer.toString();
                                sema_sync.release();
                        }

			Log.i(TAG, "*** HWSER getBytes DONE serial:" + mSerialNumber);

                        dis.close();
                        ps.close();
                        is.close();
                        os.close();
                        socket.close();
                } catch (Exception e) {
                        Log.e(TAG, "*** ENABLE GET HW:SERIAL FROMSOCKET EXCEPTION " + e.getMessage());
                        e.printStackTrace();

                }
        }
    }

    private TcpClientThread tcpClientThread = null;
    private final Semaphore sema_sync = new Semaphore(0, true);
}
