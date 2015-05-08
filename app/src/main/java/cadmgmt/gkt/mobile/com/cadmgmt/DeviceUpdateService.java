package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class DeviceUpdateService extends Service implements LocationListener {
    static boolean flag = false;
    static String token = null;
    static String device_id;
    static String groupname = null;
    static double latitude = 0;
    static double longitude = 0;
    static int signal_strength = 0;
    private final IBinder mBinder = new MyBinder();
    PhoneStateListener myListener = new MyPhoneStateListener();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static void schedule(Context context) {
        final Intent intent = new Intent(context, DeviceUpdateService.class);
        final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        Calendar c = new GregorianCalendar();
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);

        alarm.setRepeating(AlarmManager.RTC, c.getTimeInMillis(),
                5 * 30 * 1000, pending);
    }

    public static void setToken(String value) {
        token = new String(value);
    }

    public static void setGroupname(String value) {
        groupname = new String(value);
    }

    @Override
    public void onCreate() {
        if (token == null || flag || groupname == null)
            stopSelf();
        device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3500, 10.0f, this);

        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(myListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        if (location == null) {
            latitude = 0;
            longitude = 0;
        } else {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
        URL url = null;

        try {
            url = new URL("http://54.148.66.118/cadmgmt/devices.php");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        new UpdateDeviceTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.i("LocalService", "Received start id " + startid + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public class MyBinder extends Binder {
        DeviceUpdateService getService() {
            return DeviceUpdateService.this;
        }
    }

    public class UpdateDeviceTask extends AsyncTask<URL, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(URL... urls) {
            if (isNetworkConnected() == true) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPut method = new HttpPut(urls[0].toString());

                method.addHeader("auth-token", token);
                JSONObject object = new JSONObject();

                try {
                    object.put("device_id", device_id);
                    object.put("groupname", groupname);
                    Calendar c = Calendar.getInstance();
                    object.put("time_stamp", dateFormat.format(c.getTime()));
                    object.put("memory_available", new File(getApplicationContext().getFilesDir().getAbsoluteFile().toString()).getFreeSpace() / 1048576);
                    object.put("latitude", latitude);
                    object.put("longitude", longitude);
                    object.put("signal_strength", signal_strength);

                    List<PackageInfo> PackList = getPackageManager().getInstalledPackages(0);
                    JSONArray app_array = new JSONArray();
                    for (int i = 0; i < PackList.size(); i++) {
                        PackageInfo PackInfo = PackList.get(i);
                        if ((PackInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            app_array.put(PackInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                        }
                    }
                    object.put("apps", app_array);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    method.setEntity(new StringEntity(object.toString(), "UTF8"));
                    method.addHeader("Content-type", "application/json");

                    HttpResponse response = httpclient.execute(method);
                    if (response != null) {
                        if (response.getStatusLine().getStatusCode() == 200) {

                        } else {
                            String msg;
                            HttpEntity entity = response.getEntity();
                            InputStream inputStream = entity.getContent();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                            StringBuilder sb = new StringBuilder();
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line + "\n");
                            }
                            msg = sb.toString();
                        }
                    }
                } catch (ClientProtocolException e) {
                    Log.d("Exception CP", e.getMessage());

                } catch (IOException e) {
                    Log.d("Exception IO", e.getMessage());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {

            }
            return null;
        }

        public boolean isNetworkConnected() {
            ConnectivityManager status = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo network = status.getActiveNetworkInfo();

            if (network != null && network.isConnected()) {
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Void value) {
            super.onPostExecute(value);
            stopSelf();
        }
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            signal_strength = signalStrength.getGsmSignalStrength();
        }
    }
}


