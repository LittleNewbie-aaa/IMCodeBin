package com.dagger.baselib.di;

import android.app.Activity;
import android.app.Application;

import com.dagger.baselib.di.component.BaseAppComponent;
import com.dagger.baselib.di.component.DaggerBaseAppComponent;
import com.dagger.baselib.di.module.BaseAppModule;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;

/**
 * Created by sunhapper on 2018/9/22 .
 */
public class BaseLibComponentManager {
    @Inject
    DispatchingAndroidInjector<Activity> activityInjector;

    Application mApplication;
    private BaseAppComponent mBaseAppComponent;

    private BaseLibComponentManager() {

    }

    private static BaseLibComponentManager singleton = null;

    public static BaseLibComponentManager getInstance() {
        if (singleton == null) {
            synchronized (BaseLibComponentManager.class) {
                if (singleton == null) {
                    singleton = new BaseLibComponentManager();
                }
            }
        }
        return singleton;
    }

    public BaseLibComponentManager init(Application application) {
        mApplication = application;
        mBaseAppComponent = DaggerBaseAppComponent
                .builder()
                .baseAppModule(new BaseAppModule(mApplication))
                .build();
        mBaseAppComponent.inject(this);
        return this;
    }

    public BaseAppComponent getBaseAppComponent() {
        return mBaseAppComponent;
    }

    public void inject(Activity activity){
        activityInjector.inject(activity);
    }
}
