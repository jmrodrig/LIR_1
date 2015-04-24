package com.example.ze.lir_1;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.gson.Gson;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;


/**
 * A login screen that offers login via email/password and via Google+ sign in.
 * <p/>
 * ************ IMPORTANT SETUP NOTES: ************
 * In order for Google+ sign in to work with your app, you must first go to:
 * https://developers.google.com/+/mobile/android/getting-started#step_1_enable_the_google_api
 * and follow the steps in "Step 1" to create an OAuth 2.0 client for your package.
 */
public class LoginActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOGIN_REQUEST_TAG = "login_request_tag";
    private static final int RC_SIGN_IN = 0;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIntentInProgress;
    private boolean mSignInClicked;
    private SignInButton mPlusSignInButton;
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;

    private View mLoginFormView;
    private View mProgressView;
    private View mEmailLoginFormView;
    private View mSignOutButtons;

    private RequestQueue queue;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        /**
         *     CONNECT GOOGLE API CLIENT
         */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

        /* GOOGLE+ SIGN IN BUTTON */
        mPlusSignInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);
        mPlusSignInButton.setOnClickListener(new SignInButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /*if (!mGoogleApiClient.isConnected()) {
                            if (v.getId() == R.id.plus_sign_in_button && !mGoogleApiClient.isConnecting()) {
                                mSignInClicked = true;
                                mGoogleApiClient.connect();
                            }
                        } else {
                            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                            mGoogleApiClient.disconnect();
                            //mGoogleApiClient.connect();
                        }*/
                        loadMainActivity();
                    }
                });

        /* USERNAME + PASSWORD SIGN IN */
        // Instantiate the RequestQueue.
        queue = RequestsSingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        // Instantiate UI.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptUserPassSignIn();
            }
        });

        findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://lostinreality.net/signup"));
                startActivity(browserIntent);
            }
        });



        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mEmailLoginFormView = findViewById(R.id.email_login_form);
        mSignOutButtons = findViewById(R.id.plus_sign_out_buttons);
    }

    private void attemptUserPassSignIn() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);



        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        saveLoginCredentials(email,password);

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            signIn(email,password);
        }
    }

    private void signIn(String email,String password) {

        String signInUrl = "http://lostinreality.net/authenticate/userpass"
                + "?username=" + email + "&password=" + password;

        StringRequest signInRequest = new StringRequest(signInUrl, new LoginResponseListener(this), new LoginErrorListener(this));
        signInRequest.setTag(LOGIN_REQUEST_TAG);
        queue.add(signInRequest);
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    protected void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();

        String[] cred = getSavedLoginCredentials();
        mEmailView.setText(cred[0]);
        mPasswordView.setText(cred[1]);
    }

    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        if (queue != null) {
            queue.cancelAll(LOGIN_REQUEST_TAG);
        }
    }

    public void onConnected(Bundle connectionHint) {
        mSignInClicked = false;
        //TODO: move to Main Activity
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
        loadMainActivity();
        //FetchTokenTask  tokenTask = new FetchTokenTask(this,mGoogleApiClient);
        //tokenTask.execute();
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress) {
            if (mSignInClicked  && result.hasResolution()) {
                try {
                    mIntentInProgress = true;
                    startIntentSenderForResult(result.getResolution().getIntentSender(),
                            RC_SIGN_IN, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    // The intent was canceled before it was sent.  Return to the default
                    // state and attempt to connect to get an updated ConnectionResult.
                    mIntentInProgress = false;
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
        // TODO: Understand when this is called and if there is a way to confirm log out
        Toast.makeText(this, "User signed out!", Toast.LENGTH_LONG).show();
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }


    private class LoginResponseListener implements Response.Listener<String> {
        private Context context;

        public LoginResponseListener(Context ctx) {context = ctx;}

        @Override
        public void onResponse(String response) {
            //Toast.makeText(context, "User is connected!", Toast.LENGTH_LONG).show();

            openSessionUser();
        }
    }

    private class LoginErrorListener implements Response.ErrorListener {
        private Context context;

        public LoginErrorListener(Context ctx) {context = ctx;}
        @Override
        public void onErrorResponse(VolleyError error) {
            showProgress(false);
            Toast.makeText(context, "Sign in error. Please Try Again.", Toast.LENGTH_LONG).show();
        }
    }

    private void openSessionUser() {
        String url = "http://lostinreality.net/user";
        JsonObjectRequest fetchUserInfoRequest = new JsonObjectRequest(Request.Method.GET, url, null, new UserResponseListener(this), new UserErrorListener(this));
        queue.add(fetchUserInfoRequest);
    }

    /**
     * Runs when a JsonArrayRequest object successfully gets an response.
     */
    class UserResponseListener implements Response.Listener<JSONObject>{
        private Context context;
        public UserResponseListener(Context ctx) {context = ctx;}
        @Override
        public void onResponse(JSONObject response) {
            SessionUser.User user = new Gson().fromJson(response.toString(), SessionUser.User.class) ;
            SessionUser.initializeSessionUser(user);
            Toast.makeText(context, "Hi, " + SessionUser.getInstance().getUserFullName().split(" ",2)[0] + "!",
                                                                                    Toast.LENGTH_LONG).show();
            loadMainActivity();
        }
    }

    class UserErrorListener implements Response.ErrorListener{
        private Context context;
        public UserErrorListener(Context ctx) {context = ctx;}
        @Override
        public void onErrorResponse(VolleyError error) {
            // TODO Make an error handler
            Toast.makeText(context, "Sign in error. Please Try Again.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void saveLoginCredentials(String email, String password) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("username", email);
        editor.putString("password", password);
        editor.commit();
    }

    private String[] getSavedLoginCredentials(){
        String[] cred = new String[2];
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        cred[0] = sharedPref.getString("username","");
        cred[1] = sharedPref.getString("password","");

        return cred;
    }





}



