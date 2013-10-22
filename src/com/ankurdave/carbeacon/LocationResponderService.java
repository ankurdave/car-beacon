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

public class LocationResponderService extends Service {
    private static final String TAG = "LocationResponderService";
    private ServiceHandler s;

    private final class ServiceHandler extends Handler {
        private LocationManager lm = null;
        private LocationListener ll = null;
        public ServiceHandler(Looper l_, LocationManager lm) {
            super(l_);
            this.lm = lm;
        }
        public void handleMessage(Message m) {
            final String dest = (String) m.obj;
            Log.i(TAG, "Received locate request for " + dest);
            SmsManager.getDefault().sendTextMessage(dest, null, "Received locate request", null, null);

            ll = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        sendLocation(dest, location);
                    }
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    public void onProviderEnabled(String provider) {}
                    public void onProviderDisabled(String provider) {}
                };
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);
        }
        private void sendLocation(String dest, Location l) {
            String loc = String.format("%f, %f", l.getLatitude(), l.getLongitude());
            if (l.hasAccuracy()) {
                loc += " +- " + l.getAccuracy();
            }
            Log.i(TAG, "Sending location " + loc + " to " + dest);
            // if (lm != null && ll != null) {
            //     lm.removeUpdates(ll);
            // }
            SmsManager.getDefault().sendTextMessage(dest, null, loc, null, null);
        }
    }

    public void onCreate() {
        HandlerThread thread = new HandlerThread("LocationResponderService");
        thread.start();
        s = new ServiceHandler(
            thread.getLooper(),
            (LocationManager) this.getSystemService(Context.LOCATION_SERVICE));
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = s.obtainMessage();
        msg.obj = intent.getStringExtra("dest");
        s.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
    }
}
