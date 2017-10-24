package com.flarbread.mpstme.chattervedi;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import butterknife.BindView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.JsonElement;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements AIListener {

    private CoordinatorLayout coordinatorLayout;

    @BindView(R.id.listenButton) Button listenButton;
    TextView resultTextView;
    private AIService aiService;
    Map<String, Object> data = new HashMap<>();
    Map<String, Object> finalData = new HashMap<>();

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = (TextView) findViewById(R.id.resultTextView);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.COORDINATORLAYOUT);

        final AIConfiguration config = new AIConfiguration("9b4bab6f917e4ced89e884da41ee20e0",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        resultTextView.setText("Random");

    }

    public void listenButtonOnClick(final View view){
        aiService.startListening();
    }

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
                "\nParameters: " + parameterString);

        //Push results
        data.put("Query", result.getResolvedQuery());
        data.put("Action", result.getAction());
        data.put("Query", parameterString);
        data.put("Time", Calendar.getInstance().getTime());
        data.put("Timezone", Calendar.getInstance().getTimeZone());
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
    }


    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {
        showSnackbar("Finished");

    }

    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG);
        snackbar.show();
    }


}
