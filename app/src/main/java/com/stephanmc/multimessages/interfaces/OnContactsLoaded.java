package com.stephanmc.multimessages.interfaces;

import com.stephanmc.multimessages.model.PhoneContact;

import java.util.List;

/**
 * Any listener that want to be notified of contacts loaded should implement this.
 */
public interface OnContactsLoaded {

    /**
     * Callback when contacts are loaded.
     *
     * @param contactList the loaded contacts
     */
    void onLoaded(List<PhoneContact> contactList);
}
