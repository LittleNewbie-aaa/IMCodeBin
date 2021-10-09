package com.liheit.im.utils.json;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.liheit.im.core.protocol.MsgBody;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by daixun on 2018/6/25.
 */
public class MessageListConverter {

    private Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(MsgBody.class,new MsgBodyDeserializer())
            .create();

    @TypeConverter
    public Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public Long dateToTimestamp(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    @TypeConverter
    public String getMessageBodyListValue(ArrayList<MsgBody> model) {
        if (model == null) {
            return null;
        }
        return gson.toJson(model);
    }

    @TypeConverter
    public ArrayList<MsgBody> getBodyList(String data) {
        if (data == null || "".equals(data)) {
            return null;
        }
        return gson.fromJson(data, new TypeToken<ArrayList<MsgBody>>() {}.getType());
    }
}
