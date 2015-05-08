package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Hari on 23/11/2014.
 */
public class LoginActivity extends Activity implements Button.OnClickListener {
    String PREFS = "USER_PREFS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button btn = (Button) findViewById(R.id.btnLogin);
        btn.setOnClickListener(this);
    }

    public boolean isNetworkConnected() {
        ConnectivityManager status = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = status.getActiveNetworkInfo();

        if (network != null && network.isConnected()) {
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        EditText username = (EditText) findViewById(R.id.txtUsername);
        EditText password = (EditText) findViewById(R.id.txtPassword);

        String strUsername = username.getText().toString();
        String strPassword = password.getText().toString();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (strUsername.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (strPassword.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNetworkConnected() == true) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://54.148.66.118/cadmgmt/login.php");
            String message;

            try {
                JSONObject object = new JSONObject();
                object.put("username", strUsername);
                object.put("password", strPassword);

                message = object.toString();

                httppost.setEntity(new StringEntity(message, "UTF8"));
                httppost.setHeader("Content-type", "application/json");

                HttpResponse response = httpclient.execute(httppost);

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

                        String result = sb.toString();
                        JSONObject jObject = new JSONObject(result);
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                        editor.putString("username", jObject.getString("username"));
                        editor.putString("groupname", jObject.getString("groupname"));
                        editor.putString("token", removeQuotes(jObject.getString("token")));
                        editor.commit();
                        Log.e("Login Sucessfull", "" + response.getStatusLine().getStatusCode() + jObject.getString("token"));
                        Log.e("Group Name", jObject.getString("groupname"));
                        Log.e("User Name", jObject.getString("username"));
                        username.setText("");
                        password.setText("");
                        DeviceUpdateService.setToken(removeQuotes(jObject.getString("token")));
                        DeviceUpdateService.setGroupname(jObject.getString("groupname"));
                        AppLockService.token = removeQuotes(jObject.getString("token"));
                        if (!DeviceUpdateService.flag) {
                            DeviceUpdateService.flag = true;
                            DeviceUpdateService.schedule(getApplicationContext());
                        }
                        AppLockService.schedule(getApplicationContext());
                        Intent i = new Intent(this, DeviceUpdateService.class);
                        startService(i);
                        Intent j = new Intent(this, AppLockService.class);
                        startService(j);
                        Intent toDevicesActivity = new Intent(this, DevicesActivity.class);
                        startActivity(toDevicesActivity);
                    } else if (response.getStatusLine().getStatusCode() == 401) {
                        Toast.makeText(this, "Incorrect Username/Password", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    public String removeQuotes(String str) {
        return str.substring(1, str.length() - 1);
    }
}
