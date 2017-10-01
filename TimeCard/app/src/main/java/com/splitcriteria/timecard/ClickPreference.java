package com.splitcriteria.timecard;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

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