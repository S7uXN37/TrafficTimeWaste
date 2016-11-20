package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Random;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

@SuppressLint("SetJavaScriptEnabled")
class DatabaseLink {

    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_GENERATOR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXZY_-., 1234567890/?!";

    private static final String JSON_POSTS = "posts";
    private static final String JSON_ID = "id";
    private static final String JSON_CONTENT = "content";
    private static final String JSON_POSTED_AT = "posted_at";
    private static final String JSON_OWNER = "owner";
    private static final String JSON_VOTES_UP = "votes_up";
    private static final String JSON_VOTES_DOWN = "votes_down";
    private static final String JSON_TAGS = "tags";
    static final String JSON_SUCCESS = "success";
    static final String JSON_MESSAGE = "message";
    static final String JSON_VOTE_EXISTS = "vote_exists";
    static final String JSON_IS_LIKE = "is_like";

    private static final String GET_ALL_URL = "https://stuxnet.byethost13.com/PU/get_all_posts.php";
    private static final String GET_WITH_TAG_URL = "https://stuxnet.byethost13.com/PU/get_posts_by_tag.php";
    private static final String DO_VOTE_URL = "https://stuxnet.byethost13.com/PU/vote_on_post.php";
    private static final String REMOVE_VOTE_URL = "https://stuxnet.byethost13.com/PU/remove_vote.php";
    private static final String LOGIN_URL = "https://stuxnet.byethost13.com/PU/test_login.php";
    private static final String GET_VOTED_ON_URL = "https://stuxnet.byethost13.com/PU/get_voted_on.php";
    private static final String CREATE_USER_URL = "https://stuxnet.byethost13.com/PU/create_user.php";
    private static final String CREATE_POST_URL = "https://stuxnet.byethost13.com/PU/create_post.php";
    private static final String REMOVE_POST_URL = "https://stuxnet.byethost13.com/PU/remove_post.php";

    private static SecurePreferences prefs;
    static DatabaseLink instance = null;

    private final Activity activity;
    private X509TrustManager[] trustManagers;
    private WebView lastWebView;

    String USERNAME;
    private String PASSWORD;

    DatabaseLink(Activity parentActivity) {
        if (instance != null)
            throw new IllegalStateException("Singleton pattern violated");

        if (prefs == null)
            throw new IllegalStateException("SecurePreferences must be initialized before creating DatabaseLink instances");

        activity = parentActivity;
        try {
            // Load CA certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            Certificate ca;
            try (InputStream caInput =
                         new BufferedInputStream(parentActivity.getResources().openRawResource(R.raw.cert_intermediate_ca))) {
                ca = cf.generateCertificate(caInput);
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
            Log.e("TrafficTimeWaste", "Error instantiating DatabaseLink", e);
        }

        // DEBUG: TEST PREFERENCES -> Working
//        if (BuildConfig.DEBUG) {
//            SharedPreferences testPrefs = parentActivity.getSharedPreferences("testPreferences", Context.MODE_PRIVATE);
//            testPrefs.edit().putInt("testKey", 1).commit();
//            testPrefs = parentActivity.getSharedPreferences("testPreferences", Context.MODE_PRIVATE);
//            if (1 != testPrefs.getInt("testKey", 0)) {
//                Log.e("SharedPreferences", "Preferences not working");
//            } else
//                Log.v("SharedPreferences", "Preferences working");
//        }

        // Read credentials
        USERNAME = prefs.getString(KEY_USERNAME);
        PASSWORD = prefs.getString(KEY_PASSWORD);
        if (USERNAME != null && PASSWORD != null)
            Log.v("SecurePreferences", "Loaded credentials for: "+USERNAME);
        else
            Log.e("SecurePreferences", "Unable to load credentials");


        if (USERNAME == null || PASSWORD == null) {
            // Open dialog prompting login
            AlertDialog dialog = new AlertDialog.Builder(parentActivity)
                    .setMessage(R.string.login_prompt_message)
                    .setTitle(R.string.login_prompt_title)
                    .setPositiveButton(R.string.action_login, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Open LoginActivity
                            Intent intent = new Intent(activity, LoginActivity.class);
                            activity.startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel dialog
                        }
                    })
                    .create();
            dialog.show();
        }

