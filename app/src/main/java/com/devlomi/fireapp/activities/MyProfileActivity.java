package com.devlomi.fireapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.main.MainActivity;
import com.devlomi.fireapp.activities.settings.ProfilePreferenceFragment;
import com.devlomi.fireapp.adapters.MyFriendRecycleViewAdapter;
import com.devlomi.fireapp.fragments.FriendsFragment;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.AppVerUtil;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.CropImageRequest;
import com.devlomi.fireapp.utils.DirManager;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.IntentUtils;
import com.devlomi.fireapp.utils.NetworkHelper;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.RealmResults;

public class MyProfileActivity extends AppCompatActivity {

    public static final int PICK_IMAGE_REQUEST = 4951;
    public static final int REQUEST_LANG = 4000;

    private CircleImageView imageViewUserProfile;

    private ImageButton changeUserProfile;
    private ImageButton editUsername;
    private ImageButton editSurname;
    private ImageButton editEmail;
    private ImageButton editGender;
    private ImageButton editStatus;
    private ImageButton editLanguage;
    private ImageButton editBirth;

    private TextView tvUsername;
    private TextView tvSurname;
    private TextView tvEmail;
    private TextView tvGender;
    private TextView tvStatus;
    private TextView tvPhoneNumber;
    private TextView tvLanguage;
    private TextView tvBirth;

    private List<UserInfo> userList;
    private List<UserInfo> remainList;
    private List<UserInfo> addedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageViewUserProfile = findViewById(R.id.image_view_user_profile);
        changeUserProfile = findViewById(R.id.image_button_change_user_profile);
        tvUsername = findViewById(R.id.tv_username);
        tvSurname = findViewById(R.id.tv_surname);
        editUsername = findViewById(R.id.image_button_edit_username);
        editSurname = findViewById(R.id.image_button_edit_surname);
        editEmail = findViewById(R.id.edit_email);
        editGender = findViewById(R.id.edit_gender);
        editStatus = findViewById(R.id.edit_status);
        editLanguage = findViewById(R.id.edit_language);
        editBirth = findViewById(R.id.edit_birth);

        tvStatus = findViewById(R.id.tv_status);
        tvEmail = findViewById(R.id.tv_email);
        tvGender = findViewById(R.id.tv_gender);
        tvPhoneNumber = findViewById(R.id.tv_phone_number);
        tvLanguage = findViewById(R.id.tv_Language);
        tvBirth = findViewById(R.id.tv_birth);

        String userName = SharedPreferencesManager.getUserName();
        String status = SharedPreferencesManager.getStatus();
        String phoneNumber = SharedPreferencesManager.getPhoneNumber();
        final String myPhoto = SharedPreferencesManager.getMyPhoto();

        tvStatus.setText(status);
        tvUsername.setText(userName);
        tvPhoneNumber.setText(phoneNumber);
        //
        tvSurname.setText(SharedPreferencesManager.getSurname());
        tvEmail.setText(SharedPreferencesManager.getEmail());

        if( SharedPreferencesManager.getGender().equals("") ) {
            SharedPreferencesManager.saveGender("Male");
            tvGender.setText("Male");
        } else {
            tvGender.setText(SharedPreferencesManager.getGender());
        }

        tvBirth.setText(SharedPreferencesManager.getBirthday());
        tvLanguage.setText(SharedPreferencesManager.getLanguage());

        String photoUri = SharedPreferencesManager.getMyPhoto();

        // init click listener
        initClickGroup(myPhoto);

        Glide.with(this)
                .asBitmap()
                .load(photoUri)
                .into(imageViewUserProfile);

        Button friendsButton = findViewById(R.id.friends_button);
        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyProfileActivity.this, FriendsActivity.class);
                startActivity(intent);
            }
        });

        Button addedButton = findViewById(R.id.added_button);
        addedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyProfileActivity.this, AddedMeActivity.class);
                startActivity(intent);
            }
        });
        loadInvitedList();
        if( addedList != null && addedList.size() > 0 )
            addedButton.setText(String.format("Added Me (%d)", addedList.size()));
        else
            addedButton.setEnabled(false);

        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyProfileActivity.this, QuickAddActivity.class);
                startActivity(intent);
            }
        });
        loadRemainList();
        if( remainList != null && remainList.size() > 0 )
            addButton.setText(String.format("Quick Add (%d)", remainList.size()));
        else
            addButton.setEnabled(false);

