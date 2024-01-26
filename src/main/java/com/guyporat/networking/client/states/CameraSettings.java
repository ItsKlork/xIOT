package com.guyporat.networking.client.states;

import java.util.UUID;

public class CameraSettings extends DeviceSettings {

    private String name;

    private boolean faceRecognition;
    private UUID targetDoorLock;

    public CameraSettings(String name, boolean faceRecognition, UUID targetDoorLock) {
        this.name = name;
        this.faceRecognition = faceRecognition;
        this.targetDoorLock = targetDoorLock;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    @Override
    public String toString() {
        return "CameraSettings{" +
                "name='" + name + '\'' +
                ", faceRecognition=" + faceRecognition +
                ", targetDoorLock=" + targetDoorLock +
                '}';
    }
}
