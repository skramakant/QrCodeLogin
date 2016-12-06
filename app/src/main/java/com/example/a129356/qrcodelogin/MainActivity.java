package com.example.a129356.qrcodelogin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private LoginButton loginButton;
    private AccessTokenTracker accessTokenTracker;
    CallbackManager callbackManager;
    private Button scanButton;
    private TextView greetingMsg;
    private ImageView profilePicture;
    private ProfileTracker profileTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        FacebookSdk.getApplicationSignature(getApplicationContext());
        setContentView(R.layout.activity_main);

        loginButton = (LoginButton) findViewById(R.id.login_button);
        scanButton = (Button) findViewById(R.id.scan_button);
        greetingMsg = (TextView) findViewById(R.id.text_msg);
        profilePicture = (ImageView) findViewById(R.id.profile_image);

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null)
        {
            Log.v("###","Access Token: "+accessToken.getToken());
            scanButton.setVisibility(View.VISIBLE);
            updateUI();
        }
        else
        {
            scanButton.setVisibility(View.INVISIBLE);
            Log.v("###","## NOT LOgged IN");
        }

        callbackManager = CallbackManager.Factory.create();

        //loginButton.setReadPermissions("email");

        loginButton.setReadPermissions(Arrays.asList("public_profile","email"));
        // If using in a fragment
        //loginButton.setFragment(this);
        // Other app specific specialization

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Log.v("###", "Login Success");
                showText("Login Success");
                accessTokenTracker.startTracking();
                scanButton.setVisibility(View.VISIBLE);
                //updateUI();
            }

            @Override
            public void onCancel() {
                // App code
                Log.v("###", "Cancel");
                showText("User Cancel");

                scanButton.setVisibility(View.INVISIBLE);

                updateUI();
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.v("###", "Error Login");
                showText("Error login facebook");

                scanButton.setVisibility(View.INVISIBLE);

                updateUI();
            }
        });

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                if(oldProfile != null && currentProfile == null){
                    scanButton.setVisibility(View.INVISIBLE);
                }
                updateUI();
            }
        };



        final IntentIntegrator scanIntegrator = new IntentIntegrator(this);

        scanButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                scanIntegrator.initiateScan();
                //Intent intent = new Intent(getApplicationContext(),CaptureActivity.class);
                //intent.setAction("com.google.zxing.client.android.SCAN");
                //intent.putExtra("SAVE_HISTORY", false);
                //startActivityForResult(intent, 0);

            }
        });



        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    //write your code here what to do when user logout
                    profilePicture.setImageResource(R.mipmap.ic_launcher);
                    greetingMsg.setText("Welcome");
                }
            }
        };
    }

/*    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }*/


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.v("####","Request:"+requestCode+ "resultCOde:"+resultCode+" Data:"+data);
        if(data != null && data.getAction() != null && data.getAction().equals(ACTION_SCAN)) //QR Code
        {

            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanningResult != null) {
                final String qrStr = scanningResult.getContents();
                String scanFormat = scanningResult.getFormatName();


                final AccessToken accessToken = AccessToken.getCurrentAccessToken();

                if(accessToken == null)
                {
                    showText("User not Logged In");

                }
                else
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendToServer(accessToken.getToken(), qrStr);
                            Log.v("network","in send to server thread");
                        }
                    }).start();

                }
                super.onActivityResult(requestCode, resultCode, data);


                //we have a result
            }
            else
            {
                showText("No QR scan data received");


            }

        }
        else if(data != null)
        {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }


    }


    private void sendToServer(String accessToken,String qrStr)
    {
        HttpClient httpClient = new DefaultHttpClient();


        //HttpPost httpPost = new HttpPost("http://nodejs-whatsappauth.rhcloud.com/auth");

        HttpPost httpPost = new HttpPost("http://10.122.40.185:3000/auth");

        String jsonStr="{\"uuid\":\""+qrStr+"\",\"access_token\":\""+accessToken+"\"}";

        Log.v("##"," JSON to post:"+jsonStr);
        try {

            StringEntity se = new StringEntity(jsonStr);
            httpPost.setEntity(se);
            HttpResponse response = httpClient.execute(httpPost);

            Log.v("network","Respnse:"+response.toString());
            Log.v("network","Ruccessfully posted token");
            //showText("Successfully posted token");

        }
        catch (ClientProtocolException e) {
            // Log exception
            //showText("Unablet to Post Token");
            Log.v("network",e.toString());
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {

            //showText("Unablet to Post Token");
            e.printStackTrace();
            Log.v("network",e.toString());
        }
        catch (IOException e) {
            // Log exception
            //showText("Unablet to Post Token");

            e.printStackTrace();
            Log.v("network",e.toString());
        }

    }

    private void showText(String message)
    {
        Toast toast = Toast.makeText(getApplicationContext(),
                message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void updateUI() {
        boolean enableButtons = AccessToken.getCurrentAccessToken() != null;

        Profile profile = Profile.getCurrentProfile();
        if (enableButtons && profile != null) {
            greetingMsg.setText(profile.getFirstName());
            //"https://graph.facebook.com/me/picture?access_token=" + enableButtons +"/"
            final Uri photo_url_str = profile.getProfilePictureUri(150,150);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    URL myUrl = null;
                    try {
                        myUrl = new URL(photo_url_str.toString());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    InputStream inputStream = null;
                    try {
                        inputStream = (InputStream)myUrl.getContent();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    final Drawable drawable = Drawable.createFromStream(inputStream, null);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            profilePicture.setImageDrawable(drawable);
                        }
                    });


                }
            }).start();


            //profilePicture.setImageURI(photo_url_str);
            //profilePicture.setImageURI();
        } else {
            //greetingMsg.setText("");
            profilePicture.setImageResource(R.mipmap.ic_launcher);
            greetingMsg.setText("Welcome");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

       // updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        profileTracker.stopTracking();
    }
}
