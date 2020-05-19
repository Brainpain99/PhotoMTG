package com.brainpain99.photomtg;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button buttonImage;
    private TextView resultTextView;
    public static final String LOGTAG="PhotoMTG_MainActivity";

    private static final int REQUEST_ID_IMAGE_CAPTURE = 10;
    private static final int REQUEST_CODE_PICK_ACCOUNT = 11;
    private static final int REQUEST_ACCOUNT_AUTHORIZATION = 12;
    private static final int REQUESTCODE_PERMISSIONS = 13;

    private static String accessToken;
    Account googleAccount;
    PhotoHandler photohandler = new PhotoHandler();
    Uri pictureUri;
    String filePath;

    //Button and ResultView initialization
    //Permission-check
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.buttonImage = this.findViewById(R.id.button);
        resultTextView = findViewById(R.id.result);
        resultTextView.setMovementMethod(new ScrollingMovementMethod());

        this.buttonImage.setOnClickListener(v -> {
            captureImage();
        });

        //Asking for permissions and set permissions
        String[] permissions = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.GET_ACCOUNTS};
        if( (ContextCompat.checkSelfPermission(this, permissions.toString())) != PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(this, permissions, REQUESTCODE_PERMISSIONS);
        }
    }

    //When no camera-,write- and account-permission is set the app can't work.
    //So the button to perform any action is disabled when no permssion is set

    //TODO Implement a fully permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == REQUESTCODE_PERMISSIONS){
                if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    buttonImage.setEnabled(true);
                    getAuthToken();
                } else {
                    buttonImage.setEnabled(false);
                    Toast.makeText(MainActivity.this, "Keine Genehmigung erteilt!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    //Get the result of the intent and preform different operation
    //When the picture of taken and everything went fine: Call the CloudVision for text detection
    //When no account was picked for authentication then create one and get a new authentication token
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ID_IMAGE_CAPTURE && resultCode == RESULT_OK && pictureUri != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            callCloudVision(bitmap);
        } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AccountManager am = AccountManager.get(this);
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                for (Account account : accounts) {
                    if (account.name.equals(email)) {
                        googleAccount = account;
                        break;
                    }
                }
                getAuthToken();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Kein Account ausgewählt.", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == REQUEST_ACCOUNT_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                Bundle extra = data.getExtras();
                onTokenReceived(extra.getString("authtoken"));
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Authorisierung fehlgeschlagen.", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    //Create an file an start the intent for camera
    //Save the picture to created file
    private void captureImage() {

        File pictureFile = photohandler.createFile();

        // Create an implicit intent, for image capture.
        pictureUri = FileProvider.getUriForFile(getApplicationContext(),  BuildConfig.APPLICATION_ID + ".fileprovider",pictureFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
        filePath = pictureFile.getAbsolutePath();

        // Start camera and wait for the results.
        this.startActivityForResult(intent, REQUEST_ID_IMAGE_CAPTURE);

    }

    //Set global static Activity-variable to token
    public void onTokenReceived(String token) {
        accessToken = token;
    }

    //If no user is set for authentication
    private void pickUserAccount() {
        Intent intent = AccountPicker.newChooseAccountIntent(new AccountPicker.AccountChooserOptions.Builder()
                .setAllowableAccountsTypes(Arrays.asList("com.google"))
                .build());
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    //Get the authentication token from Google Cloud API
    private void getAuthToken() {
        String SCOPE = "oauth2:https://www.googleapis.com/auth/cloud-platform";
        if (googleAccount == null) {
            pickUserAccount();
        } else {
            new GoogleHandler(MainActivity.this, googleAccount, SCOPE, REQUEST_ACCOUNT_AUTHORIZATION)
                    .execute();
        }
    }

    //Convert response from Google Cloud Vision to output-String for display
    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("Results:\n\n");
        message.append("Texts:\n");
        List<EntityAnnotation> texts = response.getResponses().get(0)
                .getTextAnnotations();
        if (texts != null) {
            for (EntityAnnotation text : texts) {
                message.append(String.format(Locale.getDefault(), "%s", text.getDescription()));
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }
        return message.toString();
    }

    //Send picture to CloudVision and perform the text detection
    //Display Result on TextView
    private void callCloudVision(final Bitmap bitmap) {
        resultTextView.setText(R.string.Ergebnis_Text_2);
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    //TODO Replace GoogleCredential with not deprecated method
                    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
                    //TODO Replace HttpTransport with not deprecated method
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    Vision.Builder builder = new Vision.Builder
                            (httpTransport, jsonFactory, credential);
                    builder.setApplicationName("PhotoMTG");
                    Vision vision = builder.build();
                    List<Feature> featureList = new ArrayList<>();
                    Feature textDetection = new Feature();
                    textDetection.setType("TEXT_DETECTION");
                    featureList.add(textDetection);
                    List<AnnotateImageRequest> imageList = new ArrayList<>();
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                    Image base64EncodedImage = photohandler.getBase64EncodedJpeg(bitmap);
                    annotateImageRequest.setImage(base64EncodedImage);
                    annotateImageRequest.setFeatures(featureList);
                    imageList.add(annotateImageRequest);
                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(imageList);
                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(LOGTAG, "Sende Anfrage:");
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);
                } catch (GoogleJsonResponseException e) {
                    Log.e(LOGTAG, "Anfrage fehlgeschlagen: " + e.getContent());
                } catch (IOException e) {
                    Log.d(LOGTAG, "Anfrage fehlgeschlagen: " + e.getMessage());
                }
                return "Cloud Vision API Anfrage fehlgeschlagen.";
            }
            protected void onPostExecute(String result) {
                resultTextView.setText(result);
            }
        }.execute();
    }

}
