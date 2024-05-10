package com.guyporat.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.guyporat.networking.client.states.*;

import java.io.IOException;
import java.util.Base64;

public class GsonUtils {

    private final static Gson gson;

    static {
        RuntimeTypeAdapterFactory<DeviceSettings> deviceSettingsAdapterFactory = RuntimeTypeAdapterFactory.of(DeviceSettings.class, "type")
                .registerSubtype(AirConSettings.class, "AIRCON")
                .registerSubtype(CameraSettings.class, "CAMERA")
                .registerSubtype(WaterHeaterSettings.class, "WATER_HEATER")
                .registerSubtype(LightSettings.class, "LIGHT")
                .registerSubtype(DoorLockSettings.class, "DOOR_LOCK");

        gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64TypeAdapter())
                .registerTypeAdapterFactory(deviceSettingsAdapterFactory)
                .create();
    }

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
