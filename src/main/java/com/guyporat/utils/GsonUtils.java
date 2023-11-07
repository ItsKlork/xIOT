package com.guyporat.utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Base64;

public class GsonUtils {

    private final static Gson gson = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new Base64TypeAdapter())
            .create();

    public static Gson getGson() {
        return gson;
    }

    public static class Base64TypeAdapter extends TypeAdapter<byte[]> {
        @Override
        public void write(JsonWriter out, byte[] value) throws IOException {
            out.value(Base64.getEncoder().withoutPadding().encodeToString(value));
        }
        @Override
        public byte[] read(JsonReader in) throws IOException {
            return Base64.getDecoder().decode(in.nextString());
        }
    }

}
