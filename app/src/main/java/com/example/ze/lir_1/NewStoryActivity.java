package com.example.ze.lir_1;

import android.app.ActionBar;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.*;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created by Ze on 18-04-2015.
 */
public class NewStoryActivity extends ActionBarActivity {
    private static final int RESULT_LOAD_IMAGE = 1;
    private SessionUser mSessionUser;
    private ImageLoader mImageLoader;
    private ProgressBar mArticleProgressBar;
    private LinearLayout mArticleLayout;
    private FrameLayout mImageLayout;
    private ImageView mStoryPicture;
    private Boolean updatingArticleInfo = false;

    private String mWebAddress = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_story);

        mSessionUser = SessionUser.getInstance();
        mImageLoader = RequestsSingleton.getInstance(this).getImageLoader();


        //TODO Load session user info (thumbnail)
        NetworkImageView mUserImage = (NetworkImageView) findViewById(R.id.user_image);
        mUserImage.setImageUrl(mSessionUser.getUserAvatar(),mImageLoader);
        TextView mLocationText = (TextView) findViewById(R.id.story_location);
        mLocationText.setText(mSessionUser.getUserFullName() + " at " + getLocationAddress());

        EditText mStoryText = (EditText) findViewById(R.id.story_text);
        mArticleProgressBar = (ProgressBar) findViewById(R.id.article_process_progress);
        mArticleLayout = (LinearLayout) findViewById(R.id.article_layout);
        mImageLayout = (FrameLayout) findViewById(R.id.image_layout);
        mStoryPicture = (ImageView) findViewById(R.id.story_picture);

        mImageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        findViewById(R.id.discard_image_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                mStoryPicture.setImageBitmap(null);
            }
        });

        findViewById(R.id.dicard_article_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mArticleLayout.setVisibility(View.GONE);
                mImageLayout.setVisibility(View.VISIBLE);
            }
        });

        mStoryText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                //TODO replace search patterns by constants and allocate them on the string resources
                String address = null;
                String tx = s.toString();
                if (tx.contains("http://"))
                    address = grabWebpageAddress(tx,"http://");
                else if (tx.contains("https://"))
                    address = grabWebpageAddress(tx,"https://");
                else if (tx.contains("www."))
                    address = grabWebpageAddress(tx,"www.");
                else
                    return;

                if (address.equals(getWebAddress())) {
                    mArticleProgressBar.setVisibility(View.GONE);
                    return;
                }
                else
                    setWebAddress(address);

                //TODO avoid trigger of GrabWebpage everytime the text is changed
                if (!isUpdating()) {
                    mArticleLayout.setVisibility(View.GONE);
                    mImageLayout.setVisibility(View.GONE);
                    mArticleProgressBar.setVisibility(View.VISIBLE);
                    changeUpdatingState(true);
                    new GrabWebpageMetadataTask().execute();
                }
            }
        });


        //TODO submit story to server

        //TODO OPTIONAL Change Location

        //TODO OPTIONAL Get address
    } // END OF onCreate()


    public String grabWebpageAddress(String text, String regex) {
        mArticleProgressBar.setVisibility(View.VISIBLE);
        String[] address = text.split(regex,2);
        address = address[1].split(" ",2);
        if (regex.equals(("www.")))
            return "http://" + regex + address[0];
        return regex + address[0];
    }

    private class GrabWebpageMetadataTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList doInBackground(String... address) {
            // params comes from the execute() call: params[0] is the address.
            try {
                Thread.sleep(2600);
                return downloadUrl(getWebAddress());
            } catch (IOException e) {
                return null; //"Unable to retrieve web page. URL may be invalid.";
            } catch (InterruptedException e) {
                return null;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<String> result) {
            mArticleProgressBar.setVisibility(View.GONE);

            if (result == null || result.get(0).equals("") || result.get(1).equals("")) {
                mImageLayout.setVisibility(View.VISIBLE);
                mArticleLayout.setVisibility(View.GONE);
                changeUpdatingState(false);
                return;
            }

            TextView mArticleText = (TextView) findViewById(R.id.article_text);
            TextView mArticleTitle = (TextView) findViewById(R.id.article_title);
            TextView mArticleHost = (TextView) findViewById(R.id.article_host);
            NetworkImageView mArticleImage = (NetworkImageView) findViewById(R.id.article_picture);
            FrameLayout mArticleImagePictureFrame = (FrameLayout) findViewById(R.id.article_picture_frame);

            mArticleTitle.setText(result.get(1));
            mArticleText.setText(result.get(0));
            mArticleHost.setText(result.get(3));
            mArticleImage.setImageUrl(result.get(2),mImageLoader);

            if (result.get(2).equals("")) {
                mArticleImagePictureFrame.setVisibility(View.GONE);
                mArticleImage.setImageUrl(result.get(2),mImageLoader);
            }

            mImageLayout.setVisibility(View.GONE);
            mArticleLayout.setVisibility(View.VISIBLE);
            changeUpdatingState(false);
        }


        private ArrayList downloadUrl(String address) throws IOException {
            InputStream is = null;
            try {
                URL url = new URL(address);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d("DEBUG_TAG", "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is);

                ArrayList<String> articleData = parseMetaData(contentAsString);
                articleData.add(url.getHost());

                return articleData;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        // Reads an InputStream and converts it to a String.
        private String readIt(InputStream stream) throws IOException {
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer);
//            String response = writer.toString();

//            Reader reader = null;
//            reader = new InputStreamReader(stream);
//            char[] buffer = new char[10000];
//            reader.read(buffer);
//            String response = new String(buffer);

            return writer.toString();
        }

        private ArrayList<String> parseMetaData(String html) {
            ArrayList<String> articleParseResult = new ArrayList<>();
            String headerHtml = html.split("<body")[0];

            //Description Regexs
            String[] descriptionRegexs = {
                    "<meta name=\"twitter:description\"",
                    "<meta property=\"twitter:description\"",
                    "<meta property=\"og:description\"",
                    "<meta name=\"description\""
            };

            articleParseResult.add(
                    StringEscapeUtils.unescapeHtml4(parseRegex(headerHtml,descriptionRegexs)));

            //title Regexs
            String[] titleRegexs = {
                    "<meta name=\"twitter:title\"",
                    "<meta property=\"twitter:title\"",
                    "<meta property=\"og:title\"",
                    "<meta itemprop=\"name\"",
                    "<meta name=\"title\""
            };
            articleParseResult.add(
                    StringEscapeUtils.unescapeHtml4(parseRegex(headerHtml,titleRegexs)));

            //image Regexs
            String[] imageRegexs = {
                    "<meta name=\"twitter:image:src\" ",
                    "<meta property=\"og:image\"",
                    "<meta itemprop=\"image\""
            };



            articleParseResult.add(parseRegex(headerHtml,imageRegexs));

            return articleParseResult;
        }

        private String parseRegex(String header, String[] rgs) {
            if (header.equals(""))
                return "";

            String dcrpRegex = "";
            for(String rg : rgs ) {
                if (header.contains(rg)) {
                    dcrpRegex = rg;
                    break;
                }
            }

            if (dcrpRegex != "") {
                return header.split(dcrpRegex, 2)[1]
                        .split("content=\"",2)[1]
                        .split("\"",2)[0];
            }
            return "";
        }

    }

    private synchronized Boolean isUpdating() {
        return updatingArticleInfo;
    }

    private synchronized void changeUpdatingState(Boolean isUpt) {
        updatingArticleInfo = isUpt;
    }

    private synchronized String getWebAddress() {
        return mWebAddress;
    }

    private synchronized void setWebAddress(String adrss) {
        mWebAddress = adrss;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data

                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imgDecodableString = cursor.getString(columnIndex);
                cursor.close();

                if (imgDecodableString == null) {
                    Toast.makeText(this, "Oh! Something went wrong!", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                mArticleLayout.setVisibility(View.GONE);
                mImageLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.discard_image_button).setVisibility(View.VISIBLE);
                // Set the Image in ImageView after decoding the String
                mStoryPicture.setImageBitmap(BitmapFactory
                        .decodeFile(imgDecodableString));

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Oh! Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }

    public String getLocationAddress() {
        String address = getIntent().getExtras().getString("location_address");
        return address.split("\n",2)[0] + ", " + address.split("\n",2)[1];
    }

}
