package com.ankurdave.carbeacon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.net.wifi.WifiManager;

public class LocationResponderService extends Service {
    private static final String TAG = "LocationResponderService";
    private ServiceHandler s;

    private final class ServiceHandler extends Handler {
        private LocationManager lm = null;
        private LocationListener ll = null;
        private WifiManager wm = null;
        private String dest = null;
        public ServiceHandler(Looper l_, LocationManager lm, WifiManager wm) {
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
            this.wm = wm;
        }
        public void handleMessage(Message m) {
            dest = (String) m.obj;
            if (m.arg1 == 1) {
                Log.i(TAG, "Received subscribe request for " + dest);
                SmsManager.getDefault().sendTextMessage(dest, null, "Starting location updates", null, null);
                wm.setWifiEnabled(true);
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);
            } else {
                Log.i(TAG, "Received unsubscribe request for " + dest);
                lm.removeUpdates(ll);
                wm.setWifiEnabled(false);
                SmsManager.getDefault().sendTextMessage(dest, null, "Stopped location updates", null, null);
            }
        }
        private void sendLocation(Location l) {
            String loc = String.format("%s: %f, %f", l.getProvider(), l.getLatitude(), l.getLongitude());
            if (l.hasAccuracy()) {
                loc += " +- " + l.getAccuracy();
            }
            Log.i(TAG, "Sending location " + loc + " to " + dest);
            SmsManager.getDefault().sendTextMessage(dest, null, loc, null, null);
        }
    }

    public void onCreate() {
        HandlerThread thread = new HandlerThread("LocationResponderService");
        thread.start();
        s = new ServiceHandler(
            thread.getLooper(),
            (LocationManager) this.getSystemService(Context.LOCATION_SERVICE),
            (WifiManager) this.getSystemService(Context.WIFI_SERVICE));
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = s.obtainMessage();
        msg.obj = intent.getStringExtra("dest");
        msg.arg1 = intent.getBooleanExtra("subscribe", false) ? 1 : 0;
        s.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
    }
}
