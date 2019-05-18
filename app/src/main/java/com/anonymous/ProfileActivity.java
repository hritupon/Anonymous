package com.anonymous;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserId;
    private String currentState;
    private String senderUserId;

    private CircleImageView userProfileImage;
    private TextView userProfileName;
    private TextView userProfileStatus;
    private Button sendMessageRequestButton;
    private Button declineRequestButton;
    private DatabaseReference userRef;
    private DatabaseReference chatRequestRef;
    private DatabaseReference contactsRef;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();
        senderUserId = mAuth.getCurrentUser().getUid();

        userProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        userProfileName = (TextView) findViewById(R.id.visit_user_name);
        userProfileStatus = (TextView) findViewById(R.id.visit_profile_status);
        sendMessageRequestButton = (Button) findViewById(R.id.send_message_request_button);
        declineRequestButton = (Button) findViewById(R.id.decline_message_request_button);
        currentState = "new";

        retrieveUserInfo();
    }

    private void retrieveUserInfo() {
        userRef.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if(dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        String name = dataSnapshot.child("name").getValue().toString();
                        String status = dataSnapshot.child("status").getValue().toString();

                        Picasso.get().load(image).placeholder(R.drawable.profile_image).into(userProfileImage);
                        userProfileName.setText(name);
                        userProfileStatus.setText(status);

                    } else {
                        String name = dataSnapshot.child("name").getValue().toString();
                        String status = dataSnapshot.child("status").getValue().toString();
                        userProfileName.setText(name);
                        userProfileStatus.setText(status);
                    }

                    manageChatRequest();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void manageChatRequest() {
        chatRequestRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiverUserId)){
                    String requestType = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();
                    if(requestType.equals("sent")) {
                        currentState="request_sent";
                        sendMessageRequestButton.setText("Cancel Pseudo Request");
                    } else if(requestType.equals("received")) {
                        currentState = "request_received";
                        sendMessageRequestButton.setText("Accept Pseudo Request");
                        declineRequestButton.setVisibility(View.VISIBLE);
                        declineRequestButton.setEnabled(true);

                        declineRequestButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cancelChatRequest();
                            }
                        });
                    }
                } else {
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(receiverUserId)) {
                                currentState = "friends";
                                sendMessageRequestButton.setText("Remove Pseudo Friend");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        if(!senderUserId.equals(receiverUserId)) {
            sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessageRequestButton.setEnabled(false);
                    if(currentState.equals("new")){
                        sendChatRequest();
                    }
                    if(currentState.equals("request_sent")) {
                        cancelChatRequest();
                    }
                    if(currentState.equals("request_received")) {
                        acceptChatRequest();
                    }
                    if(currentState.equals("friends")) {
                        removeSpecificContact();
                    }
                }
            });
        } else{
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }

    }

    private void sendChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId)
                .child("request_type")
                .setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                            .child("request_type").setValue("received").
                            addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        sendMessageRequestButton.setEnabled(true);
                                        currentState = "request_sent";
                                        sendMessageRequestButton.setText("Cancel Pseudo Request");
                                    }
                                }
                            });
                        }
                    }
                });
    }

    private void cancelChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    chatRequestRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                sendMessageRequestButton.setEnabled(true);
                                currentState = "new";
                                sendMessageRequestButton.setText("Pseudo Request");

                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void acceptChatRequest() {
        contactsRef.child(senderUserId).child(receiverUserId).child("Contacts").setValue("Saved")
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    contactsRef.child(receiverUserId).child(senderUserId).child("Contacts").setValue("Saved")
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                chatRequestRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            chatRequestRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    sendMessageRequestButton.setEnabled(true);
                                                    currentState = "friends";
                                                    sendMessageRequestButton.setText("Remove Pseudo Friend");
                                                    declineRequestButton.setVisibility(View.INVISIBLE);
                                                    declineRequestButton.setEnabled(false);
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

    }

    private void removeSpecificContact() {
        contactsRef.child(senderUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    contactsRef.child(receiverUserId).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                sendMessageRequestButton.setEnabled(true);
                                currentState = "new";
                                sendMessageRequestButton.setText("Pseudo Request");

                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
   }
}
