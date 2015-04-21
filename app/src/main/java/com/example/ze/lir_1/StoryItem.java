package com.example.ze.lir_1;

import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ze on 10/04/2015.
 */
public class StoryItem {
    private Integer id;
    private String summary;
    private String thumbnail;
    private Location location;
    private Author author;
    private String urlUserThumbnail;

    //constructors
    public StoryItem() {}

    public StoryItem(Integer storyId, String text, String urlThumbnail, Location StoryLocation, Author storyAuthor, String userThumbnail) {
        id = id;
        summary = text;
        thumbnail = urlThumbnail;
        location = StoryLocation;
        author = storyAuthor;
        urlUserThumbnail = userThumbnail;

        if (urlUserThumbnail.equals("")) {
            urlUserThumbnail = "http://lostinreality.net/assets/images/lir-logo.png";
        }
    }

    //getters
    public  Integer getStoryId() {
        return id;
    }

    public  String getStoryText() {
        return summary;
    }

    public  String getUrlThumbnail() {
        return "http://lostinreality.net" + thumbnail;
    }

    public  Location getStoryLocation() {
        return location;
    }

    public LatLng getStoryLatLng() {
        return new LatLng(location.latitude,location.longitude);
    }

    public  String getUserName() {
        return author.getFullName();
    }

    public  String getUrlUserThumbnail() {
        return author.getAvatarUrl();
    }

    //setters
}
