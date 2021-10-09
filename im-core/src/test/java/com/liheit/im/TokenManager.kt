package com.liheit.im

import com.liheit.im.utils.forEachBlock
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import java.util.*

/**
 * Created by daixun on 2018/7/2.
 */

class TokenManager {

    @Test
    fun t(){

        val arrays = longArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        arrays.forEachBlock(100){
            println(Arrays.toString(it))
        }

    }

    @Test
    fun test() {
        for (x in 0..10) {
            getToken()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.newThread())
                    .subscribe({
                        println("结果" + it)
                    }, {

                    })
        }
        Thread.sleep(5000)
    }

    var tokens = mutableListOf<String>()
    var publishToken = PublishSubject.create<String>()
    var subjectMap = mutableMapOf<String, PublishSubject<String>>()

    init {
        publishToken
                .flatMap { req ->
                    synchronized(tokens) {
                        val subject = subjectMap.get(req)
                        if (tokens.size > 0) {
                            println("从缓存里获取token")
                            subject?.onNext(tokens.removeAt(0))
                            subject?.onComplete()
                        } else {
                            val list = Observable.create<MutableList<String>> {
                                println("请求tokens")
                                Thread.sleep(1000)
                                println("tokens 获取完毕")
                                it.onNext(mutableListOf<String>("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
                                it.onComplete()
                            }.blockingLast()
                            tokens.addAll(list)
                            subject?.onNext(tokens.removeAt(0))
                            subject?.onComplete()
                        }
                        return@flatMap Observable.just(req)
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
    }

    fun getToken(): Observable<String> {
        return Observable.create<String> {
            synchronized(tokens) {
                if (tokens.size > 0) {
                    println("从缓存里获取token")
                    it.onNext(tokens.removeAt(0))
                    it.onComplete()
                } else {
                    val list = Observable.create<MutableList<String>> {
                        println("请求tokens")
                        Thread.sleep(1000)
                        println("tokens 获取完毕")
                        it.onNext(mutableListOf<String>("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
                        it.onComplete()
                    }.blockingLast()
                    tokens.addAll(list)
                    it.onNext(tokens.removeAt(0))
                    it.onComplete()
                }
            }
        }
    }

    /*fun getToken(): Observable<String> {
        var key = UUID.randomUUID().toString()

        var subject = PublishSubject.create<String>()
        subjectMap.put(key, subject)

        return subject.doOnSubscribe {
            publishToken.onNext(key)
        }
    }*/
}
