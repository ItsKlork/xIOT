package com.guyporat.networking.client.states;

import java.time.LocalDateTime;

public class WaterHeaterSettings extends DeviceSettings {

    private boolean isOn;
    private String name;
    private String timer;

    public WaterHeaterSettings(boolean isOn, String name, String timer) {
        this.isOn = isOn;
        this.name = name;
        this.timer = timer;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    public LocalDateTime getTimer() {
        return LocalDateTime.parse(timer);
    }

    @Override
    public String toString() {
        return "WaterHeaterSettings{" +
                "isOn=" + isOn +
                ", name='" + name + '\'' +
                ", timer='" + timer + '\'' +
                '}';
    }
}
