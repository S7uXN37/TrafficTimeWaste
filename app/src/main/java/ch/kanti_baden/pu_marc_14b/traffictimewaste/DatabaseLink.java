package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class DatabaseLink {

    public static final String JSON_POSTS = "posts";
    public static final String JSON_ID = "id";
    public static final String JSON_POSTED_AT = "posted_at";
    public static final String JSON_OWNER = "owner";
    public static final String JSON_VOTES_UP = "votes_up";
    public static final String JSON_VOTES_DOWN = "votes_down";
    public static final String JSON_TAGS = "tags";
    public static final String JSON_SUCCESS = "success";

    public static final String GET_ALL_URL = "https://stuxnet.byethost13.com/PU/get_all_posts.php";

    private RequestQueue requestQueue;

    public DatabaseLink(Context context) {
        try {
            // Load CA certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(context.getResources().openRawResource(R.raw.cert_intermediate_ca));

            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                Log.v("TrafficTimeWaste", "CA=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            // Create KeyStore from trusted CA
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);


            // Create TrustManager from KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create SSLContext from TrustManager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create HttpStack from SSLContext's SocketFactory
            HttpStack httpStack = new HurlStack() {
                @Override
                protected HttpURLConnection createConnection(URL url) throws IOException {
                    HttpURLConnection httpURLConnection = super.createConnection(url);

                    if (httpURLConnection instanceof HttpsURLConnection) {
                        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) httpURLConnection;

                        try {
                            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        httpURLConnection = httpsURLConnection;
                    }

                    return httpURLConnection;
                }
            };

            requestQueue = Volley.newRequestQueue(context, httpStack);
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

    }

    public void getAllPosts(final DatabaseListener databaseListener) {
        // create json request
        StringRequest jsonRequest = new StringRequest
                (Request.Method.GET, GET_ALL_URL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.v("TrafficTimeWaste", response.toString());
//                databaseListener.onGetPosts(posts);
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                databaseListener.onError(error);
            }

        });

        // add request to queue
        requestQueue.add(jsonRequest);
    }

    public static abstract class DatabaseListener {
        abstract void onGetPosts(Post[] posts);
        abstract void onError(VolleyError error);
    }

}
