package com.stephanmc.multimessages.ui;

import static com.stephanmc.multimessages.MainActivity.PERMISSIONS_REQUEST_READ_CONTACTS;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.stephanmc.multimessages.BuildConfig;
import com.stephanmc.multimessages.R;
import com.stephanmc.multimessages.Util;
import com.stephanmc.multimessages.adapters.ContactsAdapter;
import com.stephanmc.multimessages.interfaces.ActivityInterface;
import com.stephanmc.multimessages.interfaces.OnContactsLoaded;
import com.stephanmc.multimessages.lib.fastscroller.FastScroller;
import com.stephanmc.multimessages.model.PhoneContact;
import com.stephanmc.multimessages.tasks.AsyncContactLoader;
import com.stephanmc.multimessages.tasks.SimpleImageCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment for the CONTACTS tab
 */
public class ContactsFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ContactsFragment";

    private static final int CONTACT_LOADER = 1;
    // Bundle key for saving previously selected search result item
    private static final String STATE_PREVIOUSLY_SELECTED_KEY =
            "com.stephanmc.multimessages.ui.ContactsFragment.SELECTED_ITEMS";

    private final MultiMessageApplication mApplication = MultiMessageApplication.getInstance();
    private final List<PhoneContact> EMPTY_CONTACTS_LIST = Collections.emptyList();
    private String mSearchString;
    private SearchView mSearchView;
    private ActivityInterface mActivityInterface;
    // Listener on "Write Message" floating icon
    private final View.OnClickListener mFloatingButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mActivityInterface.selectTabPage(ActivityInterface.MESSAGE_FRAGMENT_POSITION);
        }
    };
    // Stores the previously selected search item so that on a configuration change the same item
    // can be reselected again
    private ArrayList<String> mPreviouslySelectedSearchItems = new ArrayList<>();
    private FloatingActionButton mFloatingButton;
    private RecyclerView mRecyclerView;
    private View mRecyclerViewContainer;
    private View mContactsNeedPermissionView;
    private View mBtnRequestPermission;
    private ContactsAdapter mAdapter;
    // Search action listener
    private final SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            // Nothing needs to happen when the user submits the search string
            return true;
        }

        @Override
        public boolean onQueryTextChange(String query) {
            String newFilter = !TextUtils.isEmpty(query) ? query : null;

            // Don't do anything if the filter is empty
            if (mSearchString == null && newFilter == null) {
                return true;
            }

            // Don't do anything if the new filter is the same as the current filter
            if (mSearchString != null && mSearchString.equals(newFilter)) {
                return true;
            }

            mSearchString = query;
            if (mAdapter != null) {
                mAdapter.getFilter().filter(query);
            }
            return false;
        }
    };
    private FastScroller mFastScroller;

    public ContactsFragment() {
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    public List<PhoneContact> retreiveSelectedContacts() {
        return mAdapter.retreiveAllSelectedContacts();
    }

    public void setupFloatingButton(FloatingActionButton button) {
        // When the FAB is shared to another Fragment, we nullify everything
        if (button == null) {
            if (mFloatingButton != null) {
                mFloatingButton.setOnClickListener(null);
            }
            mFloatingButton = null;

        } else {
            mFloatingButton = button;
            mFloatingButton.setImageResource(R.drawable.ic_message_text_white_36dp);

            mFloatingButton.setOnClickListener(mFloatingButtonListener);

            if (getContext() != null) {
                ColorStateList tint = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorAccent));
                mFloatingButton.setBackgroundTintList(tint);
            }
        }
    }

    private void setupSearchViewMenu() {
        if (mActivityInterface == null || mActivityInterface.getSearchView() == null) {
            return;
        }

        // when fragment is visible, lets setup searchview
        mSearchView = mActivityInterface.getSearchView();
        // listening to search query text change
        mSearchView.setOnQueryTextListener(mOnQueryTextListener);
        mSearchView.setQuery(mSearchString, false);
    }

    private ArrayList<String> retreiveSelectedContactsURIS() {
        ArrayList<String> result = new ArrayList<>();
        for (PhoneContact contact : retreiveSelectedContacts()) {
            result.add(contact.getId());
        }
        return result;
    }

    private int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();

        // Resolve list item preferred height theme attribute into typedValue
        getActivity().getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, typedValue, true);

        // Create a new DisplayMetrics object
        final DisplayMetrics metrics = new android.util.DisplayMetrics();

        // Populate the DisplayMetrics
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    private void installBtnListener() {

        mBtnRequestPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity == null) {
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mActivity.checkSelfPermission(
                        Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    mActivity.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                            PERMISSIONS_REQUEST_READ_CONTACTS);

                } else {
                    Toast.makeText(getContext(), R.string.contacts_permissions_already_granted, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void showContactsInRecyclerView() {
        if (mActivity == null) {
            return;
        }
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mActivity.checkSelfPermission(
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            displayNeedPermissionView();

            return;
        }

        displayContactView();
        initRecyclerViewAdapter();
    }

    /**
     * Initialize our contact recycler view, and launches the async contact loader
     * that will help populating the recycler view.
     */
    private void initRecyclerViewAdapter() {
        if (mActivity == null) {
            return;
        }
        mActivityInterface.setBottomBarVisible();

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mApplication));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mApplication, LinearLayoutManager.VERTICAL, 100, 1));

        if (Util.isEmpty(mPreviouslySelectedSearchItems)) {
            mActivity.getSupportLoaderManager().initLoader(CONTACT_LOADER, null, ContactsFragment.this);
        }
        mRecyclerView.setAdapter(mAdapter);
        mFastScroller.setRecyclerView(mRecyclerView);
    }

    private void displayNeedPermissionView() {
        mContactsNeedPermissionView.setVisibility(View.VISIBLE);
        mRecyclerViewContainer.setVisibility(View.GONE);
    }

    private void displayContactView() {
        mContactsNeedPermissionView.setVisibility(View.GONE);
        mRecyclerViewContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted
                    showContactsInRecyclerView();
                } else {
                    Toast.makeText(getContext(), R.string.toast_contacts_permission_refused_again, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mAdapter != null) {
            return;
        }

        setHasOptionsMenu(true);
        mAdapter = new ContactsAdapter(getContext(), EMPTY_CONTACTS_LIST);

        if (savedInstanceState != null) {
            // If we're restoring state after this fragment was recreated then
            // retrieve previous search term and previously selected search
            // result.
            mSearchString = savedInstanceState.getString(SearchManager.QUERY);
            mPreviouslySelectedSearchItems = savedInstanceState.getStringArrayList(STATE_PREVIOUSLY_SELECTED_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        View contactsFragmentView = inflater.inflate(R.layout.fragment_contacts_recyclerview, container, false);

        mRecyclerView = contactsFragmentView.findViewById(R.id.recycler_view);
        mRecyclerViewContainer = contactsFragmentView.findViewById(R.id.recycler_view_container);
        mContactsNeedPermissionView = contactsFragmentView.findViewById(R.id.contacts_need_permission_view);
        mBtnRequestPermission = contactsFragmentView.findViewById(R.id.btn_request_contact_permission);

        mFastScroller = contactsFragmentView.findViewById(R.id.fastscroll);

        setRetainInstance(true);
        return contactsFragmentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mActivityInterface = (ActivityInterface) getActivity();
            mAdapter.setActivityInterface(mActivityInterface);

        } catch (ClassCastException e) {
            Log.e(TAG, getActivity() + " must implement ActivityInterface");
            throw new ClassCastException(getActivity() + " must implement ActivityInterface");
        }

        installBtnListener();
        showContactsInRecyclerView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!TextUtils.isEmpty(mSearchString)) {
            // Saves the current search string
            outState.putString(SearchManager.QUERY, mSearchString);

            // Saves the currently selected contact
            outState.putStringArrayList(STATE_PREVIOUSLY_SELECTED_KEY, retreiveSelectedContactsURIS());
        }
    }

    @Override
    public void onDestroyView() {
        SimpleImageCache.clearCache();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFloatingButton = null;

        SimpleImageCache.clearCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        setupSearchViewMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_select_all) {
            if (mAdapter != null) {
                mAdapter.selectAllContacts();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets top notification bar as white for Android >= M
     */
    private void whiteNotificationBar(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            mActivity.getWindow().setStatusBarColor(Color.WHITE);
        }
    }

    /**
     * Called by the system when we can now create our contact loader.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case CONTACT_LOADER:

                String[] projectionFields =
                        new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.PHOTO_URI, ContactsContract.Data.LOOKUP_KEY};

                final String whereClause = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
                final String sortOrder = ContactsContract.Contacts.SORT_KEY_PRIMARY;

                // Return the loader for use
                return new CursorLoader(getContext(), ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projectionFields, whereClause, null, sortOrder);
        }
        Log.e(TAG, "onCreateLoader - incorrect ID provided (" + id + ")");
        throw new IllegalArgumentException("onCreateLoader - incorrect ID provided (" + id + ")");
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, final Cursor cursor) {
        cursor.moveToFirst();

        // build contact list with it.
        final Context context = getContext();
        buildContactsFromCursor(context, cursor, new OnContactsLoaded() {
            @Override
            public void onLoaded(List<PhoneContact> contactList) {
                if (context != null) {
                    mAdapter.initContactsList(contactList);
                }

                if (mActivity != null) {
                    mActivity.getSupportLoaderManager().destroyLoader(CONTACT_LOADER);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoaderReset");
        }
    }

    private void buildContactsFromCursor(final Context context, final Cursor cursor,
            final OnContactsLoaded onContactsLoaded) {
        AsyncContactLoader asyncContactLoader = new AsyncContactLoader(context, cursor, onContactsLoaded);
        asyncContactLoader.execute(getContext());
    }

    public void refreshUI() {
        if (mAdapter != null) {
            mAdapter.refreshUI();
        }
    }
}
