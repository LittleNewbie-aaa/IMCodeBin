package com.liheit.im.core

import com.liheit.im.core.IMClient.extractIp
import com.liheit.im.core.protocol.AccessReq
import com.google.gson.Gson
import io.reactivex.Single
import org.junit.Test

class IMNettyTest {
    @Test
    fun testPressure() {

        IMNetty.isDebug = true
        var password = "/3DFQ4eueHbDNKIhsMF5gg=="//123456
        IMNetty.instance().access("193.112.5.114", 3331, Gson().toJson(AccessReq(
                account = "tgaozairui",
                psw = password,
                ver = "0.1.39"
        ))).flatMap { access ->
            Single.create<Boolean> { resp ->
                val conn = access.servers!!.find { addr ->
                    var ip = extractIp(addr.addr)
                    IMNetty.instance().stop()
                    IMNetty.instance().setMessageHandler(null)
                    println("login to ${addr.addr}:${addr.port}")
                    IMNetty.instance().connect(ip, addr.port)
                }
                if (conn != null) {
                    println("连接成功")
                    resp.onSuccess(true)
                } else {
                    resp.onError(RuntimeException("连接失败"))
                }
            }

        }
                .subscribe({
                    println(it)
                }, {
                    it.printStackTrace()
                })
    }
}