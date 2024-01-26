package com.guyporat.networking.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guyporat.networking.client.states.CameraSettings;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.networking.client.states.DoorLockSettings;
import com.guyporat.utils.GsonUtils;

import java.util.UUID;

public class DeviceClient extends Client {

    private final UUID deviceUUID;
    private final IOTDeviceType deviceType;
    private DeviceSettings settings;

    private transient final ClientSocketNetworkHandler networkHandler; // Transient to prevent serialization

    public DeviceClient(ClientSocketNetworkHandler networkHandler, UUID deviceUUID, IOTDeviceType deviceType) {
        this.networkHandler = networkHandler;
        this.deviceUUID = deviceUUID;
        this.deviceType = deviceType;
    }

    public ClientSocketNetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public UUID getDeviceUUID() {
        return deviceUUID;
    }

    public IOTDeviceType getDeviceType() {
        return deviceType;
    }

    public ShortDeviceObject getAsShortObject() {
        return new ShortDeviceObject(deviceUUID, settings.getDeviceName(), deviceType);
    }

    @Override
    public ClientType getType() {
        return ClientType.IOT_DEVICE;
    }

    public DeviceSettings getSettings() {
        return settings;
    }

    public void setSettings(JsonElement jsonElement) {
        if (this.deviceType == IOTDeviceType.CAMERA) {
            this.settings = GsonUtils.getGson().fromJson(jsonElement, CameraSettings.class);
        } else if (this.deviceType == IOTDeviceType.DOOR_LOCK) {
            this.settings = GsonUtils.getGson().fromJson(jsonElement, DoorLockSettings.class);
        }
    }

    public void loadSettings() {
        // TODO: load settings from database
        if (this.deviceType == IOTDeviceType.CAMERA) {
            this.settings = new CameraSettings("מכשיר בדיקה", true, UUID.randomUUID());
        } else if (this.deviceType == IOTDeviceType.DOOR_LOCK) {
            this.settings = new DoorLockSettings("מנעול בדיקה", false);
        }
    }

    public enum IOTDeviceType {
        CAMERA,
        AIR_CONDITIONER,
        LIGHTS,
        DOOR_LOCK,
    }

    public static class ShortDeviceObject {
        private final UUID deviceUUID;
        private final String name;
        private final IOTDeviceType deviceType;

        public ShortDeviceObject(UUID uuid, String name, IOTDeviceType type) {
            this.deviceUUID = uuid;
            this.name = name;
            this.deviceType = type;
        }
    }
}
