package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    public static int sortType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        setupRecyclerViewAsync((FrameLayout) findViewById(R.id.frameLayout));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_post_list, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                // Create AlertDialog with radio buttons to select ordering
                final Activity activity = this;
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
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
                                activity.recreate();
                            }
                        });
                // Display dialog
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupRecyclerViewAsync(@NonNull final ViewGroup viewGroup) {
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Loading posts...", "Please wait", true, false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        final Context context = this;
        DatabaseLink.DatabaseListener listener = new DatabaseLink.DatabaseListener() {
            @Override
            void onGetPosts(final Post[] posts) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                final Post[] sortedPosts = sortPosts(posts);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Inflate layout post_list
                        RecyclerView recyclerView =
                                (RecyclerView) View.inflate(context, R.layout.post_list, null)
                                        .findViewById(R.id.post_list);
                        assert recyclerView != null;

                        // Set adapter with posts
                        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(sortedPosts));

                        // Add to ViewGroup
                        viewGroup.addView(recyclerView);
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
            new DatabaseLink(this).getPostsWithTag(listener, intent.getStringExtra(SearchManager.QUERY));
        else
            new DatabaseLink(this).getAllPosts(listener);
        Log.v("TrafficTimeWaste", "Querying db...");
    }

    public static Post[] sortPosts(Post[] posts) {
        Comparator<Post> comparator = null;
        switch (SORT_TYPE.values()[sortType]) {
            case NEWEST:
                comparator = new Comparator<Post>() {
                    @Override
                    public int compare(Post post, Post t1) {
                        return -Long.compare(post.postedAtMillis, t1.postedAtMillis);
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
                        return -Integer.compare(post.votesUp, t1.votesUp);
                    }
                };
                break;
            case DOWNVOTES:
                comparator = new Comparator<Post>() {
                    @Override
                    public int compare(Post post, Post t1) {
                        return -Integer.compare(post.votesDown, t1.votesDown);
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

            Log.v("TrafficTimeWaste", "Updating Holder, post: " + p.toString());
            holder.update(p, position);
        }

        @Override
        public int getItemCount() {
            return Math.max(1, posts.length);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final View mView;
            final TextView idView;
            final TextView contentView;
            final TextView postedAtView;
            Post post;

            ViewHolder(View view) {
                super(view);
                mView = view;
                idView = (TextView) view.findViewById(R.id.id);
                contentView = (TextView) view.findViewById(R.id.content);
                postedAtView = (TextView) view.findViewById(R.id.postedAt);
            }

            void update(Post item, final int listIndex) {
                post = item;
                String id = ""+item.id;
                idView.setText(id);
                contentView.setText(item.content);
                postedAtView.setText(item.postedAt);

                if (item.clickable) {
                    mView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context context = v.getContext();
                            Intent intent = new Intent(context, TipBrowserActivity.class);
                            intent.putExtra(TipBrowserActivity.ARG_SCREEN_ID, listIndex);
                            intent.putExtra(TipBrowserActivity.ARG_POSTS, posts);

                            // Start TipBrowser with transitions
                            context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(PostListActivity.this).toBundle());
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
