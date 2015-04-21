package com.example.ze.lir_1;

import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * Created by Ze on 17-04-2015.
 */
public class FetchTokenTask extends AsyncTask<Void,Void,Void> {
    private LoginActivity mActivity;
    private GoogleApiClient mGoogleApiClient;

    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

    public FetchTokenTask(LoginActivity activity, GoogleApiClient googleApiClient) {
        this.mActivity = activity;
        this.mGoogleApiClient = googleApiClient;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        try {
            fetchResponseFromServer();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void fetchResponseFromServer() throws IOException, JSONException {
        //String token = fetchToken();
//        if (token == null) {
//            // error has already been handled in fetchToken()
//            return;
//        }
        URL url = new URL("http://lostinreality.net/authenticate/google");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int sc = con.getResponseCode();
        if (sc == 200) {
            InputStream is = con.getInputStream();
            is.read();
            is.close();
        } else if (sc == 401) {
//            GoogleAuthUtil.invalidateToken(mActivity, token);
            //onError("Server auth error, please try again.", null);
            //Log.i(TAG, "Server auth error: " + readResponse(con.getErrorStream()));
        } else {
            //onError("Server returned the following error code: " + sc, null);
        }
    }

    private String fetchToken() throws IOException {

        //String scopes = "oauth2:http://lostinreality.net:client_id:<SERVER-CLIENT-ID>:api_scope:<SCOPE1> <SCOPE2>";
        String scopes = "oauth2:server:client_id:1022720210003-tc6r0q6dtp2t0vn64g6mf41pmb7n5ooc.apps.googleusercontent.com:api_scope:https://www.googleapis.com/auth/plus.login";
        try {
            return GoogleAuthUtil.getToken(
                    mActivity,
                    Plus.AccountApi.getAccountName(mGoogleApiClient),
                    scopes
            );
        } catch (UserRecoverableAuthException e) {
            Intent intent = e.getIntent();
            mActivity.startActivityForResult(intent,
                    REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
        } catch (GoogleAuthException fatalException) {
            fatalException.printStackTrace();
        }
        return null;
    }
}
