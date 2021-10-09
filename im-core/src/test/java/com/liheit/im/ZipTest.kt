package com.liheit.im

import com.liheit.im.utils.ZipUtils
import org.junit.Test

/**
 * Created by daixun on 2018/12/7.
 */

class ZipTest {

    @Test
    fun testPassword() {
        println(ZipUtils.calculateZipPwd("abDFHlQ{]4J~x}6"))

    }

    enum class T(val value: Int) {
        A(1), C(3), D(4)
    }

    @Test
    fun testEnum() {
        println(T.A.value)
        println(T.C.value)
        println(T.D.value)

        println(T.valueOf("A").value)

    }
}
