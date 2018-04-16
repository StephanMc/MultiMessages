package com.stephanmc.multimessages.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.stephanmc.multimessages.R;
import com.stephanmc.multimessages.util.Util;
import com.stephanmc.multimessages.interfaces.ActivityInterface;
import com.stephanmc.multimessages.model.PhoneContact;
import com.stephanmc.multimessages.tasks.SimpleImageCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Contacts UI
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> implements Filterable,
        SectionTitleProvider {

    private final StyleSpan mContactNamesStyleSpan;
    private final Context mContext;
    private List<PhoneContact> mContactsList;
    private List<PhoneContact> mContactsListFiltered;
    private int mSelectedContactsCount = 0;
    private ActivityInterface mActivityInterface;
    private final View.OnClickListener mOnContactClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View rowContact) {
            RecyclerView recyclerView = (RecyclerView) rowContact.getParent();
            int itemPosition = recyclerView.getChildLayoutPosition(rowContact);

            onContactClicked(itemPosition);
        }
    };
    private final View.OnClickListener mOnCheckBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View checkboxView) {
            View relativeLayout = (View) checkboxView.getParent();
            RecyclerView recyclerView = (RecyclerView) relativeLayout.getParent();
            int itemPosition = recyclerView.getChildLayoutPosition(relativeLayout);

            boolean succeed = onContactClicked(itemPosition);
            // uncheck item if selection failed
            if (!succeed) {
                ((CheckBox) checkboxView).setChecked(false);
            }
        }
    };
    private CharSequence mSearchString;

    public ContactsAdapter(Context context, List<PhoneContact> contactList) {
        this.mContactsList = contactList;
        this.mContactsListFiltered = contactList;
        this.mContext = context;

        mContactNamesStyleSpan = new StyleSpan(Typeface.BOLD);
    }

    public List<PhoneContact> retreiveAllSelectedContacts() {
        return retreiveAllSelectedContacts(mContactsList);
    }

    private List<PhoneContact> retreiveAllSelectedContacts(List<PhoneContact> contactList) {
        List<PhoneContact> selected = new ArrayList<>();

        for (PhoneContact contact : contactList) {
            if (contact.isSelected()) {
                selected.add(contact);
            }
        }
        return selected;
    }

    public void refreshUI() {
        recomputeSelectedCount();
        updateTabTitle();
        updateBottomPanel();

        notifyDataSetChanged();
    }

    public void initContactsList(List<PhoneContact> contactList) {
        if (mContactsList != null) {
            mContactsList.clear();
        }
        if (mContactsListFiltered != null) {
            mContactsListFiltered.clear();
        }
        mContactsList = mContactsListFiltered = contactList;

        refreshUI();
    }

    @Override
    public String getSectionTitle(int position) {
        if (position < 0) {
            position = 0;
        } else if (position >= mContactsListFiltered.size()) {
            position = mContactsListFiltered.size() - 1;
        }

        PhoneContact contact = mContactsListFiltered.get(position);
        if (contact != null) {
            return contact.getContactName().substring(0, 1);
        }
        return "";
    }

    private void recomputeSelectedCount() {
        mSelectedContactsCount = retreiveAllSelectedContacts().size();
    }

    private void updateBottomPanel() {
        List<PhoneContact> selectedContacts = retreiveAllSelectedContacts();
        if (mActivityInterface != null) {
            mActivityInterface.updateBottomPanel(selectedContacts);
        }
    }

    public void selectAllContacts() {
        new SelectAllTask(this).execute();
    }

    /**
     * Returns true if we succeded to select a contact.
     * Contact selection can fail if we reached the contact selection limit, due to Android SMS limitation.
     */
    private boolean onContactClicked(int itemPosition) {

        PhoneContact contact = mContactsListFiltered.get(itemPosition);
        // case when user is about to check one that is not checked yet, and we've reach the limit
        if (!contact.isSelected() && (mSelectedContactsCount + 1 > Util.getMaxSMSAllowed())) {
            Util.alertSmsLimitReached(mContext);
            return false;
        }

        boolean isSelected = !contact.isSelected();
        contact.setSelected(isSelected);
        if (isSelected) {
            mSelectedContactsCount++;
        } else {
            mSelectedContactsCount--;
        }

        updateTabTitle();
        updateBottomPanel();
        notifyItemChanged(itemPosition);

        return true;
    }

    private void updateTabTitle() {
        if (mActivityInterface == null) {
            return;
        }
        mActivityInterface.updateContactsTabBadge(mSelectedContactsCount);
    }

    public void setActivityInterface(ActivityInterface activityInterface) {
        this.mActivityInterface = activityInterface;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.item_contact_row, parent, false);
        ContactViewHolder contactViewHolder = new ContactViewHolder(view);

        view.setOnClickListener(mOnContactClickListener);
        view.findViewById(R.id.checkbox_contact).setOnClickListener(mOnCheckBoxClickListener);
        return contactViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        PhoneContact phoneContact = mContactsListFiltered.get(position);

        holder.checkboxContact.setChecked(phoneContact.isSelected());
        holder.checkboxContact.setTag(phoneContact);

        Bitmap bitmap = SimpleImageCache.getContactBitmap(phoneContact.getId());

        if (bitmap != null) {
            holder.imgContactPicture.setImageBitmap(bitmap);
        } else {
            holder.imgContactPicture.setImageResource(R.drawable.ic_default_avatar);
        }

        holder.tvContactName.setText(getSpannableForSearch(phoneContact.getContactName()));
        // make part bold
        makeContactNameBold(holder.tvContactName);

        holder.tvPhoneNumber.setText(getSpannableForSearch(phoneContact.getContactNumber()));

    }

    @Override
    public int getItemCount() {
        return mContactsListFiltered.size();
    }

    private void makeContactNameBold(TextView tvContactName) {
        String textString = tvContactName.getText().toString();
        if (TextUtils.isEmpty(textString)) {
            return;
        }

        int spaceIndex = Util.indexOfFirstnameSeparator(textString);
        int indexStart = 0;
        int indexEnd = spaceIndex > -1 ? spaceIndex : textString.length();

        SpannableString spannableString = new SpannableString(tvContactName.getText());
        spannableString.setSpan(mContactNamesStyleSpan, indexStart, indexEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        tvContactName.setText(spannableString);
    }

    /**
     * Returns a spannable text view when user is searching something.
     * It colors text to red when it matches
     *
     * @param rawTarget : the textview content in which we expect to color the search
     */
    private CharSequence getSpannableForSearch(String rawTarget) {

        CharSequence result = rawTarget;
        if (!TextUtils.isEmpty(mSearchString)) {

            String targetLowerCase = rawTarget.toLowerCase(Locale.getDefault());

            String searchStringLowercase = mSearchString.toString().toLowerCase();
            int indexOfWithLatin = Util.normalizedIndexOf(targetLowerCase, searchStringLowercase);
            if (indexOfWithLatin > -1) {
                int startPos = indexOfWithLatin;
                int endPos = startPos + mSearchString.length();

                Spannable spanString = Spannable.Factory.getInstance().newSpannable(rawTarget);
                spanString.setSpan(new ForegroundColorSpan(Color.RED), startPos, endPos,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                result = spanString;
            }
        }

        return result;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                mSearchString = charSequence;

                if (charString.isEmpty()) {
                    mContactsListFiltered = mContactsList;
                } else {
                    // normalize search text
                    charString = Util.normalizeString(charString);
                    List<PhoneContact> filteredList = new ArrayList<>();
                    for (PhoneContact row : mContactsList) {

                        if (Util.normalizedIndexOf(row.getContactName().toLowerCase(), charString.toLowerCase()) > -1
                                || row.getContactNumber().contains(charSequence)) {
                            filteredList.add(row);
                        }
                    }

                    mContactsListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mContactsListFiltered;

                return filterResults;

            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mContactsListFiltered = (ArrayList<PhoneContact>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    /**
     * Static task for contact selection
     */
    private static class SelectAllTask extends AsyncTask<Boolean, Object, Boolean> {

        private ContactsAdapter mContactsAdapter;

        SelectAllTask(ContactsAdapter contactsAdapter) {
            mContactsAdapter = contactsAdapter;
        }

        @Override
        protected Boolean doInBackground(Boolean... selected) {
            // Check if exists one that is not selected, so that we select all
            boolean shouldSelectAll = existsOneVisibleNotSelected();

            if (shouldSelectAll &&
                    // Do not allow selection if we reach the limit
                    (mContactsAdapter.mSelectedContactsCount + mContactsAdapter.mContactsList.size()
                            > Util.getMaxSMSAllowed())) {
                return false;
            }
            for (PhoneContact contact : mContactsAdapter.mContactsListFiltered) {
                contact.setSelected(shouldSelectAll);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean canSelectAll) {
            if (!canSelectAll) {
                if (mContactsAdapter.mActivityInterface != null) {
                    Util.alertSmsLimitReached(mContactsAdapter.mActivityInterface.getContext());
                }

                mContactsAdapter = null;
                return;
            }

            mContactsAdapter.refreshUI();

            // prevent leak
            mContactsAdapter = null;
        }

        /**
         * Indicates wether exists a visible selected contact to user.
         * It is used to know if we should select or unselect all.
         */
        private boolean existsOneVisibleNotSelected() {
            for (PhoneContact contact : mContactsAdapter.mContactsListFiltered) {
                if (!contact.isSelected()) {
                    return true;
                }
            }
            return false;
        }
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {

        final AppCompatImageView imgContactPicture;
        final TextView tvContactName;
        final TextView tvPhoneNumber;
        private final CheckBox checkboxContact;

        ContactViewHolder(View itemView) {
            super(itemView);
            imgContactPicture = itemView.findViewById(R.id.img_contact_picture);
            tvContactName = itemView.findViewById(R.id.txt_contact_name);
            tvPhoneNumber = itemView.findViewById(R.id.txt_contact_number);
            checkboxContact = itemView.findViewById(R.id.checkbox_contact);
        }
    }
}
