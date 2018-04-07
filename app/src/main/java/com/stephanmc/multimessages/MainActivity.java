package com.stephanmc.multimessages;

import static com.stephanmc.multimessages.util.Constants.TAB_CONTACTS_TITLE;
import static com.stephanmc.multimessages.util.Constants.TAB_MESSAGES_TITLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stephanmc.multimessages.interfaces.ActivityInterface;
import com.stephanmc.multimessages.model.PhoneContact;
import com.stephanmc.multimessages.tasks.SimpleImageCache;
import com.stephanmc.multimessages.ui.AboutFragment;
import com.stephanmc.multimessages.ui.ContactsFragment;
import com.stephanmc.multimessages.ui.MessageFragment;
import com.stephanmc.multimessages.util.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main Application Activity
 */
public class MainActivity extends AppCompatActivity implements ActivityInterface {

    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    public static final int PERMISSIONS_REQUEST_SEND_SMS = 200;

    private static final String TAG = MainActivity.class.getSimpleName();

    private final Handler mHandler = new Handler();
    private TabLayout mTabLayout;
    private FloatingActionButton mFloatingActionButton;
    private MyViewPageAdapter mViewPageAdapter;
    private TextView mBottomBarText;
    private Button mBtnViewSelectedContacts;

    private List<PhoneContact> mSelectedContacts;

    private BottomSheetBehavior<LinearLayout> mSheetBehavior;
    private ViewPager mViewPager;
    private LinearLayout mLayoutBottomSheet;
    private boolean mBottomBarIsShown = true;
    private SearchView mSearchView;

