package com.example.ze.lir_1;


import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;

import android.net.Uri;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

import android.os.Bundle;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    protected static final String TAG = "basic-location-sample";

    private ArrayList<StoryItem> fetchedStories;
    private Button openStoryListFragmentButton;
    private Button openStoryMapFragmentButton;
    private Button openCreateStoryFragmentButton;
    private StoryMapFragment storyMapFragment;
    private StoryListFragment storyListFragment;

    public RequestQueue queue;
    public JsonArrayRequest fetchStoriesRequest;
    protected static GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;
    protected LocationRequest mLocationRequest;

    private AddressResultReceiver mResultReceiver;
    protected String mAddressOutput;
    private LatLng mFocusLocation;
    private Boolean startNewStoryActivity = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().setIcon(R.drawable.lir_logo_banner_white);
        //getSupportActionBar().setDisplayUseLogoEnabled(true);

        if (savedInstanceState != null) {
            return;
        }

        storyMapFragment = new StoryMapFragment();
        storyListFragment = new StoryListFragment();

        //TODO Get storyList from Cache
        //TODO warn when no internet connection is set (wi-fi or network) http://developer.android.com/training/basics/network-ops/connecting.html

        // Get Request Queue
        queue = RequestsSingleton.getInstance(this).getRequestQueue();

        mResultReceiver = new AddressResultReceiver(new Handler());

        String url ="http://lostinreality.net/publishedstories";

        fetchStoriesRequest = new JsonArrayRequest(Request.Method.GET, url, null, new ResponseListener(), new ErrorListener());

        storyMapFragment = new StoryMapFragment();
        storyListFragment = new StoryListFragment();

        openStoryListFragmentButton = (Button) findViewById(R.id.story_list_button);
        openStoryMapFragmentButton = (Button) findViewById(R.id.story_map_button);
        openCreateStoryFragmentButton = (Button) findViewById(R.id.create_story_button);

        // Set Listeners for UI buttons
        openStoryMapFragmentButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO do nothing if already open
                // TODO save state

                if (findViewById(R.id.map_container) != null)
                    return;

                // Create an instance of StoryMapFragment
                if (storyMapFragment ==null)
                    storyMapFragment = new StoryMapFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, storyMapFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        openStoryListFragmentButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO do nothing if already open
                // TODO save state
                if (findViewById(R.id.story_container) != null)
                    return;

                if(storyListFragment == null )
                    storyListFragment = new StoryListFragment();

                setMapCurrentFocusLocation();
                setCurrentFocusedAddress();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, storyListFragment);
                transaction.addToBackStack(null);
                transaction.commit();

            }
        });

        openCreateStoryFragmentButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                loadNewStoryActivity();
            }
        });

        buildGoogleApiClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        storyMapFragment.populateMapWithStoriesAfterFetched();
        fetchStories();
    }

    private void loadNewStoryActivity() {
        startNewStoryActivityAfterAddressReceived(true);
        setMapCurrentFocusLocation();
        setCurrentFocusedAddress();
    }

    private Boolean isStartNewStoryActivityAfterAddressReceived() {
        return startNewStoryActivity;
    }

    private void startNewStoryActivityAfterAddressReceived(Boolean start) {
        startNewStoryActivity = start;
    }

    private void startNewStoryActivity() {
        Intent intent = new Intent(this,NewStoryActivity.class);
        intent.putExtra("location_address",mAddressOutput);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA + "_latitude", mFocusLocation.latitude);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA+ "_longitude", mFocusLocation.longitude);
        startActivity(intent);
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }



    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        getLastKnowLocation();
        inflateStoryMapFragment();

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        storyMapFragment.updateCurrentPosition(location);
    }

    protected Location getLastKnowLocation() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location !=null)
            mCurrentLocation = location;
        return mCurrentLocation;
    }

    public void startUpdateLocation() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Runs when a CreateOptionsMenu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            sendEmailIntent();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Runs when a JsonArrayRequest object successfully gets an response.
     */
    class ResponseListener implements Response.Listener<JSONArray>{
        @Override
        public void onResponse(JSONArray response) {
            StoryItem[] jsonStory = new Gson().fromJson(response.toString(), StoryItem[].class);
            ArrayList stories = new ArrayList<>(Arrays.asList(jsonStory));
            setFetchedStories(stories);
            if (storyMapFragment.askedToFetchStories ) {
                storyMapFragment.populateMapWithStories(stories);
                storyMapFragment.askedToFetchStories = false;
                storyMapFragment.updateStoriesCount();
            }
            if (storyListFragment.askedToFetchStories ) {
                storyListFragment.updateStoryListAdapter(stories, getMapCurrentFocusLocation());
                storyListFragment.askedToFetchStories = false;
                storyMapFragment.updateStoriesCount();
            }


        }
    }

    class ErrorListener implements Response.ErrorListener{
        @Override
        public void onErrorResponse(VolleyError error) {
            // TODO Make an error handler
        }
    }

    public void fetchStories() {
        queue.add(fetchStoriesRequest);
    }

    public ArrayList getFetchedStories() {
        return this.fetchedStories;
    }

    public void setFetchedStories(ArrayList<StoryItem> ftsts) {
        this.fetchedStories = ftsts;
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();
            TextView locationAddressText = (TextView) findViewById(R.id.current_address_text);
            if (locationAddressText != null) {
                if (mAddressOutput.equals(""))
                    locationAddressText.setText("the desert? The ocean? Nowhere? Where is this?");
                else
                    locationAddressText.setText(buildAddress(mAddressOutput));
            }

            if (isStartNewStoryActivityAfterAddressReceived()) {
                startNewStoryActivityAfterAddressReceived(false);
                startNewStoryActivity();
            }

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                //showToast(getString(R.string.address_found));
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            //mAddressRequested = false;
            //updateUIWidgets();
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService() {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
            return;
        }

        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        //intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA + "_latitude", mFocusLocation.latitude);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA+ "_longitude", mFocusLocation.longitude);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    private void inflateStoryMapFragment() {
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,storyMapFragment).commit();
    }

    private void setMapCurrentFocusLocation() {
        mFocusLocation = storyMapFragment.getMapFocusLocation();
    }

    private void setCurrentFocusedAddress() {
        startIntentService();
    }

    public LatLng getMapCurrentFocusLocation() {
        return mFocusLocation;
    }

    public String buildAddress(String address) {
        String[] adcomp = address.split("\n");
        if (adcomp.length == 1)
            return adcomp[0] + ".";
        else if (adcomp.length == 2)
            return adcomp[0] + ", " + adcomp[1] + "." ;
        else if (adcomp.length == 3)
            return adcomp[0] + ", " + adcomp[1] + ", " + adcomp[2] + "." ;
        else
            return address;

    }

    private void sendEmailIntent() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "ideas@lostinreality.net", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "User feedback");
        startActivity(Intent.createChooser(intent, "Send Email"));
    }
}
