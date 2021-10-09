package com.liheit.im.common.ext

import com.liheit.im.common.rx.SimpleObserver
import com.liheit.im.core.bean.Resp
import com.liheit.im.core.http.ApiException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Created by daixun on 2018/3/15.
 * rxjava 的扩展函数
 */

/*
定义 被观察程序定义在io线程，观察者程序执行在androidmain线程
 */
fun <T> Observable<T>.scheduler(): Observable<T> {
    return this.subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread(), true, Observable.bufferSize())
}

/*
成功后执行的方法
 */
fun <T> Observable<T>.subscribeEx(success: (T) -> Unit): Disposable {
    val observer: SimpleObserver<T> = object : SimpleObserver<T>() {

        override fun onNext(data: T) {
            super.onNext(data)
            success.invoke(data)
        }
    }
    this.subscribe(observer)
    return observer.disposable
}
/*
带有错误信息返回的执行后的方法
 */
fun <T> Observable<T>.subscribeEx(success: (T) -> Unit, error: (String) -> Unit): Disposable {
    val observer: SimpleObserver<T> = object : SimpleObserver<T>() {
        override fun onNext(data: T) {
            super.onNext(data)
            success.invoke(data)
        }

        override fun onError(msg: String) {
            error.invoke(msg)
        }
    }
    this.subscribe(observer)
    return observer.disposable
}

fun <T : Resp> Observable<T>.check(): Observable<T> {
    return this.map {
        if (it.isSuccess()) {
            return@map it
        } else {
            throw ApiException(it.responseCode, "${it.responseMessage}", "method[${it.method}] ${it.timestamp}")
        }
    }
}

fun Disposable?.safeDispose() {
    if (this != null && !this.isDisposed) {
        this.dispose()
    }
}

fun Disposable.addTo(disposable: CompositeDisposable) {
    disposable.add(this)
}