package com.devlomi.fireapp.activities.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.AddedMeActivity;
import com.devlomi.fireapp.activities.FriendsActivity;
import com.devlomi.fireapp.activities.MyProfileActivity;
import com.devlomi.fireapp.activities.ProfilePhotoActivity;
import com.devlomi.fireapp.activities.QuickAddActivity;
import com.devlomi.fireapp.activities.SettingLanguageActivity;
import com.devlomi.fireapp.activities.main.MainActivity;
import com.devlomi.fireapp.adapters.MyFriendRecycleViewAdapter;
import com.devlomi.fireapp.fragments.CallsFragment;
import com.devlomi.fireapp.fragments.FriendsFragment;
import com.devlomi.fireapp.model.PhoneContact;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.ContactUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.RealmResults;

import static android.app.Activity.RESULT_OK;
import static com.devlomi.fireapp.activities.MyProfileActivity.REQUEST_LANG;

/**
 * Created by Devlomi on 25/03/2018.
 */

public class ProfilePreferenceFragment extends PreferenceFragment {
    public static final int PICK_IMAGE_REQUEST = 4951;

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

    private RecyclerView friend_recycler;
    private List<UserInfo> friendList;
    private MyFriendRecycleViewAdapter adapter;

    //private RealmResults<User> userList;

    private List<UserInfo> userList;
    private List<UserInfo> remainList;
    private List<UserInfo> addedList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        init();
    }

    private void init() {
        friendList = new ArrayList<>();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pickImages() {
        CropImageRequest.getCropImageRequest().start(getActivity(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fagment_profile_settings, container, false);

        imageViewUserProfile = view.findViewById(R.id.image_view_user_profile);
        changeUserProfile = view.findViewById(R.id.image_button_change_user_profile);
        tvUsername = view.findViewById(R.id.tv_username);
        tvSurname = view.findViewById(R.id.tv_surname);
        editUsername = view.findViewById(R.id.image_button_edit_username);
        editSurname = view.findViewById(R.id.image_button_edit_surname);
        editEmail = view.findViewById(R.id.edit_email);
        editGender = view.findViewById(R.id.edit_gender);
        editStatus = view.findViewById(R.id.edit_status);
        editLanguage = view.findViewById(R.id.edit_language);
        editBirth = view.findViewById(R.id.edit_birth);

        tvStatus = view.findViewById(R.id.tv_status);
        tvEmail = view.findViewById(R.id.tv_email);
        tvGender = view.findViewById(R.id.tv_gender);
        tvPhoneNumber = view.findViewById(R.id.tv_phone_number);
        tvLanguage = view.findViewById(R.id.tv_Language);
        tvBirth = view.findViewById(R.id.tv_birth);

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
        tvGender.setText(SharedPreferencesManager.getGender());

        tvLanguage.setText(SharedPreferencesManager.getLanguage());

        String photoUri = SharedPreferencesManager.getMyPhoto();

        // init click listener
        initClickGroup(myPhoto);

        Glide.with(getActivity())
                .asBitmap()
                .load(Uri.parse(photoUri))
                .into(imageViewUserProfile);

        Button friendsButton = view.findViewById(R.id.friends_button);
        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FriendsActivity.class);
                startActivity(intent);
            }
        });

        Button addedButton = view.findViewById(R.id.added_button);
        addedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddedMeActivity.class);
                startActivity(intent);
            }
        });
        loadInvitedList();
        if( addedList != null && addedList.size() > 0 )
            addedButton.setText(String.format("Added Me (%d)", addedList.size()));
        else
            addedButton.setEnabled(false);

        Button addButton = view.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), QuickAddActivity.class);
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

        return view;

    }

    private RealmResults<User> getListOfUsers() {
        return RealmHelper.getInstance().getListOfUsers();
    }

    private void initClickGroup(final String photo) {

        imageViewUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ProfilePhotoActivity.class);
                String transName = "profile_photo_trans";

                intent.putExtra(IntentUtils.EXTRA_PROFILE_PATH, photo);
                startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), v, transName).toBundle());
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
                showEditTextDialog(getString(R.string.enter_your_name), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.username_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            FireManager.updateMyUserName(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {
                                        SharedPreferencesManager.saveMyUsername(text);
                                        saveValueToFirebase("name", text);
                                        tvUsername.setText(text);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // surname
        editSurname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_surname), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.surname_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            FireManager.updateMySurname(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {
                                        SharedPreferencesManager.saveSurname(text);
                                        tvSurname.setText(text);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // status
        editStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_status), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.status_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            FireManager.updateMyStatus(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {

                                        SharedPreferencesManager.saveMyStatus(text);
                                        saveValueToFirebase("status", text);
                                        tvStatus.setText(text);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // email
        editEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_email), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.email_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            FireManager.updateMyEmail(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {

                                        SharedPreferencesManager.saveEmail(text);
                                        tvEmail.setText(text);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Gender
        editGender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_gender), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.gender_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SharedPreferencesManager.saveGender(text);
                        tvGender.setText(text);
                    }
                });
            }
        });

        // set birthday
        editBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog(getString(R.string.enter_your_birthdate), new EditTextDialogListener() {
                    @Override
                    public void onOk(final String text) {
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(getActivity(), R.string.birthdate_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (NetworkHelper.isConnected(getActivity())) {
                            FireManager.updateMyBirthday(text, new FireManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    if (isSuccessful) {

                                        SharedPreferencesManager.saveBirthday(text);
                                        tvBirth.setText(text);
                                    } else {
                                        Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

                        } else {
                            Toast.makeText(getActivity(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // set language
        editLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingLanguageActivity.class);
                intent.putExtra("Language", SharedPreferencesManager.getLanguage());
                startActivityForResult(intent, REQUEST_LANG);
            }
        });
    }

    private void showEditTextDialog(String message, final EditTextDialogListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText edittext = new EditText(getActivity());
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
                                Glide.with(getActivity())
                                        .load(file)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .into(imageViewUserProfile);
                                Toast.makeText(getActivity(), R.string.image_changed, Toast.LENGTH_SHORT).show();

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

}

