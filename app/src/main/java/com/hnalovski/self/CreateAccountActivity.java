package com.hnalovski.self;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hnalovski.self.util.JournalApi;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {
    private Button loginButton;
    private Button createAccButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    //Firestore connection
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference collectionReference = db.collection("Users");
    private EditText emailEditText;
    private EditText passwordEditText;
    private ProgressBar progressBar;
    private EditText userNameEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();

        createAccButton = findViewById(R.id.create_acct_button_login);
        progressBar = findViewById(R.id.login_progress);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        userNameEditText = findViewById(R.id.username_account);

        authStateListener = firebaseAuth -> {
            currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                //user is already logged in

            } else {
                //user is not logged in
                
            }

        };


        createAccButton.setOnClickListener(view -> {
            if (!TextUtils.isEmpty(emailEditText.getText().toString())
                && !TextUtils.isEmpty(passwordEditText.getText().toString())
                && !TextUtils.isEmpty(userNameEditText.getText().toString())) {

                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String username = userNameEditText.getText().toString();

                createUserEmailAccount(email, password, username);

            } else {
                Toast.makeText(CreateAccountActivity.this, "Empty fields not allowed", Toast.LENGTH_LONG).show();
            }

        });
    }

    private void createUserEmailAccount(String email, String password, String username) {
        if (!TextUtils.isEmpty(emailEditText.getText())
                && !TextUtils.isEmpty(passwordEditText.getText())
                && !TextUtils.isEmpty(userNameEditText.getText())) {

            progressBar.setVisibility(View.VISIBLE);

            firebaseAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            //take user to AddJournalActivity
                            currentUser = firebaseAuth.getCurrentUser();
                            String currentUserId = firebaseAuth.getUid();

                            //create a map to creae a user in user collection
                            Map<String, String> userObj = new HashMap<>();
                            userObj.put("userId", currentUserId);
                            userObj.put("username", username);

                            //save to firestore database
                            collectionReference.add(userObj).addOnSuccessListener(documentReference -> {
                                documentReference.get()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.getResult().exists()){
                                                progressBar.setVisibility(View.INVISIBLE);
                                                String name = task1.getResult().getString("username");

                                                JournalApi journalApi= JournalApi.getInstance(); // global api
                                                journalApi.setUserId(currentUserId);
                                                journalApi.setUsername(name);

                                                Intent intent = new Intent(CreateAccountActivity.this, PostJournalActivity.class);
                                                intent.putExtra("username", name);
                                                intent.putExtra("userId", currentUserId);
                                                startActivity(intent);

                                            }else {

                                                progressBar.setVisibility(View.INVISIBLE);
                                            }
                                        });

                            }).addOnFailureListener(e -> {


                            });



                        } else {
                            //something went wrong
                        }

                    })
                    .addOnFailureListener(e -> {

                    });

        }else {

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}