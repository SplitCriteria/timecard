package com.splitcriteria.timecard;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;

/**
 * A ResultFragment allows the client to attach an OnResultListener which child
 * fragments should call when a result if available. A result is returned with
 * a set request code and a bundle of extras which can be retrieved by calling
 * Intent.getBundleExtra(EXTRA_BUNDLE)
 */

public abstract class ResultFragment extends DialogFragment {

    static final String EXTRA_BUNDLE = "com.splitcriteria.timecard.EXTRA_BUNDLE";

    private int mRequestCode = -1;
    private Bundle mExtras;
    private OnResultListener mListener;

    interface OnResultListener {
        void onResult(int requestCode, int resultCode, Intent intent);
    }

    ResultFragment setOnResultListener(OnResultListener listener) {
        mListener = listener;
        return this;
    }

    ResultFragment putExtras(Bundle extras) {
        mExtras = extras;
        // In case setArguments has already been called, go ahead and re-set the extras
        Bundle args = getArguments();
        if (args != null) {
            args.putBundle(EXTRA_BUNDLE, mExtras);
        }
        return this;
    }

    ResultFragment setRequestCode(int requestCode) {
        mRequestCode = requestCode;
        return this;
    }

    @Override
    public void setArguments(Bundle args) {
        // Add the extra bundle to arguments to save it across destroy/creation cycles
        args.putBundle(EXTRA_BUNDLE, mExtras);
        super.setArguments(args);
    }

    /**
     * Returns a result to a OnDialogResultListener which includes a result
     *
     * @param resultCode    the result code to return
     * @param intent        an Intent to return, or null
     * @return  true, if the listener existed and was called; false otherwise
     */
    protected boolean returnResult(int resultCode, Intent intent) {
        if (intent == null) {
            intent = new Intent();
        }
        Bundle extras = getArguments().getBundle(EXTRA_BUNDLE);
        intent.putExtra(EXTRA_BUNDLE, extras);
        if (mListener == null) {
            return false;
        } else {
            mListener.onResult(mRequestCode, resultCode, intent);
            return true;
        }
    }
}
