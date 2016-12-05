package com.example.a129356.qrcodelogin;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
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
import java.io.UnsupportedEncodingException;


public class MainActivity extends AppCompatActivity {
    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private LoginButton loginButton;
    CallbackManager callbackManager;
    private Button scanButton;
    private TextView greetingMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        loginButton = (LoginButton) findViewById(R.id.login_button);
        scanButton = (Button) findViewById(R.id.scan_button);
        greetingMsg = (TextView) findViewById(R.id.text_msg);

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null)
        {
            Log.v("###","Access Token: "+accessToken.getToken());
            scanButton.setVisibility(View.VISIBLE);
        }
        else
        {
            scanButton.setVisibility(View.INVISIBLE);
            Log.v("###","## NOT LOgged IN");
        }

        callbackManager = CallbackManager.Factory.create();

        loginButton.setReadPermissions("email");
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

                scanButton.setVisibility(View.VISIBLE);
                updateUI();
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
                String qrStr = scanningResult.getContents();
                String scanFormat = scanningResult.getFormatName();


                AccessToken accessToken = AccessToken.getCurrentAccessToken();

                if(accessToken == null)
                {
                    showText("User not Logged In");

                }
                else
                {
                    sendToServer(accessToken.getToken(), qrStr);
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

        HttpPost httpPost = new HttpPost("http://10.122.40.32:3000");

        String jsonStr="{\"uuid\":\""+qrStr+"\",\"access_token\":\""+accessToken+"\"}";

        Log.v("##"," JSON to post:"+jsonStr);
        try {

            StringEntity se = new StringEntity(jsonStr);
            httpPost.setEntity(se);
            HttpResponse response = httpClient.execute(httpPost);

            Log.v("###","Respnse:"+response.toString());
            showText("Successfully posted token");

        }
        catch (ClientProtocolException e) {
            // Log exception
            showText("Unablet to Post Token");

            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {

            showText("Unablet to Post Token");
            e.printStackTrace();
        }
        catch (IOException e) {
            // Log exception
            showText("Unablet to Post Token");

            e.printStackTrace();
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
        } else {
            greetingMsg.setText(null);
        }
    }
}
