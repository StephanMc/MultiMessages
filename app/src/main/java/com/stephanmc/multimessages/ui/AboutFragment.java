package com.stephanmc.multimessages.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.stephanmc.multimessages.R;
import com.stephanmc.multimessages.util.DeviceInfo;


public class AboutFragment extends BaseFragment {

    private static final String DONATION_URL = "http://bit.ly/donate-stephanmc";
    private static final String SOURCE_URL = "https://github.com/stephanmc/MultiMessages";
    private static final String MAIL_DEVELOPER = "stephan.kouadio@gmail.com";

    private static final String TAG = AboutFragment.class.getSimpleName();

    private final View.OnClickListener mOnReportBugListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendMailToDeveloper();
        }
    };
    private final View.OnClickListener mOnSourceCodeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openBrowser(SOURCE_URL);
        }
    };
    private final View.OnClickListener mOnDonateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openBrowser(DONATION_URL);

            Toast.makeText(getContext(), R.string.thank_you_for_donate, Toast.LENGTH_SHORT).show();
        }
    };
    private final View.OnClickListener mOnLicensesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.btn_licenses));
            startActivity(new Intent(mActivity, OssLicensesMenuActivity.class));
        }
    };

    public AboutFragment() {
    }

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    private void sendMailToDeveloper() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + MAIL_DEVELOPER));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[MultiMessages] Bug report / Suggestion");
        //@formatter:off
        emailIntent.putExtra(Intent.EXTRA_TEXT,
                "Hello,\n\n"
                + "\n\n"
                + "-----------------------------------------------------------\n"
                + "Device infos:"
                + DeviceInfo.getInfosAboutDevice(mActivity) + "\n"
                + "-----------------------------------------------------------\n"
        );
        //@formatter:on
        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_mail_using)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(mActivity, R.string.no_email_clients, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calls system default browser (if any) with the given URL.
     */
    private void openBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View aboutFragment = inflater.inflate(R.layout.fragment_about, container, false);

        Button donateBtn = aboutFragment.findViewById(R.id.btn_donation);
        donateBtn.setOnClickListener(mOnDonateListener);

        Button reportBugBtn = aboutFragment.findViewById(R.id.btn_report_bug);
        reportBugBtn.setOnClickListener(mOnReportBugListener);

        Button sourceCodeBtn = aboutFragment.findViewById(R.id.btn_get_source);
        sourceCodeBtn.setOnClickListener(mOnSourceCodeListener);

        Button licensesBtn = aboutFragment.findViewById(R.id.btn_get_licenses);
        licensesBtn.setOnClickListener(mOnLicensesListener);

        TextView appNameVersion = aboutFragment.findViewById(R.id.txt_app_name_and_version);
        String appName = getString(R.string.app_name);
        String appVersion;
        try {
            appVersion = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName;
        } catch (Exception e) {
            appVersion = "";
            Log.e(TAG, e.getMessage(), e);
        }

        appNameVersion.setText(getString(R.string.app_name_and_version, appName, appVersion));
        return aboutFragment;
    }

}
