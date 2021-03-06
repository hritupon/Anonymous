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

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private Button loginButton;
    private Button phoneLoginButton;
    private EditText userEmail;
    private EditText userPassword;
    private TextView needNewAccountLink;
    private TextView forgotPasswordLink;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        initilazeFields();

        needNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToRegisterActivity();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allowUserToLogin();
            }
        });

        phoneLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent phoneLoginIntent = new Intent(LoginActivity.this, PhoneLoginActivity.class);
                startActivity(phoneLoginIntent);
            }
        });
    }

    private void allowUserToLogin() {
        final Animation shake = AnimationUtils.loadAnimation(LoginActivity.this,
                                                             R.anim.shake);

        String email = userEmail.getText().toString();
        String password = userPassword.getText().toString();

        if(TextUtils.isEmpty(email)||!Util.isEmailValid(email)) {
            Toast.makeText(this, "Please enter email...", Toast.LENGTH_SHORT).show();
            userEmail.startAnimation(shake);

            return;
        }
        if(TextUtils.isEmpty(password)) {
            Toast.makeText(this,
                      "Please enter password...",
                           Toast.LENGTH_SHORT).show();
            userPassword.startAnimation(shake);

            return;
        }
        progressDialog.setTitle("Sing In");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            sendUserToMainActivity();
                            Toast.makeText(LoginActivity.this,
                                      "Login Successful...",
                                           Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(LoginActivity.this,
                                      "Login Failed."+message,
                                           Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            userPassword.startAnimation(shake);
                        }
                    }
                });
    }

    private void initilazeFields() {
        loginButton = (Button)findViewById(R.id.login_button);
        phoneLoginButton = (Button)findViewById(R.id.phone_login_button);
        userEmail = (EditText)findViewById(R.id.login_email);
        userPassword = (EditText)findViewById(R.id.login_password);
        needNewAccountLink = (TextView) findViewById(R.id.need_new_account_link);
        forgotPasswordLink = (TextView)findViewById(R.id.forget_password_link);
        progressDialog = new ProgressDialog(this);

    }


    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void sendUserToRegisterActivity() {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
    }
}