    private boolean mMenusAreVisible = true;
    private boolean pressedBackTwice = false;
    private final Runnable mResetPressBackRunnable = new Runnable() {
        @Override
        public void run() {
            pressedBackTwice = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        //Get the toolbar from the layout and set the actionbar with it.
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);

        mTabLayout = findViewById(R.id.tabs);
        mFloatingActionButton = findViewById(R.id.shared_fab);

        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setOffscreenPageLimit(3);

        addFragmentsToViewpager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "selected tab position: " + position);
                }
                hideKeyboard();
                checkStateOnPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                ContactsFragment contactsFragment = mViewPageAdapter.getContactsFragment();
                MessageFragment messageFragment = mViewPageAdapter.getMessageFragment();

                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        hideKeyboard();
                        hideFloatingButton();
                        mFloatingActionButton.hide();
                        break;

                    case ViewPager.SCROLL_STATE_IDLE:
                    default:
                        switch (mViewPager.getCurrentItem()) {
                            case CONTACTS_FRAGMENT_POSITION:
                                showActionBarMenus();
                                updateBottomPanel(mSelectedContacts);

                                // remove actions related to message fragment and setup contact actions
                                messageFragment.setupFloatingActionButton(null);
                                contactsFragment.setupFloatingButton(mFloatingActionButton);
                                break;
                            case MESSAGE_FRAGMENT_POSITION:
                                hideActionBarMenus();
                                updateBottomPanel(mSelectedContacts);

                                // remove actions related to contact fragment and setup message actions
                                contactsFragment.setupFloatingButton(null);
                                messageFragment.setupFloatingActionButton(mFloatingActionButton);
                                break;

                            case ABOUT_FRAGMENT_POSITION: {
                                hideActionBarMenus();

                                mFloatingActionButton.hide();
                                return;
                            }
                        }
                        mFloatingActionButton.show();
                        break;
                }
            }
        });
        mTabLayout.setupWithViewPager(mViewPager);

        // icons

        TabLayout.Tab tabAbout = mTabLayout.getTabAt(ABOUT_FRAGMENT_POSITION);
        if (tabAbout != null) {
            tabAbout.setIcon(R.drawable.ic_info_outline_white_24dp);
        }
        // setup latest icon tab
        LinearLayout layout = ((LinearLayout) ((LinearLayout) mTabLayout.getChildAt(0)).getChildAt(2));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) layout.getLayoutParams();
        layoutParams.weight = 0f;
        layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        layout.setLayoutParams(layoutParams);

        mBottomBarText = findViewById(R.id.txt_bottom_bar);
        mBtnViewSelectedContacts = findViewById(R.id.btn_view_selected_contacts);

        mBtnViewSelectedContacts.setOnClickListener(new View.OnClickListener() {

            Dialog getDialog() {

                final CharSequence[] contactsArray = buildSelectedContactsArrayForPopup();
                final boolean[] checkedItems = buildCheckedItemArray(contactsArray.length);

                final Set<Integer> selectedIndexes = buildSelectedIndexesList(contactsArray.length);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // Set the dialog title
                builder.setTitle(R.string.popup_label_selected_contacts).setMultiChoiceItems(contactsArray,
                        checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to selected items
                                    selectedIndexes.add(which);

                                } else if (selectedIndexes.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    selectedIndexes.remove(which);
                                }
                            }
                        })
                        // Set the action buttons
                        .setPositiveButton(getString(R.string.txt_ok_label), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                int selectedCount = mSelectedContacts.size();

                                for (int i = 0; i < selectedCount; i++) {
                                    if (!selectedIndexes.contains(i)) {
                                        mSelectedContacts.get(i).setSelected(false);
                                    }
                                }

                                ContactsFragment contactsFragment = getContactsFragment();
                                contactsFragment.refreshUI();
                            }
                        }).setNegativeButton(getString(R.string.txt_cancel_label), null);

                return builder.create();

            }

            private Set<Integer> buildSelectedIndexesList(int length) {
                Set<Integer> result = new HashSet<>();
                for (int i = 0; i < length; i++) {
                    result.add(i);
                }
                return result;
            }

            private boolean[] buildCheckedItemArray(int length) {
                boolean result[] = new boolean[length];
                for (int i = 0; i < length; i++) {
                    result[i] = true;
                }
                return result;
            }

            private CharSequence[] buildSelectedContactsArrayForPopup() {
                CharSequence[] contacts = new CharSequence[mSelectedContacts.size()];
                int i = 0;
                for (PhoneContact contact : mSelectedContacts) {
                    contacts[i++] = Util.makeContactNameBold(contact.getContactName());
                }

                return contacts;
            }

            @Override
            public void onClick(View v) {
                getDialog().show();
            }
        });

        mLayoutBottomSheet = findViewById(R.id.bottom_sheet);
        mSheetBehavior = BottomSheetBehavior.from(mLayoutBottomSheet);

        mSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "bottom sheet hidden");
                        }
                        mSheetBehavior.setHideable(false);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "bottom sheet expanded");
                        }
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "bottom sheet collapsed");
                        }
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "bottom sheet dragging");
                        }
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "bottom sheet settling");
                        }
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        ContactsFragment contactsFragment = mViewPageAdapter.getContactsFragment();
        contactsFragment.refreshUI();
    }

    @Override
    protected void onDestroy() {
        SimpleImageCache.clearCache();
        mHandler.removeCallbacks(mResetPressBackRunnable);

        super.onDestroy();
    }

    private void checkStateOnPageSelected(int currentPosition) {
        switch (currentPosition) {
            case ABOUT_FRAGMENT_POSITION:
                hideBottombar();
                hideActionBarMenus();
                hideFloatingButton();

                break;

            case MESSAGE_FRAGMENT_POSITION:
                hideActionBarMenus();
                if (!mBottomBarIsShown) {
                    showBottomBar();
                }
                break;

            case CONTACTS_FRAGMENT_POSITION:
            default:
                if (!mBottomBarIsShown) {
                    showBottomBar();
                }
                showActionBarMenus();

                break;
        }
    }

    private void hideFloatingButton() {
        if (mFloatingActionButton != null) {
            mFloatingActionButton.hide();
        }
    }

    private void showActionBarMenus() {
        mMenusAreVisible = true;
        invalidateOptionsMenu();
    }

    private void hideActionBarMenus() {
        mMenusAreVisible = false;
        invalidateOptionsMenu();
    }

    public void showBottomBar() {
        setBottomBarFade(0f, 1f);
    }

    private void hideBottombar() {
        setBottomBarFade(1f, 0f);
        mBottomBarIsShown = false;
    }

    private void setBottomBarFade(float from, float to) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(from, to);
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                mLayoutBottomSheet.setAlpha(alpha);

                if (alpha == 0f) {
                    mBottomBarIsShown = false;

                } else if (alpha == 1f) {
                    mBottomBarIsShown = true;
                }
            }


        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // done
                if (mLayoutBottomSheet.getAlpha() == 0f) {
                    mLayoutBottomSheet.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mLayoutBottomSheet.setVisibility(View.VISIBLE);
            }
        });
        valueAnimator.start();
    }

    private void addFragmentsToViewpager(ViewPager viewPager) {
        mViewPageAdapter = new MyViewPageAdapter(getSupportFragmentManager());

        Fragment contactsFragment = ContactsFragment.newInstance();
        ((ContactsFragment) contactsFragment).setupFloatingButton(mFloatingActionButton);

        mViewPageAdapter.addFragment(contactsFragment, CONTACTS_FRAGMENT_POSITION, TAB_CONTACTS_TITLE);
        mViewPageAdapter.addFragment(MessageFragment.newInstance(), MESSAGE_FRAGMENT_POSITION, TAB_MESSAGES_TITLE);
        mViewPageAdapter.addFragment(AboutFragment.newInstance(), ABOUT_FRAGMENT_POSITION, null);
        viewPager.setAdapter(mViewPageAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        // setup visibility
        int size = menu.size();
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setVisible(mMenusAreVisible);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        // If we are on search view
        SearchView searchView = getContactsFragment().getSearchView();
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchView.setIconified(true);
            return;
        }

        // In any case, press twice to exit
        if (pressedBackTwice) {
            super.onBackPressed();
            return;
        }

        this.pressedBackTwice = true;
        Toast.makeText(this, R.string.press_twice_to_exit, Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(mResetPressBackRunnable, 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    public ContactsFragment getContactsFragment() {
        return mViewPageAdapter.getContactsFragment();
    }

    @Override
    public MessageFragment getMessageFragment() {
        return mViewPageAdapter.getMessageFragment();
    }

    @Override
    public void selectTabPage(int pageIndex) {
        mTabLayout.setScrollPosition(pageIndex, 0f, true);
        mViewPager.setCurrentItem(pageIndex);
    }

    @Override
    public void setBottomBarVisible() {
        if (mLayoutBottomSheet != null) {
            mLayoutBottomSheet.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideKeyboard() {
        InputMethodManager systemService = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        systemService.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void updateBottomPanel(List<PhoneContact> selectedContacts) {
        mSelectedContacts = selectedContacts;

        int selectedCount = mSelectedContacts != null ? mSelectedContacts.size() : 0;
        CharSequence bottomText = "";
        if (selectedCount == 0) {
            String helpText;
            boolean messageFragmentVisible =
                    mViewPager != null && mViewPager.getCurrentItem() == MESSAGE_FRAGMENT_POSITION;

            // Help text on Message Fragment
            helpText = getString(messageFragmentVisible ? R.string.txt_select_recipients
                    : R.string.txt_select_recipients_firstname_bold);

            // Apply help text depending on the platform (due to an api change)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                bottomText = Html.fromHtml(helpText, Html.FROM_HTML_MODE_LEGACY);
            } else {
                bottomText = Html.fromHtml(helpText);
            }
        } else {
            // When there are contacts selected, indicate the proper text count

            Resources resources = getResources();
            if (resources != null) {
                bottomText = resources.getQuantityString(R.plurals.user_selected_n_contacts, selectedCount,
                        selectedCount);
            }
        }

        mBottomBarText.setText(bottomText);
        mBtnViewSelectedContacts.setText(getString(R.string.recipients_labels, selectedCount));

        mBtnViewSelectedContacts.setVisibility(selectedCount > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void updateContactsTabBadge(int selectedContactsCount) {
        if (mTabLayout == null) {
            return;
        }
        TabLayout.Tab contactTab = mTabLayout.getTabAt(CONTACTS_FRAGMENT_POSITION);
        if (contactTab != null) {
            String countBadge = selectedContactsCount > 0 ? " (" + selectedContactsCount + ")" : "";
            contactTab.setText(TAB_CONTACTS_TITLE + countBadge);
        }
    }

    @Override
    public SearchView getSearchView() {
        return mSearchView;
    }

    static class MyViewPageAdapter extends FragmentStatePagerAdapter {

        private final SparseArray<Fragment> mFragments = new SparseArray<>();
        private final SparseArray<String> mFragmentTitles = new SparseArray<>();

        MyViewPageAdapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(Fragment fragment, int position, String title) {
            mFragments.put(position, fragment);
            mFragmentTitles.put(position, title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        // Prevents android from recreating the fragments
        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        public ContactsFragment getContactsFragment() {
            return (ContactsFragment) mFragments.get(CONTACTS_FRAGMENT_POSITION);
        }

        public MessageFragment getMessageFragment() {
            return (MessageFragment) mFragments.get(MESSAGE_FRAGMENT_POSITION);
        }

        /**
         * If the fragment doesnt exits yet, create it by calling factory
         */
        public void setupFragment(int fragmentPosition, String fragmentTitle, FragmentCreator fragmentCreator) {
            if (getItem(fragmentPosition) == null) {
                addFragment(fragmentCreator.makeFragment(), fragmentPosition, fragmentTitle);
            }
        }

        interface FragmentCreator {
            Fragment makeFragment();
        }
    }
}