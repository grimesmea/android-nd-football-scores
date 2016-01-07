package barqsoft.footballscores;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yehya khaled on 2/27/2015.
 */
public class PagerFragment extends Fragment {
    public static final int NUM_PAGES = 5;
    public ViewPager mPagerHandler;
    private myPageAdapter mPagerAdapter;
    private MainFragment[] viewFragments = new MainFragment[NUM_PAGES];

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        mPagerHandler = (ViewPager) rootView.findViewById(R.id.pager);
        mPagerAdapter = new myPageAdapter(getChildFragmentManager());

        if (ViewCompat.getLayoutDirection(container) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            for (int i = 0; i < NUM_PAGES; i++) {
                Date fragmentDate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
                SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
                viewFragments[i] = new MainFragment();
                viewFragments[i].setFragmentDate(mFormat.format(fragmentDate));
            }
        } else {
            for (int i = NUM_PAGES; i >= 0; i--) {
                Date fragmentDate = new Date(System.currentTimeMillis() + ((i + 2) * 86400000));
                SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
                viewFragments[i] = new MainFragment();
                viewFragments[i].setFragmentDate(mFormat.format(fragmentDate));
            }
        }

        mPagerHandler.setAdapter(mPagerAdapter);
        mPagerHandler.setCurrentItem(MainActivity.currentFragment);
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mPagerHandler);

        return rootView;
    }

    private class myPageAdapter extends FragmentStatePagerAdapter {
        public myPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return viewFragments[i];
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return Utilities.getDayName(getActivity(), System.currentTimeMillis() + ((position - 2) * 86400000));
        }

    }
}
