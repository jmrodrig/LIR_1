package com.example.ze.lir_1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

/**
 * Created by Ze on 12/04/2015.
 */
public class StoryListAdapter extends BaseAdapter {
    private ImageLoader.ImageCache imageCache;
    private ImageLoader loader;
    private ArrayList<StoryItem> stories = new ArrayList<StoryItem>();
    private Context context;

    public StoryListAdapter(Context ctxt ) {
        context = ctxt;
        loader = RequestsSingleton.getInstance(context.getApplicationContext()).getImageLoader();
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

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.story_row, null, false);
            ViewHolder holder = new ViewHolder();
            holder.userNameTextView = (TextView) row.findViewById(R.id.user_name);
            holder.storyTextTextView = (TextView) row.findViewById(R.id.story_text);
            holder.storyImageView = (NetworkImageView) row.findViewById(R.id.story_thumbnail);
            holder.userImageView = (NetworkImageView) row.findViewById(R.id.user_image);

            row.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) row.getTag();
        holder.userNameTextView.setText(getItem(position).getUserName());
        holder.storyTextTextView.setText(getItem(position).getStoryText());
        String urlImage = getItem(position).getUrlThumbnail();
        holder.storyImageView.setImageUrl(urlImage, loader);
        String userImg = getItem(position).getUrlUserThumbnail();
        holder.userImageView.setImageUrl(userImg, loader);
        if (userImg.equals("")) {
            holder.userImageView.setImageResource(R.drawable.placeholder_user);
        }

        return (row);
    }

    static class ViewHolder{
        TextView userNameTextView, storyTextTextView;
        NetworkImageView storyImageView, userImageView;
    }
}


