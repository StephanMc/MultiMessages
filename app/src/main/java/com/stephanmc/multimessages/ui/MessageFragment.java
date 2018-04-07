package com.stephanmc.multimessages.ui;

import static com.stephanmc.multimessages.MainActivity.PERMISSIONS_REQUEST_SEND_SMS;
import static com.stephanmc.multimessages.util.Constants.INTENT_EXTRA_SMS_DELIVERED;
import static com.stephanmc.multimessages.util.Constants.INTENT_EXTRA_SMS_SENT;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.stephanmc.multimessages.R;
import com.stephanmc.multimessages.Util;
import com.stephanmc.multimessages.interfaces.ActivityInterface;
import com.stephanmc.multimessages.model.PhoneContact;
import com.stephanmc.multimessages.tasks.SMSSenderTask;
import com.stephanmc.multimessages.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment used for Message Tab.
 */
public class MessageFragment extends BaseFragment {

    // Text tag for "Insert Firstname" button. This content is accessible
    // when the user gets the raw text in Copy/Paste.
    private static final String TAG_ADD_FIRSTNAME = " ";

    private final MultiMessageApplication mApplication = MultiMessageApplication.getInstance();

    private EditText mEditText;
    private FloatingActionButton mFloatingButton;

    private EmoticonHandler mEmoticonHandler;
    private BroadcastReceiver sentStatusReceiver;
    private BroadcastReceiver deliveredStatusReceiver;

    private ActivityInterface mActivityInterface;

    public MessageFragment() {
    }

    public static Fragment newInstance() {
        return new MessageFragment();
    }

