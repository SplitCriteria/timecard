package com.splitcriteria.timecard;

import android.content.Context;

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
 * Provides a customized PersistentSemaphore which specifically targets
 * Timecard database and the clients which exclusively use it (i.e. RestoreService,
 * BackupService, and ProjectData)
 */

class DatabaseLock {

    private static final String KEY = "database.semaphore";

    static final String RESTORE = "restore";
    static final String BACKUP = "backup";
    static final String DATABASE = "database";

    static boolean acquire(Context context, String lockSource) {
        return new PersistentSemaphore(context).acquire(KEY, lockSource);
    }

    static boolean release(Context context, String lockSource) {
        return new PersistentSemaphore(context).release(KEY, lockSource);
    }

    static boolean forceRelease(Context context) {
        return new PersistentSemaphore(context).forceRelease(KEY);
    }
}