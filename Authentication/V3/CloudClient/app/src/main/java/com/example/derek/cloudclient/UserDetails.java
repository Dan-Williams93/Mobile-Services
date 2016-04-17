package com.example.derek.cloudclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class UserDetails extends Activity {

    final static String PREFS_NAME = "myPreferences";
    String JsonResponse,GraphJsonResponse, fbAccessToken, expirationDateTime, userEmail, userID, userName, userSex, userDOB, userLocation,
            userTimeZone, userAbout, userAge, userBio, userHometown, userRelationshipStatus, profilePicURL, coverPicURL, education;

    ArrayList<String> userDetails = new ArrayList<String>();
    ArrayList<String> EducationEstablishment = new ArrayList<String>();
    ArrayList<String> EducationCourse = new ArrayList<String>();

    Bitmap userProfilePic, userCoverPic, defaultprofile, defaultCover;

    ImageView coverImage, profileImage;
    TextView tvUserName, tvUserID, tvGender, tvDOB, tvAge, tvHometown, tvRelationship, tvEmail, tvEducation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        //region CASTING
        coverImage = (ImageView)findViewById(R.id.imgCover);
        profileImage = (ImageView)findViewById(R.id.imgProfile);
        tvUserName = (TextView)findViewById(R.id.tvUsersName);
        tvUserID = (TextView)findViewById(R.id.tvUserID);
        tvGender = (TextView)findViewById(R.id.tvGender);
        tvDOB = (TextView)findViewById(R.id.tvDOB);
        tvAge = (TextView)findViewById(R.id.tvUserAge);
        tvHometown = (TextView)findViewById(R.id.tvHometown);
        tvRelationship = (TextView)findViewById(R.id.tvRelationship);
        tvEmail = (TextView)findViewById(R.id.tvEmail);
        tvEducation = (TextView)findViewById(R.id.tvEducation);
        //endregion

        defaultCover = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder1);
        defaultprofile = BitmapFactory.decodeResource(getResources(), R.drawable.anonymous);

        JsonResponse = getIntent().getExtras().getString("JsonResponse");

        coverImage.setImageBitmap(defaultCover);
        profileImage.setImageBitmap(defaultprofile);

        //SET DEFAULT PICS

        if (JsonResponse.equals("") || JsonResponse == "" || JsonResponse == null){
            //SHOW ALERT
        }else new parseJsonResponse().execute();
    }

    public Boolean CheckConnection(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void noConnectionDialog(){
        //CREATES AND SHOWS AN ALERT DIALOG
        AlertDialog alertNoActiveUser = new AlertDialog.Builder(this).create();
        alertNoActiveUser.setTitle("No Connection!");
        alertNoActiveUser.setMessage("No network connection found.\nPlease connect to a network and press 'ok' to re-attempt the collection of your details");

        alertNoActiveUser.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();    //CLOSES ALERT DIALOG
                Start();
            }
        });
        alertNoActiveUser.show();
    }

    public void Start(){
        if (CheckConnection()) {
            new callGraphAPI().execute();
        }else{
            noConnectionDialog();
        }
    }

    public void logOut(View view){

        //region REMOVE USER ID AND ACCESS TOKEN STORED IN SHARED PREFERENCES
        SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPrefs.edit();
        editor.putString("USER_ID", "undefined");
        editor.putString("ACCESS_TOKEN", "undefined");
        editor.commit();
        //endregion

        //region REMOVE ALL COOKIES USED BY THE AUTHENTICATION WEBVIEW TO DISABLE AUTO LOG IN
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        //endregion

        startActivity(new Intent(this, MainActivity.class));
    }

    private class parseJsonResponse extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            try {
                if (JsonResponse != null){

                    //region PARSE JSON
                    JSONArray jsonArray = new JSONArray(JsonResponse);

                    for (int i = 0; i < jsonArray.length(); i++) {

                        JSONObject json_message = jsonArray.getJSONObject(i);

                        if (json_message != null) {

                            fbAccessToken = json_message.getString("access_token");
                            expirationDateTime = json_message.getString("expires_on");
                            userEmail = json_message.getString("user_id");

                            //region PARSE EXTENDED USER SCOPE DETAILS
                            JSONArray extendedUserScope = json_message.getJSONArray("user_claims");

                            for (int ii = 0; ii < extendedUserScope.length(); ii++) {

                                JSONObject userCredentials = extendedUserScope.getJSONObject(ii);

                                userDetails.add(userCredentials.getString("val"));
                            }

                            if (userDetails.size() > 6) {
                                userID = userDetails.get(0);
                                userName = userDetails.get(2);
                                userSex = userDetails.get(6);
                                //userDOB = userDetails.get(7);
                                //userLocation = userDetails.get(8); //--
                                //userTimeZone = userDetails.get(9); //--
                            }
                            //endregion

                        }else{
                            fbAccessToken = null;
                            //region SET DEFAULTS
                            userEmail = "Not Specified";
                            userID = "Not Specified";
                            userName = "Not Specified";
                            userSex = "Not Specified";
                            //endregion
                        }
                    }
                    //endregion

                    ////====== original location for user details set from userDetails
                }else{
                    //region SET DEFAULTS
                    userEmail = "Not Specified";
                    userID = "Not Specified";
                    userName = "Not Specified";
                    userSex = "Not Specified";
                    //endregion
                }

            } catch (JSONException e) {
                e.printStackTrace();
                //region SET DEFAULTS
                userEmail = "Not Specified";
                userID = "Not Specified";
                userName = "Not Specified";
                userSex = "Not Specified";
                //endregion
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //region SET DISPLAY VALUES
            tvUserName.setText(userName);
            tvUserID.setText("ID: " + userID);
            tvGender.setText("Gender: " + userSex);
            tvEmail.setText("Email: " + userEmail);
            //endregion

            if (JsonResponse != null || fbAccessToken != null) {
                Start();
            }
        }
    }

    private class callGraphAPI extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            String fields = "birthday,age_range,cover,picture,education,hometown,relationship_status";
            String profilePicURL = "https://graph.facebook.com/me?access_token="+fbAccessToken+"&fields="+fields;

            try {
                if (CheckConnection()) {
                    URL url = new URL(profilePicURL);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("Content-length", "0");
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setAllowUserInteraction(false);
                    httpURLConnection.setConnectTimeout(10000);
                    httpURLConnection.setReadTimeout(10000);
                    httpURLConnection.connect();
                    int status = httpURLConnection.getResponseCode();
                    Log.d("myTag", String.valueOf(status) + " You are are here");

                    switch (status) {
                        case 201:

                            //region SHOW ERROR DIALOG
                            AlertDialog failedDialog1 = new AlertDialog.Builder(UserDetails.this).create();
                            failedDialog1.setTitle("Failed!");
                            failedDialog1.setMessage("The systems was Unable to obtain additional user details");

                            failedDialog1.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();    //CLOSES ALERT DIALOG
                                }
                            });
                            failedDialog1.show();
                            //endregion

                            //region SET DEFAULT VALUES
                            userRelationshipStatus = "Not Specified";
                            userDOB = "Not Specified";
                            userAge = "Not Specified";
                            userHometown = "Not Specified";
                            education = "Not Specified";
                            userCoverPic = defaultCover;
                            userProfilePic = defaultprofile;
                            //endregion

                            break;

                        case 200:

                            //region SUCCESSFUL HTTP REQUEST
                            //region GET HTTP REQUEST RESPONSE
                            BufferedReader br = null;
                            try {
                                br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

                                String line = "";
                                GraphJsonResponse = "";

                                while ((line = br.readLine()) != null) {
                                    GraphJsonResponse += line;
                                }

                                br.close();
                                httpURLConnection.disconnect();
                            } catch (IOException e) {
                                e.printStackTrace();
                                GraphJsonResponse = null;
                            }
                            //endregion

                            if (GraphJsonResponse != null || GraphJsonResponse != "" || !GraphJsonResponse.equals("")) {

                                //region PARSE JSON
                                JSONObject jsonObject = new JSONObject(GraphJsonResponse);

                                //region JSON COMPONENT INSTANTIATION
                                JSONObject ageObject = null;
                                JSONObject coverPicObject = null;
                                JSONObject profilePicObject = null;
                                JSONObject hometownObject = null;
                                JSONArray educationArray = null;
                                //endregion

                                //region GET CORE JSON OBJECTS
                                if (jsonObject != null) {
                                    try {
                                        ageObject = jsonObject.getJSONObject("age_range");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        coverPicObject = jsonObject.getJSONObject("cover");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        profilePicObject = jsonObject.getJSONObject("picture");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        hometownObject = jsonObject.getJSONObject("hometown");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        educationArray = jsonObject.getJSONArray("education");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //endregion

                                //region GET USERS RELATIONSHIP STATUS
                                if (jsonObject != null) {
                                    try {
                                        userRelationshipStatus = jsonObject.getString("relationship_status");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        userRelationshipStatus = "Not Specified";
                                    }
                                } else userRelationshipStatus = "Not Specified";
                                //endregion

                                //region GET USERS DATE OF BIRTH
                                if (jsonObject != null) {
                                    try {
                                        userDOB = jsonObject.getString("birthday");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        userDOB = "Not Specified";
                                    }
                                } else userDOB = "Not Specified";
                                //endregion

                                //region GET USERS AGE
                                if (ageObject != null) {
                                    try {
                                        userAge = ageObject.getString("min");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        userAge = "Not Specified";
                                    }
                                } else userAge = "Not Specified";
                                //endregion

                                //region GET HOMETOWN
                                if (hometownObject != null) {
                                    try {
                                        userHometown = hometownObject.getString("name");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        userHometown = "Not Specified";
                                    }
                                } else userHometown = "Not Specified";
                                //endregion

                                //region GET EDUCATION INFORMATION
                                if (educationArray != null) {
                                    for (int i = 0; i < educationArray.length(); i++) {

                                        JSONObject educationObject = educationArray.getJSONObject(i);

                                        String Course = "";

                                        try {
                                            JSONArray courseArray = educationObject.getJSONArray("concentration");
                                            for (int ii = 0; ii < courseArray.length(); ii++) {
                                                JSONObject courseObject = courseArray.getJSONObject(ii);
                                                Course = Course + courseObject.getString("name");
                                                EducationCourse.add(Course);
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            EducationCourse.add("Not Specified");
                                        }

                                        JSONObject schoolObject = educationObject.getJSONObject("school");
                                        EducationEstablishment.add(schoolObject.getString("name"));
                                    }
                                } else {
                                    EducationCourse.add("Not Specified");
                                    EducationEstablishment.add("Not Specified");
                                }
                                //endregion

                                //region GET PROFILE PIC
                                if (profilePicObject != null) {
                                    JSONObject dataObject = profilePicObject.getJSONObject("data");
                                    profilePicURL = dataObject.getString("url");


                                    if (profilePicURL.length() != 0) {

                                        URL u = new URL(profilePicURL);
                                        HttpURLConnection httpCon = null;
                                        int intStatusCode;

                                        if (CheckConnection()) {
                                            httpCon = (HttpURLConnection) u.openConnection();
                                            intStatusCode = httpCon.getResponseCode();
                                        } else intStatusCode = 201;

                                        InputStream is;

                                        if (intStatusCode != 200) {
                                            //set default image
                                        } else {
                                            if (CheckConnection()) {
                                                is = httpCon.getInputStream();
                                                userProfilePic = BitmapFactory.decodeStream(is);
                                                is.close();
                                                httpCon.disconnect();
                                            } else {
                                                userProfilePic = defaultprofile;
                                            }
                                        }
                                    } else userProfilePic = defaultprofile;
                                } else userProfilePic = defaultprofile;
                                //endregion

                                //region GET COVER PHOTO
                                if (coverPicObject != null) {
                                    coverPicURL = coverPicObject.getString("source");

                                    if (coverPicURL.length() != 0) {

                                        URL u = new URL(coverPicURL);
                                        HttpURLConnection httpCon = null;
                                        int intStatusCode;

                                        //check connection
                                        if (CheckConnection()) {
                                            httpCon = (HttpURLConnection) u.openConnection();
                                            intStatusCode = httpCon.getResponseCode();
                                        } else intStatusCode = 201;

                                        InputStream is;

                                        if (intStatusCode != 200) {
                                            //set default image
                                        } else {
                                            //checkCOnnection
                                            if (CheckConnection()) {
                                                is = httpCon.getInputStream();
                                                userCoverPic = BitmapFactory.decodeStream(is);
                                                is.close();
                                                httpCon.disconnect();
                                            } else {
                                                userCoverPic = defaultCover;
                                            }
                                        }
                                    } else userCoverPic = defaultCover;
                                } else userCoverPic = defaultCover;
                                //endregion
                                //endregion

                                //region CONSTRUCT EDUCATION STRING FOR DISPLAY
                                education = "";
                                if(!EducationEstablishment.get(0).equals("Not Specified")) {
                                    for (int i = 0; i < EducationEstablishment.size(); i++) {
                                        String educationLine = EducationEstablishment.get(i) + ": \t" + EducationCourse.get(i);
                                        education += educationLine + "\n";
                                    }
                                }else education = "Not Specified";
                                //endregion

                            }else{

                                //region SHOW ERROR DIALOG
                                AlertDialog failedDialog2 = new AlertDialog.Builder(UserDetails.this).create();
                                failedDialog2.setTitle("Failed!");
                                failedDialog2.setMessage("The systems was Unable to obtain additional user details");

                                failedDialog2.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();    //CLOSES ALERT DIALOG
                                    }
                                });
                                failedDialog2.show();
                                //endregion

                                //region SET DEFAULT VALUES
                                userRelationshipStatus = "Not Specified";
                                userDOB = "Not Specified";
                                userAge = "Not Specified";
                                userHometown = "Not Specified";
                                education = "Not Specified";
                                userCoverPic = defaultCover;
                                userProfilePic = defaultprofile;
                                //endregion
                            }
                            //endregion

                            break;

                        case 401:

                            //region SHOW ERROR DIALOG
                            AlertDialog faliedDialog3 = new AlertDialog.Builder(UserDetails.this).create();
                            faliedDialog3.setTitle("Failed!");
                            faliedDialog3.setMessage("The systems was Unable to obtain additional user details");

                            faliedDialog3.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();    //CLOSES ALERT DIALOG
                                }
                            });
                            faliedDialog3.show();
                            //endregion

                            //region SET DEFAULT VALUES
                            userRelationshipStatus = "Not Specified";
                            userDOB = "Not Specified";
                            userAge = "Not Specified";
                            userHometown = "Not Specified";
                            education = "Not Specified";
                            userCoverPic = defaultCover;
                            userProfilePic = defaultprofile;
                            //endregion

                            break;
                    }
                }else{

                    //region SHOW ERROR DIALOG
                    AlertDialog failedDialog4 = new AlertDialog.Builder(UserDetails.this).create();
                    failedDialog4.setTitle("Failed!");
                    failedDialog4.setMessage("The systems was unable to obtain additional user details");

                    failedDialog4.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();    //CLOSES ALERT DIALOG
                        }
                    });
                    failedDialog4.show();
                    //endregion

                    //region SET DEFAULT VALUES
                    userRelationshipStatus = "Not Specified";
                    userDOB = "Not Specified";
                    userAge = "Not Specified";
                    userHometown = "Not Specified";
                    education = "Not Specified";
                    userCoverPic = defaultCover;
                    userProfilePic = defaultprofile;
                    //endregion
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                //region SET DEFAULT VALUES
                userRelationshipStatus = "Not Specified";
                userDOB = "Not Specified";
                userAge = "Not Specified";
                userHometown = "Not Specified";
                education = "Not Specified";
                userCoverPic = defaultCover;
                userProfilePic = defaultprofile;
                //endregion
            } catch (IOException e) {
                e.printStackTrace();
                //region SET DEFAULT VALUES
                userRelationshipStatus = "Not Specified";
                userDOB = "Not Specified";
                userAge = "Not Specified";
                userHometown = "Not Specified";
                education = "Not Specified";
                userCoverPic = defaultCover;
                userProfilePic = defaultprofile;
                //endregion
            } catch (JSONException e) {
                e.printStackTrace();
                //region SET DEFAULT VALUES
                userRelationshipStatus = "Not Specified";
                userDOB = "Not Specified";
                userAge = "Not Specified";
                userHometown = "Not Specified";
                education = "Not Specified";
                userCoverPic = defaultCover;
                userProfilePic = defaultprofile;
                //endregion
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            coverImage.setImageBitmap(userCoverPic);
            profileImage.setImageBitmap(userProfilePic);
            tvDOB.setText("DoB: " + userDOB);
            tvAge.setText("Age: " + userAge);
            tvHometown.setText("Hometown: " + userHometown);
            tvRelationship.setText("Relationship Status: " + userRelationshipStatus);
            if (education != "Not Specified") {
                tvEducation.setText("Education:\n" + education);
            }else tvEducation.setText("Education: " + education);
        }
    }

}
