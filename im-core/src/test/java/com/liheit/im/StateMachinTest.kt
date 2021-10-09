package com.liheit.im

import io.reactivex.Observable
import org.junit.Test

/**
 * Created by daixun on 2018/12/2.
 */

class StateMachineTest {
    @Test
    fun test() {

        /*val create = SingleSubject.create<String>()

        create.hide()
                .subscribe({
                    println("subscrib1 ${it}")
                }, {
                    println("subscrib1")
                    it.printStackTrace()
                })

        create.hide()
                .subscribe({
                    println("subscrib2 ${it}")
                }, {
                    println("subscrib2")
                    it.printStackTrace()
                })

        create.onError(RuntimeException("loginError"))

        Thread.sleep(1000)

        create.hide()
                .subscribe({
                    println("subscrib3 ${it}")
                }, {
                    println("subscrib3")
                    it.printStackTrace()
                })
        create.onError(RuntimeException("loginError"))*/

        Observable.create<Boolean> {emt->
            emt.onComplete()
        }.buffer(100).single(mutableListOf(true))
                .subscribe({
                    println(it)
                },{
                    it.printStackTrace()
                })
    }
}
