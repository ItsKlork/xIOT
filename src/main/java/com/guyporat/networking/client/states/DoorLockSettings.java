package com.guyporat.networking.client.states;

public class DoorLockSettings extends DeviceSettings {
    private boolean openState;
    private String name;

    public DoorLockSettings(String name, boolean openState) {
        this.name = name;
        this.openState = openState;
    }

    public boolean isOpen() {
        return openState;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    @Override
    public String toString() {
        return "DoorLockSettings{" +
                "openState=" + openState +
                ", name='" + name + '\'' +
                '}';
    }
}
