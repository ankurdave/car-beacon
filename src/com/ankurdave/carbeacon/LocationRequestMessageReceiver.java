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
            String command = m.getMessageBody().trim().toLowerCase();
            if (command.equals("start") || command.equals("teststart")
                || command.equals("stop") || command.equals("teststop")) {
                boolean subscribe = command.equals("start") || command.equals("teststart");
                boolean test = command.equals("teststart") || command.equals("teststop");
                Log.i(TAG, String.format(
                          "Got %s%ssubscribe request: %s",
                          test ? "test " : " ",
                          subscribe ? "" : "un",
                          m.getMessageBody()));
                Intent in = new Intent(context.getApplicationContext(), LocationResponderService.class);
                in.putExtra("dest", m.getOriginatingAddress());
                in.putExtra("subscribe", subscribe);
                in.putExtra("test", test);
                context.getApplicationContext().startService(in);
            }
        }
    }
}
