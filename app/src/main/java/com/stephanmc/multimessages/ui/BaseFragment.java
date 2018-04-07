package com.stephanmc.multimessages.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;

/**
 * Our Base fragment for any Multi Message fragments.
 * If helps handling attach and detach lifecycle methods.
 */
public class BaseFragment extends Fragment {

    protected FragmentActivity mActivity;
    protected IActivityEnabledListener mListener;

    /**
     * Utility method in this base fragment, to help executing methods when activity is available
     */
    protected void addOnActivityAvailableListener(IActivityEnabledListener listener) {
        if (getActivity() == null) {
            mListener = listener;

        } else {
            listener.onActivityEnabled(getActivity());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentActivity) {
            mActivity = (FragmentActivity) context;
        }

        if (mListener != null && context instanceof AppCompatActivity) {
            mListener.onActivityEnabled((AppCompatActivity) context);
            mListener = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentActivity) {
            mActivity = (FragmentActivity) activity;
        }

        if (mListener != null && activity instanceof FragmentActivity) {
            mListener.onActivityEnabled((FragmentActivity) activity);
            mListener = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActivity = null;
        mListener = null;
    }

    protected interface IActivityEnabledListener {

        void onActivityEnabled(FragmentActivity activity);
    }
}
