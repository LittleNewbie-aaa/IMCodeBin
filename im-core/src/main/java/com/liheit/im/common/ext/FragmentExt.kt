package com.liheit.im.common.ext

import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager

/**
 * Created by daixun on 2018/3/22.
 */
fun FragmentManager.addMultipleFragment(containerId: Int, showPosition: Int, vararg toFragments: Fragment) {
    this.beginTransaction()?.apply {
        toFragments.forEachIndexed { index, fragment ->
            add(containerId, fragment)
            if (index != showPosition) {
                hide(fragment)
            }
        }
        commitNowAllowingStateLoss()
    }
}

fun FragmentManager.showFragment(toFragment: Fragment) {
    this.beginTransaction()?.apply {
        show(toFragment)
        commitNowAllowingStateLoss()
    }
}

fun FragmentManager.showHideFragment(@IdRes containerId: Int, toFragment: Fragment) {
    this.beginTransaction()?.apply {
        fragments.forEach { frag ->
            if (frag == toFragment) {
                show(frag)
            } else if (frag.id == containerId) {
                hide(frag)
            }
        }
        commitNowAllowingStateLoss()
    }
}

fun FragmentManager.showHideFragment(showFragment: Fragment, hideFragment: Fragment) {
    beginTransaction()?.apply {
        hide(hideFragment)
        show(showFragment)
        commitNowAllowingStateLoss()
    }
}

fun FragmentManager.addFragment(containerId: Int, toFragment: Fragment, addToBackStack: Boolean = false, hideBrother: Boolean = false) {
    this.beginTransaction()?.apply {
        if (hideBrother) {
            fragments.forEach {
                if (it.id == containerId) {
                    hide(it)
                }
            }
        }
        add(containerId, toFragment)
        if (addToBackStack) {
            addToBackStack(toFragment.javaClass.name)
            commitAllowingStateLoss()
        }else {
            commitNowAllowingStateLoss()
        }
    }
}

fun FragmentManager.replaceFragment(containerId: Int, toFragment: Fragment) {
    beginTransaction()?.apply {
        replace(containerId, toFragment)
        commitNowAllowingStateLoss()
    }
}

fun <T : Fragment> FragmentManager.findFragment(fragmentClass: Class<T>): T? {
    val fragments = this.fragments
    for (fragment in fragments) {
        if (fragment.javaClass.name == fragmentClass.name) {
            return fragment as T
        }
    }
    return null
}

/**
 * 取名恐惧症，有好名字帮我改了
 * 如果该Fragment存在，就显示他，隐藏其他fgramgnt 不然就创建这个fragment，replace其他fragment
 */
fun <T : Fragment> FragmentManager.replaceOrShow(containerId: Int, fragmentClass: Class<T>, factory: () -> Fragment) {
    var fragment = findFragment(fragmentClass)
    if (fragment == null) {
        replaceFragment(containerId, factory.invoke())
    } else {
        showHideFragment(containerId, fragment)
    }
}

/**
 * 如果该Fragment不存在则添加，否则直接show
 */
fun <T : Fragment> FragmentManager.showOrAdd(containerId: Int, fragmentClass: Class<T>, factory: () -> T, hideBrother: Boolean = true) {
    var fragment = findFragment(fragmentClass)
    if (fragment == null) {
        addFragment(containerId, factory.invoke(), false, true)
    } else {
        showHideFragment(containerId, fragment)
    }
}