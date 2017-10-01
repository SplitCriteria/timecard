package com.splitcriteria.timecard;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a generic Preference object which contains an OnClickListener interface
 * which is called when the preference is clicked
 */

class ClickPreference extends Preference {

    interface OnClickListener {
        void onClick(Preference preference);
    }

    private List<OnClickListener> mListeners = new ArrayList<>();

    public ClickPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClickPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickPreference(Context context) {
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
            listener.onClick(this);
        }
    }
}