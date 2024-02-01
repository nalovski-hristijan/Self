package com.hnalovski.self;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hnalovski.self.model.Journal;
import com.hnalovski.self.util.JournalApi;

import java.util.ArrayList;
import java.util.Date;

public class PostJournalActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int GALLERY_CODE = 1;
    private static final int RESULT_SPEECH = 2;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView addPhotoButton;
    private EditText titleEditText;
    private EditText thoughtsEditText;
    private TextView currentUserTextView;
    private ImageView imageView;
    private ImageButton micTitleButton;
    private ImageButton micThoughtsButton;
    private String currentUserId;
    private String currentUserName;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;

    //Connection to Firestore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;
    private CollectionReference collectionReference = db.collection("Journal");
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_journal);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.post_progressBar);
        titleEditText = findViewById(R.id.post_title_et);
        thoughtsEditText = findViewById(R.id.post_description_et);
        currentUserTextView = findViewById(R.id.post_username_textview);
        imageView = findViewById(R.id.post_imageView);
        micTitleButton = findViewById(R.id.micTitle);
        micThoughtsButton = findViewById(R.id.micThoughts);

        saveButton = findViewById(R.id.post_save_journal_button);
        saveButton.setOnClickListener(this);
        addPhotoButton = findViewById(R.id.postCameraButton);
        addPhotoButton.setOnClickListener(this);

        progressBar.setVisibility(View.INVISIBLE);

        if (JournalApi.getInstance() != null) {
            currentUserId = JournalApi.getInstance().getUserId();
            currentUserName = JournalApi.getInstance().getUsername();

            currentUserTextView.setText(currentUserName);

        }

        micTitleButton.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            try {
                micTitleLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(), "Your device doesn't support Speech to Text", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        micThoughtsButton.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            try {
                micThoughtsLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(), "Your device doesn't support Speech to Text", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });


        authStateListener = firebaseAuth ->
                user = firebaseAuth.getCurrentUser();


    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.post_save_journal_button) {
            //save journal
            saveJournal();

        } else if (view.getId() == R.id.postCameraButton) {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            resultLauncher.launch(galleryIntent);
        }
    }

    private void saveJournal() {
        String title = titleEditText.getText().toString();
        String thoughts = thoughtsEditText.getText().toString();

        progressBar.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(thoughts) && imageUri != null) {

            StorageReference filepath = storageReference
                    .child("journal_images")
                    .child("my_image_" + Timestamp.now().getSeconds());

            filepath.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->


                            filepath.getDownloadUrl().addOnSuccessListener(uri -> {

                        String imageUrl = uri.toString();
                        //Journal object
                        Journal journal = new Journal();
                        journal.setTitle(title);
                        journal.setThought(thoughts);
                        journal.setImageUrl(imageUrl);
                        journal.setTimeAdded(new Timestamp(new Date()));
                        journal.setUserName(currentUserName);
                        journal.setUserId(currentUserId);

                        //collection reference
                        collectionReference.add(journal).addOnSuccessListener(documentReference -> {

                            progressBar.setVisibility(View.INVISIBLE);
                            startActivity(new Intent(PostJournalActivity.this, JournalListActivity.class));
                            finish();
                        }).addOnFailureListener(e -> {

                        });

                    })).addOnFailureListener(e ->
                            progressBar.setVisibility(View.INVISIBLE));
        } else {

            progressBar.setVisibility(View.INVISIBLE);
        }
    }


    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), n -> {
        imageUri = n.getData().getData();
        imageView.setImageURI(imageUri);
    });

    ActivityResultLauncher<Intent> micTitleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        assert o.getData() != null;
        ArrayList<String> text = o.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        titleEditText.setText(text.get(0));
    });

    ActivityResultLauncher<Intent> micThoughtsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        ArrayList<String> text = o.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        thoughtsEditText.setText(text.get(0));
    });






    @Override
    protected void onStart() {
        super.onStart();
        user = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}