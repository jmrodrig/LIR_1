package com.example.ze.lir_1;

import android.app.Activity;
import android.graphics.Camera;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Ze on 13/04/2015.
 */
public class StoryMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    public Boolean askedToFetchStories = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getChildFragmentManager();
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        fm.beginTransaction().replace(R.id.map_container, mapFragment).commit();
        mapFragment.getMapAsync(this);



        return inflater.inflate(R.layout.story_map_fragment, container, false);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        map.setMyLocationEnabled(true);
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //TODO
                Button storyListButton = (Button) getActivity().findViewById(R.id.story_list_button);
                ArrayList<StoryItem> stories = ((MainActivity) getActivity()).getFetchedStories();
                if (stories != null) {
                    Integer storyCount = countStoriesAround(stories, cameraPosition.target, 500);
                    storyListButton.setText("LIST (" + storyCount + ")");
                }
            }
        });
        //TODO Get storyList from Cache
        updateCurrentPosition(((MainActivity) getActivity()).getLastKnowLocation());
        //((MainActivity) getActivity()).fetchStories();
        ArrayList<StoryItem> stories = ((MainActivity) getActivity()).getFetchedStories();
        if (stories != null)
            populateMapWithStories( stories );
        else {
            ((MainActivity) getActivity()).fetchStories();
            populateMapWithStoriesAfterFetched();
        }

    }

    public void updateCurrentPosition(android.location.Location location) {
        if (location != null) {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15));
        }
    }

    public void populateMapWithStories(ArrayList<StoryItem> stories) {

        for (Integer i = 0; i < stories.size(); i++) {
            StoryItem si = stories.get(i);
            Location location = si.getStoryLocation();
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.latitude, location.longitude)));
                    //TODO add custom icons
                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow)));
        }
    }

    public LatLng getMapFocusLocation() {
        return mMap.getCameraPosition().target;
    }

    public void populateMapWithStoriesAfterFetched() {
        askedToFetchStories = true;
    }

    private Integer countStoriesAround(ArrayList<StoryItem> storyList, LatLng location, float radius) {
        ArrayList<StoryItem> storiesAround = new ArrayList<>();
        for (StoryItem st : storyList) {
            float distance = calculateTwoPointsDistance(st.getStoryLatLng(),location);
            if (distance <= radius)
                storiesAround.add(st);
        }
        return storiesAround.size();
    }

    private float calculateTwoPointsDistance(LatLng lc1, LatLng lc2) {
        float[] results = new float[4];
        android.location.Location.distanceBetween(lc1.latitude, lc1.longitude, lc2.latitude, lc2.longitude, results);
        return results[0];
    }

}
