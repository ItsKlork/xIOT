package com.guyporat.networking.client;

public abstract class Client {

    private final String clientName;

    public Client(String clientName) {
        this.clientName = clientName;
    }

    public abstract ClientType getType();

    public String getClientName() {
        return clientName;
    }

    public enum ClientType {
        IOT_DEVICE,
        USER
    }

}
