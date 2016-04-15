package com.example.derek.cloudclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.*;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends Activity {

    // Create an object to connect to your mobile app service
    private MobileServiceClient mClient;

    // Create an object for  a table on your mobile app service
    private MobileServiceTable<ToDoItem> mToDoTable;

    // global variable to update a TextView control text
    TextView display;

    // simple stringbulder to store textual data retrieved from mobile app service table
    StringBuilder sb = new StringBuilder();

    private static final String PREFS_NAME = "myPreferences";
    String JsonResponse;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       try {

           // using the MobileServiceClient global object, create a reference to YOUR service
           mClient = new MobileServiceClient(
                   "https://mobilecomputingservice1.azurewebsites.net",
                   this
           );

           authenticate();
           // using the MobileServiceTable object created earlier, create a reference to YOUR table
           //mToDoTable = mClient.getTable(ToDoItem.class);

           // reference to textview where you can bind data from table when authenticated
           //display = (TextView) findViewById(R.id.displayData);


       } catch (MalformedURLException e) {
            e.printStackTrace();
       }
    }

    public void logIn(View view){
        authenticate();
    }

    private void authenticate() {

        //CHECK IF THERE IS A USER STILL LOGGED IN OR NOT
        if (loadTokenCache(mClient) == true){

            Toast.makeText(MainActivity.this, "RETURNING USER", Toast.LENGTH_SHORT).show();
            token = mClient.getCurrentUser().getAuthenticationToken();
            new RetrieveUserDetails().execute();
        }else {

            ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);

            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    createAndShowDialog((exc.toString()), "Error");
                }

                @Override
                public void onSuccess(MobileServiceUser user) {
                    createAndShowDialog(String.format("You are now logged in - %1$2s", user.getUserId()), "Success");
                    cacheAccessToken(mClient.getCurrentUser());

                    token = user.getAuthenticationToken();
                    new RetrieveUserDetails().execute();

                }
            });
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

        //LOG OUT OF THE CURRENT CLIENT
        mClient.logout();
    }

    //STORE USERS ACCESS TOKEN WHEN AUTHENTICATED BY AZURE / FACEBOOK
    private void cacheAccessToken(MobileServiceUser user){

        //region STORE IN USER ID AND ACCESS TOKEN IN SHARED PREFERENCES
        SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPrefs.edit();
        editor.putString("USER_ID", user.getUserId());
        editor.putString("ACCESS_TOKEN", user.getAuthenticationToken());
        editor.commit();
        //endregion
    }

    private boolean loadTokenCache(MobileServiceClient client){

        //region GET STORED USER ID AND ACCESS TOKEN FROM SHARED PREFERENCES
        SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userID = myPrefs.getString("USER_ID", "undefined");
        String accessToken = myPrefs.getString("ACCESS_TOKEN", "undefined");
        //endregion

        if (userID.equals("undefined") ||accessToken.equals("undefined" ))
            return false;

        //region CREATE NEW USER WITH STORED CREDENTIALS AND SET CLIENT TO USE THE NEW USER
        MobileServiceUser user = new MobileServiceUser(userID);
        user.setAuthenticationToken(accessToken);
        client.setCurrentUser(user);
        //endregion

        return true;
    }

    private void createAndShowDialog(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // class used to work with ToDoItem table in mobile service, this needs to be edited if you wish to use with another table
    public class ToDoItem {
        private String id;
        private String text;
    }

    private class RetrieveUserDetails extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            final String UserDataURL = "https://mobilecomputingservice1.azurewebsites.net/.auth/me";

            try {
                URL url = new URL(UserDataURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-length", "0");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("X-ZUMO-AUTH", token);
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
                        StringBuilder sb = new StringBuilder();
                        String line = "";
                        JsonResponse = "";


                        while ((line = br.readLine()) != null) {
                            JsonResponse += line;
                        }

                        br.close();
                        httpURLConnection.disconnect();

                        break;
                    case 401:
                        break;
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

//            SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//            SharedPreferences.Editor editor = myPrefs.edit();
//            editor.putString("JSON", JsonResponse);
//            editor.commit();

            Intent userDetailsInent = new Intent(MainActivity.this, UserDetails.class);
            userDetailsInent.putExtra("JsonResponse", JsonResponse);
            startActivity(userDetailsInent);
        }
    }
}


//    private void authenticate() {
//
//        ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);
//
//        Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
//            @Override
//            public void onFailure(Throwable exc) {
//                createAndShowDialog((exc.toString()), "Error");
//            }
//
//            @Override
//            public void onSuccess(MobileServiceUser user) {
//                createAndShowDialog(String.format("You are now logged in - %1$2s", user.getUserId()), "Success");
//
//            }
//        });
//    }