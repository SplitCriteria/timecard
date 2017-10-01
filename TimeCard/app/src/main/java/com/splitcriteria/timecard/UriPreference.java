package com.splitcriteria.timecard;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

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