//        userList = getListOfUsers();
        loadFriendListJsonString();
        if( userList == null )
            friendsButton.setEnabled(false);
        else
            friendsButton.setText(String.format("Friends (%d)", userList.size()));

    }

    private boolean loadInvitedList() {
        String jsonstring = SharedPreferencesManager.getAddedMeListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return false;

        Gson gson = new Gson();
        TypeToken<List<UserInfo>> token = new TypeToken<List<UserInfo>>() {};
        addedList = gson.fromJson(jsonstring, token.getType());
        return true;
    }

    private boolean loadRemainList() {
        String jsonstring = SharedPreferencesManager.getRemainListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return false;

        Gson gson = new Gson();
        TypeToken<List<UserInfo>> token = new TypeToken<List<UserInfo>>() {};
        remainList = gson.fromJson(jsonstring, token.getType());
        return true;
    }

    private boolean loadFriendListJsonString() {
        String jsonstring = SharedPreferencesManager.getFriendsListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return false;

        Gson gson = new Gson();
        TypeToken<List<UserInfo>> token = new TypeToken<List<UserInfo>>() {};
        userList = gson.fromJson(jsonstring, token.getType());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (AppVerUtil.isLogin) {
                Intent intent = new Intent(MyProfileActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                onBackPressed();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void pickImages() {
        CropImageRequest.getCropImageRequest().start(this);
    }

    private RealmResults<User> getListOfUsers() {
        return RealmHelper.getInstance().getListOfUsers();
    }

    private void initClickGroup(final String photo) {

        imageViewUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyProfileActivity.this, ProfilePhotoActivity.class);
                String transName = "profile_photo_trans";

                intent.putExtra(IntentUtils.EXTRA_PROFILE_PATH, photo);
                startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(MyProfileActivity.this, v, transName).toBundle());
            }
        });

        // change picture image
        changeUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImages();
            }
        });

        // full name
        editUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_name), new MyProfileActivity.EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(MyProfileActivity.this, R.string.username_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(MyProfileActivity.this)) {
                            FireManager.updateMyUserName(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {
                                        SharedPreferencesManager.saveMyUsername(text);
                                        saveValueToFirebase("name", text);
                                        tvUsername.setText(text);
                                    } else {
                                        Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // surname
        editSurname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_surname), new MyProfileActivity.EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(MyProfileActivity.this, R.string.surname_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(MyProfileActivity.this)) {
                            FireManager.updateMySurname(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {
                                        SharedPreferencesManager.saveSurname(text);
                                        tvSurname.setText(text);
                                    } else {
                                        Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // status
        editStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_status), new MyProfileActivity.EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(MyProfileActivity.this, R.string.status_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(MyProfileActivity.this)) {
                            FireManager.updateMyStatus(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {

                                        SharedPreferencesManager.saveMyStatus(text);
                                        saveValueToFirebase("status", text);
                                        tvStatus.setText(text);
                                    } else {
                                        Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // email
        editEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_email), new MyProfileActivity.EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(MyProfileActivity.this, R.string.email_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(MyProfileActivity.this)) {
                            FireManager.updateMyEmail(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {

                                        SharedPreferencesManager.saveEmail(text);
                                        tvEmail.setText(text);
                                    } else {
                                        Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Gender
        editGender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] listItems = {"Male", "Female"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MyProfileActivity.this);
                builder.setTitle("Choose gender");

                int checkedItem = 0; //this will checked the item when user open the dialog
                builder.setSingleChoiceItems(listItems, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvGender.setText(listItems[which]);
                    }
                });

                builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = tvGender.getText().toString();

                        if (NetworkHelper.isConnected(MyProfileActivity.this)) {
                            FireManager.updateMyGender(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    SharedPreferencesManager.saveGender(text);
                                }
                            });
                        }

                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        // set birthday
        editBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = new DatePickerDialog(
                        MyProfileActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                String birthText = new String(dayOfMonth + "/" + (month + 1) + "/" + year);

                                if (NetworkHelper.isConnected(MyProfileActivity.this)) {
                                    FireManager.updateMyBirthday(birthText, new FireManager.OnComplete() {
                                        @Override
                                        public void onComplete(boolean isSuccessful) {
                                            if (isSuccessful) {

                                                SharedPreferencesManager.saveBirthday(birthText);
                                                tvBirth.setText(birthText);
                                            } else {
                                                Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                            }
                                        }
                                    });

                                } else {
                                    Toast.makeText(MyProfileActivity.this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                                }

                            }
                        },
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show();
            }
        });

        // set language
        editLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyProfileActivity.this, SettingLanguageActivity.class);
                if( SharedPreferencesManager.getLanguage().equals("") )
                    intent.putExtra("Language", "English");
                else
                    intent.putExtra("Language", SharedPreferencesManager.getLanguage());
                startActivityForResult(intent, REQUEST_LANG);
            }
        });
    }

    private void showDatePickDialog() {

    }

    private void showEditTextDialog(String message, final MyProfileActivity.EditTextDialogListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(MyProfileActivity.this);
        final EditText edittext = new EditText(this);
        alert.setMessage(message);


        alert.setView(edittext);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {


                if (listener != null)
                    listener.onOk(edittext.getText().toString());


            }
        });

        alert.setNegativeButton(R.string.cancel, null);

        alert.show();


    }

    private void saveValueToFirebase(String childKey, String value) {
        FireConstants.usersRef.child(FireManager.getUid()).child(childKey).setValue(value);
    }

    private interface EditTextDialogListener {
        void onOk(String text);
    }

    public interface ImageSelectedListener {
        void onSelect(String imagePath);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();


                final File file = DirManager.getMyPhotoPath();
                BitmapUtils.compressImage(resultUri.getPath(), file, 30);


                FireManager.updateMyPhoto(file.getPath(), new FireManager.OnComplete() {
                    @Override
                    public void onComplete(boolean isSuccessful) {
                        if (isSuccessful) {
                            try {
                                Glide.with(MyProfileActivity.this)
                                        .load(file)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .into(imageViewUserProfile);
                                Toast.makeText(MyProfileActivity.this, R.string.image_changed, Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }


//

                        }
                    }
                });


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        } else if( requestCode == REQUEST_LANG ) {
            String lang = SharedPreferencesManager.getLanguage();

            tvLanguage.setText(lang);

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("language", lang);
            FireConstants.languageRef.child(FireManager.getUid()).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    int kkk = 0;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    int kkk = 0;
                }
            });
        }
    }

}
