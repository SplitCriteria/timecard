package com.splitcriteria.timecard;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

/**
 * Created by Deuce on 9/30/17.
 */

class PersistentSemaphore {

    private WeakReference<Context> mContext;

    PersistentSemaphore(Context context) {
        mContext = new WeakReference<>(context);
    }

    /**
     * Returns the number of permits the ID has acquired from the "key" semaphore
     *
     * @param key   a key to represent a specific semaphore
     * @param id    the id to check
     * @return  -1 if the unable to acquire permits, 0 if no permits are acquired but are
     *          available, or an integer > 0 showing the number of permits acquired by "id"
     */
    synchronized int getPermitsAcquired(String key, String id) {
        // Get the Context
        Context context = mContext.get();
        if (context == null) {
            return -1;
        }
        try {
            // Read the ID from the semaphore
            InputStream in = context.openFileInput(key);
            byte[] buffer = new byte[4096];
            int bytesRead = in.read(buffer);
            in.close();
            // Read in, and split, then id and permit count (separated by a comma)
            String[] idAndPermitCount = new String(buffer, 0, bytesRead, "UTF-8").split(",");
            if (idAndPermitCount[0].equals(id)) {
                return Integer.parseInt(idAndPermitCount[1]);
            } else {
                return -1;
            }
        } catch (FileNotFoundException e) {
            return 0;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Acquires a permit from this semaphore. This method will return immediately
     *
     * @param key   the unique semaphore key
     * @param id    the ID attempting to acquire a permit
     * @return  true, if a permit was acquired
     */
    synchronized boolean acquire(String key, String id) {
        // Get the Context
        Context context = mContext.get();
        if (context == null) {
            return false;
        }
        // Get the number of permits acquired by for this key by the given ID
        int permitsAcquired = getPermitsAcquired(key, id);
        // If able to acquire a permit (i.e. not equal to -1) then increase the number of permits
        return permitsAcquired != -1  && writeToSemaphore(context, key, id, permitsAcquired + 1);
    }

    /**
     * Release a permit from this semaphore.
     *
     * @param key   a unique semaphore key
     * @param id    the ID attempting to release a permit
     * @return  true, if the permit was released
     */
    synchronized boolean release(String key, String id) {
        // Get the Context
        Context context = mContext.get();
        if (context == null) {
            return false;
        }
        // Get a reference to the semaphore file
        File filesDir = context.getFilesDir();
        File semaphore = new File(filesDir, key);
        int permitsAcquired = getPermitsAcquired(key, id);
        if (permitsAcquired == -1) {
            // No permits acquired for this semaphore, or unable to acquire
            return false;
        } else if (permitsAcquired == 0) {
            // No permits acquired, but they're available -- return success
            return true;
        } else if (permitsAcquired == 1) {
            // If there's only one permit acquire, then delete the semaphore file
            return semaphore.delete();
        } else {
            // Otherwise, decrease the number of permits acquired
            return writeToSemaphore(context, key, id, permitsAcquired - 1);
        }
    }

    /**
     * Writes an ID and the number of acquired permits to a persistent semaphore file
     *
     * @param context   a non-Null Context
     * @param key       a unique semaphore name
     * @param id        the ID which acquired the permits
     * @param permits   the number of permits given to ID
     * @return  true, if the write to the file was successful
     */
    private synchronized boolean writeToSemaphore(Context context, String key,
                                                  String id, int permits) {
        try {
            OutputStream out = context.openFileOutput(key, Context.MODE_PRIVATE);
            String reservation = id + "," + Integer.toString(permits);
            out.write(reservation.getBytes("UTF-8"));
            out.close();
            return true;
        } catch (IOException ioError) {
            ioError.printStackTrace();
            return false;
        }
    }

    /**
     * Forces the release of the semaphore represented by "key"
     *
     * @param key   a unique semaphore key
     * @return  true, if the semaphore was released (i.e. persistent file deleted)
     */
    synchronized boolean forceRelease(String key) {
        // Get the Context
        Context context = mContext.get();
        if (context == null) {
            return false;
        }
        // Get a reference to the semaphore file
        File filesDir = context.getFilesDir();
        File semaphore = new File(filesDir, key);
        // Delete it to release any lock to the given key
        return semaphore.delete();
    }

}