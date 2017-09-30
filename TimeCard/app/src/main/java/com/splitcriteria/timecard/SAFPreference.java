package com.splitcriteria.timecard;

import android.content.Context;
import android.net.Uri;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Deuce on 9/29/17.
 */

class SAFPreference extends Preference {

    interface OnClickListener {
        void onClick();
    }

    private List<OnClickListener> mListeners = new ArrayList<>();

    public SAFPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SAFPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SAFPreference(Context context) {
        super(context);
    }

    void addOnClickListener(OnClickListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    void removeOnClickListener(OnClickListener listener) {
        mListeners.remove(listener);
    }

    @Override
    protected void onClick() {
        super.onClick();
        for (OnClickListener listener : mListeners) {
            listener.onClick();
        }
    }

    void setUri(Uri uri) {
        persistString(uri.toString());
    }

    Uri getUri() {
        String uriString = getPersistedString(null);
        return !TextUtils.isEmpty(uriString) ? Uri.parse(uriString) : null;
    }
}