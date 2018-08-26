package com.zaurkandokhov.signature;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        requestAccessToken("alice", "password", "ro.client", "secret", "http://192.168.0.120:5000");
    }

    private void requestAccessToken(String username, String password, String clientId, String clientSecret, String site) {
//        OAuth2Client.Builder builder = new OAuth2Client.Builder(clientId, clientSecret, site)
//                .grantType("refresh_token")
//                .scope("api1")
//                .username(username)
//                .password(password);

//        OkHttpClient httpClient = new OkHttpClient.Builder()
//                .connectTimeout(10, TimeUnit.SECONDS)
//                .readTimeout(10, TimeUnit.SECONDS)
//                .build();
//
//        Map<String, String> map = new HashMap<>();
//        map.put("grant_type", "client_credentials");
//        map.put("scope", "api1");
//
//        OAuth2Client client = new OAuth2Client.Builder(username, password, clientId, clientSecret, site)
//        //OAuth2Client client = new OAuth2Client.Builder(clientId, clientSecret, site)
//                .grantType("password")
//                .scope("api1")
//                .okHttpClient(httpClient)
//                //.parameters(map)
//                .build();
//
//        client.requestAccessToken(new OAuthResponseCallback() {
//            @Override
//            public void onResponse(OAuthResponse response) {
//                if (response.isSuccessful()) {
//                    String accessToken = response.getAccessToken();
//                    String refreshToken = response.getRefreshToken();
//                } else {
//                    OAuthError error = response.getOAuthError();
//                    String errorMsg = error.getError();
//                }
//
//                response.getCode();   // HTTP Status code
//            }
//        });
    }

//    private void refreshToken(String clientId, String clientSecret, String site, String token) {
//        OAuth2Client client = new OAuth2Client.Builder(clientId, clientSecret, site).build();
//        client.refreshAccessToken(token, new OAuthResponseCallback() {
//            @Override
//            public void onResponse(OAuthResponse response) {
//                if (response.isSuccessful()) {
//                    String accessToken = response.getAccessToken();
//                } else {
//                    OAuthError oAuthError = response.getOAuthError();
//                }
//            }
//        });
//    }
}
