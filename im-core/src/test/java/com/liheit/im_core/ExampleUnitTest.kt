package com.liheit.im_core

import io.netty.buffer.ByteBufAllocator
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.FlowableSubscriber
import org.junit.Test
import org.reactivestreams.Subscription
import java.nio.charset.Charset

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    //    @Test
//    @Throws(Exception::class)
    fun addition_isCorrect() {

        Flowable.create(object : FlowableOnSubscribe<Int> {
            override fun subscribe(e: FlowableEmitter<Int>) {
                var index = 0
                while (index < 100) {
                    if (e.requested() > 10) {
                        e.onNext(index)
                    } else {
                        Thread.sleep(1000)
                    }
                    println("requested ${e.requested()}, emitter ${index}")
                    index = index + 1

                }
                e.onComplete()
            }
        }, BackpressureStrategy.BUFFER)
                .subscribe(object : FlowableSubscriber<Int> {
                    var subscriber: Subscription? = null
                    override fun onComplete() {

                    }

                    override fun onSubscribe(s: Subscription) {
                        subscriber = s
                        s.request(10)
                    }

                    override fun onNext(t: Int) {
                        println(t)
                        //subscriber?.request(10)
                    }

                    override fun onError(t: Throwable?) {
                    }
                })

        /*Observable.create<String> { emt ->
            var index: Int = 0
            when (index < 1000000) {
                emt.onNext("")
                        index = index +1
            }
            emt.onComplete()
        }.subscribe(object : Subscriber<String> {
            override fun onComplete() {

            }

            override fun onSubscribe(s: Subscription?) {
                s.request(2000)
            }

            override fun onNext(t: String?) {
            }

            override fun onError(t: Throwable?) {
            }


        })*/
    }

    @Test
    fun testDecode() {
        val b = byteArrayOf(-5, 0, 0, 22, 32, 1, 0, -10, 123, 34, 114, 101, 115, 117, 108, 116, 34, 58, 48, 44, 34, 118, 101, 114, 34, 58, 34, 34, 44, 34, 117, 114, 108, 34, 58, 34, 34, 44, 34, 117, 112, 103, 114, 97, 100, 101, 34, 58, 48, 44, 34, 105, 110, 102, 111, 34, 58, 34, 34, 44, 34, 115, 101, 114, 118, 101, 114, 115, 34, 58, 91, 123, 34, 97, 100, 100, 114, 34, 58, 34, 119, 119, 119, 46, 108, 105, 104, 101, 105, 116, 46, 99, 111, 109, 34, 44, 34, 112, 111, 114, 116, 34, 58, 51, 51, 51, 50, 125, 93, 44, 34, 116, 111, 107, 101, 110, 34, 58, 34, 107, 76, 119, 71, 76, 115, 47, 47, 115, 70, 57, 78, 73, 72, 119, 115, 82, 82, 72, 67, 51, 53, 49, 79, 107, 119, 119, 68, 121, 54, 117, 52, 90, 119, 76, 104, 79, 115, 74, 88, 57, 48, 117, 76, 50, 109, 48, 88, 85, 110, 99, 50, 82, 73, 113, 120, 65, 47, 119, 55, 57, 113, 84, 103, 34, 44, 34, 101, 120, 116, 114, 97, 34, 58, 34, 34, 44, 34, 117, 105, 100, 34, 58, 57, 48, 48, 48, 48, 57, 44, 34, 115, 101, 120, 34, 58, 116, 114, 117, 101, 44, 34, 99, 110, 97, 109, 101, 34, 58, 34, -28, -69, -93, -27, -117, -117, 34, 44, 34, 101, 110, 97, 109, 101, 34, 58, 34, 100, 97, 105, 120, 117, 110, 34, 125, 1, -2)
        val buf = ByteBufAllocator.DEFAULT.buffer(b.size)
        buf.writeBytes(b)
        println(buf.readUnsignedByte())
        println(buf.readUnsignedByte())
        println(buf.readUnsignedShort())
        println(buf.readUnsignedShort())
        var size=buf.readUnsignedShort()
        println(size)
        println(buf.readBytes(size).toString(Charset.defaultCharset()))
        println(buf.readUnsignedByte())
        println(buf.readUnsignedByte())
    }


}