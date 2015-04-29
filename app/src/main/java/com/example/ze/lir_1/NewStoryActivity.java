package com.example.ze.lir_1;

import android.app.ActionBar;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.*;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
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


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ch.boye.httpclientandroidlib.entity.mime.content.FileBody;


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
    private EditText mStoryText;
    private Boolean updatingArticleInfo = false;
    private ArrayList<String> mArticleInfo = new ArrayList<>();

    private String mArticleWebAddress = "";
    private String mStoryImagePath = "";

    private RequestQueue queue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_story);

        queue = RequestsSingleton.getInstance(this.getApplicationContext()).getRequestQueue();


        mSessionUser = SessionUser.getInstance();
        mImageLoader = RequestsSingleton.getInstance(this).getImageLoader();

        //TODO Load session user info (thumbnail)
        NetworkImageView mUserImage = (NetworkImageView) findViewById(R.id.user_image);
        mUserImage.setImageUrl(mSessionUser.getUserAvatar(), mImageLoader);
        TextView mLocationText = (TextView) findViewById(R.id.story_location);
        mLocationText.setText(mSessionUser.getUserFullName() + " at " + getLocationAddress());

        mStoryText = (EditText) findViewById(R.id.story_text);
        mArticleProgressBar = (ProgressBar) findViewById(R.id.article_process_progress);
        mArticleLayout = (LinearLayout) findViewById(R.id.article_layout);
        mImageLayout = (FrameLayout) findViewById(R.id.image_layout);
        mStoryPicture = (ImageView) findViewById(R.id.story_picture);

        mImageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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

        findViewById(R.id.submit_story_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send story data, recebe o story Id e manda a imagem
                JSONObject storyData = collectStoryDataJSON();
                submitStoryDataToServer(storyData);
            }
        });

        mStoryText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                //TODO replace search patterns by constants and allocate them on the string resources
                String address = null;
                String tx = s.toString();
                if (tx.contains("http://"))
                    address = grabWebpageAddress(tx, "http://");
                else if (tx.contains("https://"))
                    address = grabWebpageAddress(tx, "https://");
                else if (tx.contains("www."))
                    address = grabWebpageAddress(tx, "www.");
                else
                    return;

                if (address.equals(getWebAddress())) {
                    mArticleProgressBar.setVisibility(View.GONE);
                    return;
                } else
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

    private void submitStoryDataToServer(JSONObject storyData) {
        Toast.makeText(this, "Publishing story...", Toast.LENGTH_LONG).show();
        String url = "http://lostinreality.net/story";
        if (storyData!=null) {
            JsonObjectRequest submitStoryDataRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    storyData,
                    new SubmitStorySuccess(this),
                    new SubmitStoryError(this));
            queue.add(submitStoryDataRequest);
        }
    }


    public String grabWebpageAddress(String text, String regex) {
        mArticleProgressBar.setVisibility(View.VISIBLE);
        String[] address = text.split(regex, 2);
        address = address[1].split(" ", 2);
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
            mArticleInfo.clear();

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
            mArticleImage.setImageUrl(result.get(2), mImageLoader);

            for (String artItem : result) {
                mArticleInfo.add(artItem);
            }

            if (result.get(2).equals("")) {
                mArticleImagePictureFrame.setVisibility(View.GONE);
                mArticleImage.setImageUrl(result.get(2), mImageLoader);
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
                    StringEscapeUtils.unescapeHtml4(parseRegex(headerHtml, descriptionRegexs)));

            //title Regexs
            String[] titleRegexs = {
                    "<meta name=\"twitter:title\"",
                    "<meta property=\"twitter:title\"",
                    "<meta property=\"og:title\"",
                    "<meta itemprop=\"name\"",
                    "<meta name=\"title\""
            };
            articleParseResult.add(
                    StringEscapeUtils.unescapeHtml4(parseRegex(headerHtml, titleRegexs)));

            //image Regexs
            String[] imageRegexs = {
                    "<meta name=\"twitter:image:src\" ",
                    "<meta property=\"og:image\"",
                    "<meta itemprop=\"image\""
            };


            articleParseResult.add(parseRegex(headerHtml, imageRegexs));

            return articleParseResult;
        }

        private String parseRegex(String header, String[] rgs) {
            if (header.equals(""))
                return "";

            String dcrpRegex = "";
            for (String rg : rgs) {
                if (header.contains(rg)) {
                    dcrpRegex = rg;
                    break;
                }
            }

            if (dcrpRegex != "") {
                return header.split(dcrpRegex, 2)[1]
                        .split("content=\"", 2)[1]
                        .split("\"", 2)[0];
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
        return mArticleWebAddress;
    }

    private synchronized void setWebAddress(String adrss) {
        mArticleWebAddress = adrss;
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
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

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

                mStoryImagePath = imgDecodableString;

                mArticleLayout.setVisibility(View.GONE);
                mImageLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.discard_image_button).setVisibility(View.VISIBLE);
                // Set the Image in ImageView after decoding the String
                Bitmap bm = ShrinkBitmap(imgDecodableString,480,480);
                //mStoryPicture.setImageBitmap(bm);
                //Bitmap bm = BitmapFactory.decodeFile(mStoryImagePath);
                mStoryPicture.setImageBitmap(bm);

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Oh! Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }

    /**
     * Runs when a JsonArrayRequest object successfully gets an response.
     */
    class SendFileSuccess implements Response.Listener<String> {
        private Context context;
        private Integer storyId;

        public SendFileSuccess(Context ctx, Integer id) {
            context = ctx;
            storyId = id;
        }

        @Override
        public void onResponse(String response) {
            publishStory(storyId);
            //Toast.makeText(context, "image uploaded!",Toast.LENGTH_LONG).show();
        }
    }

    private void publishStory(Integer storyId) {
        String url = "http://lostinreality.net/story/" + storyId + "/publish/1";
        StringRequest publishRequest = new StringRequest(Request.Method.POST,url, new PublishSuccess(this), new PublishError(this));
        queue.add(publishRequest);
    }

    class SendFileError implements Response.ErrorListener {
        private Context context;

        public SendFileError(Context ctx) {
            context = ctx;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            // TODO Make an error handler
            Toast.makeText(context, "Image upload error!", Toast.LENGTH_LONG).show();
        }
    }

    public String getLocationAddress() {
        String address = getIntent().getExtras().getString("location_address");
        if (address.equals(""))
            return "the desert? The ocean? Nowhere? Where is this?";
        return buildAddress(address);
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

    private JSONObject collectStoryDataJSON() {

        Date date = new Date();
        StoryItem storyItem = new StoryItem();
        storyItem.setStoryTitle("Story_" + String.valueOf(new Timestamp(date.getTime())));
        storyItem.setStoryText(mStoryText.getText().toString());
        Double lat = getIntent().getExtras().getDouble(Constants.LOCATION_DATA_EXTRA + "_latitude");
        Double lng = getIntent().getExtras().getDouble(Constants.LOCATION_DATA_EXTRA + "_longitude");
        storyItem.setStoryLocation(lat, lng);
        storyItem.setAddress(getLocationAddress());
        if (!mArticleInfo.isEmpty()) {
            storyItem.setArticleTitle(mArticleInfo.get(1));
            storyItem.setArticleDescription(mArticleInfo.get(0));
            storyItem.setArticleImage(mArticleInfo.get(2));
            storyItem.setArticleLink(mArticleWebAddress);
        }

        String jsonStoryString = new Gson().toJson(storyItem);
        JSONObject jsonStoryObj = null;
        try {
            jsonStoryObj = new JSONObject(jsonStoryString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonStoryObj;
    }

    /**
     * Runs when a JsonArrayRequest object successfully gets an response.
     */
    class SubmitStorySuccess implements Response.Listener<JSONObject> {
        private Context context;

        public SubmitStorySuccess(Context ctx) {
            context = ctx;
        }

        @Override
        public void onResponse(JSONObject response) {
            StoryItem jsonStory = new Gson().fromJson(response.toString(), StoryItem.class);
            Integer storyId = jsonStory.getStoryId();
            if (!mStoryImagePath.equals(""))
                sendImageFile(storyId);
            else
                publishStory(storyId);
            //Toast.makeText(context, "Story published!",Toast.LENGTH_LONG).show();
        }
    }

    private void sendImageFile(Integer storyId) {
        if (mStoryImagePath.equals(""))
            return;


        // CREATE TMP FILE
        File outputDir = this.getCacheDir(); // context being the Activity pointer
        File outputFile = null;
        //Bitmap bitmap = BitmapFactory.decodeFile(mStoryImagePath);
        Bitmap bitmap = ShrinkBitmap(mStoryImagePath,1280,1280);
        FileOutputStream outputStream = null;
        try {
            outputFile = File.createTempFile("story_image", ".jpeg", outputDir);
            outputStream = new FileOutputStream(outputFile);
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                //outputStream = new BufferedOutputStream(new FileOutputStream(file));
            } finally {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileBody fileBody = new FileBody(outputFile);

        File imageFile = new File(mStoryImagePath);
        //FileBody fileBody = new FileBody(imageFile);
        String url = "http://lostinreality.net/story/" + storyId + "/uploadimage";
        MultipartRequest multipartRequest = new MultipartRequest(url, "file", fileBody, new SendFileSuccess(this,storyId), new SendFileError(this));
        queue.add(multipartRequest);
    }

    class SubmitStoryError implements Response.ErrorListener {
        private Context context;

        public SubmitStoryError(Context ctx) {
            context = ctx;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            // TODO Make an error handler
            Toast.makeText(context, "Nothing happened!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Runs when a JsonArrayRequest object successfully gets an response.
     */
    class PublishSuccess implements Response.Listener<String> {
        private Context context;

        public PublishSuccess(Context ctx) {
            context = ctx;
        }

        @Override
        public void onResponse(String response) {
            Toast.makeText(context, "Story is published!",Toast.LENGTH_LONG).show();
            leaveActivityAndReloadStories();
        }
    }


    class PublishError implements Response.ErrorListener {
        private Context context;

        public PublishError(Context ctx) {
            context = ctx;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            // TODO Make an error handler
            Toast.makeText(context, "Nothing happened!", Toast.LENGTH_LONG).show();
        }
    }

    private void leaveActivityAndReloadStories() {
        NavUtils.navigateUpFromSameTask(this);
    }

    private Bitmap ShrinkBitmap(String file, int width, int height){

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);

        int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);
        int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);

        if (heightRatio > 1 || widthRatio > 1)
        {
            if (heightRatio > widthRatio)
            {
                bmpFactoryOptions.inSampleSize = heightRatio;
            } else {
                bmpFactoryOptions.inSampleSize = widthRatio;
            }
        }

        bmpFactoryOptions.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
        return bitmap;
    }

}