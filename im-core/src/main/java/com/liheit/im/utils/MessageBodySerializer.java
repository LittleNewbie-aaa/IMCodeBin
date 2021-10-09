package com.liheit.im.utils;

import android.support.annotation.Keep;

import com.liheit.im.core.bean.MessageType;
import com.liheit.im.core.protocol.MessageBody;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Created by daixun on 2018/8/11.
 */
@Keep
public class MessageBodySerializer implements JsonSerializer<MessageBody> {
    @Override
    public JsonElement serialize(MessageBody src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject object = ((JsonObject) context.serialize(src));
        if(src.getMtype()== MessageType.REPLY_BEGIN.getValue()){
            Set<String> keys = object.keySet();
            for (String key : keys) {
                if(!key.equals("mType")&&!key.equals("mid")&&!key.equals("fromid")&&!key.equals("text")){
                    object.remove(key);
                }
            }
        }

        return object;

    }
}
