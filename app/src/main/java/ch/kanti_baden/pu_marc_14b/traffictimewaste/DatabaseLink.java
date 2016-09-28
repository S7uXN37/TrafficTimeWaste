package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
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

    private static final String JSON_POSTS = "posts";
    private static final String JSON_ID = "id";
    private static final String JSON_CONTENT = "content";
    private static final String JSON_POSTED_AT = "posted_at";
    private static final String JSON_OWNER = "owner";
    private static final String JSON_VOTES_UP = "votes_up";
    private static final String JSON_VOTES_DOWN = "votes_down";
    private static final String JSON_TAGS = "tags";
    private static final String JSON_SUCCESS = "success";
    private static final String JSON_VOTE_EXISTS = "vote_exists";

    private static final String GET_ALL_URL = "https://stuxnet.byethost13.com/PU/get_all_posts.php";
    private static final String GET_WITH_TAG_URL = "https://stuxnet.byethost13.com/PU/get_posts_by_tag.php";
    private static final String DO_VOTE_URL = "https://stuxnet.byethost13.com/PU/vote_on_post.php";
    private static final String REMOVE_VOTE_URL = "https://stuxnet.byethost13.com/PU/remove_vote.php";
    private static final String LOGIN_URL = "https://stuxnet.byethost13.com/PU/test_login.php";
    private static final String GET_VOTED_ON_URL = "https://stuxnet.byethost13.com/PU/get_voted_on.php";

    private Activity activity;
    private X509TrustManager[] trustManagers;
    private WebView lastWebView;

    DatabaseLink(Activity parentActivity) {
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

    }

    void getAllPosts(DatabaseListener databaseListener) {
        loadPostsFromURL(databaseListener, GET_ALL_URL, null);
    }
    void getPostsWithTag(DatabaseListener databaseListener, String tag) {
        loadPostsFromURL(databaseListener, GET_WITH_TAG_URL, "tag_name=" + tag);
    }
    void testAuthentication(String username, String password) {
        loadJsonFromURL(LOGIN_URL, "username="+username+"&password="+password);
    }
    void voteOnPost(int postId, boolean isUpvote) {
        loadJsonFromURL(DO_VOTE_URL, "username="+username+"&password="+password+"&post_id="+postId+"&isUpvote="+(isUpvote?'1':'0'));
    }
    void removeVote(int postId) {
        loadJsonFromURL(REMOVE_VOTE_URL, "username="+username+"&password="+password+"&post_id="+postId);
    }
    void getVotedOnPost(int postId) {
        loadJsonFromURL(GET_VOTED_ON_URL, "username="+username+"&password="+password+"&post_id="+postId);
    }


    private void loadPostsFromURL(
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
                                Log.v("TrafficTimeWaste", "Verification failed at i="+i+", error: " + e.getMessage());
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
                        bytes = postData.getBytes("BASE64");
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

    private static Post[] parseJson(JSONObject json) throws IllegalArgumentException, JSONException {
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
        abstract void onGetPosts(Post[] posts);
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

            try {
                JSONObject jsonObject = new JSONObject(json);
                databaseListener.onGetPosts(DatabaseLink.parseJson(jsonObject));
                resetWebView();
            } catch (IllegalArgumentException | JSONException e) {
                databaseListener.onError("JSON is invalid. Error: " + e.getMessage() + ", JSON: " + json);
            }
        }
    }

}
