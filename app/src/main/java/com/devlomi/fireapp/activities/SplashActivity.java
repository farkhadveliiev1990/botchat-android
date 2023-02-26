package com.devlomi.fireapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.main.MainActivity;
import com.devlomi.fireapp.activities.setup.SetupUserActivity;
import com.devlomi.fireapp.model.FriendSystem;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.DetachableClickListener;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.PermissionsUtil;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.ServiceHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

//this is the First Activity that launched when user starts the App
public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 451;
    private static boolean isPersistenceEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Set persistence if not set:
        // isPersistenceEnabled stays after Instant Run, so Instant Run does not call setPersistenceEnabled() again
        // as if it did, the app would crash.
        if (!isPersistenceEnabled) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            isPersistenceEnabled = true;
        }

        if (PermissionsUtil.hasPermissions(this)) {
            //check if user isLoggedIn
            if (!FireManager.isLoggedIn())
                startLoginActivity();
            else
                startNextActivity();
        }
        //request permissions if there are no permissions granted
        else {
            requestPermissions();
        }

    }


    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PermissionsUtil.permissions, PERMISSION_REQUEST_CODE);
    }


    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startNextActivity() {

        FireManager.addFriendsToRealm(this);

        if (SharedPreferencesManager.isUserInfoSaved()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(this, SetupUserActivity.class);
            startActivity(intent);
            finish();
        }

    }


    private void showAlertDialog() {
        DetachableClickListener positiveClickListener = DetachableClickListener.wrap(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestPermissions();

            }
        });

        DetachableClickListener negativeClickListener = DetachableClickListener.wrap(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });


        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle(R.string.missing_permissions)
                .setMessage(R.string.you_have_to_grant_permissions)
                .setPositiveButton(R.string.ok, positiveClickListener)
                .setNegativeButton(R.string.no_close_the_app, negativeClickListener)
                .create();

        //avoid memory leaks
        positiveClickListener.clearOnDetach(builder);
        negativeClickListener.clearOnDetach(builder);
        builder.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionsUtil.permissionsGranted(grantResults)) {
            if (!FireManager.isLoggedIn())
                startLoginActivity();
            else
                startNextActivity();
        } else
            showAlertDialog();
    }

}



