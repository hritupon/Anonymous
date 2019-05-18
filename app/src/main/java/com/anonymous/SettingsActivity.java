package com.anonymous;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName;
    private EditText userStatus;
    private CircleImageView userProfileImage;
    private String currentUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private StorageReference userProfileImagesRef;
    private ProgressDialog loadingBar;

    private static final int GalleryPick=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        userProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        currentUserId = mAuth.getCurrentUser().getUid();
        initializeFields();

        userName.setVisibility(View.INVISIBLE);

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        retrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GalleryPick);
            }
        });
    }

    private void initializeFields() {
        updateAccountSettings = (Button)findViewById(R.id.update_settings_button);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GalleryPick && resultCode == RESULT_OK && data!=null){
            Uri imageUri = data.getData();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, we are updating your profile image.");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri = result.getUri();
                final StorageReference  filePath = userProfileImagesRef.child(currentUserId + ".jpg");


                filePath.putFile(resultUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            loadingBar.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this,
                                    "Error: "+message,
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Continue with the task to get the download URL
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this,
                                    "Profile Image uploaded successfully.",
                                    Toast.LENGTH_SHORT).show();

                            String downloadUrl = task.getResult().toString();
                            rootRef.child("Users").child(currentUserId).child("image")
                                    .setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        loadingBar.dismiss();
                                        Toast.makeText(SettingsActivity.this,
                                                "Profile Image updated successfully.",
                                                Toast.LENGTH_SHORT).show();
                                    } else{
                                        loadingBar.dismiss();
                                        String messge = task.getException().toString();
                                        Toast.makeText(SettingsActivity.this,
                                                "Error: "+messge,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            loadingBar.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this,
                                    "Error: "+message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }
    }

    private void UpdateSettings() {
        String setUserName = userName.getText().toString();
        String setUserStatus = userStatus.getText().toString();

        if(TextUtils.isEmpty(setUserName)) {
            Toast.makeText(this,
                      "Please enter your user name first...",
                           Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(setUserStatus)) {
            Toast.makeText(this,
                    "Please enter your status...",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("uid", currentUserId);
        profileMap.put("name",setUserName);
        profileMap.put("status", setUserStatus);
        rootRef.child("Users").child(currentUserId).updateChildren(profileMap)
         .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(SettingsActivity.this,
                              "Profile Updated Successfully.",
                                   Toast.LENGTH_SHORT).show();
                    sendUserToMainActivity();

                } else {
                    String message = task.getException().toString();
                    Toast.makeText(SettingsActivity.this,
                            "Error: "+message,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void retrieveUserInfo() {
        rootRef.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("image")) {
                    String retrievedUserName = dataSnapshot.child("name").getValue().toString();
                    String retrievedStatus = dataSnapshot.child("status").getValue().toString();
                    String retrievedProfileImage = dataSnapshot.child("image").getValue().toString();

                    userName.setText(retrievedUserName);
                    userStatus.setText(retrievedStatus);
                    Picasso.get().load(retrievedProfileImage).into(userProfileImage);



                } else if (dataSnapshot.exists() && dataSnapshot.hasChild("name")){
                    String retrievedUserName = dataSnapshot.child("name").getValue().toString();
                    String retrievedStatus = dataSnapshot.child("status").getValue().toString();

                    userName.setText(retrievedUserName);
                    userStatus.setText(retrievedStatus);

                } else {
                    userName.setVisibility(View.VISIBLE);
                    Toast.makeText(SettingsActivity.this,
                            "Please set your profile information.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
