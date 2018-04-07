package com.stephanmc.multimessages.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.stephanmc.multimessages.R;

public class InfoDialogFragment extends DialogFragment {

    private String title;
    private CharSequence mMessage;

    public InfoDialogFragment() {
    }

    public static InfoDialogFragment newInstance(String title, CharSequence message) {
        InfoDialogFragment simplePopupFragment = new InfoDialogFragment();
        simplePopupFragment.setTitle(title);
        simplePopupFragment.setMessage(message);

        return simplePopupFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mMessage).setTitle(title).setPositiveButton(getString(R.string.txt_ok_label), null);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setMessage(CharSequence message) {
        mMessage = message;
    }
}
