package barqsoft.footballscores;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import barqsoft.footballscores.syncadapter.SyncAdapter;

public class MainActivity extends AppCompatActivity {
    public static int selectedMatchId;
    public static int currentFragment = 2;
    public static String LOG_TAG = MainActivity.class.getSimpleName();
    private final String saveTag = "Save Test";
    private PagerFragment myMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "reached onCreate");
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            myMain = new PagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, myMain)
                    .commit();
        }
        SyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent start_about = new Intent(this, AboutActivity.class);
            startActivity(start_about);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(saveTag, "will save");
        Log.v(saveTag, "fragment: " + String.valueOf(myMain.mPagerHandler.getCurrentItem()));
        Log.v(saveTag, "selected id: " + selectedMatchId);
        outState.putInt("Pager_Current", myMain.mPagerHandler.getCurrentItem());
        outState.putInt("Selected_match", selectedMatchId);
        getSupportFragmentManager().putFragment(outState, "myMain", myMain);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(saveTag, "will retrive");
        Log.v(saveTag, "fragment: " + String.valueOf(savedInstanceState.getInt("Pager_Current")));
        Log.v(saveTag, "selected id: " + savedInstanceState.getInt("Selected_match"));
        currentFragment = savedInstanceState.getInt("Pager_Current");
        selectedMatchId = savedInstanceState.getInt("Selected_match");
        myMain = (PagerFragment) getSupportFragmentManager().getFragment(savedInstanceState, "myMain");
        super.onRestoreInstanceState(savedInstanceState);
    }
}
