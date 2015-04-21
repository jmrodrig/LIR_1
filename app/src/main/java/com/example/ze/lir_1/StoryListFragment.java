package com.example.ze.lir_1;

import android.location.*;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        storyListAdapter = new StoryListAdapter(getActivity());
        return inflater.inflate(R.layout.story_list_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        storyListView = (ListView) getActivity().findViewById(R.id.story_container);
        storyListView.setAdapter(storyListAdapter);

        //fetch stories from server
        ((MainActivity) getActivity()).fetchStories();

        // TODO: save fragment state
    }

    @Override
    public void onResume() {
        super.onResume();


        //updateStoryListAdapter(this.storyList);

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
            Double newD = calculateTwoPointsDistance(userLatLng,newItem.getStoryLatLng());
            for (int i = 0; i < sortedList.size();i++ ) {
                StoryItem pivot = sortedList.get(i);
                Double d = calculateTwoPointsDistance(userLatLng,pivot.getStoryLatLng());
                if (newD <= d) {
                    sortedList.add(i, newItem);
                    stories.remove(newItem);
                    break;
                } else if (i==sortedList.size()-1) {
                    sortedList.add(i, newItem);
                    stories.remove(newItem);
                }
            }
        }
        return sortedList;
    }

    public Double calculateTwoPointsDistance(LatLng lc1, LatLng lc2) {
        return Math.sqrt(Math.pow((lc1.latitude - lc2.latitude), 2) + Math.pow((lc1.longitude - lc2.longitude), 2));
    }

    public void updateStoryListAdapter(ArrayList storyList, Location currentLocation) {
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            this.storyList = sortStoryListByProximity(storyList, currentLatLng);
            storyListAdapter.setStoryList(this.storyList);
            storyListAdapter.notifyDataSetChanged();
        }
    }
}
