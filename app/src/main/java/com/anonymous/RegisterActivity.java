package com.anonymous;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.anonymous.Utils.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText userEmail;
    private EditText userPassword;
    private Button createAccount;
    private TextView alreadyHaveAccountLink;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        initilazeFields();
        alreadyHaveAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToLoginActivity();
            }
        });

        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });
    }

    private void createNewAccount() {
        final Animation shake = AnimationUtils.loadAnimation(RegisterActivity.this, R.anim.shake);

        String email = userEmail.getText().toString();
        String password = userPassword.getText().toString();

        if(TextUtils.isEmpty(email)||!Util.isEmailValid(email)) {
            Toast.makeText(this, "Please enter email...", Toast.LENGTH_SHORT);
            findViewById(R.id.register_email).startAnimation(shake);

            return;
        }
        if(TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password...", Toast.LENGTH_SHORT);
            findViewById(R.id.register_password).startAnimation(shake);

            return;
        }
        progressDialog.setTitle("Creating New Account");
        progressDialog.setMessage("Please wait, while we are creating new account for your...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
             .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            String currentUserId = mAuth.getCurrentUser().getUid();
                            rootRef.child("Users").child(currentUserId).setValue("null");
                            sendUserToMainActivity();
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Account created successfully...", Toast.LENGTH_LONG);

                        } else {
                            progressDialog.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(RegisterActivity.this, "Error: "+message, Toast.LENGTH_LONG);
                            findViewById(R.id.register_password).startAnimation(shake);
                        }
                    }
             }
        );

    }

    private void initilazeFields() {
        createAccount = (Button)findViewById(R.id.register_button);
        userEmail = (EditText)findViewById(R.id.register_email);
        userPassword = (EditText)findViewById(R.id.register_password);
        alreadyHaveAccountLink = (TextView) findViewById(R.id.already_have_account_link);
        progressDialog = new ProgressDialog(this);
    }

    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }

    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }



}
