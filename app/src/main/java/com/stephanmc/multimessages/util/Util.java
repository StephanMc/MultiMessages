package com.stephanmc.multimessages.util;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;

import com.stephanmc.multimessages.BuildConfig;
import com.stephanmc.multimessages.R;

import java.text.Normalizer;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for Multi Messages.
 */
public class Util {

    public static final int SMS_LIMIT = 160;
    private static final String REGEX_NAME_SEPARATOR = "[ .]";

    private final static String TAG = Util.class.getSimpleName();
    private static final StyleSpan sStyleBoldSpan;
    private static final Pattern sPattern = Pattern.compile(REGEX_NAME_SEPARATOR);
    private static int sMaxAllowed;

    static {
        sStyleBoldSpan = new StyleSpan(Typeface.BOLD);

        setSmsDefaultLimitations();
    }

    // Prevent instanciation
    private Util() {
    }

    /**
     * Initialize SMS limitations depending on the platform.
     */
    private static void setSmsDefaultLimitations() {

        int apiLevel = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;

        int sCheckPeriod;
        switch (apiLevel) {
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                sMaxAllowed = 100;
                sCheckPeriod = 3600000;
                break;
            case 16:
                sMaxAllowed = 30;
                sCheckPeriod = 1800000;
                break;
            case 17:
                sMaxAllowed = 30;
                if (versionRelease.contains("4.2.2")) {
                    sCheckPeriod = 60000;
                } else {
                    sCheckPeriod = 1800000;
                }
                break;
            case 18:
                sMaxAllowed = 30;
                sCheckPeriod = 60000;
                break;
            default:
                sMaxAllowed = 30;
                sCheckPeriod = 1800000;
                break;
        }
        sMaxAllowed = sMaxAllowed - 2; // for safety
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "maxAllowed = " + sMaxAllowed + "; checkPeriod = " + (sCheckPeriod / 60000) + " minutes");
        }
    }

    /**
     * Returns the contact first name, by getting the first portion of the contact name.
     * A portion is delimited by a space of a ".", as specified in REGEX_NAME_SEPARATOR field.
     */
    public static String getFirstName(String contactName) {

        if (contactName == null || contactName.trim().equals("")) {
            return "";
        }
        return contactName.split(REGEX_NAME_SEPARATOR)[0];
    }

    /**
     * This acts like String.indexOf(), but looks for the firstname separator regex (space or ".") in the String input.
     * If not found, we return -1.
     */
    public static int indexOfFirstnameSeparator(String input) {
        Matcher matcher = sPattern.matcher(input);
        if (matcher.find()) {
            return matcher.start();
        }
        return -1;
    }

    /**
     * Returns a new {@link CharSequence} in which the first part (which acts like the firstname) is styled as Bold.
     */
    public static CharSequence makeContactNameBold(String contactName) {

        if (TextUtils.isEmpty(contactName)) {
            return contactName;
        }

        int spaceIndex = Util.indexOfFirstnameSeparator(contactName);
        int indexStart = 0;
        int indexEnd = spaceIndex > -1 ? spaceIndex : contactName.length();

        SpannableString spannableString = new SpannableString(contactName);

        spannableString.setSpan(sStyleBoldSpan, indexStart, indexEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }


    public static int getMaxSMSAllowed() {
        return sMaxAllowed;
    }

    public static void makePopupInfo(Context context, String title, String message) {
        if (context != null) {
            new AlertDialog.Builder(context).setMessage(message).setTitle(title).setPositiveButton(
                    context.getString(R.string.txt_ok_label), null).create().show();
        }
    }

    public static void alertSmsLimitReached(Context context) {
        if (context != null) {
            makePopupInfo(context, context.getString(R.string.txt_message_limit),
                    context.getString(R.string.txt_alert_cannot_send_more_than_limit, getMaxSMSAllowed()));
        }
    }

    /**
     * Normalized string takes into account locales where accents are often used.
     * This is useful in handling of Searchs feature, where users can type for example a string like
     * "Stéphane" and expect to have result of "Stephane" and Stéphane
     */
    public static String normalizeString(String charString) {
        return Normalizer.normalize(charString, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Search "search" into "source" string with normalization feature enabled
     */
    public static int normalizedIndexOf(String source, String search) {
        return Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .indexOf(search);
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

}
