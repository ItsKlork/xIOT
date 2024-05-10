package com.guyporat.database.model;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.utils.Logger;

import java.util.UUID;

public class DeviceModel {

    public UUID deviceUUID;
    public DeviceClient.IOTDeviceType deviceType;
    public DeviceSettings deviceSettings;
    public String secret; // TODO: only keep encrypted secret

    public DeviceModel(UUID deviceUUID, DeviceClient.IOTDeviceType deviceType, DeviceSettings deviceSettings, String hashedSecret) {
        this.deviceUUID = deviceUUID;
        this.deviceType = deviceType;
        this.deviceSettings = deviceSettings;
        this.secret = hashedSecret;
    }

    public boolean compareSecret(String plaintextSecret) {
        if (this.secret == null) return false;
        return BCrypt.verifyer().verify(plaintextSecret.toCharArray(), this.secret).verified;
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
