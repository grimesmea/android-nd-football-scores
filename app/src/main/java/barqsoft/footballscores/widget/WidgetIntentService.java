package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.Match;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;
import barqsoft.footballscores.data.DatabaseContract;

/**
 * IntentService which handles updating all next match widgets with the latest data
 */
public class WidgetIntentService extends IntentService {

    private static final int DATE_COL = 1;
    private static final int TIME_COL = 2;
    private static String LOG_TAG = WidgetIntentService.class.getSimpleName();

    public WidgetIntentService() {
        super("WidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the next match widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                WidgetProvider.class));

        String description = "Football";
        Cursor matchesData = getMatchData();
        Cursor nextMatchCursor;
        Match nextMatch = null;

        if (matchesData != null) {
            try {
                nextMatchCursor = findNextMatch(matchesData);
                if (nextMatchCursor != null) {
                    nextMatch = new Match(nextMatchCursor);
                    nextMatchCursor.close();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            matchesData.close();
        }

        // Perform this loop procedure for each widget
        for (int appWidgetId : appWidgetIds) {
            int widgetWidth = getWidgetWidth(appWidgetManager, appWidgetId);
            int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_default_width);
            int layoutId;

            if (widgetWidth >= defaultWidth) {
                layoutId = R.layout.widget;
            } else {
                layoutId = R.layout.widget_small;
            }

            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, description);
            }

            // Add the data to the RemoteViews if a next match is found
            if (nextMatch != null) {
                if (widgetWidth >= defaultWidth) {
                    views.setImageViewResource(R.id.widget_home_crest,
                            Utilities.getTeamCrestByTeamName(nextMatch.getHomeTeamName()));
                    views.setImageViewResource(R.id.widget_away_crest,
                            Utilities.getTeamCrestByTeamName(nextMatch.getAwayTeamName()));

                    // Content Descriptions for RemoteViews were only added in ICS MR1
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        views.setContentDescription(R.id.widget_home_crest,
                                nextMatch.getHomeTeamName() + " " + getString(R.string.team_crest_text));
                        views.setContentDescription(R.id.widget_away_crest,
                                nextMatch.getAwayTeamName() + " " + getString(R.string.team_crest_text));

                    }
                }
                views.setTextViewText(R.id.widget_date,
                        getMatchDateInMillis(getApplicationContext(), nextMatch));
                views.setTextViewText(R.id.widget_start_time, nextMatch.getTime());
            } else {
                views.removeAllViews(R.id.widget_container);
                RemoteViews emptyView = new RemoteViews(getPackageName(),
                        R.layout.widget_empty_view);
                emptyView.setImageViewResource(R.id.widget_logo, R.drawable.ic_launcher);
                views.addView(R.id.widget_container, emptyView);
            }

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private int getWidgetWidth(AppWidgetManager appWidgetManager, int appWidgetId) {
        // Prior to Jelly Bean, widgets were always their default size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_default_width);
        }
        // For Jelly Bean and higher devices, widgets can be resized - the current size can be
        // retrieved from the newly added App Widget Options
        return getWidgetWidthFromOptions(appWidgetManager, appWidgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            // The width returned is in dp, but we'll convert it to pixels to match the other widths
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp,
                    displayMetrics);
        }
        return getResources().getDimensionPixelSize(R.dimen.widget_default_width);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_home_crest, description);
        views.setContentDescription(R.id.widget_away_crest, description);
    }

    private Cursor getMatchData() {
        // Sort order:  Ascending, by date and time.
        String sortOrder = DatabaseContract.scores_table.DATE_COL + " ASC, " +
                DatabaseContract.scores_table.TIME_COL + " ASC";

        Cursor cursor = getContentResolver().query(
                DatabaseContract.BASE_CONTENT_URI,
                null,
                null,
                null,
                sortOrder
        );

        return cursor;
    }

    private Cursor findNextMatch(Cursor cursor) throws ParseException {
        Date formattedMatchDate;
        Cursor matchData = cursor;

        Date mCurrentTimeMillis = new Date(System.currentTimeMillis());
        SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String currentFormattedDate = mDateFormat.format(mCurrentTimeMillis);

        while (matchData.moveToNext()) {
            formattedMatchDate = getMatchDate(matchData, mDateFormat);

            if (formattedMatchDate.after(mDateFormat.parse(currentFormattedDate))) {
                return matchData;
            }
        }
        return null;
    }

    private Date getMatchDate(Cursor matchCursor, SimpleDateFormat dateFormat) throws ParseException {
        String matchDate = matchCursor.getString(DATE_COL) + " " + matchCursor.getString(TIME_COL);
        return dateFormat.parse(matchDate);
    }

    private String getMatchDateInMillis(Context context, Match match) {
        String matchDateTime = match.getDate() + " " + match.getTime();
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).parse(matchDateTime);
            long dateInMillis = date.getTime();
            return Utilities.getDayName(context, dateInMillis);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return match.getDate();
    }
}
