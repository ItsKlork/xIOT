package com.guyporat.networking.client.states;

public class LightSettings extends DeviceSettings {

    private String name;
    private String color;
    private int brightness;
    private boolean isOn;

    public LightSettings(String name, String color, int brightness, boolean isOn) {
        this.name = name;
        this.color = color;
        this.brightness = brightness;
        this.isOn = isOn;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "LightSettings{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", brightness=" + brightness +
                ", isOn=" + isOn +
                '}';
    }
}
