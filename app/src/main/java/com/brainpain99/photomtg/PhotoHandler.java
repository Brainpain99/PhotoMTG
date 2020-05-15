package com.brainpain99.photomtg;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class PhotoHandler {

    private Bitmap picture;

    PhotoHandler(Bitmap pic){
        setPicture(pic);
    }

    public Bitmap getPicture() {
        return picture;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    //Speichert das Bild an einem festen Pfad im internen Speicher des Handys
    // Gibt den den Pfad der Datei als String zurück
    public String saveImage() throws IOException {

        //Vorgegebenen Ordner erstellen falls er noch nicht existiert
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

        //Datei schreiben
        OutputStream fOut = new FileOutputStream(file);
        picture.compress(Bitmap.CompressFormat.PNG,100,fOut);
        fOut.flush();
        fOut.close();

        //Speicherpfad zurückgeben
        return file.getAbsolutePath();

    }
}
