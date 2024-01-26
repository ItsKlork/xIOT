package com.guyporat.networking.client;

import java.util.UUID;

public class DeviceClient extends Client {

    private final UUID deviceUUID;
    private final IOTDeviceType deviceType;

    private transient final ClientSocketNetworkHandler networkHandler; // Transient to prevent serialization

    public DeviceClient(ClientSocketNetworkHandler networkHandler, UUID deviceUUID, String clientName, IOTDeviceType deviceType) {
        super(clientName);
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

    @Override
    public ClientType getType() {
        return ClientType.IOT_DEVICE;
    }

    public enum IOTDeviceType {
        CAMERA,
        AIR_CONDITIONER,
        LIGHTS,
        DOOR_LOCK,
    }
}
