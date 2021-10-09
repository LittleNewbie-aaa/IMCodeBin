package com.liheit.im.utils

import io.reactivex.subjects.PublishSubject
import org.junit.Test

/**
 * Created by daixun on 2018/6/25.
 */
class IDGeneratorTest {
    @Test
    fun createSingleChatId() {


        /*val c1 = Completable.create {
            Thread.sleep(3000)
            println("onCompletable 1")
            it.onComplete()
        }.subscribeOn(Schedulers.io())

        val c2 = Completable.create {
            Thread.sleep(2000)
            println("onCompletable 2")
            it.onComplete()
        }.subscribeOn(Schedulers.io())

        val c3 = Completable.create {
            Thread.sleep(1000)
            println("onCompletable 3")
            it.onComplete()
        }.subscribeOn(Schedulers.io())
        Completable.merge(listOf(c1, c2, c3))
                .subscribeOn(Schedulers.io())
                .subscribe({
                    println("onSuccess")
                })*/


        /*var publish = PublishSubject.create<Boolean>()
        Observable.create<Boolean> {
            Thread.sleep(1000)
            it.onNext(true)
            it.onComplete()
        }
                .takeUntil(publish.filter { !it })
                .lastOrError()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    println(it)
                }, {
                    it.printStackTrace()
                })
        Thread.sleep(2000)
        publish.onNext(false)

        Thread.sleep(3000)*/

        var publish = PublishSubject.create<Long>()

        publish.subscribe {
            println(it)
        }
        for (i in 0..Long.MAX_VALUE){
            publish.onNext(i)
        }
    }

}