package com.stephanmc.multimessages.util;

public final class Constants {
    /** Extra for Contact name in Sent/Delivery intent */
    public static final String INTENT_EXTRA_CONTACT_NAME = "CONTACT_NAME";
    /** Extra for Sent intent */
    public static final String INTENT_EXTRA_SMS_SENT = "SMS_SENT";
    /** Extra for Delivery intent */
    public static final String INTENT_EXTRA_SMS_DELIVERED = "SMS_DELIVERED";

    /** Contact tab title */
    public static final String TAB_CONTACTS_TITLE = "Contacts";
    /** Message tab title */
    public static final String TAB_MESSAGES_TITLE = "Messages";

    /**
     * Prevent instantiation
     */
    private Constants() {
    }

}
