package com.splitcriteria.timecard;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

/**
 * Created by Deuce on 9/30/17.
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

    public void setUri(Uri uri) {
        persistString(uri.toString());
    }

    public Uri getUri() {
        return Uri.parse(getPersistedString(""));
    }
}
