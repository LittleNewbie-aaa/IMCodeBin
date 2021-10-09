package com.liheit.im.common.ext

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.dagger.baselib.utils.ToastUtils
import com.liheit.im.common.rx.SimpleObserver
import com.liheit.im.core.bean.Resource
import com.liheit.im.core.bean.Status
import io.reactivex.Observable

/**
 * Created by daixun on 2018/3/16.
 */

fun <T> LiveData<Resource<T>>.observeEx(ovner: LifecycleOwner, onSuccess: (T) -> Unit, onError: (String) -> Unit = { ToastUtils.showToast(it) }) {
    this.observe(ovner, android.arch.lifecycle.Observer {
        it?.let {
            when (it.status) {
                Status.SUCCESS -> it.data?.let { onSuccess.invoke(it) }
                Status.ERROR -> onError.invoke(it.message)
                else -> {
                }
            }
        }
    })
}

fun <T> LiveData<Resource<T>>.observeEx(ovner: LifecycleOwner, onSuccess: (T) -> Unit) {
    this.observeEx(ovner, onSuccess = onSuccess, onError = { ToastUtils.showToast(it) })
}

fun <T> LiveData<T>.onNext(ovner: LifecycleOwner, onNext: (T) -> Unit) {
    this.observe(ovner, android.arch.lifecycle.Observer {
        it?.let { onNext.invoke(it) }
    })
}

fun <T> LiveData<T>.onNextOrNull(ovner: LifecycleOwner, onNext: (T?) -> Unit) {
    this.observe(ovner, android.arch.lifecycle.Observer {
        onNext.invoke(it)
    })
}


fun <T> Observable<T>.bindLiveData(data: MutableLiveData<Resource<T>>) {
    this.doOnSubscribe { data.value = Resource.loading(null) }
            .subscribe(object : SimpleObserver<T>() {
                override fun onNext(value: T) {
                    super.onNext(value)
                    data.value = Resource.success(value)
                }

                override fun onError(msg: String?) {
                    data.value = Resource.error(msg + "")
                }

            })
}

fun <T> LiveData<Resource<T>>.onLoading(ovner: LifecycleOwner, onLoading: (T?) -> Unit) {
    this.observe(ovner, android.arch.lifecycle.Observer {
        it?.let {
            if (it.status == Status.LOADING) {
                onLoading.invoke(it.data)
            }
        }
    })
}

fun <T> LiveData<Resource<T>>.onFinal(ovner: LifecycleOwner, onFinal: (T?) -> Unit) {
    this.observe(ovner, android.arch.lifecycle.Observer {
        it?.let {
            if (it.status == Status.SUCCESS || it.status == Status.ERROR) {
                onFinal.invoke(it.data)
            }
        }
    })
}