        instance = this;
    }

    static void initPreferences(Activity activity) {
        try {
            Random rand = new Random(Installation.id(activity).hashCode());
            StringBuilder key = new StringBuilder("SampleKey");
            int m = Build.VERSION.SDK_INT;
            for (int i = 0; i < 113; i+=1) {
                key.append(KEY_GENERATOR.charAt(m % KEY_GENERATOR.length()));
                key.append(KEY_GENERATOR.charAt(i % KEY_GENERATOR.length()));
                key.append(key.charAt(i % key.length()));
                m += rand.nextInt()/513;
            }
            prefs = new SecurePreferences(activity, key.toString(), key.reverse().insert(1, Build.VERSION.SDK_INT).toString());
        } catch (GeneralSecurityException e) {
            Log.e("DatabaseLink", "Could not initialize SecurePreferences", e);
        }
    }

    void getAllPosts(DatabaseListener databaseListener) {
        loadUrl(databaseListener, GET_ALL_URL, null);
    }
    void getPostsWithTag(DatabaseListener databaseListener, String tag) {
        loadUrl(databaseListener, GET_WITH_TAG_URL, "tag_name=" + tag);
    }
    void createPost(DatabaseListener listener, String content, String tags) {
        loadUrl(listener, CREATE_POST_URL, "post_content=" + content + "&tags=" + tags + "&username=" + USERNAME + "&password=" + PASSWORD);
    }
    void deletePost(DatabaseListener listener, int postId) {
        loadUrl(listener, REMOVE_POST_URL, "post_id=" + postId + "&username=" + USERNAME + "&password=" + PASSWORD);
    }
    private void createUser(DatabaseListener listener, String username, String password) {
        loadUrl(listener, CREATE_USER_URL, "username=" + username + "&password=" + password);
    }
    String createUserSync(String username, String password) {
        final SparseArray<String> responseHolder = new SparseArray<>(1);
        final int id = 0;

        DatabaseListener listener = new DatabaseListener() {
            @Override
            void onGetResponse(String json) {
                responseHolder.put(id, json);
            }

            @Override
            void onError(String errorMsg) {
                responseHolder.put(id, errorMsg);
            }
        };

        createUser(listener, username, password);

        String stored;
        while ((stored = responseHolder.get(id)) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e("TrafficTimeWaste", "Error creating user", e);
            }
        }

        return stored;
    }
    private void testAuthentication(DatabaseListener listener, String username, String password) {
        loadUrl(listener, LOGIN_URL, "username=" + username + "&password=" + password);
    }
    String testAuthenticationSync(String username, String password) {
        final SparseArray<String> responseHolder = new SparseArray<>(1);
        final int id = 0;

        DatabaseListener listener = new DatabaseListener() {
            @Override
            void onGetResponse(String json) {
                responseHolder.put(id, json);
            }

            @Override
            void onError(String errorMsg) {
                responseHolder.put(id, errorMsg);
            }
        };

        testAuthentication(listener, username, password);

        String stored;
        while ((stored = responseHolder.get(id)) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e("TrafficTimeWaste", "Error testing authentication", e);
            }
        }

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

    boolean isLoggedIn() {
        return USERNAME != null && PASSWORD != null;
    }

    static void saveCredentials(String username, String password) {
        if (username == null || password == null)
            return;

        prefs.put(KEY_USERNAME, username);
        prefs.put(KEY_PASSWORD, password);

        instance.USERNAME = username;
        instance.PASSWORD = password;

        Log.v("SecurePreferences", "Saving credentials for: "+username);
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
                            Log.v("TrafficTimeWaste", "Manually trusting certificate: " + error.getCertificate());
                            handler.proceed();
                        } else {
                            Log.v("TrafficTimeWaste", "SSl error: " + error);
                            databaseListener.onError(error.toString());
                            super.onReceivedSslError(view,handler,error);
                        }
                    }
                });

                // load page
                if (postData != null) {
                    byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
                    webView.postUrl(url, postDataBytes);
                } else {
                    webView.loadUrl(url);
                }

                lastWebView = webView;
                Log.v("TrafficTimeWaste", "Request sent...");
            }
        };
        activity.runOnUiThread(run);
    }

    static Post[] parseJson(JSONObject json) throws JSONException {
        Log.v("TrafficTimeWaste", "JSON: " + json);

        if (json.getInt(JSON_SUCCESS) != 1)
            return new Post[0];

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
        private final DatabaseListener databaseListener;

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

    static String encodeSpecial(String str) {
        str = str.replaceAll("ä", "[ae]");
        str = str.replaceAll("ö", "[oe]");
        str = str.replaceAll("ü", "[ue]");
        str = str.replaceAll("\"", "[quot]");
        str = str.replaceAll("=", "[eq]");
        return str;
    }

    static String decodeSpecial(String str) {
        str = str.replaceAll("\\[ae\\]", "ä");
        str = str.replaceAll("\\[oe\\]", "ö");
        str = str.replaceAll("\\[ue\\]", "ü");
        str = str.replaceAll("\\[quot\\]", "\"");
        str = str.replaceAll("\\[eq\\]", "=");
        return str;
    }

}
