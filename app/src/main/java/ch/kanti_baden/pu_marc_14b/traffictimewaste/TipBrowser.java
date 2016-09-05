package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

public class TipBrowser extends AppCompatActivity {
    //TODO replace with GET request
    Post[] samplePosts = new Post[]{
            new Post("Pumpe isch voll geil", "lenny", new String[]{"swag", "bizeps"}),
            new Post("Velofahre isch huere cool", "simon", new String[]{"bike", "chillig"}),
            new Post("Atombombe baue findi lustig", "saemi", new String[]{"boom", "ups sorry"})
    };

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_browser);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), samplePosts);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class TipFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_POST = "post_object";

        public TipFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static TipFragment newInstance(Post content) {
            TipFragment fragment = new TipFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_POST, content);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tip_browser, container, false);

            if (!(getArguments().getSerializable(ARG_POST) instanceof Post))
                throw new IllegalArgumentException("ARG_POST must be of type Post");

            Post post = (Post) getArguments().getSerializable(ARG_POST);

            if (post == null)
                throw new IllegalArgumentException("Post cannot be NULL");

            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(post.content);
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Post[] posts;

        public SectionsPagerAdapter(FragmentManager fm, Post[] data) {
            super(fm);
            posts = data;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a TipFragment (defined as a static inner class below).
            return TipFragment.newInstance(posts[position]);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return posts.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POST " + position;
        }
    }
}
