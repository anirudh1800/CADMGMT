package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;


public class AppViewActivity extends Activity implements AdapterView.OnItemClickListener, Switch.OnCheckedChangeListener {
    MyAdapter myAdapter;
    String PREFS = "USER_PREFS";
    String currentApp;
    Boolean currentAppLock;
    String device_id;
    private ArrayList<App> AppList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_view);
        device_id = getIntent().getStringExtra("deviceid");
        try {
            JSONArray appsArray = new JSONArray(getIntent().getStringExtra("jsonobj"));
            AppList = new ArrayList<App>(appsArray.length());
            for (int i = 0; i < appsArray.length(); i++) {
                AppList.add(new App(appsArray.getString(i), false));
            }
            ListView lv = (ListView) findViewById(R.id.appList);
            lv.setOnItemClickListener(this);
            URL url = null;
            try {
                url = new URL("http://54.148.66.118/cadmgmt/apps.php?deviceid=" + device_id);
                new AppsLocksTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        RelativeLayout parentRow = (RelativeLayout) compoundButton.getParent();
        TextView appView = (TextView) parentRow.getChildAt(0);
        currentApp = appView.getText().toString();
        currentAppLock = b;
        Log.e("app locked ", currentApp);
        URL url = null;
        try {
            url = new URL("http://54.148.66.118/cadmgmt/apps.php");
            new LockAppTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public class MyAdapter extends ArrayAdapter<App> {

        public MyAdapter(Context context, ArrayList<App> list) {
            super(context, R.layout.app_row_layout, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.app_row_layout, parent, false);
            TextView itemTxt = (TextView) convertView.findViewById(R.id.appItem);
            Switch swt = (Switch) convertView.findViewById(R.id.switchAppLock);
            itemTxt.setText(AppList.get(position).appname);
            swt.setChecked(AppList.get(position).app_lock);
            swt.setOnCheckedChangeListener(AppViewActivity.this);
            return convertView;
        }
    }

    public class LockAppTask extends AsyncTask<URL, Void, Boolean> {
        Boolean result = false;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(AppViewActivity.this);
            dialog.setMessage("Locking app");
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(URL... urls) {
            if (isNetworkConnected() == true) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPut method = new HttpPut(urls[0].toString());
                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                method.addHeader("auth-token", prefs.getString("token", null));
                JSONObject object = new JSONObject();
                try {
                    object.put("device_id", device_id);
                    object.put("appname", currentApp);
                    object.put("app_lock", currentAppLock);
                    method.addHeader("Content-type", "application/json");
                    method.setEntity(new StringEntity(object.toString(), "UTF8"));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                try {
                    HttpResponse response = httpclient.execute(method);
                    if (response != null) {
                        if (response.getStatusLine().getStatusCode() == 200) {
                            result = true;
                        } else {
                            Log.e("response is not 200 in AppView", String.valueOf(response.getStatusLine().getStatusCode()));
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
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            dialog.dismiss();
        }
    }

    public class AppsLocksTask extends AsyncTask<URL, Void, String> {
        String result;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(AppViewActivity.this);
            dialog.setMessage("Fetching apps");
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
                            Log.e("Result", result);
                            JSONObject object = new JSONObject(result);
                            for (int i = 0; i < AppList.size(); i++) {
                                App name1 = AppList.get(i);
                                Iterator<String> iterator2 = object.keys();
                                while (iterator2.hasNext()) {
                                    String name2 = iterator2.next();
                                    Log.e("jsonobject", name2);
                                    if (name1.appname.equals(name2)) {
                                        Log.e("match", object.getString(name2));
                                        int value = Integer.valueOf(object.getString(name2));
                                        if (value == 1)
                                            AppList.set(i, new App(name2, true));
                                        else
                                            AppList.set(i, new App(name2, false));
                                        break;
                                    }
                                }
                            }
                            Log.e("Result", result);
                        } else {
                            Log.e("response is not 200 in AppView", String.valueOf(response.getStatusLine().getStatusCode()));
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
            dialog.dismiss();
            if (AppList != null) {
                myAdapter = new MyAdapter(AppViewActivity.this, AppList);
                Iterator<App> iterator = AppList.iterator();
                while (iterator.hasNext()) {
                    App temp = iterator.next();
                    Log.e(temp.appname, Boolean.toString(temp.app_lock));
                }
                ListView lv = (ListView) findViewById(R.id.appList);
                lv.setAdapter(myAdapter);
                myAdapter.notifyDataSetChanged();
                Toast.makeText(AppViewActivity.this, "Apps loaded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AppViewActivity.this, "No apps found", Toast.LENGTH_LONG).show();
            }
        }
    }
}
