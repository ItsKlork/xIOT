package com.guyporat.networking.client.states;

import java.util.UUID;

public class CameraSettings extends DeviceSettings {

    private String name;
    private boolean faceRecognition;
    private UUID targetDoorLock;
    private int httpPort;

    public CameraSettings(String name, boolean faceRecognition, UUID targetDoorLock, int httpPort) {
        this.name = name;
        this.faceRecognition = faceRecognition;
        this.targetDoorLock = targetDoorLock;
        this.httpPort = httpPort;
    }

    public UUID getTargetDoorLock() {
        return targetDoorLock;
    }

    @Override
    public String getDeviceName() {
        return name;
    }

    public int getHttpPort() {
        return httpPort;
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
