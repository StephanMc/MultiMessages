package com.stephanmc.multimessages.interfaces;

import android.content.Context;
import android.support.v7.widget.SearchView;

import com.stephanmc.multimessages.model.PhoneContact;
import com.stephanmc.multimessages.ui.ContactsFragment;
import com.stephanmc.multimessages.ui.MessageFragment;

import java.util.List;

/**
 * This interface is used for communication between fragments and their host activity.
 * Activity should implement this, and fragments can have a reference to their activity through
 * this interface.
 */
public interface ActivityInterface {

    // positions of our fragments
    int CONTACTS_FRAGMENT_POSITION = 0;
    int MESSAGE_FRAGMENT_POSITION = 1;
    int ABOUT_FRAGMENT_POSITION = 2;

    /**
     * Returns a reference to our contacts fragment
     */
    ContactsFragment getContactsFragment();

    /**
     * Returns a reference to our contacts fragment
     */
    MessageFragment getMessageFragment();

    /**
     * Allow to slide to a given page number
     *
     * @param pageIndex : tab number
     */
    void selectTabPage(int pageIndex);

    /**
     * Allow setting visible the bottom bar
     */
    void setBottomBarVisible();

    /**
     * Allow fragments clients to ask host to hide any visible keyboard
     */
    void hideKeyboard();

    /**
     * Returns application context
     */
    Context getContext();

    /**
     * Given the selected contact list, the activity will update the bottom panel text and visibility
     */
    void updateBottomPanel(List<PhoneContact> selectedContacts);

    /**
     * Given the selected count, the activity will update the badge of the contact tab.
     */
    void updateContactsTabBadge(int selectedContactsCount);

    SearchView getSearchView();
}
