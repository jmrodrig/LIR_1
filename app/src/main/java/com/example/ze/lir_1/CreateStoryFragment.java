package com.example.ze.lir_1;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by Ze on 13/04/2015.
 */
public class CreateStoryFragment extends Fragment {
    private RequestQueue queue;
    private TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        queue = RequestsSingleton.getInstance(getActivity().getApplicationContext()).getRequestQueue();

        String url ="http://lostinreality.net/story";
        //newURL = new URL(url);

        StringRequest storyRequest = new StringRequest(url, new StoryResponseListener(), new StoryErrorListener());
        queue.add(storyRequest);

        return inflater.inflate(R.layout.activity_new_story, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        //textView = (TextView) getActivity().findViewById(R.id.textView2);
    }


    private class StoryResponseListener implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            //textView.setText(response);
            Toast.makeText(getActivity(), "Successefully got stories.", Toast.LENGTH_LONG).show();
        }
    }

    private class StoryErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Toast.makeText(getActivity(), "Sign in error. Please Try Again.", Toast.LENGTH_LONG).show();
        }
    }
}
