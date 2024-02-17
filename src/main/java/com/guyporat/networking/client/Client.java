package com.guyporat.networking.client;

import java.util.UUID;

public abstract class Client {

    protected UUID deviceUUID;

    public UUID getDeviceUUID() {
        return deviceUUID;
    }

    public abstract ClientType getType();

    public enum ClientType {
        IOT_DEVICE,
        USER
    }

}
