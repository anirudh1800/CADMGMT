package cadmgmt.gkt.mobile.com.cadmgmt;

/**
 * Created by Anirudh Ruia on 11/28/2014.
 */

class App {
    public String appname;
    public boolean app_lock;

    public App(String name, boolean value) {
        appname = new String(name);
        app_lock = value;
    }
}
