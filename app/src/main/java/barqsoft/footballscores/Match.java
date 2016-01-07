package barqsoft.footballscores;

import android.database.Cursor;

/**
 * Created by comrade.marie on 12/18/2015.
 */
public class Match {

    public static final int _ID = 0;
    public static final int COL_DATE = 1;
    public static final int COL_TIME = 2;
    public static final int COL_HOME = 3;
    public static final int COL_AWAY = 4;
    public static final int COL_LEAGUE = 5;
    public static final int COL_HOME_GOALS = 6;
    public static final int COL_AWAY_GOALS = 7;
    public static final int COL_ID = 8;
    public static final int COL_MATCHDAY = 9;
    private final String date;
    private final String time;
    private final int matchDay;
    private final String league;
    private final String homeTeamName;
    private final String awayTeamName;
    private final String score;

    public Match(Cursor cursor) {
        date = cursor.getString(COL_DATE);
        time = cursor.getString(COL_TIME);
        matchDay = cursor.getInt(COL_MATCHDAY);
        league = Utilities.getLeague(cursor.getInt(COL_LEAGUE));
        homeTeamName = cursor.getString(COL_HOME);
        awayTeamName = cursor.getString(COL_AWAY);
        score = Utilities.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS));
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getMatchDay() {
        return matchDay;
    }

    public String getLeague() {
        return league;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public String getScore() {
        return score;
    }
}
