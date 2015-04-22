package com.example.ze.lir_1;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ze on 18-04-2015.
 */
public class SessionUser extends Application {
    private static SessionUser mInstance;
    private User userInfo;
    private Context activityContext;

    private SessionUser(User user){
        userInfo = user;
    }

    public static synchronized SessionUser getInstance() {
        return mInstance;
    }

    public static synchronized SessionUser initializeSessionUser(User user) {
        if (mInstance == null) {
            mInstance = new SessionUser(user);
        }
        return mInstance;
    }

    class User {
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String avatarUrl;

        public User(String first, String last, String full, String ml, String url) {
            firstName=first;
            lastName=last;
            fullName=full;
            email=ml;
        }
    }

    // Getters
    public String getUserFullName() { return userInfo.fullName; }

    public String getUserEmail() { return userInfo.email; }

    public String getUserAvatar() {
        if (userInfo.avatarUrl==null) {
            //TODO: replace
            return "http://lostinreality.net/assets/images/lir-logo.png";
        }
        return userInfo.avatarUrl;
    }

    public static List<Cookie> getSessionCookieList() {
        CookieStore store = RequestsSingleton.getHttpClient().getCookieStore();
        List<Cookie> cookieList = store.getCookies();
        return cookieList;
    }
}
