package com.liheit.im.common.glide;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStream;

/**
 * Created by daixun on 17-4-22.
 */
@GlideModule
public class MyGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        RequestOptions options = RequestOptions.formatOf(DecodeFormat.PREFER_ARGB_8888);
        options.diskCacheStrategy(DiskCacheStrategy.ALL);
        builder.setDefaultRequestOptions(options);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
        registry.prepend(ImImageModelLoader.ImImage.class, InputStream.class, new ImImageModelLoader.ImImageModelLoaderFactory());
        registry.prepend(AccountInfo.class, InputStream.class, new AccountImageModelLoader.AccountImageModelLoaderFactory());
        registry.prepend(SessionIconModelLoader.SessionImage.class, InputStream.class, new SessionIconModelLoader.SessionImageModelLoaderFactory());
    }
}
