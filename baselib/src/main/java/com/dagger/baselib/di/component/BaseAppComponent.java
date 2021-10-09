package com.dagger.baselib.di.component;

import android.app.Application;

import com.dagger.baselib.di.BaseLibComponentManager;
import com.dagger.baselib.di.module.BaseActivitysModule;
import com.dagger.baselib.di.module.BaseAppModule;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

/**
 * Created by sunhapper on 2018/9/19 .
 */
@Component(modules = {
        BaseActivitysModule.class,
        AndroidInjectionModule.class,
        BaseAppModule.class,
        AndroidSupportInjectionModule.class})
@Singleton
public interface BaseAppComponent extends AndroidInjector<BaseLibComponentManager> {
    Application application();
    Integer versionCode();
}
