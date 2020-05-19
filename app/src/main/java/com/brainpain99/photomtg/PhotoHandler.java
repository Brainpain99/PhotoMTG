package com.brainpain99.photomtg;

import android.graphics.Bitmap;
import android.os.Environment;

import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class PhotoHandler {

    //Creates a file at an specific Position
    public File createFile(){

        //Create file if it doesn't exist.
        File picFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "/PhotoMTG");
        if (!picFolder.exists()) {
            boolean isCreated = picFolder.mkdirs();
        }

        //Save the picture in file and give it a set name. (MTGCard_<Number>.png)
        int imgNum = 0;
        String fileName = "MTGCard_"+imgNum+".png";
        File file = new File(picFolder,fileName);

        //If the picture number exist count the Number up.
        while (file.exists()){
            imgNum++;
            fileName = "MTGCard_"+imgNum+".png";
            file = new File(picFolder,fileName);
        }
        return file;
    }

    //Convert Image for text recognition
    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);
        return image;
    }

}
