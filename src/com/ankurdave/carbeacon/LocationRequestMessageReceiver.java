package com.ankurdave.carbeacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

public class LocationRequestMessageReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationRequestMessageReceiver";
    public void onReceive(Context context, Intent intent) {
        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        for (int i = 0; i < pdus.length; i++) {
            SmsMessage m = SmsMessage.createFromPdu((byte[]) pdus[i]);
            if (m.getMessageBody().equalsIgnoreCase("locate")) {
                Log.i(TAG, "Got locate request: " + m.getMessageBody());
                Intent in = new Intent(context.getApplicationContext(), LocationResponderService.class);
                in.putExtra("dest", m.getOriginatingAddress());
                context.getApplicationContext().startService(in);
            }
        }
    }
}
