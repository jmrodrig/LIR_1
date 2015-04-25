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
import android.widget.ImageView;

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
import java.util.List;

/**
 * Created by Ze on 13/04/2015.
 */
public class StoryMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    public Boolean askedToFetchStories = false;
    private SupportMapFragment mapFragment;
    private ImageView mapUserSight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
        }

        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction().replace(R.id.map_container, mapFragment).commit();
        mapFragment.getMapAsync(this);



        return inflater.inflate(R.layout.story_map_fragment, container, false);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (mMap==null) {
            mMap = map;
            map.setMyLocationEnabled(true);
            updateCurrentPosition(((MainActivity) getActivity()).getLastKnowLocation());
        }
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                updateStoriesCount();
                updateSightArea(cameraPosition.zoom);
            }
        });

        ((MainActivity) getActivity()).fetchStories();
        populateMapWithStoriesAfterFetched();

        mapUserSight = (ImageView) getActivity().findViewById(R.id.sight_image);
        updateSightArea(map.getCameraPosition().zoom);
    }

    private void updateSightArea(float zoom) {
        float scale = (float) Math.pow(2,zoom-15);
        mapUserSight.setScaleX(scale);
        mapUserSight.setScaleY(scale);
        //TODO set in values file
        float maxZoom = 21;
        float defaultZoom = 15;
        float alpha = 1+(defaultZoom-zoom)/(maxZoom-defaultZoom);
        mapUserSight.setAlpha(alpha);
    }

    public void updateStoriesCount() {
        if (mMap==null)
            return;

        Button storyListButton = (Button) getActivity().findViewById(R.id.story_list_button);
        ArrayList<StoryItem> stories = ((MainActivity) getActivity()).getFetchedStories();
        if (stories != null) {
            Integer storyCount = countStoriesAround(stories, mMap.getCameraPosition().target, 250);
            storyListButton.setText("LIST (" + storyCount + ")");
        }
    }

    public void updateCurrentPosition(android.location.Location location) {
        if (mMap==null)
            return;

        if (location != null) {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15));
        }
    }

    public void populateMapWithStories(ArrayList<StoryItem> stories) {
        if (mMap==null)
            return;
        mMap.clear();
        for (Integer i = 0; i < stories.size(); i++) {
            StoryItem si = stories.get(i);
            Location location = si.getStoryLocation();
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.latitude, location.longitude))
                    .anchor(1/2,1/2)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_custom)));

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
