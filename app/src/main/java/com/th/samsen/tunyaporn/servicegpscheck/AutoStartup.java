package com.th.samsen.tunyaporn.servicegpscheck;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

public class AutoStartup extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void repeat(){
        Log.d("Tag", "On Loop 5 minutes");
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceString = myDevice.getName();
        Log.d("Tag", "DEVICE ==> " + deviceString);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                //performe the deskred task


                repeat();
            }
        }, 5000);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Tag", "Open Device");
        repeat();


    }
}
