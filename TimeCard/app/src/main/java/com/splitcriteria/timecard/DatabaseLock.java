package com.splitcriteria.timecard;

import android.content.Context;

/**
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