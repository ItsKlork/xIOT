package com.guyporat.networking.client;

import java.util.UUID;

public class Client {

    private final ClientNetworkHandler networkHandler;
    private final UUID uuid;
    private final ClientType type;
    private final String deviceName;

    public Client(ClientNetworkHandler networkHandler, UUID uuid, ClientType type, String deviceName) {
        this.networkHandler = networkHandler;
        this.uuid = uuid;
        this.type = type;
        this.deviceName = deviceName;
    }

    public ClientNetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public UUID getUUID() {
        return uuid;
    }

    public ClientType getType() {
        return type;
    }

    public String getDeviceName() {
        return deviceName;
    }

    protected enum ClientType {
        IOT_DEVICE,
        USER
    }

}
