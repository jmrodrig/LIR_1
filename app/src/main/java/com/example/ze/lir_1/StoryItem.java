package com.example.ze.lir_1;

import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ze on 10/04/2015.
 */
public class StoryItem {
    private Integer id;
    private String title;
    private String summary;
    private String thumbnail;
    private Location location;
    private String locationName;
    private Author author;
    private String articleTitle;
    private String articleDescription;
    private String articleImage;
    private String articleLink;

    //constructors
    public StoryItem() {}

    public StoryItem(Integer storyId, String storyTitle, String text, String urlThumbnail, Location StoryLocation, String address, Author storyAuthor,
                            String stArticleTitle, String stArticleDescription, String stArticleImage, String stArticleLink) {
        id = storyId;
        title = storyTitle;
        summary = text;
        thumbnail = urlThumbnail;
        location = StoryLocation;
        locationName = address;
        author = storyAuthor;
        articleTitle = stArticleTitle;
        articleDescription = stArticleDescription;
        articleImage = stArticleImage;
        articleLink = stArticleLink;
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

    public  String getAddress() {
        return locationName;
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

    public  String getArticleTitle() {
        return articleTitle;
    }

    public  String getArticleDescription() {
        return articleDescription;
    }

    public  String getArticleImage() {
        return articleImage;
    }

    public  String getArticleLink() {
        return articleLink;
    }

    public  Author getAuthor() {
        return author;
    }


    //setters
    public void setStoryId(Integer id) { this.id = id; }

    public void setStoryTitle(String text) { this.title = text; }

    public void setStoryText(String text) { this.summary = text; }

    public void setStoryLocation (Double lat, Double lng ) { this.location = new Location(lat,lng); }

    public void setArticleTitle(String text) { this.articleTitle = text; }

    public void setArticleDescription(String text) { this.articleDescription = text; }

    public void setArticleImage (String text) { this.articleImage = text; }

    public void setArticleLink (String text) { this.articleLink = text; }

    public void setAddress(String text) { locationName = text; }

}
