package bhople.nikhil.youtubechannelsubscribe;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTubeScopes;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static bhople.nikhil.youtubechannelsubscribe.R.id.playerView;

public class YouTubeSubscribeActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener,EasyPermissions.PermissionCallbacks,YouTubeActivityView {
    // if you are using YouTubePlayerView in xml then activity must extend YouTubeBaseActivity

    private static final int RECOVERY_DIALOG_REQUEST = 1;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final int RC_SIGN_IN = 12311;

    private static final String PREF_ACCOUNT_NAME = "accountName";;
    private String youtubeKey = "AIzaSyA-F-SoBTgtZr-em965Po5Wzw_Upxrf4U8";// paste your youtube key here

    private GoogleAccountCredential mCredential;
    private ProgressDialog pDialog;
    private YouTubeActivityPresenter presenter;
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_you_tube_subscribe);

        // initialize presenter
        presenter = new YouTubeActivityPresenter(this,this);

        final String emailId = getIntent().getExtras().getString(GoogleSignInActivity.USER_EMAIL);

        YouTubePlayerSupportFragment supportFragment = (YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(R.id.playerView);
        supportFragment.initialize(youtubeKey,this); // paste your youtube key

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(YouTubeScopes.YOUTUBE))
                .setBackOff(new ExponentialBackOff());


        findViewById(R.id.B_subscribe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              /*              FIRST GOTO FOLLOWING LINK AND ENABLE THE YOUTUBE API ACCESS
                https://console.developers.google.com/apis/api/youtube.googleapis.com/overview?project=YOUR_PROJECT_ID**/

                SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_ACCOUNT_NAME, emailId);
                editor.apply();

                getResultsFromApi();
            }
        });

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {

        if (!wasRestored) {
            // paste youtube video id here
            youTubePlayer.cueVideo("cHEahGHseGc"); //Use cueVideo()  method, if you don't want to play it automatically
          //  youTubePlayer.loadVideo("cHEahGHseGc"); //loadVideo() will auto play video
            // Hiding seek player controls
           youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
        }

    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        if (youTubeInitializationResult.isUserRecoverableError())
        {
            youTubeInitializationResult.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        }
        else
        {
            String errorMessage = String.format("Error: ", youTubeInitializationResult.toString());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(playerView);
    }


    private void getResultsFromApi() {

        if (! isGooglePlayServicesAvailable())
        {
            acquireGooglePlayServices();
        }
        else if (mCredential.getSelectedAccountName() == null)
        {
            chooseAccount();
        }
        else
        {
            pDialog = new ProgressDialog(YouTubeSubscribeActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.show();
            // handing subscribe task by presenter
            presenter.subscribeToYouTubeChannel(mCredential,"UC_x5XG1OV2P6uZZ5FSM9Ttw"); // pass youtube channelId as second parameter
        }

    }

    // checking google play service is available on phone or not
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }


    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (apiAvailability.isUserResolvableError(connectionStatusCode))
        {
            Dialog dialog = apiAvailability.getErrorDialog(
                    YouTubeSubscribeActivity.this,  // showing dialog to user for getting google play service
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
        }
    }


    private void chooseAccount() {

        if (EasyPermissions.hasPermissions(this, android.Manifest.permission.GET_ACCOUNTS))
        {
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null)
            {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            }
            else
            {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        }
        else
            {
                // Request the GET_ACCOUNTS permission via a user dialog
                 EasyPermissions.requestPermissions(
                        this,
                        "This app needs to access your Google account for YouTube channel subscription.",
                        REQUEST_PERMISSION_GET_ACCOUNTS, android.Manifest.permission.GET_ACCOUNTS);
            }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {

            case RECOVERY_DIALOG_REQUEST: // on YouTube player initialization error
                getYouTubePlayerProvider().initialize(youtubeKey, this);
                break;


            case REQUEST_GOOGLE_PLAY_SERVICES: // if user don't have google play service
                if (resultCode != RESULT_OK)
                {
                    Toast.makeText(this, "This app requires Google Play Services. Please " +
                            "install Google Play Services on your device and relaunch this app.", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    getResultsFromApi();
                }
                break;


            case REQUEST_ACCOUNT_PICKER: // when user select google account
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
                {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null)
                    {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;


            case REQUEST_AUTHORIZATION: // when your grant account access permission
                if (resultCode == RESULT_OK)
                {
                    getResultsFromApi();
                }
                break;


            case RC_SIGN_IN: // if user do sign in
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess())
                {
                    getResultsFromApi();
                }
                else
                {
                    Toast.makeText(this, "Permission Required if granted then check internet connection", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        getResultsFromApi(); // user have granted permission so continue
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(this, "This app needs to access your Google account for YouTube channel subscription.", Toast.LENGTH_SHORT).show();

    }




    @Override  // responce from presenter on success
    public void onSubscribetionSuccess(String title) {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        Toast.makeText(YouTubeSubscribeActivity.this, "Successfully subscribe to "+title, Toast.LENGTH_SHORT).show();
    }

    @Override // responce from presenter on failure
    public void onSubscribetionFail() {

        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        // user don't have youtube channel subscribe permission so grant it form him
        // as we have not taken at the time of sign in
        if(counter < 3)
        {
            counter++; // attempt three times on failure
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(new Scope("https://www.googleapis.com/auth/youtube")) // require this scope for youtube channel subscribe
                    .build();

            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN); 
        }
        else
        {
            Toast.makeText(this, "goto following link and enable the youtube api access\n" +
                    "https://console.developers.google.com/apis/api/youtube.googleapis.com/overview?project=YOUR_PROJECT_ID",
                    Toast.LENGTH_LONG).show();
        }
      

    }
}
