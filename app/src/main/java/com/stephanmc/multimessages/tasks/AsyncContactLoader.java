package com.stephanmc.multimessages.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;

import com.stephanmc.multimessages.interfaces.OnContactsLoaded;
import com.stephanmc.multimessages.model.PhoneContact;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Async contact loader task.
 * It basically loads contacts from Cursor, and call any registered listener at the end
 */
public class AsyncContactLoader extends AsyncTask<Object, Integer, ArrayList<PhoneContact>> {

    private Cursor mCursor;
    private OnContactsLoaded mContactsLoadedListener;
    private WeakReference<Context> mContextReference;

    public AsyncContactLoader(Context context, Cursor cursor, OnContactsLoaded contactsLoadedListener) {
        this.mCursor = cursor;
        this.mContactsLoadedListener = contactsLoadedListener;
        this.mContextReference = new WeakReference<>(context);
    }

    private static void buildImageCache(Context context, ArrayList<PhoneContact> contacts) {
        SimpleImageCache.fillCache(context, contacts);
    }

    @Override
    protected ArrayList<PhoneContact> doInBackground(Object... contexts) {
        mCursor.moveToPosition(-1);

        ArrayList<PhoneContact> myContactList = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();

        while (mCursor.moveToNext()) {
            String lookupKey = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
            if (existingKeys.contains(lookupKey)) {
                continue;
            }
            existingKeys.add(lookupKey);

            PhoneContact contact = new PhoneContact();
            contact.setId(lookupKey);
            contact.setContactName(mCursor.getString(mCursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)));
            contact.setContactNumber(mCursor.getString(mCursor.getColumnIndex(Phone.NUMBER)));
            contact.setPhotoURI(mCursor.getString(mCursor.getColumnIndex(Phone.PHOTO_URI)));
            myContactList.add(contact);
        }

        Context context = mContextReference.get();
        if (context != null) {
            buildImageCache(context, myContactList);
        }
        return myContactList;
    }

    @Override
    protected void onPreExecute() {
        SimpleImageCache.clearCache();
    }

    @Override
    protected void onPostExecute(ArrayList<PhoneContact> contacts) {
        if (mContactsLoadedListener != null) {
            mContactsLoadedListener.onLoaded(contacts);
        }

        // prevent leak
        mCursor = null;
        mContactsLoadedListener = null;
        mContextReference = null;
    }
}
