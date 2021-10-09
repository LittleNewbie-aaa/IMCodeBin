package com.liheit.im

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Test


/**
 * Created by daixun on 2018/7/5.
 */

class GsonTest {


    /*@Test
    fun testGson() {
//        print(9.and(0b1000))
//        print(8.and(0b1000).shr(3))
//

        val reg = "(?:@[\u4e00-\u9fa5_a-zA-Z0-9]{1,}\\s|\\[(.[^\\[]*)\\])"
//        var pattern = Pattern.compile("@[\u4e00-\u9fa5_a-zA-Z0-9]{1,}\\s");
        var pattern = Pattern.compile(reg);
        var content="d@老大 sfadsfas@dsadsfasf @张三@李四 @dsfasdf [wx]dsf[sdfsdf]  @@王麻子 "
        var matcher = pattern.matcher(content);
        var processStart = 0
        while (matcher.find()) {
            if (matcher.start() != processStart) {
                val item = content.substring(processStart, matcher.start())
                println(item)
            }
            val matchText = matcher.group()
            if(matchText.startsWith("@")){
                println(matchText)
            }else{
                println(matcher.group(1))
            }
            processStart = matcher.end()
        }

        println(LogoutReq.ONLINE.and(GetUserStatusRsp.MOBILE_STATUS_MASK).equals(LogoutReq.ONLINE))
        println(LogoutReq.LEAVE.and(GetUserStatusRsp.MOBILE_STATUS_MASK).equals(LogoutReq.LEAVE))
        println(LogoutReq.OFFLINE.and(GetUserStatusRsp.MOBILE_STATUS_MASK).equals(LogoutReq.OFFLINE))
        println(LogoutReq.LOGIN.and(GetUserStatusRsp.MOBILE_STATUS_MASK).equals(LogoutReq.LOGIN))
    }*/

    private var gson = GsonBuilder().create()
    private inline fun <reified T : Any> String.getObject1(): T {
        return gson.fromJson(this, object : TypeToken<T>() {}.type)
    }

    @Test
    fun testFun() {
        var d = mutableListOf<Int>(1, 3, 4, 5)
        var json = gson.toJson(d)
        val u = json.getObject1<MutableList<Int>>()
        println(u)
    }

    /*data class TestMsg(
            val id: String = "",
            val bodys: MessageListConverter.MsgBodyList
    )

    @Test
    fun testBodyDeserializer() {
        val bodys = MessageListConverter.MsgBodyList()
        bodys.add(TextBody("sdfsdfsd"))
        bodys.add(EmojiBody("dx", "大笑"))
        val gson = GsonBuilder().registerTypeHierarchyAdapter(MsgBody::class.java, MsgBodyDeserializer())
                .create()

        val json = gson.toJson(TestMsg("id", bodys))

        println(json)
        val msg = gson.fromJson<TestMsg>(json, TestMsg::class.java)

        println(msg)

    }*/
}

enum class UserState(val state: Int) {
    DISABLED(-2),
    KICKED_OUT(-1),
    LOGOUT(0),
    LOGIN(1),
    ONLINE(2),
    LEAVE(3)
}

