package com.devlomi.fireapp.activities.setup

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.main.MainActivity
import com.devlomi.fireapp.common.ScopedActivity
import com.devlomi.fireapp.common.extensions.toDeffered
import com.devlomi.fireapp.common.extensions.toDefferedWithTask
import com.devlomi.fireapp.events.FetchingUserGroupsAndBroadcastsFinished
import com.devlomi.fireapp.exceptions.BackupFileMismatchedException
import com.devlomi.fireapp.utils.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import io.realm.exceptions.RealmMigrationNeededException
import kotlinx.android.synthetic.main.activity_setup_user.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class SetupUserActivity : ScopedActivity() {


    internal var storedPhotoUrl: String? = null
    internal var choosenPhoto: String? = null
    private var thumbImg: String? = null
    private var progressDialog: ProgressDialog? = null
    private var datePicker: DatePickerDialog.OnDateSetListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_user)

        // set values
        et_username_setup!!.setText(SharedPreferencesManager.getUserName())
        et_surname_setup!!.setText(SharedPreferencesManager.getSurname())
        et_email_setup!!.setText(SharedPreferencesManager.getEmail())
        et_birthdate_setup!!.setText(SharedPreferencesManager.getBirthday())
        et_gender_setup!!.setText(SharedPreferencesManager.getGender())

        ett_gender!!.setOnClickListener {
            showGenderDialog()
        }

        val cal = Calendar.getInstance()
        datePicker = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val dateFormat = "dd/MM/yyyy"
            val sdf = SimpleDateFormat(dateFormat, Locale.US)
            et_birthdate_setup.setText(sdf.format(cal.time))
        }
        /**
         * Prevent keyboard from showing up when we press the date edittext
         */
        if (Build.VERSION.SDK_INT >= 21) {
            et_birthdate_setup!!.showSoftInputOnFocus = false
        } else if (Build.VERSION.SDK_INT >= 11) {
            et_birthdate_setup!!.setRawInputType(InputType.TYPE_CLASS_TEXT)
            et_birthdate_setup!!.setTextIsSelectable(true)
        } else {
            et_birthdate_setup!!.setRawInputType(InputType.TYPE_NULL)
            et_birthdate_setup!!.isFocusable = true
        }

        ett_birthdate.setOnClickListener {
            DatePickerDialog(this@SetupUserActivity, datePicker,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        fetchUserPhoto()


        fab_setup_user.setOnClickListener { completeSetup() }
        user_img_setup.setOnClickListener { pickImage() }

        /**
         * This is no longer needed because we don't have one field now.
         */
        //On Done Keyboard Button Click
        /* et_username_setup!!.setOnEditorActionListener(TextView.OnEditorActionListener
        { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                completeSetup()
                return@OnEditorActionListener true
            }
            false
        }) */


        if (RealmBackupRestore.isBackupFileExists()) {
            check_text_view_number.visibility = View.VISIBLE
        } else {
            check_text_view_number.visibility = View.GONE
        }
    }

    private fun showGenderDialog() {
        lateinit var dialog: AlertDialog

        // Initialize an array of gender
        val array = arrayOf("Male","Female")

        //
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Choose a gender")

        builder.setSingleChoiceItems(array, 0 ) {dialogInterface, i->

            when(i) {
                0 -> et_gender_setup!!.setText("Male")
                1 -> et_gender_setup!!.setText("Female")
            }

        }

        builder.setPositiveButton("Done") {dialog, which ->
            dialog.dismiss()
        }

        val mDialog = builder.create()
        mDialog.show()
    }


    private fun fetchUserPhoto() {
        launch {
            try {


                val dataSnapshot = FireConstants.usersRef.child(FireManager.getUid())
                        .child("photo").toDeffered().await()

                if (dataSnapshot.value == null) {
                    storedPhotoUrl = ""
                    progress_bar_setup_user_img.visibility = View.GONE
                } else {
                    //otherwise get the stored user image url
                    storedPhotoUrl = dataSnapshot.getValue(String::class.java)


                    //load the image
                    //we are using listener to determine when the image loading is finished
                    //so we can hide the progressBar
                    Glide.with(this@SetupUserActivity).load(storedPhotoUrl)
                            .listener(object : RequestListener<Drawable> {

                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                    progress_bar_setup_user_img.visibility = View.GONE
                                    return false;
                                }
                                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                    progress_bar_setup_user_img.visibility = View.GONE
                                    return false
                                }

                            }).into(user_img_setup)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }


    private fun completeSetup() {
        var hasError = false;
        //check if user not entered his username
        if (TextUtils.isEmpty(et_username_setup!!.text.toString())) {
            et_username_setup!!.error = getString(R.string.username_is_empty)
            hasError = true;
        }

        if(TextUtils.isEmpty(et_surname_setup!!.text.toString())) {
            et_surname_setup!!.error = getString(R.string.surname_is_empty)
            hasError = true;
        }

        if(TextUtils.isEmpty(et_gender_setup!!.text.toString())) {
            et_gender_setup!!.error = getString(R.string.gender_is_empty)
            hasError = true
        }

        if(TextUtils.isEmpty(et_email_setup!!.text.toString())) {
            et_email_setup!!.error = getString(R.string.email_is_empty)
            hasError = true;
        } else if(!isEmailValid(et_email_setup!!.text.toString())) {
            et_email_setup!!.error = getString(R.string.email_is_invalid)
            hasError = true;
        }

        if(TextUtils.isEmpty(et_birthdate_setup!!.text.toString())) {
            et_birthdate_setup!!.error = getString(R.string.birthdate_is_empty)
            hasError = true;
        }

        if(!hasError ) {
            val dialog = ProgressDialog(this@SetupUserActivity)
            dialog.setMessage(getString(R.string.loading))
            dialog.setCancelable(false)
            dialog.show()

            ServiceHelper.startSyncContacts(this)

            if (check_text_view_number.visibility == View.VISIBLE && check_text_view_number.isChecked) {
                try {
                    RealmBackupRestore(this).restore()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.error_restoring_backup, Toast.LENGTH_SHORT).show()
                } catch (e: RealmMigrationNeededException) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.error_restoring_backup, Toast.LENGTH_SHORT).show()
                } catch (e: BackupFileMismatchedException) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.backup_file_mismatched, Toast.LENGTH_SHORT).show()
                }

            }

            try {

                // if there user does not choose a new photo
                if (choosenPhoto == null) {
                    //if stored photo on database not exists
                    //then get the defaultUserProfilePhoto from database and download it
                    if (storedPhotoUrl != null && storedPhotoUrl == "") {
                        getDefaultUserProfilePhoto(dialog)
                        //if datasnapshot is not ready yet or there is a connection issue
                    } else if (storedPhotoUrl == null) {
                        dialog.dismiss()
                        showSnackbar()
                    } else {
                        //otherwise get the stored user image from database
                        //download it and save it and save user info then finish setup
                        getUserImage(dialog)

                    }
                    //user picked an image
                    //upload it  then save user info and finish setup
                } else {
                    uploadUserPhoto(dialog)
                }
            } catch (e: Exception) {

            }

            writeLanguage()
        }

    }

    private fun uploadUserPhoto(dialog: ProgressDialog) {
        launch {
            val task = FireConstants.imageProfileRef.child(UUID.randomUUID().toString() + ".jpg")
                    .putFile(Uri.fromFile(File(choosenPhoto)))
                    .toDefferedWithTask()
                    .await()
            if (task.isSuccessful) {
                val imageUrl = task.result!!.uploadSessionUri.toString()
                val userInfoHashmap = getUserInfoHashmap(et_username_setup!!.text.toString(),et_surname_setup!!.text.toString()
                        , et_email_setup!!.text.toString(), et_birthdate_setup!!.text.toString(), et_gender_setup!!.text.toString(),
                        imageUrl, choosenPhoto)

                val isSuccessful = FireConstants.usersRef.child(FireManager.getUid()!!)
                        .updateChildren(userInfoHashmap)
                        .toDeffered().await()

                dialog.dismiss()
                if (isSuccessful) {
                    saveUserInfo(File(choosenPhoto))
                } else
                    showSnackbar()

                //FireManager.setUidsByPhone()

            } else
                showSnackbar()
        }
    }


    private fun getUserImage(dialog: ProgressDialog) {
        launch {

            val file = DirManager.getMyPhotoPath()
            val isSuccessful = FirebaseStorage.getInstance().getReferenceFromUrl(storedPhotoUrl!!).getFile(file).toDeffered().await()
            if (isSuccessful) {
                val userInfoHashmap = getUserInfoHashmap(et_username_setup!!.text.toString(),et_surname_setup!!.text.toString()
                        , et_email_setup!!.text.toString(), et_birthdate_setup!!.text.toString(), et_gender_setup!!.text.toString(),
                        storedPhotoUrl)

                val isSuccessfull = FireConstants.usersRef.child(FireManager.getUid()!!)
                        .updateChildren(userInfoHashmap)
                        .toDeffered()
                        .await()

                dialog.dismiss()
                if (isSuccessfull) {
                    saveUserInfo(file)
                } else
                    showSnackbar()

            } else
                showSnackbar()
        }
    }


    private fun getDefaultUserProfilePhoto(dialog: ProgressDialog) {
        launch {
            try {
                val photoFile = DirManager.getMyPhotoPath()
                val dataSnapshot = FireConstants.mainRef.child("defaultUserProfilePhoto").toDeffered().await()
                if (dataSnapshot.value == null) {
                    dialog.dismiss()
                    showSnackbar()
                } else {
                    val photoUrl = dataSnapshot.getValue(String::class.java)
                    //download image
                    val isSuccessful = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl!!)
                            .getFile(photoFile)
                            .toDeffered()
                            .await()

                    if (isSuccessful) {
                        //save user info and finish setup
                        val userName = et_username_setup!!.text.toString()
                        val map = getUserInfoHashmap(et_username_setup!!.text.toString(),et_surname_setup!!.text.toString()
                                , et_email_setup!!.text.toString(), et_birthdate_setup!!.text.toString(),
                                et_gender_setup!!.text.toString(), photoFile.path)
                        val isSuccess = FireConstants.usersRef.child(FireManager.getUid()!!).updateChildren(map).toDeffered().await()
                        dialog.dismiss()
                        if (isSuccess) {
                            saveUserInfo(photoFile)
                        } else {
                            showSnackbar()
                        }


                    } else
                        showSnackbar()

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun writeLanguage() {
        launch {
            val map = hashMapOf<String, Any>()
            map["language"] = "English"

            val isSuccess = FireConstants.languageRef.child(FireManager.getUid()!!).updateChildren(map).toDeffered().await()
            if (isSuccess) {
                SharedPreferencesManager.saveLanguage("English")
            }
        }
    }

    private fun showSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.no_internet_connection, Snackbar.LENGTH_SHORT).show()
    }

    private fun getUserInfoHashmap(name: String, surname: String, email: String, birthDate: String, gender: String, photoUrl: String?, filePath: String? = null): HashMap<String, Any> {
        val map = hashMapOf<String, Any>()
        map["photo"] = photoUrl!!
        map["name"] = name
        map["surname"] = surname
        map["email"] = email
        map["birthDate"] = birthDate
        map["phone"] = FireManager.getPhoneNumber()
        map["gender"] = gender
        val defaultStatus = String.format(getString(R.string.default_status), getString(R.string.app_name))
        map["status"] = defaultStatus
        val appVersion = AppVerUtil.getAppVersion(this)
        if (appVersion != "")
            map["ver"] = appVersion

        //create thumbImg and original image and compress them if the user chosen a new photo
        if (filePath != null) {
            val circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(filePath))
            thumbImg = BitmapUtils.decodeImageAsPng(circleBitmap)
            map["thumbImg"] = thumbImg!!
        }

        return map
    }


    //save user info locally
    private fun saveUserInfo(photoFile: File) {
        SharedPreferencesManager.saveMyPhoto(photoFile.absolutePath)
        if (thumbImg == null) {
            val circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(photoFile.path))
            thumbImg = BitmapUtils.decodeImageAsPng(circleBitmap)
        }
        SharedPreferencesManager.saveMyThumbImg(thumbImg)
        SharedPreferencesManager.saveMyUsername(et_username_setup!!.text.toString())
        SharedPreferencesManager.savePhoneNumber(FireManager.getPhoneNumber())
        SharedPreferencesManager.saveMyStatus(getString(R.string.default_status))
        SharedPreferencesManager.saveSurname(et_surname_setup!!.text.toString())
        SharedPreferencesManager.saveEmail(et_email_setup!!.text.toString())
        SharedPreferencesManager.saveBirthday(et_birthdate_setup!!.text.toString())
        SharedPreferencesManager.saveGender(et_gender_setup!!.text.toString())
        SharedPreferencesManager.setAppVersionSaved(true)
        saveCountryCode()

        //show progress while getting user groups
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage(resources.getString(R.string.loading))
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()

        ServiceHelper.fetchUserGroupsAndBroadcasts(this)

    }


    @Subscribe
    fun fetchingGroupsAndBroadcastsComplete(event: FetchingUserGroupsAndBroadcastsFinished) {
        SharedPreferencesManager.setUserInfoSaved(true)
        progressDialog!!.dismiss()
        startMainActivity()
    }


    //save country code to shared preferences (see ContactUtils class for more info)
    private fun saveCountryCode() {
        val phoneUtil = PhoneNumberUtil.createInstance(this)
        val numberProto: Phonenumber.PhoneNumber
        try {
            //get the countryName code Like "+1 or +44 etc.." from the user number
            //so if the user number is like +1 444-444-44 we will save only "+1"
            numberProto = phoneUtil.parse(FireManager.getPhoneNumber(), "")
            val countryCode = phoneUtil.getRegionCodeForNumber(numberProto)
            SharedPreferencesManager.saveCountryCode(countryCode)
        } catch (e: NumberParseException) {
            e.printStackTrace()
        }

    }

    private fun pickImage() {
        CropImageRequest.getCropImageRequest().start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri


                val file = DirManager.getMyPhotoPath()
                try {
                    //copy image to the App Folder
                    FileUtils.copyFile(resultUri.path, file)

                    Glide.with(this).load(file).into(user_img_setup!!)
                    choosenPhoto = file.path
                    progress_bar_setup_user_img!!.visibility = View.GONE
                } catch (e: IOException) {
                    e.printStackTrace()
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, R.string.could_not_get_this_image, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    fun isEmailValid(email: String): Boolean {
        val regExpn = ("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$")

        val pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(email)

        return matcher.matches()
    }

    override fun onResume() {
        EventBus.getDefault().register(this)
        super.onResume()
        if (SharedPreferencesManager.isFetchedUserGroups()) {
            if (progressDialog != null)
                progressDialog!!.dismiss()
            SharedPreferencesManager.setUserInfoSaved(true)
            startMainActivity()
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
        if (progressDialog != null) {
            progressDialog!!.dismiss()
        }
    }
}

