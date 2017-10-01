package com.splitcriteria.timecard;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

/**
 * A simple implementation of ClickPreference which allows a client to
 * persist a Uri (held in a String)
 */

public class UriPreference extends ClickPreference {

    public UriPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UriPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UriPreference(Context context) {
        super(context);
    }

    void setUri(Uri uri) {
        persistString(uri.toString());
    }

    Uri getUri() {
        return Uri.parse(getPersistedString(""));
    }
}
