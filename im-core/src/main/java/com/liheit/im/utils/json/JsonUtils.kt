package com.liheit.im.utils.json

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.liheit.im.core.protocol.GraphicBody
import com.liheit.im.core.protocol.MsgBody
import com.liheit.im.core.protocol.NewReplyBody
import java.lang.reflect.Type

/**
 * Created by daixun on 2018/6/18.
 */

//class JsonUtils {
var gson: Gson = GsonBuilder()
        .registerTypeHierarchyAdapter(Int::class.java, IntDeserializer())
        .registerTypeHierarchyAdapter(Integer::class.java, IntDeserializer())
        .registerTypeHierarchyAdapter(MsgBody::class.java, MsgBodyDeserializer())
        .registerTypeHierarchyAdapter(NewReplyBody::class.java, MsgBodyDeserializer())
        .create()

inline fun <reified T : Any> String.fromJson(): T {
    return gson.fromJson(this, object : TypeToken<T>() {}.type)
}

class IntDeserializer : JsonDeserializer<Int> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Int {
        return json?.asNumber?.toInt() ?: 0
    }
}
/*inline fun <reified T : Any> String.fromJson(): T {
    return gson.fromJson(this, object :TypeToken<T>(){}.type)
}*/
