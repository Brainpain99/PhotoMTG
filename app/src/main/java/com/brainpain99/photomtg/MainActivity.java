package com.brainpain99.photomtg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button buttonImage;
    public static final String LOGTAG="PhotoMTG";

    private static final int REQUESTCODE_PERMISSIONS = 665;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;
    private static final int REQUEST_ID_IMAGE_CAPTURE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.buttonImage = this.findViewById(R.id.button);
        this.buttonImage.setOnClickListener(v -> captureImage());

        if((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA}, REQUESTCODE_PERMISSIONS);
            buttonImage.setEnabled(false);
        }
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTCODE_PERMISSIONS);
            buttonImage.setEnabled(false);
        }
    }

    private void captureImage() {
        // Create an implicit intent, for image capture.
        File picFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "/PhotoMTG");
        if (!picFolder.exists()) {
            picFolder.mkdirs();
        }

        //Bild in dem vorgegebenen Ordner ablegen und mit Namen versehen
        int imgNum = 0;
        String fileName = "MTGCard_"+imgNum+".png";
        File file = new File(picFolder,fileName);

        //Wenn die Datei schon existiert wird eine neue Datei erstellt
        while (file.exists()){
            imgNum++;
            fileName = "MTGCard_"+imgNum+".png";
            file = new File(picFolder,fileName);
        }

        Uri pictureUri = FileProvider.getUriForFile(getApplicationContext(),  BuildConfig.APPLICATION_ID + ".fileprovider",file);
        Log.w(LOGTAG,pictureUri.toString());
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
        // Start camera and wait for the results.
        this.startActivityForResult(intent, REQUEST_ID_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode==REQUESTCODE_PERMISSIONS && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
            buttonImage.setEnabled(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ID_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
               /*
               try {
                    Uri capturedImageUri = data.getData();
                    Log.w(LOGTAG,capturedImageUri.toString());
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), capturedImageUri);
                    PhotoHandler handler = new PhotoHandler(bitmap);
                    String SavePath = handler.saveImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Action canceled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Action Failed", Toast.LENGTH_LONG).show();
            }
        }
    }

}
