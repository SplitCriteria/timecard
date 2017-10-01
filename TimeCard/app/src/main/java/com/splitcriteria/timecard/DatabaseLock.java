package com.splitcriteria.timecard;

import android.content.Context;

/**
 * Created by Deuce on 9/30/17.
 */

public class DatabaseLock {

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