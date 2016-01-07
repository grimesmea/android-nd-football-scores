package barqsoft.footballscores.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.BuildConfig;
import barqsoft.footballscores.R;
import barqsoft.footballscores.data.DatabaseContract;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String LOG_TAG = SyncAdapter.class.getSimpleName();
    public static final String ACTION_DATA_UPDATED =
            "barqsoft.football.app.ACTION_DATA_UPDATED";
    // Interval at which to sync the match scores, in seconds.
    // 60 seconds (1 minute) * 60 = 1 hour
    public static final int SYNC_INTERVAL = 60 * 60;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "barqsoft.footballscores";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "barqsoft.footballscores.account";
    // The account name
    public static final String ACCOUNT = "dummyaccount";

    static Account mAccount;
    static ContentResolver mContentResolver;

    String previousDaysTimeFrame = "p2";
    String nextDaysTimeFrame = "n2";
    String JSONMatchData;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    public static void initializeSyncAdapter(Context context) {
        createSyncAccount(context);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account createSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount) {
        mAccount = newAccount;
        /*
         * Since we've created an account
         */
        SyncAdapter.configurePeriodicSync(SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, AUTHORITY, true);

        /*
         * Finally, let's do a sync to get things started
         */
        Log.d(LOG_TAG, "manually requesting sync");
        manualSync();
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(int syncInterval, int flexTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(mAccount, AUTHORITY).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(mAccount,
                    AUTHORITY, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     */
    public static void manualSync() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        Log.d(LOG_TAG, "requesting sync");
        ContentResolver.requestSync(mAccount, AUTHORITY, bundle);
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {
        Log.d(LOG_TAG, "starting sync");

        // Fetching a processing match data for the PREVIOUS 2 days
        JSONMatchData = getData(previousDaysTimeFrame);
        processJSONData(JSONMatchData);

        // Fetching a processing match data for the NEXT 2 days
        JSONMatchData = getData(nextDaysTimeFrame);
        processJSONData(JSONMatchData);
    }

    private String getData(String timeFrame) {
        // Creating fetch URL
        final String BASE_URL = "http://api.football-data.org/alpha/fixtures"; //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
        //final String QUERY_MATCH_DAY = "matchday";

        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().
                appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();
        // Log.v(LOG_TAG, "The url we are looking at is: " + fetch_build.toString());
        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSONData = null;
        // Opening Connection
        try {
            URL fetch = new URL(fetch_build.toString());
            m_connection = (HttpURLConnection) fetch.openConnection();
            m_connection.setRequestMethod("GET");
            m_connection.addRequestProperty("X-Auth-Token", BuildConfig.FOOT_BALL_DATA_API_TOKEN);
            m_connection.connect();

            // Read the input stream into a String
            InputStream inputStream = m_connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            JSONData = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception here" + e.getMessage());
        } finally {
            if (m_connection != null) {
                m_connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error Closing Stream");
                }
            }
            return JSONData;
        }
    }

    private void processJSONData(String JSONData) {
        try {
            if (JSONData != null) {
                //This bit is to check if the data contains any matches. If not, we call process the dummy data
                JSONArray matches = new JSONObject(JSONData).getJSONArray("fixtures");
                Vector<ContentValues> matchContentValues;
                if (matches.length() == 0) {
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    matchContentValues = getContentValuesToBeInserted(
                            getContext().getString(R.string.dummy_data),
                            false);
                    insertMatchDataIntoContentProvider(matchContentValues);
                    updateWidgets();
                    return;
                }
                matchContentValues = getContentValuesToBeInserted(
                        JSONData,
                        true);
                insertMatchDataIntoContentProvider(matchContentValues);
                updateWidgets();
            } else {
                //Could not connect
                Log.d(LOG_TAG, "Could not connect to server.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private Vector getContentValuesToBeInserted(String JSONData, boolean isReal) {
        // JSON data
        // This set of league codes is for the 2015/2016 season. In fall of 2016, they will need to
        // be updated. Feel free to use the codes
        final String BUNDESLIGA1 = "394";
        final String BUNDESLIGA2 = "395";
        final String LIGUE1 = "396";
        final String LIGUE2 = "397";
        final String PREMIER_LEAGUE = "398";
        final String PRIMERA_DIVISION = "399";
        final String SEGUNDA_DIVISION = "400";
        final String SERIE_A = "401";
        final String PRIMEIRA_LIGA = "402";
        final String Bundesliga3 = "403";
        final String EREDIVISIE = "404";
        final String DUMMY_LEAGUE = "357";


        final String SEASON_LINK = "http://api.football-data.org/alpha/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/alpha/fixtures/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";

        // Match data
        String League;
        String date;
        String time;
        String homeTeam;
        String awayTeam;
        String homeGoals;
        String awayGoals;
        String matchId;
        String matchDay;

        try {
            JSONArray matches = new JSONObject(JSONData).getJSONArray(FIXTURES);

            // ContentValues to be inserted
            Vector<ContentValues> values = new Vector<>(matches.length());

            for (int i = 0; i < matches.length(); i++) {
                JSONObject matchData = matches.getJSONObject(i);
                League = matchData.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON)
                        .getString("href");
                League = League.replace(SEASON_LINK, "");
                //This if statement controls which leagues we're interested in the data from.
                //add leagues here in order to have them be added to the DB.
                // If you are finding no data in the app, check that this contains all the leagues.
                // If it doesn't, that can cause an empty DB, bypassing the dummy data routine.
                if (League.equals(PREMIER_LEAGUE) ||
                        League.equals(SERIE_A) ||
                        League.equals(BUNDESLIGA1) ||
                        League.equals(BUNDESLIGA2) ||
                        League.equals(PRIMERA_DIVISION) ||
                        League.equals(DUMMY_LEAGUE)) {
                    matchId = matchData.getJSONObject(LINKS).getJSONObject(SELF)
                            .getString("href");
                    matchId = matchId.replace(MATCH_LINK, "");
                    if (!isReal) {
                        //This if statement changes the match ID of the dummy data so that it all goes into the database
                        matchId = matchId + Integer.toString(i);
                    }

                    date = matchData.getString(MATCH_DATE);
                    time = date.substring(date.indexOf("T") + 1, date.indexOf("Z"));
                    date = date.substring(0, date.indexOf("T"));
                    SimpleDateFormat matchDate = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                    matchDate.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        Date parsedDate = matchDate.parse(date + time);
                        SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                        new_date.setTimeZone(TimeZone.getDefault());
                        date = new_date.format(parsedDate);
                        time = date.substring(date.indexOf(":") + 1);
                        date = date.substring(0, date.indexOf(":"));

                        if (!isReal) {
                            // This if statement changes the dummy data's date to match our current date range.
                            Date fragmentDate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            date = dateFormat.format(fragmentDate);
                        }
                    } catch (Exception e) {
                        Log.d(LOG_TAG, "error here!");
                        Log.e(LOG_TAG, e.getMessage());
                    }

                    homeTeam = matchData.getString(HOME_TEAM);
                    awayTeam = matchData.getString(AWAY_TEAM);
                    homeGoals = matchData.getJSONObject(RESULT).getString(HOME_GOALS);
                    awayGoals = matchData.getJSONObject(RESULT).getString(AWAY_GOALS);
                    matchDay = matchData.getString(MATCH_DAY);

                    ContentValues matchValues = new ContentValues();
                    matchValues.put(DatabaseContract.scores_table.MATCH_ID, matchId);
                    matchValues.put(DatabaseContract.scores_table.DATE_COL, date);
                    matchValues.put(DatabaseContract.scores_table.TIME_COL, time);
                    matchValues.put(DatabaseContract.scores_table.HOME_COL, homeTeam);
                    matchValues.put(DatabaseContract.scores_table.AWAY_COL, awayTeam);
                    matchValues.put(DatabaseContract.scores_table.HOME_GOALS_COL, homeGoals);
                    matchValues.put(DatabaseContract.scores_table.AWAY_GOALS_COL, awayGoals);
                    matchValues.put(DatabaseContract.scores_table.LEAGUE_COL, League);
                    matchValues.put(DatabaseContract.scores_table.MATCH_DAY, matchDay);
                    values.add(matchValues);
                }
            }
            return values;
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }
    }

    private void insertMatchDataIntoContentProvider(Vector matchContentValues) {
        int insertedData;
        ContentValues[] insertData = new ContentValues[matchContentValues.size()];
        matchContentValues.toArray(insertData);
        if (insertData != null) {
            insertedData = mContentResolver.bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI, insertData);
            Log.v(LOG_TAG, "Successfully Inserted : " + String.valueOf(insertedData));
        }

    }

    private void updateWidgets() {
        Context context = getContext();
        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(context.getPackageName());
        context.sendBroadcast(dataUpdatedIntent);
    }
}
