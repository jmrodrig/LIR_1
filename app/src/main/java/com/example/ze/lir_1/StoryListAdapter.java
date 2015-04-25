package com.example.ze.lir_1;

import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.net.Uri;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Ze on 12/04/2015.
 */
public class StoryListAdapter extends BaseAdapter {
    private ImageLoader.ImageCache imageCache;
    private ImageLoader loader;
    private ArrayList<StoryItem> stories = new ArrayList<StoryItem>();
    private Context appContext;
    private StoryListFragment parentObj;

    public StoryListAdapter(Context Appctxt, StoryListFragment parent ) {
        appContext = Appctxt;
        parentObj = parent;
        loader = RequestsSingleton.getInstance(appContext.getApplicationContext()).getImageLoader();
    }

    public void setStoryList(ArrayList<StoryItem> storyList) {
        stories = storyList;
    }

    public int getCount() {
        return stories.size();
    }

    public StoryItem getItem(int position) {
        return stories.get(position);
    }

    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) appContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.story_row, null, false);
            ViewHolder holder = new ViewHolder();
            holder.userNameTextView = (TextView) row.findViewById(R.id.user_name);
            holder.storyTextTextView = (TextView) row.findViewById(R.id.story_text);
            holder.storyImageView = (NetworkImageView) row.findViewById(R.id.story_thumbnail);
            holder.userImageView = (NetworkImageView) row.findViewById(R.id.user_image);
            holder.upVoteStoryButton = (Button) row.findViewById(R.id.like_story_button);
            holder.deleteStoryButton = (Button) row.findViewById(R.id.delete_story_button);
            holder.distanceTextView = (TextView) row.findViewById(R.id.distance_text);
            holder.articleTitleTextView = (TextView) row.findViewById(R.id.article_title);
            holder.articleTextTextView = (TextView) row.findViewById(R.id.article_text);
            holder.articleHostTextView = (TextView) row.findViewById(R.id.article_host);
            holder.articleImageView = (NetworkImageView) row.findViewById(R.id.article_image);
            holder.articleLayout = (LinearLayout) row.findViewById(R.id.article_layout);
            holder.confirmDeleteDialog = (FrameLayout) row.findViewById(R.id.confirm_delete_dialog);
            holder.confirmDeleteButton = (TextView) row.findViewById(R.id.confirm_button);
            holder.cancelDeleteButton = (TextView) row.findViewById(R.id.cancel_button);

            row.setTag(holder);
        }

        final StoryItem storyItem = getItem(position);
        final ViewHolder holder = (ViewHolder) row.getTag();
        holder.userNameTextView.setText(storyItem.getUserName());
        holder.storyTextTextView.setText(storyItem.getStoryText());
        holder.storyImageView.setImageUrl(storyItem.getUrlThumbnail(), loader);
        holder.userImageView.setImageUrl(storyItem.getUrlUserThumbnail(), loader);

        holder.deleteStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.confirmDeleteDialog.setVisibility(View.VISIBLE);

            }
        });

        holder.confirmDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parentObj.deleteStory(getItem(position).getStoryId());
            }
        });

        holder.cancelDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.confirmDeleteDialog.setVisibility(View.GONE);
            }
        });

        //ARTICLE
        if (storyItem.getArticleTitle()!=null && !storyItem.getArticleTitle().equals("") ) {
            holder.storyImageView.setVisibility(View.GONE);
            holder.articleTitleTextView.setText(storyItem.getArticleTitle());
            holder.articleTextTextView.setText(storyItem.getArticleDescription());
            holder.articleHostTextView.setText(storyItem.getArticleHost());
            // ARTICLE IMAGE
            holder.articleLayout.setVisibility(View.VISIBLE);
            if (!storyItem.getArticleImage().equals("")) {
                holder.articleImageView.setImageUrl(storyItem.getArticleImage(), loader);
                holder.articleImageView.setVisibility(View.VISIBLE);
            } else
                holder.articleImageView.setVisibility(View.GONE);
        } else
            holder.articleLayout.setVisibility(View.GONE);

        holder.articleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openArticleInBrowser(storyItem.getArticleLink());
            }
        });

        // DELETE BUTTON + UPVOTE BUTTON
        if (isUserOwnerOfStory(getItem(position))) {
            holder.deleteStoryButton.setVisibility(View.VISIBLE);
            holder.upVoteStoryButton.setVisibility(View.GONE);
        } else {
            holder.deleteStoryButton.setVisibility(View.GONE);
            holder.upVoteStoryButton.setVisibility(View.VISIBLE);
        }

        //DISTANCE
        android.location.Location location = ((MainActivity) parentObj.getActivity()).getLastKnowLocation();
        LatLng latLngLocation = new LatLng(location.getLatitude(),location.getLongitude());
        LatLng latLngStory = storyItem.getStoryLatLng();
        float distance = parentObj.calculateTwoPointsDistance(latLngLocation,latLngStory);
        holder.distanceTextView.setText(Math.round(distance) + " m");

        return (row);
    }

    static class ViewHolder{
        TextView userNameTextView, storyTextTextView, articleTitleTextView, articleTextTextView, articleHostTextView, distanceTextView, confirmDeleteButton, cancelDeleteButton;
        NetworkImageView storyImageView, userImageView, articleImageView;
        Button deleteStoryButton, upVoteStoryButton;
        LinearLayout articleLayout;
        FrameLayout confirmDeleteDialog;
    }

    private Boolean isUserOwnerOfStory(StoryItem st) {
        String userId = SessionUser.getInstance().getUserEmail();
        if (userId.equals(st.getAuthor().getAuthorId()))
            return true;
        else
            return false;
    }

    private void openArticleInBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        parentObj.getActivity().startActivity(browserIntent);
    }
}


