package cadmgmt.gkt.mobile.com.cadmgmt;

import android.app.Activity;
import android.content.Intent;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Hari on 23/11/2014.
 */
public class MainActivity extends Activity implements Button.OnClickListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setTitle("Centralized Android Device Management");

        Button b1 = (Button) findViewById(R.id.btnSignIn);
        Button b2 = (Button) findViewById(R.id.btnRegister);
        b1.setOnClickListener(this);
        b2.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSignIn:
                Intent toLoginActivity = new Intent(this, LoginActivity.class);
                startActivity(toLoginActivity);
                break;
            case R.id.btnRegister:
                Intent toRegisterActivity = new Intent(this, RegisterActivity.class);
                startActivity(toRegisterActivity);
                break;
        }
    }

    @Override
    public void finish() {
        super.finish();
    }
}
