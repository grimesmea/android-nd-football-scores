package barqsoft.footballscores;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by yehya khaled on 2/26/2015.
 */
public class ViewHolder {
    public TextView home_name;
    public TextView away_name;
    public TextView score;
    public TextView time;
    public ImageView home_crest;
    public ImageView away_crest;
    public double match_id;

    public ViewHolder(View view) {
        home_name = (TextView) view.findViewById(R.id.home_name);
        away_name = (TextView) view.findViewById(R.id.away_name);
        score = (TextView) view.findViewById(R.id.score_textview);
        time = (TextView) view.findViewById(R.id.match_start_time_textview);
        home_crest = (ImageView) view.findViewById(R.id.home_crest);
        away_crest = (ImageView) view.findViewById(R.id.away_crest);
    }
}
