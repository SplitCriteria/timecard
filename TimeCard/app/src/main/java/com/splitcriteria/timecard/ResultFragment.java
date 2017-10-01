package com.splitcriteria.timecard;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;

/**
 * Timecard - Allows users to easily track time-based data for analysis.
 * Copyright (C) 2017  Nicholas Johnson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Timecard - Copyright (C) 2017  Nicholas Johnson
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
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
