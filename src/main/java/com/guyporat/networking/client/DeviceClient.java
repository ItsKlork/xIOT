package com.guyporat.networking.client;

import com.guyporat.networking.client.states.CameraSettings;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.utils.Logger;

import java.util.UUID;

public class DeviceClient extends Client {

    private final IOTDeviceType deviceType;
    private DeviceSettings settings;

    private transient final ClientSocketNetworkHandler networkHandler; // Transient to prevent serialization

    public DeviceClient(ClientSocketNetworkHandler networkHandler, UUID deviceUUID, IOTDeviceType deviceType, DeviceSettings deviceSettings) {
        this.networkHandler = networkHandler;
        this.deviceUUID = deviceUUID;
        this.deviceType = deviceType;
        this.settings = deviceSettings;
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

    public void setSettings(DeviceSettings deviceSettings) {
        this.settings = deviceSettings;
    }

    public String getHLSUrl() {
        if (this.deviceType != IOTDeviceType.CAMERA) {
            Logger.error("Attempted to get HLS URL for non-camera device");
            return null;
        }
        return "http://" + this.networkHandler.getSocket().getInetAddress().getHostAddress() + ":" + ((CameraSettings)this.settings).getHttpPort(); // TODO: change to https
    }

    public enum IOTDeviceType {
        CAMERA,
        AIRCON,
        LIGHT,
        DOOR_LOCK,
        WATER_HEATER,
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
