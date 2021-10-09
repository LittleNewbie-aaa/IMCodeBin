package retrofit2.converter.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Converter;

/**
 * Created by daixun on 2018/3/20.
 */

public class ToStringConverter implements Converter<Object, String> {
    public static final ToStringConverter INSTANCE = new ToStringConverter();

    Gson gson = new GsonBuilder().create();

    @Override
    public String convert(Object value) {
        if (value.getClass().isEnum()) {
            return gson.toJson(value).replace("\"", "");
        }
        return value.toString();
    }
}
