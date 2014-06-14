package com.kenny.utils;

import android.graphics.Bitmap;
import android.util.LruCache;

public class MemoryCache {
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * Stores bitmaps in memory for caching
     *
     * @param cacheSize size which we will allow our memory to take up. The number we pass is what the available memory gets divided by. For example,
     *                  if cacheSize is 4, we will use 1/4 of the available memory, 8 will be 1/8 on so on
     */
    public MemoryCache(int cacheSize) {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //Don't allow anything less than zero
        if (cacheSize <= 0) {
            cacheSize = 8;
        }
        final int allowance = maxMemory / cacheSize;
        mMemoryCache = new LruCache<String, Bitmap>(allowance) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    /**
     * Adds bitmap to our cache
     *
     * @param key    value which the bitmap will be associated to
     * @param bitmap the bitmap that is being stored
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * Get the bitmap from the associated key
     *
     * @param key value which the bitmap is associated to
     * @return Bitmap, or null if not present in cache
     */
    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * Clear out the cache so the system can reclaim the RAM
     */
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }
    }
}
