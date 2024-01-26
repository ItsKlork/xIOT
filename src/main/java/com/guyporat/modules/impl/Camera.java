package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.config.Config;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.Event;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Camera extends Module {

    private ModuleStatus status;
    private final UUID uuid = UUID.fromString("c2ca9921-80fe-48b0-9d64-6c74a0d03e9f");

    private Map<UUID, CameraOptions> cameraOptions;
    private Map<String, List<byte[]>> savedFaces;

    @Override
    public void start() {
        if (this.status == ModuleStatus.RUNNING) {
            Logger.warn("Module " + this.getName() + " is already running");
            return;
        }
        this.status = ModuleStatus.RUNNING;
        Logger.info("Started module " + this.getName());
    }

    @Override
    public void stop() {
        if (this.status == ModuleStatus.STOPPED) {
            Logger.warn("Module " + this.getName() + " is already stopped");
            return;
        }
        this.status = ModuleStatus.STOPPED;
        Logger.info("Stopped module " + this.getName());
    }

    @Override
    public void initialize() {
        this.status = ModuleStatus.STOPPED;

        // Load current options from database
        this.cameraOptions = new HashMap<>();
        this.cameraOptions.put(UUID.fromString("2e0c3bd9-a50b-4adc-befa-4e81e7683a8c"), new CameraOptions("מצלמה 1", true));

        this.savedFaces = new HashMap<>();
        try {
            savedFaces.put("Barack Obama", List.of(
                    Files.readAllBytes(new File("faces/Barack Obama/1.jpg").toPath())
            ));
            savedFaces.put("Guy Porat", List.of(
                    Files.readAllBytes(new File("faces/Guy Porat/1.jpg").toPath())
            ));
            savedFaces.put("Joe Biden", List.of(
                    Files.readAllBytes(new File("faces/Joe Biden/1.jpg").toPath())
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ModuleStatus getStatus() {
        return this.status;
    }

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public String getDescription() {
        return "Camera and face recognition module";
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String getVersion() {
        return "b0.2";
    }

    @Override
    public Config getConfig() {
        return null;
    }

    private void handleConnection(DeviceClient deviceClient, JsonObject data) {
        System.out.println(data.toString());
        if (deviceClient.getDeviceType() == DeviceClient.IOTDeviceType.CAMERA) {
            if (data.get("type").getAsString().equals("get_camera_options")) {
                deviceClient.getNetworkHandler().sendPacket(this.uuid, this.cameraOptions.getOrDefault(deviceClient.getDeviceUUID(),
                        new CameraOptions("מצלמה לא ידועה", false)));
            } else if (data.get("type").getAsString().equals("get_faces")) {
                deviceClient.getNetworkHandler().sendPacket(this.uuid, savedFaces);
            } else if (data.get("type").getAsString().equals("report_faces")) {
                Logger.debug("Received a face report from client " + deviceClient.getDeviceUUID());
                String[] faces = GsonUtils.getGson().fromJson(data.get("faces"), String[].class);
                MainServer.getEventManager().callEvent(new FaceRecognitionEvent(faces));
            }
        }
    }

    private void handleConnection(WebClient webClient, JsonObject data) {
        if (data.get("type").getAsString().equals("get_cameras")) {
            webClient.send("camera", GsonUtils.getGson().toJson(this.cameraOptions));
        }
    }

    @Override
    public void handleConnection(Client client, JsonObject data) {
        if (client.getType() == Client.ClientType.IOT_DEVICE) {
            DeviceClient deviceClient = (DeviceClient) client;
            this.handleConnection(deviceClient, data);
        } else {
            WebClient webClient = (WebClient) client;
            this.handleConnection(webClient, data);
        }
    }

    class CameraOptions {
        @com.google.gson.annotations.SerializedName("camera_name")
        public String cameraName;
        @com.google.gson.annotations.SerializedName("face_recognition")
        public boolean isFaceRecognitionEnabled;

        public CameraOptions(String cameraName, boolean isFaceRecognitionEnabled) {
            this.cameraName = cameraName;
            this.isFaceRecognitionEnabled = isFaceRecognitionEnabled;
        }
    }

    public static class FaceRecognitionEvent extends Event {

        private final String[] faces;

        public FaceRecognitionEvent(String[] faces) {
            this.faces = faces;
        }

        public String[] getFaces() {
            return faces;
        }
    }
}
