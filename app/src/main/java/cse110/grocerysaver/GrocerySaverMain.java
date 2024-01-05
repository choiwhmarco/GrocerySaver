package cse110.grocerysaver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.MenuItem;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import java.util.LinkedList;

import cse110.grocerysaver.notification.SendNotificationService;
import cse110.grocerysaver.notification.SetRepeatAlarmService;

public class GrocerySaverMain extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private enum MainFragment {
        MyFridgeFragment, FavoritesFragment, SettingsFragment
    }

    public MyFridgeFragment myFridgeFragment;
    public FavoritesFragment favoritesFragment;
    public PreferencesFragment settingsFragment;
    BottomNavigationView navigationView;

    private LinkedList<MainFragment> fragmentBackStack = new LinkedList<>();

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == fragmentBackStack.peek().ordinal()) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_fridge:
                pushFragment(MainFragment.MyFridgeFragment);
                break;
            case R.id.menu_favorites:
                pushFragment(MainFragment.FavoritesFragment);
                break;
            case R.id.menu_settings:
                pushFragment(MainFragment.SettingsFragment);
                break;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (fragmentBackStack.size() == 1) {
            super.onBackPressed();
            return;
        }

        popFragment();
    }

    private void changeFragment(MainFragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.rootLayout, fragment(fragment));
        transaction.commit();

        navigationView.getMenu().getItem(fragment.ordinal()).setChecked(true);
    }

    private void pushFragment(MainFragment fragment) {
        if (fragmentBackStack.contains(fragment)) {
            fragmentBackStack.remove(fragment);
        }

        fragmentBackStack.push(fragment);
        changeFragment(fragment);
    }

    private void popFragment() {
        fragmentBackStack.pop();
        changeFragment(fragmentBackStack.peek());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_saver_main);

        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(this);

        pushFragment(MainFragment.MyFridgeFragment);

        // register onSharedPreferenceChangeListener
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        //cancelNotification();
        //Intent myIntent = new Intent(this,SetRepeatAlarmService.class);
        //this.startService(myIntent);
    }

    @NonNull
    private Fragment fragment(MainFragment fragment) {
        switch (fragment) {
            case FavoritesFragment:
                if (favoritesFragment == null) {
                    favoritesFragment = new FavoritesFragment();
                }
                return favoritesFragment;
            case SettingsFragment:
                if (settingsFragment == null) {
                    settingsFragment = new PreferencesFragment();
                }
                return settingsFragment;
            default:
                if (myFridgeFragment == null) {
                    myFridgeFragment = new MyFridgeFragment();
                }
                return myFridgeFragment;
        }
    }

    // this method should handle the change of any preference during the application runtime. Set up
    // an alarm, or cancel an alarm.
    // most change need to cancel and reset the alarm, because the alarm is based on period and time
    // don't need cancel and reset alarm for email notification change, because that is handled in
    // SendNotificationService
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d("debug","change happened");
        // in the case when notification check box is touched
        if ("notification_onoff_preference".equals(s)) {
            Log.d("debug","notification check box is touched");
            if(sharedPreferences.getBoolean(s,false)) {
                Log.d("debug","notification is turned on");
                startService(new Intent(this, SetRepeatAlarmService.class));
            }
            else{
                Log.d("debug","notification is turned off");
                cancelNotification();
            }
        }
        // the case notification period is changed
        else if ("notify_period_preference".equals(s)) {
            Log.d("debug","notification period is touched");
            Log.d("debug","new period is "+Integer.parseInt(sharedPreferences.getString(s,"")));
            if(sharedPreferences.getBoolean("notification_onoff_preference",false)) {
                cancelNotification();
                startService(new Intent(this, SetRepeatAlarmService.class));
            }
        }
        // the case notification time is changed
        else if ("notify_time_preference".equals(s)) {
            Log.d("debug","notification time is touched");
            Log.d("debug","new time is "+Integer.parseInt(sharedPreferences.getString(s,"")));
            if(sharedPreferences.getBoolean("notification_onoff_preference",false)){
                cancelNotification();
                startService(new Intent(this, SetRepeatAlarmService.class));
            }
        }
        else if ("email_onoff_preference".equals(s)) {
            Log.d("debug","email notification check box is touched");
            if (sharedPreferences.getBoolean("email_onoff_preference",false)) {
                Log.d("debug","email notification is turned on");
                if(sharedPreferences.getString("email_address_preference","").equals("")){
                    Toast.makeText(getApplicationContext(),"Please enter a valid email address", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Log.d("debug","email notification is turned off");
            }
        }
        else if ("email_address_preference".equals(s)){
            Log.d("debug","email address is changed");
            Log.d("debug","new email address is "+sharedPreferences.getString(s,""));
            if(sharedPreferences.getString("email_address_preference","").equals("")){
                Toast.makeText(getApplicationContext(),"Email address cannot be empty", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(getApplicationContext(),"Email address updated", Toast.LENGTH_LONG).show();
        }

    }

    //This method may be needed, not sure need to cancel an alarm before start it again
    public void cancelNotification() {
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(this , SendNotificationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, myIntent, 0);
        alarmManager.cancel(pendingIntent);
    }
}
