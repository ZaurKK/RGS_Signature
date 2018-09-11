package com.zaurkandokhov.signature;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    private static final String LOGIN_HINT = "login_hint";

    public static final String LOG_TAG = "AppAuthSample";

//    MainApplication mMainApplication;

    // state
    AuthState mAuthState;

    // views
    AppCompatButton mAuthorize;
    AppCompatButton mMakeApiCall;
    AppCompatButton mSignOut;
//    AppCompatTextView mGivenName;
//    AppCompatTextView mFamilyName;
//    AppCompatTextView mFullName;
//    ImageView mProfileView;

    // login hint
    protected String mLoginHint;

    // broadcast receiver for app restrictions changed broadcast
    BroadcastReceiver mRestrictionsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
//        mMainApplication = (MainApplication) getApplication();
        mAuthorize = findViewById(R.id.authorize);
        mMakeApiCall = findViewById(R.id.makeApiCall);
        mSignOut = findViewById(R.id.signOut);
//        mGivenName = (AppCompatTextView) findViewById(R.id.givenName);
//        mFamilyName = (AppCompatTextView) findViewById(R.id.familyName);
//        mFullName = (AppCompatTextView) findViewById(R.id.fullName);
//        mProfileView = (ImageView) findViewById(R.id.profileImage);

        enablePostAuthorizationFlows();

        // wire click listeners
        mAuthorize.setOnClickListener(new AuthorizeListener(this));

        // Retrieve app restrictions and take appropriate action
        getAppRestrictions();
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Retrieve app restrictions and take appropriate action
        getAppRestrictions();

        // Register a receiver for app restrictions changed broadcast
        registerRestrictionsReceiver();
    }

    @Override
    protected void onStop(){
        super.onStop();

        // Unregister receiver for app restrictions changed broadcast
        unregisterReceiver(mRestrictionsReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());

        // Register a receiver for app restrictions changed broadcast
        //registerRestrictionsReceiver();
    }

    private void enablePostAuthorizationFlows() {
        mAuthState = restoreAuthState();
        if (mAuthState != null && mAuthState.isAuthorized()) {
            if (mMakeApiCall.getVisibility() == View.GONE) {
                mMakeApiCall.setVisibility(View.VISIBLE);
                mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(this)));
            }
            if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                mSignOut.setOnClickListener(new SignOutListener(this));
            }
        } else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
        }
    }

    /**
     * Exchanges the code, for the {@link TokenResponse}.
     *
     * @param intent represents the {@link Intent} from the Custom Tabs or the System Browser.
     */
    private void handleAuthorizationResponse(@NonNull Intent intent) {
        // code from the step 'Handle the Authorization Response' goes here.
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        if (response != null) {
            Log.i(LOG_TAG, String.format("Handled Authorization Response %s ", authState.jsonSerializeString()));
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w(LOG_TAG, "Token Exchange failed", exception);
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, exception);
                            persistAuthState(authState);
                            Log.i(LOG_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                        }
                    }
                }
            });
        }
    }

    private void persistAuthState(@NonNull AuthState authState) {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE, authState.jsonSerializeString())
                .apply();
        enablePostAuthorizationFlows();
    }

    private void clearAuthState() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .apply();
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }

        return null;
    }

    /**
     * Kicks off the authorization flow.
     */
    public static class AuthorizeListener implements Button.OnClickListener {

        private final SettingsActivity mSettingsActivity;

        AuthorizeListener(@NonNull SettingsActivity settingsActivity) {
            mSettingsActivity = settingsActivity;
        }

        @Override
        public void onClick(View view) {
            // code from the step 'Create the Authorization Request',
            // and the step 'Perform the Authorization Request' goes here.
            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/v2/auth") /* auth endpoint */,
                    Uri.parse("https://www.googleapis.com/oauth2/v4/token") /* token endpoint */
            );
            AuthorizationService authorizationService = new AuthorizationService(view.getContext());
            String clientId = "mvc";
            Uri redirectUri = Uri.parse("http://localhost:5002/signin-oidc");
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    AuthorizationRequest.CODE_CHALLENGE_METHOD_S256,
                    redirectUri
            );
            builder.setScopes("profile");

            if (mSettingsActivity.getLoginHint() != null) {
                HashMap<String, String> loginHintMap = new HashMap<>();
                loginHintMap.put(LOGIN_HINT, mSettingsActivity.getLoginHint());
                builder.setAdditionalParameters(loginHintMap);

                Log.i(LOG_TAG, String.format("login_hint: %s", mSettingsActivity.getLoginHint()));
            }

            AuthorizationRequest request = builder.build();
            String action = "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(view.getContext(), request.hashCode(), postAuthorizationIntent, 0);
            authorizationService.performAuthorizationRequest(request, pendingIntent);
        }
    }

    public static class MakeApiCallListener implements Button.OnClickListener {

        private final SettingsActivity mSettingsActivity;
        private AuthState mAuthState;
        private AuthorizationService mAuthorizationService;

        MakeApiCallListener(@NonNull SettingsActivity settingsActivity, @NonNull AuthState authState, @NonNull AuthorizationService authorizationService) {
            mSettingsActivity = settingsActivity;
            mAuthState = authState;
            mAuthorizationService = authorizationService;
        }

        @Override
        public void onClick(View view) {
            // code from the section 'Making API Calls' goes here
            mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                @Override public void execute(
                        String accessToken,
                        String idToken,
                        AuthorizationException ex) {
                    if (ex != null) {
                        // negotiation for fresh tokens failed, check ex for more details
                        return;
                    }

                    // use the access token to do something ...
                    Log.i(LOG_TAG, String.format("TODO: make an API call with [Access Token: %s, ID Token: %s]", accessToken, idToken));
                }
            });
        }
    }

    public static class SignOutListener implements Button.OnClickListener {

        private final SettingsActivity mSettingsActivity;

        SignOutListener(@NonNull SettingsActivity settingsActivity) {
            mSettingsActivity = settingsActivity;
        }

        @Override
        public void onClick(View view) {
            mSettingsActivity.mAuthState = null;
            mSettingsActivity.clearAuthState();
            mSettingsActivity.enablePostAuthorizationFlows();
        }
    }

    private void getAppRestrictions(){
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) this
                        .getSystemService(Context.RESTRICTIONS_SERVICE);

        if (restrictionsManager != null) {
            Bundle appRestrictions = restrictionsManager.getApplicationRestrictions();

            // Block user if KEY_RESTRICTIONS_PENDING is true, and save login hint if available
            if (appRestrictions != null && !appRestrictions.isEmpty()) {
                if (!appRestrictions.getBoolean(UserManager.KEY_RESTRICTIONS_PENDING)) {
                    mLoginHint = appRestrictions.getString(LOGIN_HINT);
                } else {
                    Toast.makeText(this, R.string.restrictions_pending_block_user,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void registerRestrictionsReceiver(){
        IntentFilter restrictionsFilter =
                new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);

        mRestrictionsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getAppRestrictions();
            }
        };

        registerReceiver(mRestrictionsReceiver, restrictionsFilter);
    }

    public String getLoginHint(){
        return mLoginHint;
    }
}
