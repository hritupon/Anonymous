package com.anonymous;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = mAuth.getCurrentUser().getUid();
        initializeFields();

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        retrieveUserInfo();
    }

    private void initializeFields() {
        updateAccountSettings = (Button)findViewById(R.id.update_settings_button);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_profile_status);
        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
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

        HashMap<String, String> profileMap = new HashMap<>();
        profileMap.put("uid", currentUserId);
        profileMap.put("name",setUserName);
        profileMap.put("status", setUserStatus);

        rootRef.child("Users").child(currentUserId).setValue(profileMap)
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


                } else if (dataSnapshot.exists() && dataSnapshot.hasChild("name")){
                    String retrievedUserName = dataSnapshot.child("name").getValue().toString();
                    String retrievedStatus = dataSnapshot.child("status").getValue().toString();

                    userName.setText(retrievedUserName);
                    userStatus.setText(retrievedStatus);

                } else {
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
