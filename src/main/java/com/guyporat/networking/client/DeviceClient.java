package com.guyporat.networking.client;

import com.google.gson.JsonElement;
import com.guyporat.modules.impl.Devices;
import com.guyporat.networking.client.states.CameraSettings;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.networking.client.states.DoorLockSettings;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;

import java.util.NoSuchElementException;
import java.util.UUID;

public class DeviceClient extends Client {

    private final IOTDeviceType deviceType;
    private DeviceSettings settings;

    private transient final ClientSocketNetworkHandler networkHandler; // Transient to prevent serialization
    private String cameraHLSPort = "7171"; // TODO: accept on camera handshake

    public DeviceClient(ClientSocketNetworkHandler networkHandler, UUID deviceUUID, IOTDeviceType deviceType) {
        this.networkHandler = networkHandler;
        this.deviceUUID = deviceUUID;
        this.deviceType = deviceType;
    }

    public ClientSocketNetworkHandler getNetworkHandler() {
        return networkHandler;
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
        try {
            this.settings = Devices.getDevicesInDB().stream().filter(device -> device.deviceUUID.equals(this.deviceUUID)).findFirst().orElseThrow().deviceSettings;
        } catch (NoSuchElementException e) {
            Logger.error("[UNEXPECTED ERROR] Device " + this.deviceUUID + " not found in database. This is AFTER authentication.");
        }
    }

    public String getHLSUrl() {
        if (this.deviceType != IOTDeviceType.CAMERA) {
            Logger.error("Attempted to get HLS URL for non-camera device");
            return null;
        }
        return "http://" + this.networkHandler.getSocket().getInetAddress().getHostAddress() + ":" + this.cameraHLSPort; // TODO: change to https
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
