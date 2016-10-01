package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class TipBrowserActivity extends AppCompatActivity {

    public static final String ARG_POSTS = "posts";
    public static final String ARG_SCREEN_ID = "post_id";

    private ViewPager viewPager;
    private Menu optionsMenu;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_browser);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        Bundle bundle = getIntent().getExtras();
        if (bundle.getSerializable(ARG_POSTS) == null || !(bundle.getSerializable(ARG_POSTS) instanceof Post[]))
            throw new IllegalArgumentException("ARG_POSTS must be of type Post[]");
        int postId = bundle.getInt(ARG_SCREEN_ID, 0);

        Post[] posts = (Post[]) bundle.getSerializable(ARG_POSTS);

        // set up PagerAdapter
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), posts);

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(postId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_tip_browser, menu);
        optionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SectionsPagerAdapter adapter = (SectionsPagerAdapter) viewPager.getAdapter();
        TipFragment fragment = (TipFragment) adapter.getItem(viewPager.getCurrentItem());
        int postId = fragment.post.id;

        if (!fragment.receivedVotedOn)
            return super.onOptionsItemSelected(item);

        boolean voteUp = false;
        boolean removeVote = fragment.votedDown;

        switch (item.getItemId()) {
            case R.id.action_vote_up:
                voteUp = true;
                removeVote = fragment.votedUp;
            case R.id.action_vote_down:
                final Context context = this;
                if (!removeVote) {
                    new DatabaseLink(this, true).voteOnPost(new DatabaseLink.DatabaseListener() {
                        @Override
                        void onGetResponse(String message) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        void onError(String errorMsg) {
                            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                        }
                    }, postId, voteUp);
                } else {
                    new DatabaseLink(this, true).removeVote(new DatabaseLink.DatabaseListener() {
                        @Override
                        void onGetResponse(String message) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        void onError(String errorMsg) {
                            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                        }
                    }, postId);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The fragment holding the tip
     */
    public static class TipFragment extends Fragment {

        private static final String ARG_POST = "post_object";

        public Post post;

        boolean receivedVotedOn = false;
        boolean votedUp = false;
        boolean votedDown = false;

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
            post = (Post) getArguments().getSerializable(ARG_POST);

            if (post == null)
                throw new IllegalArgumentException("Post cannot be NULL");

            // update fields
            // content
            ((TextView) rootView.findViewById(R.id.detail_content))
                    .setText(post.content);

            // date
            ((TextView) rootView.findViewById(R.id.date))
                    .setText(post.postedAt);

            // owner
            ((TextView) rootView.findViewById(R.id.ownerName))
                    .setText(post.ownerName);

            // tags TODO

            // Look up votedOn
            receivedVotedOn = false;
            new DatabaseLink(getActivity()).getVotedOnPost(new DatabaseLink.DatabaseListener() {
                @Override
                void onGetResponse(String json) {
                    try {
                        JSONObject response = new JSONObject(json);
                        boolean vote_exists = response.getInt(DatabaseLink.JSON_VOTE_EXISTS) == 1;
                        boolean is_like = response.getInt(DatabaseLink.JSON_IS_LIKE) == 1;
                        votedUp = vote_exists && is_like;
                        votedDown = vote_exists && !is_like;
                        receivedVotedOn = true;

                        if (votedUp || votedDown) {
                            // TODO Change menu icons
                            TipBrowserActivity activity = (TipBrowserActivity) getActivity();
                            if (votedUp)
                                activity.optionsMenu.getItem(0).setIcon(R.drawable.ic_thumb_up_white_24px_active);
                            if (votedDown)
                                activity.optionsMenu.getItem(1).setIcon(R.drawable.ic_thumb_down_white_24px_active);
                        }
                    } catch (JSONException e) {
                        receivedVotedOn = false;
                        e.printStackTrace();
                    }
                }

                @Override
                void onError(String errorMsg) {
                    receivedVotedOn = false;
                    Log.e("TrafficTimeWaste", "Error getting votedOn: " + errorMsg);
                }
            }, post.id);

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Post[] posts;

        SectionsPagerAdapter(FragmentManager fm, Post[] data) {
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
