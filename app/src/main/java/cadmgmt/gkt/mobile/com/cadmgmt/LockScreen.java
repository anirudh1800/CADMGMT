package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class LockScreen extends Activity implements Button.OnClickListener {
    static String password = "xyz";
    String actname = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(this);
        actname = getIntent().getStringExtra("name");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.lock_screen, menu);
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
    public void onClick(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        if (editText.getText().toString().equals(password)) {
            finish();
        } else {
            Intent startHomescreen = new Intent(Intent.ACTION_MAIN);
            startHomescreen.addCategory(Intent.CATEGORY_HOME);
            startHomescreen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(startHomescreen);
            finish();
        }
    }


}
