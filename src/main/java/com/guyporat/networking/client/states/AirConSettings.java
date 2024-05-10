package com.guyporat.networking.client.states;

public class AirConSettings extends DeviceSettings {

    private String name;
    private int temperature;
    private boolean isOn;

    public AirConSettings(String name, int temperature, boolean isOn) {
        this.name = name;
        this.temperature = temperature;
        this.isOn = isOn;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    @Override
    public String toString() {
        return "AirConSettings{" +
                "name='" + name + '\'' +
                ", temperature=" + temperature +
                ", isOn=" + isOn +
                '}';
    }
}
