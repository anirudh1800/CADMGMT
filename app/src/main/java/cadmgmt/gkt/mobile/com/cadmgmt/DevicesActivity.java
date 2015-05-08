package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class DevicesActivity extends Activity implements AdapterView.OnItemClickListener {
    String[] items;
    String[] drawer_items;
    String PREFS = "USER_PREFS";
    MyAdapter myAdapter;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    String json = null;
    Context context;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        context = getApplicationContext();

        drawer_items = getResources().getStringArray(R.array.devices_drawer_items);
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_item, drawer_items));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String deviceid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String groupname = prefs.getString("groupname", null);

        try {
            URL url = new URL("http://54.148.66.118/cadmgmt/devices.php?deviceid=" + deviceid + "&groupname=" + groupname);
            new DevicesTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url);

            ListView lv = (ListView) findViewById(R.id.deviceList);
            lv.setOnItemClickListener(this);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.devices, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        if(mDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context context, String[] values) {
            super(context, R.layout.row_layout, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_layout, parent, false);
            TextView itemTxt = (TextView) convertView.findViewById(R.id.listitem);
            itemTxt.setText(items[position]);
            return convertView;
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        Intent toDeviceViewActivity = new Intent(this, DeviceViewActivity.class);
        toDeviceViewActivity.putExtra("index", arg2);
        toDeviceViewActivity.putExtra("json", json);
        startActivity(toDeviceViewActivity);
    }

    public class DevicesTask extends AsyncTask<URL, Void, String> {
        private ProgressDialog dialog;
        String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(DevicesActivity.this);
            dialog.setMessage("Fetching devices");
            dialog.show();
        }

        @Override
        protected String doInBackground(URL... urls) {
            if (isNetworkConnected() == true) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet method = new HttpGet(urls[0].toString());
                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                method.addHeader("auth-token", prefs.getString("token", null));
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
                            json = new String(result);
                            Log.e("Result", result);
                            JSONArray finalResult = new JSONArray(result);
                            items = new String[finalResult.length()];

                            for (int i = 0; i < finalResult.length(); i++) {
                                JSONObject jsonObject = finalResult.getJSONObject(i);
                                items[i] = jsonObject.getString("device_id");
                                Log.e("device_id", jsonObject.getString("device_id"));
                            }
                        } else {
                            Log.e("response is not 200", String.valueOf(response.getStatusLine().getStatusCode()));
                        }
                    } else {
                        Log.e("response is null", "null");
                    }
                } catch (ClientProtocolException e) {
                    Log.e("Exception CP", e.getMessage());
                } catch (IOException e) {
                    Log.e("Exception IO", e.getMessage());
                } catch (Exception e) {
                    Log.e("Errorr", "kjhkjhk");
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
            dialog.dismiss();
            if (items != null) {
                myAdapter = new MyAdapter(DevicesActivity.this, items);
                ListView lv = (ListView) findViewById(R.id.deviceList);
                lv.setAdapter(myAdapter);
                myAdapter.notifyDataSetChanged();
                Toast.makeText(DevicesActivity.this, "Devices loaded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DevicesActivity.this, "No devices found", Toast.LENGTH_LONG).show();
            }
        }
    }

    public class AddDeviceTask extends AsyncTask<URL, Void, Boolean> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(DevicesActivity.this);
            dialog.setMessage("Adding device");
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(URL... urls) {
            Boolean value = false;
            if (isNetworkConnected() == true) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost method = new HttpPost(urls[0].toString());

                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                method.addHeader("auth-token", prefs.getString("token", null));

                JSONObject object = new JSONObject();

                try {
                    object.put("device_id", Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID));
                    object.put("groupname", prefs.getString("groupname", null));
                    Calendar c = Calendar.getInstance();
                    object.put("time_stamp", dateFormat.format(c.getTime()));
                    object.put("memory_available", new File(context.getFilesDir().getAbsoluteFile().toString()).getFreeSpace() / 1048576);

                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (location == null) {
                        object.put("latitude", 0);
                        object.put("longitude", 0);
                        object.put("signal_strength", 0);
                    } else {
                        object.put("latitude", location.getLongitude());
                        object.put("longitude", location.getLatitude());
                        object.put("signal_strength", 0);
                    }

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
                            value = true;
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
                            Log.e("Error in adding::", msg);
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
            return value;
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
        protected void onPostExecute(Boolean value) {
            super.onPostExecute(value);
            dialog.dismiss();
            if (value == true)
                Toast.makeText(DevicesActivity.this, "Device added", Toast.LENGTH_SHORT);
            else
                Toast.makeText(DevicesActivity.this, "Device failed to add", Toast.LENGTH_SHORT);
        }
    }

    public class RemoveDeviceTask extends AsyncTask<URL, Void, Boolean> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(DevicesActivity.this);
            dialog.setMessage("Remove the device....");
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(URL... urls) {
            Boolean value = false;
            if (isNetworkConnected() == true) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpDeleteWithBody method = new HttpDeleteWithBody(urls[0].toString());

                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                method.addHeader("auth-token", prefs.getString("token", null));

                JSONObject object = new JSONObject();
                try {
                    object.put("device_id", Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID));
                    object.put("groupname", prefs.getString("groupname", null));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    method.setEntity(new StringEntity(object.toString(), "UTF8"));
                    method.addHeader("Content-type", "application/json");

                    HttpResponse response = httpclient.execute(method);
                    if (response != null) {
                        if (response.getStatusLine().getStatusCode() == 200) {
                            value = true;
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
                            dialog.setMessage(msg);
                        }
                    }
                } catch (ClientProtocolException e) {
                    Log.d("Exception CP", e.getMessage());
                    dialog.setMessage("Error occurred while loading the devices");
                } catch (IOException e) {
                    Log.d("Exception IO", e.getMessage());
                    dialog.setMessage("Error occurred while loading the devices");
                } catch (Exception e) {
                    dialog.setMessage("Error occurred while loading the devices");
                    e.printStackTrace();
                }
            } else {
                //dialog.setMessage("No network connection");
            }
            return value;
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
        protected void onPostExecute(Boolean value) {
            super.onPostExecute(value);
            dialog.dismiss();
            if (value == true)
                Toast.makeText(DevicesActivity.this, "Device removed", Toast.LENGTH_SHORT);
            else
                Toast.makeText(DevicesActivity.this, "Device failed to remove", Toast.LENGTH_SHORT);
        }
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mDrawerList.setItemChecked(i, true);
            URL url = null;
            switch (i) {
                case 0://Reload
                    recreate();
                    break;
                case 1://Add the device
                    try {
                        url = new URL("http://54.148.66.118/cadmgmt/devices.php");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    new AddDeviceTask().execute(url);
                    recreate();
                    mDrawerLayout.closeDrawer(mDrawerList);
                    break;
                case 2://Remove the device
                    url = null;
                    try {
                        url = new URL("http://54.148.66.118/cadmgmt/devices.php");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    new RemoveDeviceTask().execute(url);
                    recreate();
                    mDrawerLayout.closeDrawer(mDrawerList);
                    break;
                case 3:
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                    editor.remove("username");
                    editor.remove("groupname");
                    editor.remove("token");
                    editor.commit();
                    finish();
                    break;
            }
        }
    }


    class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

        public static final String METHOD_NAME = "DELETE";

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }
}

