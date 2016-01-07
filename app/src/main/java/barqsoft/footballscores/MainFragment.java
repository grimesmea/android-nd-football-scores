package barqsoft.footballscores;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import barqsoft.footballscores.data.DatabaseContract;

public class MainFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MainFragment.class.getSimpleName();
    public static final int SCORES_LOADER = 0;
    public ScoresAdapter mAdapter;
    private String[] fragmentdate = new String[1];
    private int last_selected_item = -1;

    public MainFragment() {
    }

    public void setFragmentDate(String date) {
        fragmentdate[0] = date;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final ListView score_list = (ListView) rootView.findViewById(R.id.scores_list);
        View emptyView = rootView.findViewById(R.id.empty_view);
        score_list.setEmptyView(emptyView);
        mAdapter = new ScoresAdapter(getActivity(), null, 0);
        score_list.setAdapter(mAdapter);
        mAdapter.detail_match_id = MainActivity.selectedMatchId;
        score_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder selected = (ViewHolder) view.getTag();
                mAdapter.detail_match_id = selected.match_id;
                MainActivity.selectedMatchId = (int) selected.match_id;
                mAdapter.notifyDataSetChanged();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "OnActCreated");
        getLoaderManager().initLoader(SCORES_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(),
                DatabaseContract.scores_table.buildScoreWithDate(),
                null,
                null,
                fragmentdate,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        //Log.v(FetchScoreTask.LOG_TAG,"loader finished");
        //cursor.moveToFirst();
        /*
        while (!cursor.isAfterLast())
        {
            Log.v(FetchScoreTask.LOG_TAG,cursor.getString(1));
            cursor.moveToNext();
        }
        */

       /* int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            i++;
            cursor.moveToNext();
        }*/
        //Log.v(FetchScoreTask.LOG_TAG,"Loader query: " + String.valueOf(i));
        mAdapter.swapCursor(cursor);
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }


}
