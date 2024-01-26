package com.guyporat.networking.client.states;

public class DoorLockSettings extends DeviceSettings {
    private boolean openState;
    private String name;

    public DoorLockSettings(String name, boolean openState) {
        this.name = name;
        this.openState = openState;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    public DoorLockSettings(boolean openState, String name) {
        this.openState = openState;
        this.name = name;
    }
}
