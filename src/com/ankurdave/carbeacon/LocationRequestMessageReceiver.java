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
            if (m.getMessageBody().trim().equalsIgnoreCase("start")) {
                Log.i(TAG, "Got subscribe request: " + m.getMessageBody());
                Intent in = new Intent(context.getApplicationContext(), LocationResponderService.class);
                in.putExtra("dest", m.getOriginatingAddress());
                in.putExtra("subscribe", true);
                context.getApplicationContext().startService(in);
            } else if (m.getMessageBody().trim().equalsIgnoreCase("stop")) {
                Log.i(TAG, "Got unsubscribe request: " + m.getMessageBody());
                Intent in = new Intent(context.getApplicationContext(), LocationResponderService.class);
                in.putExtra("dest", m.getOriginatingAddress());
                in.putExtra("subscribe", false);
                context.getApplicationContext().startService(in);
            }
        }
    }
}
