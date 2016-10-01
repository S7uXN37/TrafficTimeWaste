package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

@SuppressLint("SetJavaScriptEnabled")
class DatabaseLink {

    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private static final String JSON_POSTS = "posts";
    private static final String JSON_ID = "id";
    private static final String JSON_CONTENT = "content";
    private static final String JSON_POSTED_AT = "posted_at";
    private static final String JSON_OWNER = "owner";
    private static final String JSON_VOTES_UP = "votes_up";
    private static final String JSON_VOTES_DOWN = "votes_down";
    private static final String JSON_TAGS = "tags";
    static final String JSON_SUCCESS = "success";
    static final String JSON_VOTE_EXISTS = "vote_exists";
    static final String JSON_IS_LIKE = "is_like";

    private static final String GET_ALL_URL = "https://stuxnet.byethost13.com/PU/get_all_posts.php";
    private static final String GET_WITH_TAG_URL = "https://stuxnet.byethost13.com/PU/get_posts_by_tag.php";
    private static final String DO_VOTE_URL = "https://stuxnet.byethost13.com/PU/vote_on_post.php";
    private static final String REMOVE_VOTE_URL = "https://stuxnet.byethost13.com/PU/remove_vote.php";
    private static final String LOGIN_URL = "https://stuxnet.byethost13.com/PU/test_login.php";
    private static final String GET_VOTED_ON_URL = "https://stuxnet.byethost13.com/PU/get_voted_on.php";

    private Activity activity;
    private X509TrustManager[] trustManagers;
    private WebView lastWebView;

    private String USERNAME, PASSWORD;

