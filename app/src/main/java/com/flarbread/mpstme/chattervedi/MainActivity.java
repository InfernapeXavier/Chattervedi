package com.flarbread.mpstme.chattervedi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import butterknife.BindView;
import butterknife.ButterKnife;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.JsonElement;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.ContentValues.TAG;
import static android.provider.AlarmClock.*;

public class MainActivity extends Activity implements AIListener {

    private CoordinatorLayout coordinatorLayout;
    private Button signOut;
    @BindView(R.id.progressListen) ProgressBar progressBar;
    TextView resultTextView;
    private AIService aiService;
    Map<String, Object> data = new HashMap<>();
    Map<String, Object> finalData = new HashMap<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
    private CollectionReference collection = FirebaseFirestore.getInstance().collection(firebaseUser.getUid());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        resultTextView = findViewById(R.id.resultTextView);
        coordinatorLayout = findViewById(R.id.COORDINATORLAYOUT);
        signOut = findViewById(R.id.signOutButton);
        progressBar.setVisibility(View.INVISIBLE);

        final AIConfiguration config = new AIConfiguration("9b4bab6f917e4ced89e884da41ee20e0",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        resultTextView.setText("Enter your Query!");

        signOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                firebaseAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

    }

    public void listenButtonOnClick(final View view){
        aiService.startListening();
    }

    @SuppressLint("SetTextI18n")
    public void onResult(final AIResponse response) {
        Result result = response.getResult();

        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        resultTextView.setText("Query:" + result.getResolvedQuery() +
                "\nAction: " + result.getAction() +
                "\nParameters: " + parameterString +"\nResponse: " + response.toString().trim());

        //Push results
        data.put("Query", result.getResolvedQuery());
        data.put("Action", result.getAction());
        data.put("Query", parameterString);
        if (Objects.equals(result.getAction(), "web.search"))
        {
            Uri uri = Uri.parse("http://www.google.com/#q=" + result.getStringParameter("query"));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
        else if (Objects.equals(result.getAction(), "alarm.set"))
        {
//            boolean ispm=true;
//            if(Objects.equals(result.getStringParameter("am"), "am"))
//                ispm=false;
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, 5);
            i.putExtra(AlarmClock.EXTRA_MINUTES, 30);
//          i.putExtra(AlarmClock.EXTRA_HOUR, result.getIntParameter("hours",0));
//          i.putExtra(AlarmClock.EXTRA_MINUTES, result.getIntParameter("mins",0))
//          i.putExtra(AlarmClock.EXTRA_IS_PM, ispm);
            startActivity(i);
        }
        else if (Objects.equals(result.getAction(), "location.search"))
        {
            Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query="+result.getStringParameter("location"));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
        else if (Objects.equals(result.getAction(),"weather.search"))
        {
            Uri uri = Uri.parse("https://www.google.com/search?q="+result.getStringParameter("place"));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
        //data.put("Time", Calendar.getInstance().getTime());
       // data.put("Timezone", Calendar.getInstance().getTimeZone());
        finalData.put("Data", data);
        db.collection(firebaseUser.getUid())
                .add(finalData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

    }


    @Override
    public void onError(final AIError error) {
        resultTextView.setText(error.toString());
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {
        showSnackbar("Listening: Speak your Query");
        progressBar.setVisibility(View.VISIBLE);
    }


    @Override
    public void onListeningCanceled() {
        progressBar.setVisibility(View.INVISIBLE);
        showSnackbar("Cancelled");
    }

    @Override
    public void onListeningFinished() {
        progressBar.setVisibility(View.INVISIBLE);
        showSnackbar("Finished");
    }

    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG);
        snackbar.show();
    }


}

