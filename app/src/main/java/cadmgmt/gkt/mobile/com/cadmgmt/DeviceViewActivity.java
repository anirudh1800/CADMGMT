package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import org.json.JSONArray;
import org.json.JSONObject;


public class DeviceViewActivity extends Activity implements Button.OnClickListener, Switch.OnCheckedChangeListener {
    String device_id, time_stamp, appsJson;
    int latitude, longitude, memory_available, signal_strength;
    JSONObject jsonobj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_view);

        Button btn = (Button) findViewById(R.id.btnApps);
        btn.setOnClickListener(this);

        int index = getIntent().getIntExtra("index", -1);
        String json = getIntent().getStringExtra("json");

        Log.d("############## Index", String.valueOf(index));
        Log.d("############## Json", json);

        try {
            JSONArray jsonarr = new JSONArray(json);

            jsonobj = jsonarr.getJSONObject(index);

            device_id = jsonobj.getString("device_id");
            latitude = jsonobj.getInt("latitude");
            longitude = jsonobj.getInt("longitude");
            memory_available = jsonobj.getInt("memory_available");
            time_stamp = jsonobj.getString("time_stamp");
            signal_strength = jsonobj.getInt("signal_strength");

            appsJson = jsonobj.getJSONArray("0").toString();

            Log.d("######################## Apps", appsJson);

            EditText time_stamp1 = (EditText) findViewById(R.id.editTime);
            EditText latitude1 = (EditText) findViewById(R.id.editLatitude);
            EditText longitude1 = (EditText) findViewById(R.id.editLongitude);
            EditText memory_available1 = (EditText) findViewById(R.id.editMemory);
            EditText signal_strength1 = (EditText) findViewById(R.id.editSignal);

            time_stamp1.setText(time_stamp);
            latitude1.setText(String.valueOf(latitude));
            longitude1.setText(String.valueOf(longitude));
            memory_available1.setText(String.valueOf(memory_available));
            signal_strength1.setText(String.valueOf(signal_strength));


        } catch (Exception e) {
        }
    }


    public void onClick(View view) {
        Intent toViewAppsActivity = new Intent(this, AppViewActivity.class);
        toViewAppsActivity.putExtra("deviceid",device_id);
        toViewAppsActivity.putExtra("jsonobj", appsJson);
        startActivity(toViewAppsActivity);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.device_view, menu);
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
        Log.e("swith","changed");
    }
}
