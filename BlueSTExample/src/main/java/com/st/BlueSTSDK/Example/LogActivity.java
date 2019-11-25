package com.st.BlueSTSDK.Example;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureAcceleration;
import com.st.BlueSTSDK.Log.FeatureLogCSVFile;
import com.st.BlueSTSDK.Manager;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.Utils.LogFeatureActivity;

import java.util.Collections;
import java.util.List;


public class LogActivity extends LogFeatureActivity {

    /**
     * tag used for store the node id that permit us to find the node selected by the user
     */
    private final static String NODE_TAG = LogActivity.class.getCanonicalName() + "" +
            ".NODE_TAG";
    /**
     * tag used for retrieve the NodeContainerFragment
     */
    private final static String NODE_FRAGMENT = LogActivity.class.getCanonicalName() + "" +
            ".NODE_FRAGMENT";
    /**
     * node that will stream the data
     */
    public Node myNode;
    private List<FeatureAcceleration> mAccel;
    /**
     * fragment that manage the node connection and avoid a re connection each time the activity
     * is recreated
     */
    private NodeContainerFragment mNodeContainer;
    /**
     * listener that will update the displayed feature data
     */
    private Feature.FeatureListener mAccUpdate;

    //create an Intent for this activity
    public static Intent getStartIntent(Context c, @NonNull Node node) {
        Intent i = new Intent(c, LogActivity.class);
        i.putExtra(NODE_TAG, node.getTag());
        i.putExtras(NodeContainerFragment.prepareArguments(node));
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        String nodeTag = getIntent().getStringExtra(NODE_TAG);
        myNode = Manager.getSharedInstance().getNodeWithTag(nodeTag);

        //create or recover the NodeContainerFragment
        if (savedInstanceState == null) {
            Intent i = getIntent();
            mNodeContainer = new NodeContainerFragment();
            mNodeContainer.setArguments(i.getExtras());

            getFragmentManager().beginTransaction()
                    .add(mNodeContainer, NODE_FRAGMENT).commit();

        } else {
            mNodeContainer = (NodeContainerFragment) getFragmentManager()
                    .findFragmentByTag(NODE_FRAGMENT);

        }//if-else

        mAccel = myNode.getFeatures(FeatureAcceleration.class);
        TextView textView = findViewById(R.id.accText);
        mAccUpdate = new AccelUpdate(textView);
        for (Feature f : mAccel){
            f.addFeatureListener(mAccUpdate);
            myNode.enableNotification(f);
        }

        //requestSignin();
    }

    @Override
    protected List<Node> getNodesToLog() {
        return Collections.singletonList(myNode);
    }

    @Override
    protected Feature.FeatureLoggerListener getLogger() {
        String dumpPath = getLogDirectory();
        return new FeatureLogCSVFile(dumpPath, getNodesToLog());
        //return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.startLog) {
            startLogging();
            return true;
        }
        if (id == R.id.stopLog) {
            stopLogging();
            return true;
        }
            return super.onOptionsItemSelected(item);
    }


    private class AccelUpdate implements Feature.FeatureListener{
        final private TextView textView;
        public AccelUpdate(TextView text) {
            this.textView = text;
        }
        @Override
        public void onUpdate(@NonNull Feature f, @NonNull Feature.Sample sample) {
            final String featureDump = f.toString();
            LogActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(featureDump);
                }
            });
        }
    }


    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

/*DriveServiceHelper driveServiceHelper;

    private void requestSignin() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        startActivityForResult(client.getSignInIntent(), 400);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case 400:
                if (resultCode == RESULT_OK)
                {
                    handleSignInIntent(data);
                }
                break;
        }
    }*/

    /*private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        GoogleAccountCredential credential = GoogleAccountCredential
                                .usingOAuth2(LogActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));
                        credential.setSelectedAccount(googleSignInAccount.getAccount());

                        Drive googleDriveService = new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("Drive Upload")
                                .build();

                        driveServiceHelper = new DriveServiceHelper(googleDriveService);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    public void uploadFile(View view) {
        ProgressDialog progressDialog = new ProgressDialog(LogActivity.this);
        progressDialog.setTitle("Uploading to Google Drive");
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
        //TODO: append the name of the file to the filePath
        String filePath = "/storage/emulated/0/STMicroelectronics/logs/" + "20191015_121352_Gyroscope.csv";
        driveServiceHelper.createFileCSV(filePath).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                progressDialog.dismiss();
                Toast.makeText(LogActivity.this, "Uploaded Successully", Toast.LENGTH_SHORT).show();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(LogActivity.this, "Check your Google API", Toast.LENGTH_SHORT).show();
                    }
                });
    }*/

}

