package com.liheit.im.common.emoji;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;

public class EmojiCache {
    //caceh里面默认只存放32个表情
    private static final int EMOJI_CACHE_SIZE = 32;
    private static EmojiCache _instance;
    private LruCache<String, Drawable> mCache;

    public EmojiCache(int cacheSize) {
        mCache = new LruCache<String, Drawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, Drawable value) {
                return 1;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Drawable oldValue, Drawable newValue) {
                //这种情况，可能该drawable还在页面使用中，不能随便recycle。这里解除引用即可，gc会自动清除
//            	if (oldValue instanceof BitmapDrawable) {
//					((BitmapDrawable)oldValue).getBitmap().recycle();
//				}
            }
        };
    }

    public static void createInstance(int cacheSize) {
        if (_instance == null) {
            _instance = new EmojiCache(cacheSize);
        }
    }

    public static EmojiCache getInstance() {
        if (_instance == null) {
            createInstance(EMOJI_CACHE_SIZE);
        }

        return _instance;
    }

    public Drawable getDrawable(Context context, int resourceId,int size) {
        String key=resourceId+"#"+size;
        Drawable drawable = mCache.get(key);
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, resourceId).mutate();
            mCache.put(key, drawable);
        }

        return drawable;
    }
}