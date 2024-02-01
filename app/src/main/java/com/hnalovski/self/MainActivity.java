package com.hnalovski.self;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hnalovski.self.util.JournalApi;

public class MainActivity extends AppCompatActivity {
    private Button getStartedButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Users");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        authStateListener = firebaseAuth -> {
            currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                currentUser = firebaseAuth.getCurrentUser();
                String currentUserId = currentUser.getUid();

                collectionReference.whereEqualTo("userId", currentUserId).addSnapshotListener((value, error) -> {


                    assert value != null;
                    if (!value.isEmpty()) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : value) {
                            JournalApi journalApi = JournalApi.getInstance();
                            journalApi.setUserId(queryDocumentSnapshot.getString("userId"));
                            journalApi.setUsername(queryDocumentSnapshot.getString("username"));

                            startActivity(new Intent(MainActivity.this, JournalListActivity.class));
                            finish();
                        }
                    }
                });

            }
        };

        getStartedButton = findViewById(R.id.startButton);

        getStartedButton.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}