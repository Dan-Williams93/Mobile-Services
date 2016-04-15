package com.example.derek.cloudclient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class UserDetails extends Activity {

    String JsonResponse,GraphJsonResponse, fbAccessToken, expirationDateTime, userEmail, userID, userName, userSex, userDOB, userLocation,
            userTimeZone, userAbout, userAge, userBio, userHometown, userRelationshipStatus, profilePicURL, coverPicURL;

    ArrayList<String> userDetails = new ArrayList<String>();
    ArrayList<String> EducationEstablishment = new ArrayList<String>();
    ArrayList<String> EducationCourse = new ArrayList<String>();

    Bitmap userProfilePic, userCoverPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        JsonResponse = getIntent().getExtras().getString("JsonResponse");

        if (JsonResponse.equals("") || JsonResponse == "" || JsonResponse == null){
            //SHOW ALERT
        }else new parseJsonResponse().execute();
    }

    public void Start(){
        new callGraphAPI().execute();
    }

    private class parseJsonResponse extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            try {
                JSONArray jsonArray = new JSONArray(JsonResponse);

                for (int i = 0; i < jsonArray.length(); i++) {

                    //CREATES A JSON OBJECT FOR THE ITEM AT THE CURRENT POSITION
                    JSONObject json_message = jsonArray.getJSONObject(i);

                    if (json_message != null) {

                        fbAccessToken = json_message.getString("access_token");
                        expirationDateTime = json_message.getString("expires_on");
                        userEmail = json_message.getString("user_id");

                        JSONArray extendedUserScope = json_message.getJSONArray("user_claims");

                        for (int ii = 0; ii < extendedUserScope.length(); ii++){

                            JSONObject userCredentials = extendedUserScope.getJSONObject(ii);

                            userDetails.add(userCredentials.getString("val"));
                        }
                    }
                }

                userID = userDetails.get(0);
                userName = userDetails.get(2);
                userSex = userDetails.get(6);
                userDOB = userDetails.get(7);
                userLocation = userDetails.get(8);
                userTimeZone =userDetails.get(9);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Start();
        }
    }

    private class callGraphAPI extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            String fields = "about,age_range,bio,cover,picture,education,hometown,relationship_status,religion";
            String profilePicURL = "https://graph.facebook.com/me?access_token="+fbAccessToken+"&fields="+fields;

            try {
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
                        break;
                    case 200:
                        BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                        String line = "";
                        GraphJsonResponse = "";

                        while ((line = br.readLine()) != null) {
                            GraphJsonResponse += line;
                        }

                        br.close();
                        httpURLConnection.disconnect();

                        //PARSE JSON
                        JSONObject jsonObject = new JSONObject(GraphJsonResponse);

                        JSONObject ageObject = null;
                        JSONObject coverPicObject = null;
                        JSONObject profilePicObject = null;
                        JSONObject hometownObject = null;
                        JSONArray educationArray = null;

                        if (jsonObject != null) {
                            try {
                                ageObject = jsonObject.getJSONObject("age_range");
                                coverPicObject = jsonObject.getJSONObject("cover");
                                profilePicObject = jsonObject.getJSONObject("picture");
                                hometownObject = jsonObject.getJSONObject("hometown");
                                educationArray = jsonObject.getJSONArray("education");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        //region GET USERS RELATIONSHIP STATUS
                        if (jsonObject != null) {
                            try {
                                userRelationshipStatus = jsonObject.getString("relationship_status");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                userRelationshipStatus = "Not Specified";
                            }
                        }else userRelationshipStatus = "Not Specified";
                        //endregion

                        //region GET USERS AGE
                        if (ageObject != null) {
                            try {
                                userAge = ageObject.getString("min");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                userAge = "Not Specified";
                            }
                        }else userAge = "Not Specified";
                        //endregion

                        //region GET HOMETOWN
                        if (hometownObject != null) {
                            try {
                                userHometown = hometownObject.getString("name");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                userHometown = "Not Specified";
                            }
                        }else userHometown = "Not Specified";
                        //endregion

                        //region GET EDUCATION INFORMATION
                        if (educationArray.length() > 0) {
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
                        }else{
                            EducationCourse.add("Not Specified");
                            EducationEstablishment.add("Not Specified");
                        }
                        //endregion

                        //region GET PROFILE PIC
                        if (profilePicObject != null){
                            JSONObject dataObject = profilePicObject.getJSONObject("data");
                            profilePicURL = dataObject.getString("url");


                            if (profilePicURL.length() != 0) {

                                URL u = new URL(profilePicURL);
                                HttpURLConnection httpCon;
                                int intStatusCode;

                                //check connection
                                //download image cover art from url and save as a bitmap
                                httpCon = (HttpURLConnection) u.openConnection();
                                intStatusCode = httpCon.getResponseCode();
                                //else statusCode 201

                                InputStream is;

                                if (intStatusCode != 200) {
                                    //set default image
                                }
                                else{
                                    //checkCOnnection
                                    is = httpCon.getInputStream();
                                    userProfilePic = BitmapFactory.decodeStream(is);
                                    is.close();
                                    httpCon.disconnect();
                                    //else set default
                                }
                            }//elseSET DeFAULT

                        }//else SET DEFAULT IMAGE
                        //endregion

                        //region GET COVER PHOTO
                        if (coverPicObject != null){
                            coverPicURL = coverPicObject.getString("source");

                            if (coverPicURL.length() != 0) {

                                URL u = new URL(coverPicURL);
                                HttpURLConnection httpCon;
                                int intStatusCode;

                                //check connection
                                httpCon = (HttpURLConnection) u.openConnection();
                                intStatusCode = httpCon.getResponseCode();
                                //else statusCode 201

                                InputStream is;

                                if (intStatusCode != 200) {
                                    //set default image
                                }
                                else{
                                    //checkCOnnection
                                    is = httpCon.getInputStream();
                                    userCoverPic = BitmapFactory.decodeStream(is);
                                    is.close();
                                    httpCon.disconnect();
                                    //else set default
                                }
                            }// else SET DeFAULT
                        } // else SET DEFAULT IMAGE
                        //endregion

                        break;
                    case 401:
                        break;
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}
