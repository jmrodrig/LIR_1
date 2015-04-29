package com.example.ze.lir_1;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

/**
 * Created by Ze on 28-04-2015.
 */
public class ViewHolder {
    private TextView userNameTextView, storyTextTextView, articleTitleTextView, articleTextTextView, articleHostTextView, distanceTextView, confirmDeleteButton, cancelDeleteButton;
    //NetworkImageView storyImageView, userImageView, articleImageView;
    private NetworkImageView userImageView, articleImageView;
    public ImageView storyImageView;
    private Button deleteStoryButton, upVoteStoryButton;
    private LinearLayout articleLayout;
    private FrameLayout confirmDeleteDialog;
    public ProgressBar storyImageProgress;




    public void getImageFromNetwork(ImageLoader imageLoader, String imageUrl) {
        storyImageView.setVisibility(View.GONE);
        storyImageView.setImageBitmap(null);

        if (imageUrl.equals("")) {
            storyImageProgress.setVisibility(View.GONE);
            storyImageView.setVisibility(View.GONE);
            return;
        }

        imageLoader.get(imageUrl, new ImageLoader.ImageListener() {

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response != null) {
                    Bitmap bitmap = response.getBitmap();
                    if (bitmap != null) {
                        // ** code to turn off the progress wheel **
                        // ** code to use the bitmap in your imageview **
                        storyImageProgress.setVisibility(View.GONE);
                        storyImageView.setVisibility(View.VISIBLE);
                        storyImageView.setImageBitmap(bitmap);
                    }
                }
            }

            @Override
            public void onErrorResponse(VolleyError e) {
                storyImageProgress.setVisibility(View.GONE);

                // ** code to handle errors **
            }
        });

    }
}
