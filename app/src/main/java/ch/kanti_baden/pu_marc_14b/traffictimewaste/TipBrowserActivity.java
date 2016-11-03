package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.app.SearchManager;
import android.content.Intent;
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

import java.util.concurrent.atomic.AtomicLong;

import eu.fiskur.chipcloud.ChipCloud;
import eu.fiskur.chipcloud.ChipListener;

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
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                updateThumbColors();

                SectionsPagerAdapter adapter = (SectionsPagerAdapter) viewPager.getAdapter();
                TipFragment fragment = adapter.children[viewPager.getCurrentItem()];
                if (!fragment.receivedVotedOn && fragment.getRequestDuration() > 5000) {
                    fragment.cancelRequest();
                    fragment.updateVotes();
                }
            }
        });
    }

    private void updateThumbColors() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SectionsPagerAdapter adapter = (SectionsPagerAdapter) viewPager.getAdapter();
                TipFragment fragment = adapter.children[viewPager.getCurrentItem()];

                if (fragment == null)
                    return;

                // Change menu icons
                if (fragment.receivedVotedOn) {
                    if (fragment.votedUp)
                        optionsMenu.getItem(0).setIcon(R.drawable.ic_thumb_up_24px_active);
                    else
                        optionsMenu.getItem(0).setIcon(R.drawable.ic_thumb_up_24px_inactive);

                    if (fragment.votedDown)
                        optionsMenu.getItem(1).setIcon(R.drawable.ic_thumb_down_24px_active);
                    else
                        optionsMenu.getItem(1).setIcon(R.drawable.ic_thumb_down_24px_inactive);
                } else {
                    optionsMenu.getItem(0).setIcon(R.drawable.ic_thumb_up_24px_pending);
                    optionsMenu.getItem(1).setIcon(R.drawable.ic_thumb_down_24px_pending);
                }
            }
        });
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
        TipFragment fragment = adapter.children[viewPager.getCurrentItem()];
        int postId = fragment.post.id;

        switch (item.getItemId()) {
            case R.id.action_vote_up:
                if (fragment.receivedVotedOn)
                    submitVote(fragment.votedUp, postId, true);
                return true;
            case R.id.action_vote_down:
                if (fragment.receivedVotedOn)
                    submitVote(fragment.votedDown, postId, false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void submitVote(boolean removeVote, int postId, boolean voteUp) {
        if (!removeVote) {
            DatabaseLink.instance.voteOnPost(new DatabaseLink.DatabaseListener() {
                @Override
                void onGetResponse(String message) {
                    Toast.makeText(TipBrowserActivity.this, message, Toast.LENGTH_SHORT).show();
                    causeFragmentUpdate();
                }

                @Override
                void onError(String errorMsg) {
                    Toast.makeText(TipBrowserActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }, postId, voteUp);
        } else {
            DatabaseLink.instance.removeVote(new DatabaseLink.DatabaseListener() {
                @Override
                void onGetResponse(String message) {
                    Toast.makeText(TipBrowserActivity.this, message, Toast.LENGTH_SHORT).show();
                    causeFragmentUpdate();
                }

                @Override
                void onError(String errorMsg) {
                    Toast.makeText(TipBrowserActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }, postId);
        }
    }

    private void causeFragmentUpdate() {
        SectionsPagerAdapter adapter = (SectionsPagerAdapter) viewPager.getAdapter();
        TipFragment fragment = adapter.children[viewPager.getCurrentItem()];
        fragment.updateVotes();
        updateThumbColors();
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

        public TipFragment() { }

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
            post = (Post) getArguments().getSerializable(ARG_POST); Log.v("TrafficTimeWaste", "Post saved: " + post);

            if (post == null)
                throw new IllegalArgumentException("Post cannot be NULL");

            // update fields
            // content
            // FIXME: 10/27/2016
            ((TipView) rootView.findViewById(R.id.detailContent))
                    .setContent(post.content);

            // date
            ((TextView) rootView.findViewById(R.id.date))
                    .setText(post.postedAt);

            // owner
            ((TextView) rootView.findViewById(R.id.ownerName))
                    .setText(post.ownerName);

            // tags
            final ChipCloud chips = (ChipCloud) rootView.findViewById(R.id.tagView);
            chips.addChips(post.tags);
            chips.setChipListener(new ChipListener() {
                @Override
                public void chipSelected(int i) {
                    // Go back to PostList, act as if the user searched for the tag
                    Intent intent = new Intent(getActivity(), PostListActivity.class);
                    intent.setAction(Intent.ACTION_SEARCH);
                    intent.putExtra(SearchManager.QUERY, post.tags[i]);
                    startActivity(intent);
                }

                @Override
                public void chipDeselected(int i) {
                    // Do nothing.
                }
            });

            // Look up votedOn
            updateVotes();

            return rootView;
        }

        private final AtomicLong idCounter = new AtomicLong();
        private long openRequestID = -1L;
        private long requestStartMillis = Long.MIN_VALUE;
        void updateVotes() {
            receivedVotedOn = false;

            // Create ID that is not -1L (represents NULL)
            long tmpId = -1L;
            while (tmpId == -1L)
                tmpId = idCounter.getAndIncrement();
            final long id = tmpId;

            // Save request id and start time
            openRequestID = id;
            requestStartMillis = System.currentTimeMillis();

            // Send request
            DatabaseLink.instance.getVotedOnPost(new DatabaseLink.DatabaseListener() {
                @Override
                void onGetResponse(String json) {
                    if (openRequestID != id)
                        return;

                    try {
                        JSONObject response = new JSONObject(json);
                        boolean vote_exists = response.getInt(DatabaseLink.JSON_VOTE_EXISTS) == 1;
                        boolean is_like = response.getInt(DatabaseLink.JSON_IS_LIKE) == 1;
                        votedUp = vote_exists && is_like;
                        votedDown = vote_exists && !is_like;
                        receivedVotedOn = true;

                        Log.v("TrafficTimeWaste", "Received vote data: exists=" + vote_exists + " is_like=" + is_like + ", updating thumbs...");

                        TipBrowserActivity activity = (TipBrowserActivity) getActivity();
                        SectionsPagerAdapter adapter = (SectionsPagerAdapter) activity.viewPager.getAdapter();
                        TipFragment fragment = adapter.children[activity.viewPager.getCurrentItem()];

                        // Update thumbs if selected
                        if (fragment.post.id == post.id)
                            activity.updateThumbColors();
                    } catch (JSONException e) {
                        receivedVotedOn = false;
                        Log.e("TrafficTimeWaste", "Error retrieving votes", e);
                    }
                }

                @Override
                void onError(String errorMsg) {
                    if (openRequestID != id)
                        return;

                    receivedVotedOn = false;
                    Log.e("TrafficTimeWaste", "Error getting votedOn: " + errorMsg);
                }
            }, post.id);
        }

        void cancelRequest() {
            openRequestID = -1L;
        }

        long getRequestDuration() {
            return System.currentTimeMillis() - requestStartMillis;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final Post[] posts;
        final TipFragment[] children;

        SectionsPagerAdapter(FragmentManager fm, Post[] data) {
            super(fm);
            posts = data;
            children = new TipFragment[posts.length];
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a TipFragment (defined as a static inner class below).
            TipFragment fragment = TipFragment.newInstance(posts[position]);
            children[position] = fragment;
            return fragment;
        }

        @Override
        public int getCount() {
            return posts.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "POST " + position;
        }
    }
}
