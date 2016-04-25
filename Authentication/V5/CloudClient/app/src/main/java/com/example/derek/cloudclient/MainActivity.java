package com.example.derek.cloudclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//###########################################################################
//
// FACEBOOK AUTHENTICATION ADAPTED FROM MOBILE COMPUTING WORKSHOPS AND https://azure.microsoft.com/en-gb/documentation/articles/mobile-services-android-get-started-users/
// ENCRYPTION OF ACCESS TOKEN ADAPTED FROM THE SOURCE CODE FOUND AT http://www.developer.com/ws/android/encrypting-with-android-cryptography-api.html
//
//###########################################################################

public class MainActivity extends Activity {

    //region GLOBAL VARIABLES
    // Create an object to connect to your mobile app service
    private MobileServiceClient mClient;
    private static final String PREFS_NAME = "myPreferences";
    String JsonResponse;
    String token;
    String strError;
    RetrieveUserDetails retrieveUserDetailsTask;

    SecretKeySpec sks = null;
    byte[] encodedBytes = null;
    byte[] decodedBytes = null;
    byte[] keyByte = null;
    String strDecrypted = null;
    String encrypted = null;

    String KeySafe;
    //endregion

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

       } catch (MalformedURLException e) {
            e.printStackTrace();
       }
    }

    public void logIn(View view){
        authenticate();
    }

    public Boolean CheckConnection(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void authenticate() {

        if (CheckConnection()) {
            //CHECK IF THERE IS A USER STILL LOGGED IN OR NOT
            if (loadTokenCache(mClient) == true) {

                token = mClient.getCurrentUser().getAuthenticationToken();

                if (CheckConnection()) {
                    retrieveUserDetailsTask = (RetrieveUserDetails) new RetrieveUserDetails().execute();
                } else {
                    //region CANCEL AUTHENTICATION
                    noConnectionDialog();
                    logOut();
                    //endregion
                }
            } else {

                if (CheckConnection()) {
                    ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);

                    Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                        @Override
                        public void onFailure(Throwable exc) {
                            createAndShowDialog((exc.toString()), "Error");
                        }

                        @Override
                        public void onSuccess(MobileServiceUser user) {

                            cacheAccessToken(mClient.getCurrentUser());
                            token = user.getAuthenticationToken();

                            if (CheckConnection()) {
                                retrieveUserDetailsTask = (RetrieveUserDetails) new RetrieveUserDetails().execute();
                            } else {
                                //region CANCEL AUTHENTICATION
                                noConnectionDialog();
                                logOut();
                                //endregion
                            }
                        }
                    });
                } else {
                    noConnectionDialog();
                }
            }
        }else {
            noConnectionDialog();
        }
    }

    public void logOut(){

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

        //LOG OUT OF THE CURRENT CLIENT
        mClient.logout();
    }

    private void generateEncryptionKey(){
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed("encryption seed data".getBytes());
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128, sr);
            SecretKey secretKey = kg.generateKey();//---
            keyByte = secretKey.getEncoded();//---
            //sks = new SecretKeySpec(keyByte, "AES"); //----

            KeySafe = Base64.encodeToString(keyByte, Base64.DEFAULT);
            //sks = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void encryptToken(String strAccessToken){
        try {
            sks = new SecretKeySpec(keyByte, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, sks);
            encodedBytes = c.doFinal(strAccessToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        encrypted = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    }

    private String decryptToken(){
        try {
            sks = new SecretKeySpec(keyByte, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, sks);
            decodedBytes = c.doFinal(encodedBytes);

            strDecrypted = new String(decodedBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return strDecrypted;
    }

    private void cacheAccessToken(MobileServiceUser user){

        String strAccessToken = user.getAuthenticationToken();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //  GENERATE KEY
        generateEncryptionKey();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //  ENCRYPT TOKEN
        encryptToken(strAccessToken);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //region STORE IN USER ID AND ACCESS TOKEN IN SHARED PREFERENCES
        SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPrefs.edit();
        editor.putString("USER_ID", user.getUserId());
        editor.putString("ACCESS_TOKEN", encrypted);
        editor.putString("Key", KeySafe);
        editor.commit();
        //endregion
    }

    private boolean loadTokenCache(MobileServiceClient client){

        //region GET STORED USER ID AND ACCESS TOKEN FROM SHARED PREFERENCES
        SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userID = myPrefs.getString("USER_ID", "undefined");
        //String accessToken = myPrefs.getString("ACCESS_TOKEN", "undefined");
        encrypted = myPrefs.getString("ACCESS_TOKEN", "undefined");
        KeySafe = myPrefs.getString("Key", null);
        //endregion


        if (userID.equals("undefined") || /*accessToken*/encrypted.equals("undefined" ))
            return false;

        keyByte = Base64.decode(KeySafe, Base64.DEFAULT);
        encodedBytes = Base64.decode(encrypted, Base64.DEFAULT);

        //  DECRYPT TOKEN
        String accessToken = decryptToken();

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

    private void noConnectionDialog(){
        //CREATES AND SHOWS AN ALERT DIALOG
        AlertDialog alertNoActiveUser = new AlertDialog.Builder(this).create();
        alertNoActiveUser.setTitle("No Connection!");
        alertNoActiveUser.setMessage("No network connection found, please connect to a network and attempt to login again");

        alertNoActiveUser.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();    //CLOSES ALERT DIALOG
            }
        });
        alertNoActiveUser.show();
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

    private class RetrieveUserDetails extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            final String UserDataURL = "https://mobilecomputingservice1.azurewebsites.net/.auth/me";

            try {
                if (CheckConnection()) {
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
                        case 200:
                            BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                            String line = "";
                            JsonResponse = "";

                            while ((line = br.readLine()) != null) {
                                JsonResponse += line;
                            }

                            br.close();
                            httpURLConnection.disconnect();

                            break;

                        case 400:
                        case 401:
                        case 404:

                            //error
                            strError = "failed";
                            retrieveUserDetailsTask.cancel(true);

                            break;
                    }
                }else {

                    strError = "connection";
                    retrieveUserDetailsTask.cancel(true);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                strError = "failed";
                retrieveUserDetailsTask.cancel(true);
            } catch (IOException e) {
                e.printStackTrace();
                strError = "failed";
                retrieveUserDetailsTask.cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //region CHECK WHETHER THERE IS A VALID JSON RESPONSE AND START INTENT
            if (JsonResponse != null || JsonResponse != "" || !JsonResponse.equals("")) {
//                SharedPreferences myPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//                SharedPreferences.Editor editor = myPrefs.edit();
//                editor.putString("JSON", JsonResponse);
//                editor.commit();

                Intent userDetailsIntent = new Intent(MainActivity.this, UserDetails.class);
                userDetailsIntent.putExtra("JsonResponse", JsonResponse);
                startActivity(userDetailsIntent);
            }else{
                noConnectionDialog();
                logOut();
            }
            //endregion
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            if (strError == "failed" || strError.equals("failed")){
                AlertDialog failedRequest = new AlertDialog.Builder(MainActivity.this).create();
                failedRequest.setTitle("No Connection!");
                failedRequest.setMessage("No network connection found, please connect to a network and attempt to login again");

                failedRequest.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();    //CLOSES ALERT DIALOG
                        logOut();
                    }
                });
                failedRequest.show();
            }else if (strError == "connection" || strError.equals("connection")){
                noConnectionDialog();
                logOut();
            }
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