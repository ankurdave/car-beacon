package com.ankurdave.carbeacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class BatteryNotifier extends BroadcastReceiver {
    private static final String TAG = "BatteryNotifier";
    public void onReceive(Context context, Intent intent) {
      SmsManager.getDefault().sendTextMessage("6507017705", null, "Low battery", null, null);
    }
}
