package com.dagger.baselib.base;

import android.app.Activity;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.dagger.baselib.R;
import com.dagger.baselib.di.ViewModelFactory;
import com.gyf.barlibrary.ImmersionBar;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasFragmentInjector;
import dagger.android.support.HasSupportFragmentInjector;

public abstract class MainBaseActivity extends AppCompatActivity implements HasFragmentInjector, HasSupportFragmentInjector {
    @Inject
    public ViewModelFactory mModelFactory;
    @Inject
    DispatchingAndroidInjector<Fragment> supportFragmentInjector;
    @Inject
    DispatchingAndroidInjector<android.app.Fragment> frameworkFragmentInjector;

    protected int activityCloseEnterAnimation;

    protected int activityCloseExitAnimation;

    protected ImmersionBar mImmersionBar;

    public Activity activity = null;

    private BaseApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        initCloseAnimation();
        if (isImmersionBarEnabled()) {
            mImmersionBar = ImmersionBar.with(this);
            mImmersionBar.statusBarColor(R.color.toolbarBgColor);
            mImmersionBar.init();
        }

        if (application == null) {
            // 得到Application对象
            application = (BaseApplication) getApplication();
        }
        activity = this;
        addActivity();
    }

    private void initCloseAnimation() {
        TypedArray activityStyle = getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowAnimationStyle});
        int windowAnimationStyleResId = activityStyle.getResourceId(0, 0);
        activityStyle.recycle();
        activityStyle = getTheme().obtainStyledAttributes(windowAnimationStyleResId, new int[]{android.R.attr.activityCloseEnterAnimation, android.R.attr.activityCloseExitAnimation});
        activityCloseEnterAnimation = activityStyle.getResourceId(0, 0);
        activityCloseExitAnimation = activityStyle.getResourceId(1, 0);
        activityStyle.recycle();
    }

    protected boolean isImmersionBarEnabled() {
        return true;
    }

    // 添加Activity方法
    public void addActivity() {
        application.addActivity_(activity);// 调用myApplication的添加Activity方法
    }

    //销毁所有Activity方法
    public void removeALLActivity() {
        application.removeALLActivity_();// 调用myApplication的销毁所有Activity方法
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return supportFragmentInjector;
    }

    @Override
    public AndroidInjector<android.app.Fragment> fragmentInjector() {
        return frameworkFragmentInjector;
    }

    public <T extends ViewModel> T getViewModel(Class<T> tClass) {
        return ViewModelProviders.of(this).get(tClass);
    }

    public <T extends ViewModel> T getViewModel(Class<T> tClass, ViewModelFactory factory) {
        return ViewModelProviders.of(this, factory).get(tClass);
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(activityCloseEnterAnimation, activityCloseExitAnimation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImmersionBar != null)
            mImmersionBar.destroy();
        application.removeActivity_(activity);
    }
}
