package com.guyporat.utils;

import com.google.gson.Gson;

public class GsonUtils {

    private final static Gson gson = new Gson();

    public static Gson getGson() {
        return gson;
    }

}
