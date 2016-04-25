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
import android.text.format.DateUtils;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class UserDetails extends Activity {

    final static String PREFS_NAME = "myPreferences";
    String JsonResponse,GraphJsonResponse, fbAccessToken, expirationDateTime, userEmail, userID, userName, userSex, userDOB,
            userAge, userHometown, userRelationshipStatus, profilePicURL, ppURL, coverPicURL, education, strError;

    Boolean isStored;

    ArrayList<String> userDetails = new ArrayList<String>();
    ArrayList<String> EducationEstablishment = new ArrayList<String>();
    ArrayList<String> EducationCourse = new ArrayList<String>();

    Bitmap userProfilePic, userCoverPic, defaultprofile, defaultCover;

    ImageView coverImage, profileImage;
    TextView tvUserName, tvUserID, tvGender, tvDOB, tvAge, tvHometown, tvRelationship, tvEmail, tvEducation;

    private parseJsonResponse parseTask;
    private callGraphAPI graphCallTask;

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
        }else parseTask = (parseJsonResponse) new parseJsonResponse().execute();
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

    private void expiredDialog(){
        AlertDialog alertNoActiveUser = new AlertDialog.Builder(this).create();
        alertNoActiveUser.setTitle("Access Expired!");
        alertNoActiveUser.setMessage("Your authentication has expired, to view your details please re-authenticate with the applications");

        alertNoActiveUser.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logOutFunction();
                dialog.dismiss();
            }
        });
        alertNoActiveUser.show();
    }

    public void Start(){
        if (CheckConnection()) {
            graphCallTask = (callGraphAPI) new callGraphAPI().execute();
        }else{
            noConnectionDialog();
        }
    }

    public void logOutFunction(){

        //region REMOVE USER ID AND ACCESS TOKEN STORED IN SHARED PREFERENCES
        SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPrefs.edit();
        editor.clear();
        editor.commit();
        //endregion

        //region REMOVE ALL COOKIES USED BY THE AUTHENTICATION WEBVIEW TO DISABLE AUTO LOG IN
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        //endregion

        startActivity(new Intent(this, MainActivity.class));
    }

    public void logOut(View view){

        logOutFunction();
    }

    private class parseJsonResponse extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            isStored = myPrefs.getBoolean("DetailsStored", false);

            if(isStored){

                userName = myPrefs.getString("userName", "Not Specified");
                userID = myPrefs.getString("userID", "Not Specified");
                userSex = myPrefs.getString("userGender", "Not Specified");
                userEmail = myPrefs.getString("userEmail", "Not Specified");
                userDOB = myPrefs.getString("userDOB", "Not Specified");
                userAge = myPrefs.getString("userAge", "Not Specified");
                userHometown = myPrefs.getString("userHometown", "Not Specified");
                userRelationshipStatus = myPrefs.getString("userRelationship", "Not Specified" );
                education = myPrefs.getString("userEducation", "Not Specified");
                profilePicURL = myPrefs.getString("userProfilePic", null);
                coverPicURL = myPrefs.getString("userCoverPic", null);

                //region GET PROFILE PIC
                if (profilePicURL != null) {

                    if (profilePicURL.length() != 0) {

                        URL u = null;
                        try {
                            u = new URL(profilePicURL);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        HttpURLConnection httpCon = null;
                        int intStatusCode = 200;

                        if (CheckConnection()) {
                            try {
                                httpCon = (HttpURLConnection) u.openConnection();
                                intStatusCode = httpCon.getResponseCode();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else intStatusCode = 201;

                        InputStream is;

                        if (intStatusCode != 200) {
                            userProfilePic = defaultprofile;
                        } else {
                            if (CheckConnection()) {
                                try {
                                    is = httpCon.getInputStream();
                                    userProfilePic = BitmapFactory.decodeStream(is);
                                    is.close();
                                    httpCon.disconnect();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                userProfilePic = defaultprofile;
                            }
                        }
                    } else userProfilePic = defaultprofile;
                } else userProfilePic = defaultprofile;
                //endregion

                //region GET COVER PHOTO
                if (coverPicURL != null) {

                    if (coverPicURL.length() != 0) {

                        URL u = null;
                        try {
                            u = new URL(coverPicURL);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        HttpURLConnection httpCon = null;
                        int intStatusCode= 200;

                        //check connection
                        if (CheckConnection()) {
                            try {
                                httpCon = (HttpURLConnection) u.openConnection();
                                intStatusCode = httpCon.getResponseCode();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else intStatusCode = 201;

                        InputStream is;

                        if (intStatusCode != 200) {
                            //set default image
                            userCoverPic = defaultCover;
                        } else {
                            //checkCOnnection
                            if (CheckConnection()) {
                                try {
                                    is = httpCon.getInputStream();
                                    userCoverPic = BitmapFactory.decodeStream(is);
                                    is.close();
                                    httpCon.disconnect();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                userCoverPic = defaultCover;
                            }
                        }
                    } else userCoverPic = defaultCover;
                } else userCoverPic = defaultCover;
                //endregion

            }else {
                try {
                    if (JsonResponse != null) {

                        //region PARSE JSON
                        JSONArray jsonArray = new JSONArray(JsonResponse);

                        for (int i = 0; i < jsonArray.length(); i++) {

                            JSONObject json_message = jsonArray.getJSONObject(i);

                            if (json_message != null) {

                                fbAccessToken = json_message.getString("access_token");
                                expirationDateTime = json_message.getString("expires_on");
                                userEmail = json_message.getString("user_id");

                                //region CHECK IF FB ACCESS TOKEN HAS EXPIRED
                                String[] accessExpiration = expirationDateTime.split("T");
                                String expirationDate = accessExpiration[0];

                                //region FORM DATES
                                Calendar c = Calendar.getInstance();
                                String strCurrentYear = String.valueOf(c.get(Calendar.YEAR)),
                                        strCurrentMonth = String.valueOf(c.get(Calendar.MONTH) + 1),
                                        strCurrentDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));

                                String[] arDate = expirationDate.split("-");
                                String strYear = arDate[0], strMonth = arDate[1], strDay = arDate[2];
                                //endregion

                                //region FORMAT DATES AND CHECK IF EXPIRED
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

                                try {
                                    Date dateCurrentDate = simpleDateFormat.parse(strCurrentYear + "-" + strCurrentMonth + "-" + strCurrentDay);
                                    Date dateParsedDate = simpleDateFormat.parse(strYear + "-" + strMonth + "-" + strDay);

                                    //CHECKS IF PARSED DATE IS BEFORE CURRENT DATE
                                    if (dateParsedDate.before(dateCurrentDate) || dateParsedDate == dateCurrentDate) {
                                        //show alert and onclick logout
                                        parseTask.cancel(true);
                                    }

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                //endregion
                                //endregion

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

                            } else {
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
                    } else {
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
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (isStored){
                //region SET DISPLAY VALUES
                tvUserName.setText(userName);
                tvUserID.setText("ID: " + userID);
                tvGender.setText("Gender: " + userSex);
                tvEmail.setText("Email: " + userEmail);
                coverImage.setImageBitmap(userCoverPic);
                profileImage.setImageBitmap(userProfilePic);
                tvDOB.setText("DoB: " + userDOB);
                tvAge.setText("Age: " + userAge);
                tvHometown.setText("Hometown: " + userHometown);
                tvRelationship.setText("Relationship Status: " + userRelationshipStatus);
                if (education != "Not Specified") {
                    tvEducation.setText("Education:\n" + education);
                }else tvEducation.setText("Education: " + education);
                //endregion
            }else {
                //region SET DISPLAY VALUES
                tvUserName.setText(userName);
                tvUserID.setText("ID: " + userID);
                tvGender.setText("Gender: " + userSex);
                tvEmail.setText("Email: " + userEmail);
                //endregion

                if (JsonResponse != null && fbAccessToken != null) {
                    Start();
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            expiredDialog();
        }
    }

    private class callGraphAPI extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            String fields = "birthday,age_range,cover,picture,education,hometown,relationship_status";
            String profilePicURL = "https://graph.facebook.com/me?access_token="+fbAccessToken+"&fields="+fields; //me?

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
                                    ppURL = profilePicURL;


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
                                            userProfilePic = defaultprofile;
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
                                            userCoverPic = defaultCover;
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

                                strError = "failed";
                                graphCallTask.cancel(true);
                            }
                            //endregion

                            break;

                        case 400:
                        case 401:
                        case 404:

                            strError = "failed";
                            graphCallTask.cancel(true);
                            break;
                    }
                }else{

                    strError = "connection";
                    graphCallTask.cancel(true);
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


            SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = myPrefs.edit();
            editor.putBoolean("DetailsStored", true);
            editor.putString("userName", userName);
            editor.putString("userID", userID);
            editor.putString("userGender", userSex);
            editor.putString("userEmail", userEmail);
            editor.putString("userDOB", userDOB);
            editor.putString("userAge", userAge);
            editor.putString("userHometown", userHometown);
            editor.putString("userRelationship", userRelationshipStatus);
            editor.putString("userEducation", education);
            editor.putString("userProfilePic", ppURL);
            editor.putString("userCoverPic", coverPicURL);
            editor.commit();


        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            if (strError == "failed" || strError.equals("failed")){
                AlertDialog failedDialog = new AlertDialog.Builder(UserDetails.this).create();
                failedDialog.setTitle("Failed!");
                failedDialog.setMessage("The systems was unable to obtain additional user details");

                failedDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();    //CLOSES ALERT DIALOG
                    }
                });
                failedDialog.show();
            }else if (strError == "connection" || strError.equals("connection")){
                noConnectionDialog();
            }

            //region SET DEFAULT VALUES
            userRelationshipStatus = "Not Specified";
            userDOB = "Not Specified";
            userAge = "Not Specified";
            userHometown = "Not Specified";
            education = "Not Specified";
            userCoverPic = defaultCover;
            userProfilePic = defaultprofile;
            //endregion

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
