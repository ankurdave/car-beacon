package com.ankurdave.carbeacon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;

public class LocationResponderService extends Service {
    private static final String TAG = "LocationResponderService";
    private ServiceHandler s;

    private final class ServiceHandler extends Handler {
        private LocationManager lm;
        private LocationListener ll;
        private Context c;
        private String dest = "";
        private boolean subscribe = false;
        private boolean test = true;
        public ServiceHandler(Looper l_, LocationManager lm, Context c) {
            super(l_);
            this.lm = lm;
            this.ll = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        sendLocation(location);
                    }
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    public void onProviderEnabled(String provider) {}
                    public void onProviderDisabled(String provider) {}
                };
            this.c = c;
        }
        public void handleMessage(Message m) {
            dest = (String) m.obj;
            subscribe = m.arg1 == 1;
            test = m.arg2 == 1;

            Intent battery = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int rawlevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            double scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            double level = (rawlevel >= 0 && scale > 0) ? rawlevel / scale : -1;

            String confirmation = String.format(
                "%s location updates (%2f%% battery)",
                subscribe ? "Starting" : "Stopping",
                level * 100);
            Log.i(TAG, confirmation + " for " + dest);
            if (!test) {
                SmsManager.getDefault().sendTextMessage(dest, null, confirmation, null, null);
            }

            if (subscribe) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 0, ll);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300000, 0, ll);
            } else {
                lm.removeUpdates(ll);
            }
        }
        private void sendLocation(Location l) {
            String loc = String.format("%s: %f, %f", l.getProvider(), l.getLatitude(), l.getLongitude());
            if (l.hasAccuracy()) {
                loc += " +- " + l.getAccuracy();
            }
            Log.i(TAG, "Sending location " + loc + " to " + dest);
            if (!test) {
                SmsManager.getDefault().sendTextMessage(dest, null, loc, null, null);
            }
        }
    }

    public void onCreate() {
        HandlerThread thread = new HandlerThread("LocationResponderService");
        thread.start();
        s = new ServiceHandler(
            thread.getLooper(),
            (LocationManager) this.getSystemService(Context.LOCATION_SERVICE),
            this.getApplicationContext());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = s.obtainMessage();
        msg.obj = intent.getStringExtra("dest");
        msg.arg1 = intent.getBooleanExtra("subscribe", false) ? 1 : 0;
        msg.arg2 = intent.getBooleanExtra("test", false) ? 1 : 0;
        s.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
    }
}
