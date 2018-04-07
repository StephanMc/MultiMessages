package com.stephanmc.multimessages.tasks;

import static com.stephanmc.multimessages.util.Util.SMS_LIMIT;
import static com.stephanmc.multimessages.util.Constants.INTENT_EXTRA_CONTACT_NAME;
import static com.stephanmc.multimessages.util.Constants.INTENT_EXTRA_SMS_DELIVERED;
import static com.stephanmc.multimessages.util.Constants.INTENT_EXTRA_SMS_SENT;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.EditText;
import android.widget.Toast;

import com.stephanmc.multimessages.R;
import com.stephanmc.multimessages.util.Util;
import com.stephanmc.multimessages.interfaces.ActivityInterface;
import com.stephanmc.multimessages.model.PhoneContact;
import com.stephanmc.multimessages.ui.MultiMessageApplication;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SMSSenderTask extends AsyncTask<Object, Object, Object> {

    private final ActivityInterface mActivityInterface;
    private final List<PhoneContact> mSelectedContacts;
    private final WeakReference<EditText> mEditText;


    public SMSSenderTask(ActivityInterface activityInterface, EditText editText, List<PhoneContact> selectedContacts) {
        mEditText = new WeakReference<>(editText);
        mActivityInterface = activityInterface;
        mSelectedContacts = selectedContacts;
    }

    @Override
    protected Object doInBackground(Object... objects) {
        Looper.prepare();
        for (PhoneContact contact : mSelectedContacts) {
            sendSMSToContact(contact);
        }

        return null;
    }

    @Override
    protected void onPreExecute() {
        mActivityInterface.hideKeyboard();
        Toast.makeText(getContext(), R.string.toast_sending_messages, Toast.LENGTH_SHORT).show();
    }

    private Context getContext() {
        return MultiMessageApplication.getInstance();
    }

    private void sendSMSToContact(PhoneContact contact) {
        EditText editText;
        if (mEditText.get() == null) {
            return;
        }
        editText = mEditText.get();

        String phone = contact.getContactNumber();

        //message = message.replace()
        ImageSpan[] spans = editText.getText().getSpans(0, editText.length(), ImageSpan.class);
        Editable editableText = editText.getEditableText();
        String contactName = Util.getFirstName(contact.getContactName());

        SpannableString spannableString = new SpannableString(editableText);

        for (ImageSpan span : spans) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannableString);
            spannableStringBuilder.replace(spannableString.getSpanStart(span), spannableString.getSpanEnd(span),
                    contactName);

            spannableString = SpannableString.valueOf(spannableStringBuilder);
        }
        String finalMessage = spannableString.toString();

        //Check if the phoneNumber is empty
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(getContext(), R.string.enter_valid_phone_number, Toast.LENGTH_SHORT).show();

        } else if (TextUtils.isEmpty(finalMessage)) {
            Toast.makeText(getContext(), R.string.cannot_send_empty_sms, Toast.LENGTH_SHORT).show();

        } else {
            SmsManager smsManager = SmsManager.getDefault();
            int length = finalMessage.length();

            Intent smsSentIntent = new Intent(INTENT_EXTRA_SMS_SENT);
            smsSentIntent.putExtra(INTENT_EXTRA_CONTACT_NAME, contactName);

            Intent smsDeliveredIntent = new Intent(INTENT_EXTRA_SMS_DELIVERED);
            smsDeliveredIntent.putExtra(INTENT_EXTRA_CONTACT_NAME, contactName);

            PendingIntent sentIntent = PendingIntent.getBroadcast(getContext(), 0, smsSentIntent, 0);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(getContext(), 0, smsDeliveredIntent, 0);

            if (length > SMS_LIMIT) {
                ArrayList<String> messagelist = smsManager.divideMessage(finalMessage);
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveryIntents = new ArrayList<>();

                for (int i = 0; i < messagelist.size(); i++) {
                    sentIntents.add(sentIntent);
                    deliveryIntents.add(deliveredIntent);
                }

                smsManager.sendMultipartTextMessage(phone, null, messagelist, sentIntents, deliveryIntents);

            } else {
                smsManager.sendTextMessage(phone, null, finalMessage, sentIntent, deliveredIntent);
            }
        }
    }
}
