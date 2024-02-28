package com.guyporat.database.model;

import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.states.DeviceSettings;

import java.util.UUID;

public class DeviceModel {

    public UUID deviceUUID;
    public DeviceClient.IOTDeviceType deviceType;
    public DeviceSettings deviceSettings;
    public String secret; // TODO: only keep encrypted secret

    public DeviceModel(UUID deviceUUID, DeviceClient.IOTDeviceType deviceType, DeviceSettings deviceSettings, String secret) {
        this.deviceUUID = deviceUUID;
        this.deviceType = deviceType;
        this.deviceSettings = deviceSettings;
        this.secret = secret;
    }

    public CensoredDeviceModel getCensored() {
        return new CensoredDeviceModel(deviceUUID, deviceType, deviceSettings);
    }

    public static class CensoredDeviceModel {
        public UUID deviceUUID;
        public DeviceClient.IOTDeviceType deviceType;

        public DeviceSettings settings;

        public CensoredDeviceModel(UUID deviceUUID, DeviceClient.IOTDeviceType deviceType, DeviceSettings deviceSettings) {
            this.deviceUUID = deviceUUID;
            this.deviceType = deviceType;
            this.settings = deviceSettings;
        }
    }

}
