package com.guyporat.networking.client;

public abstract class Client {

    public abstract ClientType getType();

    public enum ClientType {
        IOT_DEVICE,
        USER
    }

}