    DatabaseLink(Activity parentActivity) {
        this(parentActivity, false);
    }
    DatabaseLink(Activity parentActivity, boolean forceLogin) {
        activity = parentActivity;
        try {
            // Load CA certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            Certificate ca;
            try (InputStream caInput =
                         new BufferedInputStream(parentActivity.getResources().openRawResource(R.raw.cert_intermediate_ca))) {
                ca = cf.generateCertificate(caInput);
                Log.v("TrafficTimeWaste", "CA=" + ((X509Certificate) ca).getSubjectDN());
            } catch (IOException e) {
                throw new IOException("Failed to load CA certificate", e);
            }

            // Create KeyStore from trusted CA
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create TrustManagerFactory from KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Cast TrustManagers from TrustManagerFactory to X509TrustManagers and store in trustManagers
            trustManagers = new X509TrustManager[tmf.getTrustManagers().length];
            for (int i = 0; i < trustManagers.length; i++) {
                trustManagers[i] = (X509TrustManager) tmf.getTrustManagers()[i];
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }

        // Read credentials
        SecurePreferences preferences = getPreferences(parentActivity);
        USERNAME = preferences.getString(KEY_USERNAME);
        PASSWORD = preferences.getString(KEY_PASSWORD);

        if ((USERNAME == null || PASSWORD == null) && forceLogin) {
            // Open LoginActivity
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.putExtra(LoginActivity.ARG_PREFS, (Serializable) preferences);
            parentActivity.startActivity(intent);
        }
    }

    void getAllPosts(DatabaseListener databaseListener) {
        loadUrl(databaseListener, GET_ALL_URL, null);
    }
    void getPostsWithTag(DatabaseListener databaseListener, String tag) {
        loadUrl(databaseListener, GET_WITH_TAG_URL, "tag_name=" + tag);
    }
    void testAuthentication(DatabaseListener listener, String username, String password) {
        loadUrl(listener, LOGIN_URL, "username=" + username + "&password=" + password);
    }
    private final SparseArray<String> syncedAccesses = new SparseArray<>();
    String testAuthenticationSync(String username, String password) {
        int syncedSize;
        synchronized (syncedAccesses) {
            syncedSize = syncedAccesses.size();
            syncedAccesses.put(syncedSize, null);
        }
        final int id = syncedSize;

        DatabaseListener listener = new DatabaseListener() {
            @Override
            void onGetResponse(String json) {
                syncedAccesses.put(id, json);
            }

            @Override
            void onError(String errorMsg) {
                syncedAccesses.put(id, errorMsg);
            }
        };

        testAuthentication(listener, username, password);

        String stored;
        while ((stored = syncedAccesses.get(id)) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        syncedAccesses.put(id, null);

        return stored;
    }
    void voteOnPost(DatabaseListener listener, int postId, boolean voteUp) {
        loadUrl(listener, DO_VOTE_URL, "username=" + USERNAME + "&password=" + PASSWORD + "&post_id=" + postId + "&vote=" + (voteUp ? '1' : '0'));
    }
    void removeVote(DatabaseListener listener, int postId) {
        loadUrl(listener, REMOVE_VOTE_URL, "username=" + USERNAME + "&password=" + PASSWORD + "&post_id=" + postId);
    }
    void getVotedOnPost(DatabaseListener listener, int postId) {
        loadUrl(listener, GET_VOTED_ON_URL, "username=" + USERNAME + "&password=" + PASSWORD + "&post_id=" + postId);
    }

    static void saveCredentials(Activity activity, String username, String password) {
        SecurePreferences prefs = getPreferences(activity);
        prefs.put(KEY_USERNAME, username);
        prefs.put(KEY_PASSWORD, password);
    }
    private static SecurePreferences getPreferences(Activity activity) {
        String key = "MyTopSecretKey";
        for (int i = 0; i < 43; i+=3)
            key += key.charAt(i%key.length());
        return new SecurePreferences(activity, "credentials", key, true);
    }

    private void loadUrl(
            final DatabaseListener databaseListener, final String url, final String postData) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                // Create a WebView to load page and execute contained JavaScript
                WebView webView = new WebView(activity);
                webView.getSettings().setJavaScriptEnabled(true);
                // Add JavaScriptInterface to return JSON
                webView.addJavascriptInterface(new JsonGrabberJavaScriptInterface(databaseListener), "JsonGrabber");

                // Add WebViewClient to handle certificate verification and return response JSON
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // Called after page is done loading, inject JavaScript to send JSON to JavaScriptInterface
                        // Produces error and doesn't invoke listener if no JSON is found
                        Log.v("TrafficTimeWaste", "Page finished, injecting JavaScript...");
                        view.loadUrl("javascript:window.JsonGrabber.submitJson(document.getElementsByTagName('json')[0].innerHTML);");
                    }

                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        // SSL certificate was not trusted, check against trustManagers

                        // Extract X509Certificate from SslError
                        Bundle bundle = SslCertificate.saveState(error.getCertificate());
                        X509Certificate x509Certificate;
                        byte[] bytes = bundle.getByteArray("x509-certificate");
                        if (bytes == null) {
                            x509Certificate = null;
                        } else {
                            try {
                                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                                x509Certificate = (X509Certificate) cert;
                            } catch (CertificateException e) {
                                x509Certificate = null;
                            }
                        }

                        // Check against each TrustManager, stop as soon as it's trusted
                        boolean trusted = false;
                        for (int i = 0; i < trustManagers.length && !trusted; i++) {
                            X509TrustManager tm = trustManagers[i];
                            try {
                                tm.checkServerTrusted(new X509Certificate[]{x509Certificate}, "RSA");
                                trusted = true;
                            } catch (CertificateException e) {
                                Log.v("TrafficTimeWaste", "Verification failed at i=" + i + ", error: " + e.getMessage());
                            }
                        }

                        // Callback to handler
                        if (trusted) {
                            Log.v("TrafficTimeWaste", "Manually trusting certificate: " + error.getCertificate().toString());
                            handler.proceed();
                        } else {
                            Log.v("TrafficTimeWaste", "SSl error: " + error.toString());
                            databaseListener.onError(error.toString());
                            super.onReceivedSslError(view,handler,error);
                        }
                    }
                });

                // load page
                if (postData != null) {
                    byte[] bytes;
                    try {
                        bytes = URLEncoder.encode(postData, "UTF-8").getBytes();
                    } catch (UnsupportedEncodingException e) {
                        bytes = postData.getBytes();
                    }
                    webView.postUrl(url, bytes);
                } else {
                    webView.loadUrl(url);
                }

                lastWebView = webView;
                Log.v("TrafficTimeWaste", "Request sent");
            }
        };
        activity.runOnUiThread(run);
    }

    static Post[] parseJson(JSONObject json) throws IllegalArgumentException, JSONException {
        Log.v("TrafficTimeWaste", "JSON: " + json);

        if (json.getInt(JSON_SUCCESS) != 1)
            throw new IllegalArgumentException("JSON indicating failure in database access");

        JSONArray postsJson = json.getJSONArray(JSON_POSTS);
        ArrayList<Post> posts = new ArrayList<>();
        for (int i = 0; i < postsJson.length(); i++) {
            JSONObject postJson = postsJson.getJSONObject(i);
            int id = postJson.getInt(JSON_ID);
            String content = postJson.getString(JSON_CONTENT);
            String postedAt = postJson.getString(JSON_POSTED_AT);
            String owner = postJson.getString(JSON_OWNER);
            int votesUp = postJson.getInt(JSON_VOTES_UP);
            int votesDown = postJson.getInt(JSON_VOTES_DOWN);

            JSONArray tagsJson = postJson.getJSONArray(JSON_TAGS);
            String[] tags = new String[tagsJson.length()];

            for (int j = 0; j < tags.length; j++) {
                tags[j] = tagsJson.getString(j);
            }


            posts.add(new Post(id, content, postedAt, owner, votesUp, votesDown, tags));
        }

        Post[] postArray = new Post[posts.size()];
        for (int i = 0; i < postArray.length; i++)
            postArray[i] = posts.get(i);

        return postArray;
    }

    private void resetWebView() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                // Reset WebView, aborting all ongoing tasks on page
                lastWebView.loadUrl("about:blank");
                Log.v("TrafficTimeWaste", "WebView was reset");
            }
        };
        activity.runOnUiThread(run);
    }

    static abstract class DatabaseListener {
        abstract void onGetResponse(String json);
        abstract void onError(String errorMsg);
    }

    private class JsonGrabberJavaScriptInterface {
        private DatabaseListener databaseListener;

        JsonGrabberJavaScriptInterface(DatabaseListener listener) {
            databaseListener = listener;
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void submitJson(String json) {
            Log.v("TrafficTimeWaste", "submitJson(): " + json);

            databaseListener.onGetResponse(json);
            resetWebView();
        }
    }

    Activity getActivity() {
        return activity;
    }

}
