package com.stephanmc.multimessages.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;

import com.stephanmc.multimessages.R;
import com.stephanmc.multimessages.interfaces.ActivityInterface;
import com.stephanmc.multimessages.model.PhoneContact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Dialog Fragment used when we click on 'Preview'
 */
public class MessagePreviewDialogFragment extends DialogFragment {

    private List<PhoneContact> selectedContacts;
    private List<String> mContactsNamesHeader;
    private HashMap<String, CharSequence> mContactsMessages;

    public MessagePreviewDialogFragment() {
    }

    public static MessagePreviewDialogFragment newInstance(List<PhoneContact> selectedContacts) {
        MessagePreviewDialogFragment fragment = new MessagePreviewDialogFragment();
        fragment.setSelectedContacts(selectedContacts);

        return fragment;
    }

    View getPreviewView() {

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.layout_dialog_preview, null);
        ExpandableListView expandableListView = view.findViewById(R.id.expandable_list_contacts_preview);

        // preparing list data
        prepareExpandableListData();

        ExpandableListAdapter listAdapter = new ExpandableListAdapter(getContext(), mContactsNamesHeader,
                mContactsMessages);

        // setting list adapter
        expandableListView.setAdapter(listAdapter);

        return view;
    }


    /*
     * Preparing the list data
     */
    private void prepareExpandableListData() {
        mContactsNamesHeader = new ArrayList<>();
        mContactsMessages = new HashMap<>();

        if (getActivity() == null) {
            return;
        }

        MessageFragment messageFragment = ((ActivityInterface) getActivity()).getMessageFragment();

        for (PhoneContact phoneContact : selectedContacts) {
            String contactName = phoneContact.getContactName();
            String messagePreview = messageFragment.makeMessagePreview(phoneContact, "");

            mContactsNamesHeader.add(contactName);
            mContactsMessages.put(contactName, messagePreview); // Header, Child data
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        // Get the layout inflater
        LayoutInflater inflater = activity.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.layout_dialog_preview, null)).setView(getPreviewView())
                // Add action buttons
                .setPositiveButton(getString(R.string.txt_ok_label), null);

        return builder.create();
    }

    public List<PhoneContact> getSelectedContacts() {
        return selectedContacts;
    }

    public void setSelectedContacts(List<PhoneContact> selectedContacts) {
        this.selectedContacts = selectedContacts;
    }
}
