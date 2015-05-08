package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

public class AppLockService extends Service {
    public static String token = null;
    static ArrayList<String> blockedApps = null;
    static String deviceid = null;
    private final IBinder mBinder = new MyBinder();
    int check = 0;

    public AppLockService() {
    }

    static void schedule(Context context) {
        final Intent intent = new Intent(context, AppLockService.class);
        final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        Calendar c = new GregorianCalendar();
        c.add(Calendar.DAY_OF_YEAR, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);

        alarm.setRepeating(AlarmManager.RTC, c.getTimeInMillis(),
                10 * 1000, pending);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        deviceid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        URL url = null;
        try {
            url = new URL("http://54.148.66.118/cadmgmt/apps.php?deviceid=" + deviceid);
            new AppsLocksServiceTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url);

            ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> RunningTask = mActivityManager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo ar = RunningTask.get(0);
            String activity = ar.topActivity.getClassName();

            if (blockedApps != null)
                for (int i = 0; i < blockedApps.size(); i++) {
                    if (activity.startsWith(blockedApps.get(i))) {
                        Intent lockIntent = new Intent(this, LockScreen.class);
                        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        lockIntent.putExtra("name", activity);
                        startActivity(lockIntent);
                    }
                }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.i("LocalAppLockService", "Received start id " + startid + ": " + intent);
        return START_STICKY;
    }

    public class MyBinder extends Binder {
        AppLockService getService() {
            return AppLockService.this;
        }
    }

    public class AppsLocksServiceTask extends AsyncTask<URL, Void, String> {
        String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(URL... urls) {
            if (isNetworkConnected() == true) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet method = new HttpGet(urls[0].toString());
                method.addHeader("auth-token", token);
                try {
                    HttpResponse response = httpclient.execute(method);
                    if (response != null) {
                        if (response.getStatusLine().getStatusCode() == 200) {
                            HttpEntity entity = response.getEntity();
                            InputStream inputStream = entity.getContent();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                            StringBuilder sb = new StringBuilder();
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line + "\n");
                            }
                            result = sb.toString();
                            Log.e("Result", result);
                            JSONObject object = new JSONObject(result);
                            ArrayList<String> blockedAppList = new ArrayList<String>();
                            Iterator<String> iterator = object.keys();
                            while (iterator.hasNext()) {
                                String appname = iterator.next();
                                int value = Integer.valueOf(object.getString(appname));
                                if (value == 1)
                                    blockedAppList.add(appname);
                            }
                            if (blockedApps == null)
                                blockedApps = new ArrayList<String>();

                            synchronized (blockedApps) {
                                blockedApps.clear();
                                PackageManager packageManager = getPackageManager();
                                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                                List<ResolveInfo> appList = packageManager.queryIntentActivities(mainIntent, 0);
                                Collections.sort(appList, new ResolveInfo.DisplayNameComparator(packageManager));
                                List<PackageInfo> packs = packageManager.getInstalledPackages(0);
                                for (int i = 0; i < packs.size(); i++) {
                                    PackageInfo p = packs.get(i);
                                    ApplicationInfo a = p.applicationInfo;
                                    if ((a.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                                        String name = a.loadLabel(getPackageManager()).toString();
                                        if (blockedAppList.contains(name)) {
                                            blockedApps.add(p.packageName);
                                        }
                                    }
                                }
                            }
                            Log.e("Result", result);
                        } else {
                            Log.e("response is not 200 in app lock Service", String.valueOf(response.getStatusLine().getStatusCode()));
                        }
                    } else {
                        Log.e("response is null", "null");
                    }
                } catch (ClientProtocolException e) {
                    Log.e("Exception CP", e.getMessage());
                } catch (IOException e) {
                    Log.e("Exception IO", e.getMessage());
                } catch (Exception e) {
                    Log.e("Errorr", result);
                    e.printStackTrace();
                }
            } else {

            }
            return result;
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
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            stopSelf();
        }
    }
}
