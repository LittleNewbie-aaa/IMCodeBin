package com.dagger.baselib.base;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;


import com.blankj.utilcode.util.Utils;
import com.dagger.baselib.R;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshFooterCreator;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator;
import com.sjtu.yifei.IBridge;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasBroadcastReceiverInjector;
import dagger.android.HasContentProviderInjector;
import dagger.android.HasFragmentInjector;
import dagger.android.HasServiceInjector;
import dagger.android.support.HasSupportFragmentInjector;

/**
 * Created by daixun on 17-8-13.
 */

public abstract class BaseApplication extends Application implements HasActivityInjector,
        HasFragmentInjector,
        HasServiceInjector,
        HasBroadcastReceiverInjector,
        HasContentProviderInjector, HasSupportFragmentInjector {
    @Inject
    DispatchingAndroidInjector<Fragment> supportFragmentInjector;
    @Inject
    DispatchingAndroidInjector<Activity> activityInjector;
    @Inject
    DispatchingAndroidInjector<BroadcastReceiver> broadcastReceiverInjector;
    @Inject
    DispatchingAndroidInjector<android.app.Fragment> fragmentInjector;
    @Inject
    DispatchingAndroidInjector<Service> serviceInjector;
    @Inject
    DispatchingAndroidInjector<ContentProvider> contentProviderInjector;
    private volatile boolean needToInject = true;

    private List<Activity> oList;//???????????????????????????Activity?????????

    @Override
    public void onCreate() {
        super.onCreate();

        oList = new ArrayList<Activity>();

        injectIfNecessary();

        SmartRefreshLayout.setDefaultRefreshHeaderCreator(new DefaultRefreshHeaderCreator() {
            @NonNull
            @Override
            public RefreshHeader createRefreshHeader(@NonNull Context context, @NonNull RefreshLayout layout) {
                //???????????????????????????????????????????????????????????? DefaultRefreshInitializer ????????????????????????ClassicsHeader?????????
                layout.setEnableHeaderTranslationContent(true);
                return new MaterialHeader(context)
                        .setColorSchemeResources(R.color.colorScheme1, R.color.colorScheme2, R.color.colorScheme3);
            }
        });
        SmartRefreshLayout.setDefaultRefreshFooterCreator(new DefaultRefreshFooterCreator() {
            @NonNull
            @Override
            public RefreshFooter createRefreshFooter(@NonNull Context context, @NonNull RefreshLayout layout) {
                return new ClassicsFooter(context);
            }
        });

        Utils.init(this);
    }


    protected abstract AndroidInjector<? extends BaseApplication> applicationInjector();

    @Override
    public DispatchingAndroidInjector<Fragment> supportFragmentInjector() {
        return supportFragmentInjector;
    }

    /**
     * Lazily injects the {@link dagger.android.DaggerApplication}'s members. Injection cannot be performed in {@link
     * Application#onCreate()} since {@link ContentProvider}s' {@link
     * ContentProvider#onCreate() onCreate()} method will be called first and might
     * need injected members on the application. Injection is not performed in the the constructor, as
     * that may result in members-injection methods being called before the constructor has completed,
     * allowing for a partially-constructed instance to escape.
     */
    private void injectIfNecessary() {
        if (needToInject) {
            synchronized (this) {
                if (needToInject) {
                    @SuppressWarnings("unchecked")
                    AndroidInjector<BaseApplication> applicationInjector = (AndroidInjector<BaseApplication>) applicationInjector();
                    applicationInjector.inject(this);
                    if (needToInject) {
                        throw new IllegalStateException(
                                "The AndroidInjector returned from applicationInjector() did not inject the "
                                        + "DaggerApplication");
                    }
                }
            }
        }
    }

    @Inject
    void setInjected() {
        needToInject = false;
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return activityInjector;
    }

    @Override
    public DispatchingAndroidInjector<android.app.Fragment> fragmentInjector() {
        return fragmentInjector;
    }

    @Override
    public DispatchingAndroidInjector<BroadcastReceiver> broadcastReceiverInjector() {
        return broadcastReceiverInjector;
    }

    @Override
    public DispatchingAndroidInjector<Service> serviceInjector() {
        return serviceInjector;
    }

    // injectIfNecessary is called here but not on the other *Injector() methods because it is the
    // only one that should be called (in AndroidInjection.inject(ContentProvider)) before
    // Application.onCreate()
    @Override
    public AndroidInjector<ContentProvider> contentProviderInjector() {
        injectIfNecessary();
        return contentProviderInjector;
    }

    /**
     * ??????Activity
     */
    public void addActivity_(Activity activity) {
        // ?????????????????????????????????Activity
        if (!oList.contains(activity)) {
            oList.add(activity);//?????????Activity??????????????????
        }
    }

    /**
     * ????????????Activity
     */
    public void removeActivity_(Activity activity) {
        //??????????????????????????????Activity
        if (oList.contains(activity)) {
            oList.remove(activity);//??????????????????
            activity.finish();//????????????Activity
        }
    }

    /**
     * ???????????????Activity
     */
    public void removeALLActivity_() {
        //????????????????????????????????????Activity??????
        for (Activity activity : oList) {
            activity.finish();
        }
    }
}
