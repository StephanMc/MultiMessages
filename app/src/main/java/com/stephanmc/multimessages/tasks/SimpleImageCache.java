package com.stephanmc.multimessages.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.stephanmc.multimessages.model.PhoneContact;

import java.util.ArrayList;
import java.util.HashMap;

public class SimpleImageCache {

    private static final HashMap<String, Bitmap> sImageCache = new HashMap<>();

    public synchronized static void clearCache() {
        sImageCache.clear();
    }

    public synchronized static void fillCache(Context context, ArrayList<PhoneContact> contacts) {

        if (context == null) {
            return;
        }

        for (PhoneContact contact : contacts) {
            String imageUri = contact.getPhotoURI();
            if (imageUri == null) {
                continue;
            }

            Bitmap bitmap = getBitmap(context, imageUri);
            sImageCache.put(contact.getId(), bitmap);
        }

    }

    private static Bitmap getBitmap(Context context, String imageUri) {
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(imageUri));
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap getContactBitmap(String id) {
        return sImageCache.get(id);
    }
}
