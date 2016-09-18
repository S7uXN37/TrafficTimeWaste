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

public class TipBrowserActivity extends AppCompatActivity {

    public static final String ARG_POSTS = "posts";
    public static final String ARG_SCREEN_ID = "post_id";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_browser);

        Bundle bundle = getIntent().getExtras();
        if (bundle.getSerializable(ARG_POSTS) == null || !(bundle.getSerializable(ARG_POSTS) instanceof Post[]))
            throw new IllegalArgumentException("ARG_POSTS must be of type Post[]");
        int postId = bundle.getInt(ARG_SCREEN_ID, 0);

        Post[] posts = (Post[]) bundle.getSerializable(ARG_POSTS);

        // set up PagerAdapter
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), posts);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(postId);
    }

    /**
     * The fragment holding the tip
     */
    public static class TipFragment extends Fragment {

        private static final String ARG_POST = "post_object";

        public TipFragment() {
        }

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
            // inflate view
            View rootView = inflater.inflate(R.layout.fragment_tip_browser, container, false);

            // get Post
            if (!(getArguments().getSerializable(ARG_POST) instanceof Post))
                throw new IllegalArgumentException("ARG_POST must be of type Post");
            Post post = (Post) getArguments().getSerializable(ARG_POST);

            if (post == null)
                throw new IllegalArgumentException("Post cannot be NULL");

            // update fields
            // content
            ((TextView) rootView.findViewById(R.id.content))
                .setText(post.content);

            // date
            ((TextView) rootView.findViewById(R.id.date))
                    .setText(post.postedAt);

            // owner
            ((TextView) rootView.findViewById(R.id.ownerName))
                    .setText(post.ownerName);

            // votes TODO


            // tags TODO


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