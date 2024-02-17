package com.guyporat.networking.client.states;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class CameraSettings extends DeviceSettings {

    @SerializedName("camera_name")
    private String name;

    @SerializedName("face_recognition")
    private boolean faceRecognition;

    @SerializedName("target_door_lock")
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
