package com.example.ze.lir_1;

import android.content.Context;
import android.location.*;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Ze on 13/04/2015.
 */
public class StoryListFragment extends Fragment  {
    private ListView storyListView;
    private StoryListAdapter storyListAdapter;
    private ArrayList<StoryItem> storyList;
    public Boolean askedToFetchStories = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askedToFetchStories = false;

        storyListAdapter = new StoryListAdapter(getActivity(),this);
        return inflater.inflate(R.layout.story_list_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        storyListView = (ListView) getActivity().findViewById(R.id.story_container);
        storyListView.setAdapter(storyListAdapter);

        LatLng location = (((MainActivity) getActivity()).getMapCurrentFocusLocation());
        //((MainActivity) getActivity()).fetchStories();
        ArrayList<StoryItem> stories = ((MainActivity) getActivity()).getFetchedStories();
        if (stories != null)
            updateStoryListAdapter(stories,location);
        else {
            ((MainActivity) getActivity()).fetchStories();
            updateStoryListAdapterAfterFetched();
        }

        // TODO: save fragment state
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: save fragment state
    }

    public StoryListAdapter getStoryListAdapter() {
        return storyListAdapter;
    }


    public ArrayList<StoryItem> sortStoryListByProximity(ArrayList<StoryItem> storyList ,LatLng userLatLng) {
        if (storyList.size()==0) return storyList; //Nothing to Sort
        ArrayList<StoryItem> stories = new ArrayList<>(storyList);
        ArrayList<StoryItem> sortedList = new ArrayList<>();
        StoryItem newItem  = stories.get(0);
        sortedList.add(newItem);
        stories.remove(newItem);
        while (stories.size() != 0) {
            newItem = stories.get(0);
            stories.remove(newItem);
            float newD = calculateTwoPointsDistance(userLatLng,newItem.getStoryLatLng());
            for (int i = 0; i < sortedList.size();i++ ) {
                StoryItem pivot = sortedList.get(i);
                float d = calculateTwoPointsDistance(userLatLng,pivot.getStoryLatLng());
                if (newD <= d) {
                    sortedList.add(i, newItem);
                    break;
                } else if (i==sortedList.size()-1) {
                    sortedList.add(newItem);
                }
            }
        }
        return sortedList;
    }

    private float calculateTwoPointsDistance(LatLng lc1, LatLng lc2) {
        float[] results = new float[4];
        Location.distanceBetween(lc1.latitude,lc1.longitude,lc2.latitude,lc2.longitude,results);
        float distance = results[0];
        return distance;
    }

    public void updateStoryListAdapter(ArrayList storyList, LatLng currentLocation) {
        if (currentLocation != null) {
            //TODO
            storyList = selectStoriesAround(storyList, currentLocation, 500);
            //IF NONE HIDE LIST VIEW AND DISPLAY A BUTTON TO CREATE A STORY
            if (storyList.size() == 0) {
                storyListView.setVisibility(View.GONE);
                getActivity().findViewById(R.id.no_stories_warning).setVisibility(View.VISIBLE);
            } else {
                storyList = sortStoryListByProximity(storyList, currentLocation);
                storyListAdapter.setStoryList(storyList);
                storyListAdapter.notifyDataSetChanged();
                getActivity().findViewById(R.id.no_stories_warning).setVisibility(View.GONE);
                storyListView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateStoryListAdapterAfterFetched() {
        askedToFetchStories = true;
    }

    private ArrayList selectStoriesAround(ArrayList<StoryItem> storyList, LatLng location, float radius) {
        ArrayList<StoryItem> storiesAround = new ArrayList<>();
        for (StoryItem st : storyList) {
            float distance = calculateTwoPointsDistance(st.getStoryLatLng(),location);
            if (distance <= radius)
                storiesAround.add(st);
        }
        return storiesAround;
    }

    public void deleteStory(Integer storyId) {
        RequestQueue queue = RequestsSingleton.getInstance(getActivity()).getRequestQueue();
        String url = "http://lostinreality.net/story/" + storyId + "/publish/0";
        StringRequest deleteRequest = new StringRequest(Request.Method.POST,url, new DeleteSuccess(getActivity()), new DeleteError(getActivity()));
        queue.add(deleteRequest);
    }

    /**
     * Runs when a JsonArrayRequest object successfully gets an response.
     */
    class DeleteSuccess implements Response.Listener<String> {
        private Context context;

        public DeleteSuccess(Context ctx) {
            context = ctx;
        }

        @Override
        public void onResponse(String response) {
            Toast.makeText(context, "Story was unpublished!", Toast.LENGTH_LONG).show();
            ((MainActivity) getActivity()).fetchStories();
            updateStoryListAdapterAfterFetched();
        }
    }


    class DeleteError implements Response.ErrorListener {
        private Context context;

        public DeleteError(Context ctx) {
            context = ctx;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            // TODO Make an error handler
            Toast.makeText(context, "Ups! There was an error...", Toast.LENGTH_LONG).show();
        }
    }
}
