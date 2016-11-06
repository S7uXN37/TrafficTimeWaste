package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Comparator;

enum SORT_TYPE {
    NEWEST, OLDEST, UPVOTES, DOWNVOTES
}

/**
 * An activity representing a list of Posts. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TipBrowserActivity} representing
 * item details.
 */
public class PostListActivity extends AppCompatActivity {

    public static final int ACTIVITY_SUCCESS = 3;
    public static final int MUST_RELOAD = 4;

    private static int sortType = 0;
    private static boolean warningDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        if (DatabaseLink.instance == null) {
            try {
                new DatabaseLink(this);
            } catch (IllegalStateException e) {
                DatabaseLink.initPreferences(this);
                new DatabaseLink(this);
            }
        }

        setupRecyclerViewAsync((FrameLayout) findViewById(R.id.frameLayout));
        if (!warningDisplayed) {
            final AlertDialog warning = new AlertDialog.Builder(this)
                    .setMessage(R.string.warning_message)
                    .setPositiveButton(R.string.warning_acknowledge, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            warning.show();
            warningDisplayed = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_post_list, menu);

        // Disable create post button if user isn't logged in
        if (DatabaseLink.instance.isLoggedIn()) {
            MenuItem menuItem = menu.findItem(R.id.action_login);
            menuItem.setEnabled(false);
        } else {
            MenuItem menuItem = menu.findItem(R.id.action_create);
            menuItem.setEnabled(false);
            Drawable icon = menuItem.getIcon();
            icon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            menuItem.setIcon(icon);
        }

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setTranslationX(500f);
                searchView.setAlpha(0f);
                searchView.animate().translationX(0);
                searchView.animate().alpha(1f);
            }
        });
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                // Create AlertDialog with radio buttons to select ordering
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                // Add options
                SORT_TYPE[] values = SORT_TYPE.values();
                String[] options = new String[values.length];
                for (int i = 0; i < options.length; i++)
                    options[i] = values[i].name().toLowerCase();
                builder.setTitle(R.string.select_sorting)
                        .setSingleChoiceItems(options, sortType, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sortType = which;
                                PostListActivity.this.recreate();
                            }
                        });
                // Display dialog
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            case R.id.action_login:
                Intent intent = new Intent(this, LoginActivity.class);
                startActivityForResult(intent, MUST_RELOAD);
                return true;
            case R.id.action_create:
                Intent intent1 = new Intent(this, TipCreateActivity.class);
                startActivityForResult(intent1, MUST_RELOAD);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == ACTIVITY_SUCCESS)
            switch (requestCode) {
                case MUST_RELOAD:
                    recreate();
                    setResult(ACTIVITY_SUCCESS);
                    break;
            }
    }

    private void setupRecyclerViewAsync(@NonNull final ViewGroup viewGroup) {
        final ProgressDialog progressDialog = ProgressDialog.show(this,
                getResources().getString(R.string.progress_loading_posts),
                getResources().getString(R.string.progress_please_wait),
                true, false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        DatabaseLink.DatabaseListener listener = new DatabaseLink.DatabaseListener() {
            @Override
            void onGetResponse(String str) {
                final Post[] posts;
                try {
                    JSONObject json = new JSONObject(str);
                    posts = DatabaseLink.parseJson(json);
                } catch (JSONException e) {
                    onError("JSON is invalid. Error: " + e.getMessage() + ", JSON: " + str);
                    return;
                }

                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                final Post[] sortedPosts = sortPosts(posts);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Inflate layout post_list
                        View rootView = View.inflate(PostListActivity.this, R.layout.post_list, null);
                        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.post_list);

                        // Setup refresh action
                        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh);
                        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                            @Override
                            public void onRefresh() {
                                recreate();
                            }
                        });

                        // Set adapter with posts
                        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(sortedPosts));

                        // Add to ViewGroup
                        viewGroup.addView(rootView);
                    }
                });
            }

            @Override
            void onError(String error) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                new AlertDialog.Builder(progressDialog.getContext())
                        .setTitle("Error")
                        .setMessage(error)
                        .show();
            }
        };

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
            DatabaseLink.instance.getPostsWithTag(listener, intent.getStringExtra(SearchManager.QUERY));
        else
            DatabaseLink.instance.getAllPosts(listener);
        Log.v("TrafficTimeWaste", "Querying db...");
    }

    private static Post[] sortPosts(Post[] posts) {
        Comparator<Post> comparator = null;
        switch (SORT_TYPE.values()[sortType]) {
            case NEWEST:
                comparator = new Comparator<Post>() {
                    @Override
                    public int compare(Post post, Post t1) {
                        return Long.compare(t1.postedAtMillis, post.postedAtMillis);
                    }
                };
                break;
            case OLDEST:
                comparator = new Comparator<Post>() {
                    @Override
                    public int compare(Post post, Post t1) {
                        return Long.compare(post.postedAtMillis, t1.postedAtMillis);
                    }
                };
                break;
            case UPVOTES:
                comparator = new Comparator<Post>() {
                    @Override
                    public int compare(Post post, Post t1) {
                        return Integer.compare(t1.votesUp-t1.votesDown, post.votesUp-post.votesDown);
                    }
                };
                break;
            case DOWNVOTES:
                comparator = new Comparator<Post>() {
                    @Override
                    public int compare(Post post, Post t1) {
                        return Integer.compare(t1.votesDown-t1.votesUp, post.votesDown-post.votesUp);
                    }
                };
                break;
        }

        Arrays.sort(posts, comparator);
        return posts;
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final Post[] posts;

        SimpleItemRecyclerViewAdapter(Post[] items) {
            posts = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.post_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Post p;
            if (posts.length == 0) {
                p = new Post(-1, "No posts found...", "", "", 0, 0, new String[]{});
                p.clickable = false;
            } else {
                p = posts[position];
            }

            Log.v("TrafficTimeWaste", "Updating Holder, post: " + p);
            holder.update(p, position);
        }

        @Override
        public int getItemCount() {
            return Math.max(1, posts.length);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final View mView;
            final TextView voteView;
            final TextView contentView;
            final TextView postedAtView;

            ViewHolder(View view) {
                super(view);
                mView = view;
                voteView = (TextView) view.findViewById(R.id.id);
                contentView = (TextView) view.findViewById(R.id.content);
                postedAtView = (TextView) view.findViewById(R.id.postedAt);
            }

            void update(Post item, final int listIndex) {
                String id = ""+(item.votesUp-item.votesDown);
                voteView.setText(id);

                String contentPreview = item.content;
                if (contentPreview.contains(TipView.TITLE_TRIGGER)) {
                    int startIndex = contentPreview.indexOf(TipView.TITLE_TRIGGER) + TipView.TITLE_TRIGGER.length();
                    contentPreview = contentPreview.substring(startIndex, contentPreview.indexOf("]", startIndex));
                }
                if (contentPreview.contains("["))
                    contentPreview = contentPreview.substring(0, contentPreview.indexOf("["));

                contentView.setText(contentPreview);
                postedAtView.setText(item.postedAt);

                if (item.clickable) {
                    mView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(PostListActivity.this, TipBrowserActivity.class);
                            intent.putExtra(TipBrowserActivity.ARG_SCREEN_ID, listIndex);
                            intent.putExtra(TipBrowserActivity.ARG_POSTS, posts);

                            // Start TipBrowser with transitions
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                PostListActivity.this.startActivityForResult(intent, MUST_RELOAD, ActivityOptions.makeSceneTransitionAnimation(PostListActivity.this).toBundle());
                            } else {
                                PostListActivity.this.startActivityForResult(intent, MUST_RELOAD);
                            }
                        }
                    });
                }
            }

            @Override
            public String toString() {
                return super.toString() + " '" + contentView.getText() + "'";
            }
        }
    }
}
