package com.devlomi.fireapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.main.MainActivity;
import com.devlomi.fireapp.activities.setup.SetupUserActivity;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.realms.PhoneNumber;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.PermissionsUtil;
import com.devlomi.fireapp.utils.ServiceHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.devlomi.fireapp.utils.Util;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 954;
    private static final int PERMISSION_REQUEST_CODE = 159;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        //request permissions if API >=23
        //if the device is under API23 the the permissions will granted automatically
        requestPermissions();

    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PermissionsUtil.permissions, PERMISSION_REQUEST_CODE);
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.missing_permissions)
                .setMessage(R.string.you_have_to_grant_permissions)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions();
                    }
                }).setNegativeButton(R.string.no_close_the_app, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.show();
    }

    //login using phone number
    //please note that the Emulator does NOT support Phone Authentication
    private void login() {


        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build())).build(),
                RC_SIGN_IN);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                startTheActivity();
                return;
            } else {
                // Sign in failed
                if (response == null) {

                    // User pressed back button
                    Util.showSnackbar(LoginActivity.this, getString(R.string.sign_in_cancelled));

                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {

                    Util.showSnackbar(LoginActivity.this, getString(R.string.no_internet_connection));

                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Util.showSnackbar(LoginActivity.this, getString(R.string.unknown_error));
                    return;
                }
            }

            Util.showSnackbar(LoginActivity.this, getString(R.string.unknown_sign_in_response));
        }
    }


    //start the next activity
    //if the user info (name , photo) are saved then start the MainActivity
    //otherwise launch SetupUserActivity to make user enter his info
    private void startTheActivity() {

        FireManager.addFriendsToRealm(this);

        if (SharedPreferencesManager.isUserInfoSaved()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {

            if( FireManager.getUid() != null ) {

                FireManager.getUserInfoByUid(FireManager.getUid(), new FireManager.userInfoListener() {
                    @Override
                    public void onFound(UserInfo userInfo) {
                        // save data to sharedpreference.
                        saveUserInfo(userInfo);

                        Intent intent = new Intent(LoginActivity.this, InformationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onNotFound() {

                    }
                });

            } else {
                Intent intent = new Intent(this, MyProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //check if user granted permissions
        //then check if he is already logged in start nextActivity
        // if he is not logged in then launch login activity
        //if he does not grant the permissions then show alert
        // dialog to make him grant the permissions9
        if (PermissionsUtil.permissionsGranted(grantResults)) {
            if (!FireManager.isLoggedIn())
                login();
            else
                startTheActivity();
        } else {
            showAlertDialog();
        }
    }

    private void saveUserInfo(UserInfo userInfo) {
        SharedPreferencesManager.saveMyPhoto(userInfo.getPhoto());
        SharedPreferencesManager.saveMyUsername(userInfo.getName());
        SharedPreferencesManager.saveSurname(userInfo.getSurname());
        SharedPreferencesManager.saveEmail(userInfo.getEmail());
        SharedPreferencesManager.saveBirthday(userInfo.getBirthDate());
        SharedPreferencesManager.saveMyStatus(userInfo.getStatus());
        SharedPreferencesManager.savePhoneNumber(userInfo.getPhone());
        SharedPreferencesManager.saveMyThumbImg(userInfo.getThumbImg());

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(this);
        Phonenumber.PhoneNumber numberProto;
        try {
            numberProto = phoneUtil.parse(FireManager.getPhoneNumber(), "");
            String countryCode = phoneUtil.getRegionCodeForNumber(numberProto);
            SharedPreferencesManager.saveCountryCode(countryCode);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }

        ServiceHelper.fetchUserGroupsAndBroadcasts(this);
    }

}
