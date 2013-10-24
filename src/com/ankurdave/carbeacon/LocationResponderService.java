package com.ankurdave.carbeacon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;

public class LocationResponderService extends Service {
    private static final String TAG = "LocationResponderService";
    private ServiceHandler s;

    private final class ServiceHandler extends Handler {
        private LocationManager lm = null;
        private LocationListener ll = null;
        private PowerManager pm = null;
        private WakeLock wl = null;
        private String dest = null;
        private boolean subscribe = false;
        private boolean test = false;
        public ServiceHandler(Looper l_, LocationManager lm, PowerManager pm) {
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
            this.pm = pm;
        }
        public void handleMessage(Message m) {
            dest = (String) m.obj;
            subscribe = m.arg1 == 1;
            test = m.arg2 == 1;

            String confirmation = String.format("%s location updates", subscribe ? "Starting" : "Stopping");
            Log.i(TAG, confirmation + " for " + dest);
            if (!test) {
                SmsManager.getDefault().sendTextMessage(dest, null, confirmation, null, null);
            }

            if (subscribe) {
                if (wl == null) {
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                }
                wl.acquire();
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 0, ll);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300000, 0, ll);
            } else {
                if (wl != null) {
                    wl.release();
                }
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
            (PowerManager) this.getSystemService(Context.POWER_SERVICE));
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
