package com.beyond.sync.utils;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Type;

public class JsonUtil {
    public static <T> String encode(T t, Class<T> clazz) {
        return JSONObject.toJSONString(t);
    }

    public static <T> T decode(String s, Class<T> clazz) {
        if (s == null) {
            return null;
        }
        try {
            return JSONObject.parseObject(s, (Type) clazz);
        } catch (JSONException e) {
            return null;
        }
    }
}
