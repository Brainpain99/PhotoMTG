package com.brainpain99.photomtg;

import android.accounts.Account;
import android.app.Activity;
import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

public class GoogleHandler extends AsyncTask<Void, Void, Void> {
    private Activity mActivity;
    private String mScope;
    private Account mAccount;
    private int mRequestCode;

    GoogleHandler(Activity activity, Account account, String scope, int requestcode){
        this.mActivity = activity;
        this.mAccount =  account;
        this.mRequestCode = requestcode;
        this.mScope = scope;
    }

    //Get token from fetchToken() and return it to the MainActivity
    @Override
    protected Void doInBackground(Void... params){
        try{
            String token = fetchToken();
            if (token != null){
                ((MainActivity)mActivity).onTokenReceived(token);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    //Get the authentication token from Google API for the account that is logged in
    protected String fetchToken() throws IOException {
        String accessToken;
        try {
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope);
            // used to remove stale tokens.
            GoogleAuthUtil.clearToken (mActivity, accessToken);
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope);
            return accessToken;
        } catch (UserRecoverableAuthException userRecoverableException) {
            mActivity.startActivityForResult(userRecoverableException.getIntent(), mRequestCode);
        } catch (GoogleAuthException fatalException) {
            fatalException.printStackTrace();
        }
        return null;
    }


}
