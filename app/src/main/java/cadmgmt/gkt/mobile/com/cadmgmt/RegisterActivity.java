package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Hari on 23/11/2014.
 */
public class RegisterActivity extends Activity implements Button.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button btn = (Button) findViewById(R.id.btnRegister1);
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

        EditText username1 = (EditText) findViewById(R.id.txtUsername1);
        EditText password1 = (EditText) findViewById(R.id.txtPassword1);
        EditText groupName = (EditText) findViewById(R.id.txtGroupName);

        String strUsername1 = username1.getText().toString();
        String strPassword1 = password1.getText().toString();
        String strGroupName = groupName.getText().toString();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        if (strUsername1.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (strPassword1.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (strGroupName.isEmpty()) {
            Toast.makeText(this, "Group name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNetworkConnected() == true) {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://54.148.66.118/cadmgmt/register.php");
            String message;

            try {

                JSONObject object = new JSONObject();
                object.put("username", strUsername1);
                object.put("password", strPassword1);
                object.put("groupname", strGroupName);

                message = object.toString();

                httppost.setEntity(new StringEntity(message, "UTF8"));
                httppost.setHeader("Content-type", "application/json");

                HttpResponse response = httpclient.execute(httppost);

                if (response != null) {
                    if (response.getStatusLine().getStatusCode() == 200) {

                        Toast.makeText(this, "User Registered Successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (response.getStatusLine().getStatusCode() == 406) {

                        Toast.makeText(this, "Username Already Exists", Toast.LENGTH_SHORT).show();

                    }

                }
                Log.d("Status line", "" + response.getStatusLine().getStatusCode());

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
}