    /**
     * Creates a bitmap drawable from a View. Used to insert 'Firstname' into the EditText
     */
    public static Object convertViewToDrawable(View view) {
        int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(measureSpec, measureSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(canvas);
        view.setDrawingCacheEnabled(true);

        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();

        BitmapDrawable bitmapDrawable = new BitmapDrawable(viewBmp);
        bitmapDrawable.setBounds(0, 0, viewBmp.getWidth(), viewBmp.getHeight());
        return bitmapDrawable;
    }

    /**
     * Returns a string containing the final message that will be sent to the users
     *
     * @param contact            The recipient
     * @param defaultContactName By default, we use it when no contact is selected.
     */
    public String makeMessagePreview(PhoneContact contact, String defaultContactName) {
        ImageSpan[] spans = mEditText.getText().getSpans(0, mEditText.length(), ImageSpan.class);
        Editable editableText = mEditText.getEditableText();
        String contactName = contact == null ? defaultContactName : Util.getFirstName(contact.getContactName());

        SpannableString spannableString = new SpannableString(editableText);

        for (ImageSpan span : spans) {
            SpannableStringBuilder builder = new SpannableStringBuilder(spannableString);
            builder.replace(spannableString.getSpanStart(span), spannableString.getSpanEnd(span), contactName);

            spannableString = SpannableString.valueOf(builder);
        }
        return spannableString.toString();
    }

    private void onSendButtonClicked() {
        if (mActivity == null) {
            return;
        }
        checkPermissionsAndSendSMS();
    }

    private void checkPermissionsAndSendSMS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mActivity.checkSelfPermission(
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            if (!mActivity.shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                showMessageOKCancel(getString(R.string.sms_access_needed), getString(R.string.txt_ok_label),
                        getString(R.string.txt_cancel_label), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    mActivity.requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                                            PERMISSIONS_REQUEST_SEND_SMS);
                                }
                            }
                        });
                return;
            }

            mActivity.requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_SEND_SMS);

        } else {
            sendSms();
        }
    }

    public void setupFloatingActionButton(FloatingActionButton button) {
        if (button == null) { // When the FAB is shared to another Fragment
            if (mFloatingButton != null) {
                mFloatingButton.setOnClickListener(null);
            }
            mFloatingButton = null;

        } else {
            mFloatingButton = button;
            mFloatingButton.setImageResource(R.drawable.ic_send);
            mFloatingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View fabView) {
                    onSendButtonClicked();
                }
            });
        }
    }

    /**
     * Makes some early checks before calling SMS Sender task.
     */
    private void sendSms() {
        final List<PhoneContact> selectedContacts = getSelectedContacts();

        if (selectedContacts.size() == 0) {
            Util.makePopupInfo(getContext(), getString(R.string.popup_label_selected_contacts),
                    getString(R.string.no_selected_recipients));
            return;
        }

        if (TextUtils.isEmpty(mEditText.getText().toString())) {
            Toast.makeText(getContext(), R.string.cannot_send_empty_sms, Toast.LENGTH_SHORT).show();
            return;
        }

        // Prompt 'send to X contacts ?' popup
        String popupMessage = getString(R.string.send_sms_to_n_contacts, selectedContacts.size());
        showMessageOKCancel(popupMessage, getString(R.string.btn_send), getString(R.string.txt_cancel_label),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new SMSSenderTask(mActivityInterface, mEditText, selectedContacts).execute();
            }
        });
    }

    /**
     * Retreive the selected contacts
     */
    private List<PhoneContact> getSelectedContacts() {
        ContactsFragment contactsFragment = mActivityInterface.getContactsFragment();
        return contactsFragment.retreiveSelectedContacts();
    }

    /**
     * Displays a simple popup (non modal) to user.
     *
     * @param message    the popup content
     * @param okText     Text of OK button
     * @param cancelText Text of Cancel button
     * @param okListener Callback on OK clicked
     */
    private void showMessageOKCancel(String message, String okText, String cancelText,
            DialogInterface.OnClickListener okListener) {
        if (mActivity != null) {
            new AlertDialog.Builder(mActivity).setMessage(message)
                    .setPositiveButton(okText, okListener)
                    .setNegativeButton(cancelText, null)
                    .create()
                    .show();
        }
    }

    /**
     * Creates the little bubble text (as a textview) that will be inserted into the EditText
     * when users will click on Insert Firstname
     */
    public TextView createContactTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(40);
        textView.setBackgroundResource(R.drawable.background_oval);
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_search_api_holo_light, 0);
        return textView;
    }

    /**
     * This callback will be called from Activity, when "sens sms" permission
     * will be given at runtime by users.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms();
                }
                break;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        final View viewFragment = inflater.inflate(R.layout.fragment_message, container, false);

        mEditText = viewFragment.findViewById(R.id.message_edittext);

        // Create the emoticon handler.
        mEmoticonHandler = new EmoticonHandler(mEditText);

        Button insertContactBtn = viewFragment.findViewById(R.id.btn_insert_username);
        insertContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contactName = getString(R.string.firstname_inserted_label);
                TextView textView = createContactTextView(contactName);
                BitmapDrawable drawable = (BitmapDrawable) convertViewToDrawable(textView);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                mEmoticonHandler.insert(TAG_ADD_FIRSTNAME, drawable);
            }
        });

        Button previewMessageBtn = viewFragment.findViewById(R.id.btn_preview_msg);
        previewMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity != null) {
                    MessagePreviewDialogFragment fragment = MessagePreviewDialogFragment.newInstance(
                            getSelectedContacts());

                    String fragmentTag = MessagePreviewDialogFragment.class.getSimpleName();
                    fragment.show(mActivity.getSupportFragmentManager(), fragmentTag);
                }
            }
        });

        setRetainInstance(true);
        return viewFragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivityInterface = (ActivityInterface) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        sentStatusReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String message = getString(R.string.receiver_unknown_error);
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        message = getString(R.string.receiver_message_sent_success);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        message = getString(R.string.receiver_generic_failure_error);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        message = getString(R.string.receiver_no_service_available_error);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        message = getString(R.string.receiver_null_pdu_error);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        message = getString(R.string.receiver_radio_is_off_error);
                        break;
                    default:
                        break;
                }

                if (intent != null) {
                    String contactName = intent.getStringExtra(Constants.INTENT_EXTRA_CONTACT_NAME);
                    Toast.makeText(getContext(), contactName + ": " + message, Toast.LENGTH_SHORT).show();
                }
            }
        };
        deliveredStatusReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String message = getString(R.string.delivery_message_not_delivered);
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        message = getString(R.string.delivery_message_delivered_successfully);

                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                String contactName = intent.getStringExtra(Constants.INTENT_EXTRA_CONTACT_NAME);
                Toast.makeText(getContext(), contactName + ": " + message, Toast.LENGTH_SHORT).show();
            }
        };

        if (mActivity != null) {
            mActivity.registerReceiver(sentStatusReceiver, new IntentFilter(INTENT_EXTRA_SMS_SENT));
            mActivity.registerReceiver(deliveredStatusReceiver, new IntentFilter(INTENT_EXTRA_SMS_DELIVERED));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mActivity != null) {
            mActivity.unregisterReceiver(sentStatusReceiver);
            mActivity.unregisterReceiver(deliveredStatusReceiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFloatingButton = null; // To avoid keeping/leaking the reference of the FAB
    }

    /**
     * {@link EmoticonHandler} for now manages 'firstnames' users add in
     * the edit text. We call "Emoticon" anything image related that is inserted
     * in the Edit text component, as in a future it can be something else.
     */
    private static class EmoticonHandler implements TextWatcher {

        private final EditText mEditor;

        private final ArrayList<ImageSpan> mEmoticonsToRemove = new ArrayList<>();

        EmoticonHandler(EditText editor) {
            // Attach the handler to listen for text changes.
            mEditor = editor;
            mEditor.addTextChangedListener(this);
        }

        public void insert(String emoticon, int resource) {
            // Create the ImageSpan
            Drawable drawable = mEditor.getResources().getDrawable(resource);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);

            // Get the selected text.
            int start = mEditor.getSelectionStart();
            int end = mEditor.getSelectionEnd();
            Editable message = mEditor.getEditableText();

            // Insert the emoticon.
            message.replace(start, end, emoticon);
            message.setSpan(span, start, start + emoticon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        public void insert(String emoticon, Drawable drawable) {
            // Create the ImageSpan to insert
            ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);

            // Get the selected text.
            int start = mEditor.getSelectionStart();
            int end = mEditor.getSelectionEnd();
            Editable message = mEditor.getEditableText();
            // Insert the emoticon.
            message.replace(start, end, emoticon);
            message.setSpan(span, start, start + emoticon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            // Check if some text will be removed.
            if (count > 0) {
                int end = start + count;
                Editable message = mEditor.getEditableText();
                ImageSpan[] list = message.getSpans(start, end, ImageSpan.class);

                for (ImageSpan span : list) {
                    // Get only the emoticons that are inside of the changed
                    // region.
                    int spanStart = message.getSpanStart(span);
                    int spanEnd = message.getSpanEnd(span);
                    if ((spanStart < end) && (spanEnd > start)) {
                        // Add to remove list
                        mEmoticonsToRemove.add(span);
                    }
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable text) {
            Editable message = mEditor.getEditableText();

            // Commit the imageSpans to be removed.
            for (ImageSpan span : mEmoticonsToRemove) {
                int start = message.getSpanStart(span);
                int end = message.getSpanEnd(span);

                // Remove the span
                message.removeSpan(span);

                // Remove the remaining emoticon text.
                if (start != end) {
                    message.delete(start, end);
                }
            }
            mEmoticonsToRemove.clear();
        }
    }
}
