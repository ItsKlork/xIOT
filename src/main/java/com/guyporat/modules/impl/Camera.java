package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.database.Database;
import com.guyporat.database.model.TenantModel;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.networking.client.states.CameraSettings;
import com.guyporat.utils.gson.GsonUtils;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.Event;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Camera extends Module {

    private ModuleStatus status;
    private static final UUID uuid = UUID.fromString("c2ca9921-80fe-48b0-9d64-6c74a0d03e9f");

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
        return uuid;
    }

    public static UUID getStaticUUID() {
        return uuid;
    }

    @Override
    public String getVersion() {
        return "b0.2";
    }


    private void handleConnection(DeviceClient deviceClient, PacketType packetType, JsonObject data) {
        if (deviceClient.getDeviceType() == DeviceClient.IOTDeviceType.CAMERA) { // Packets from a camera device
            switch (packetType) {
                case GET_FACE_RECOGNITION_FACE_DATASET -> {
                    HashMap<String, byte[]> dataset = new HashMap<>();
                    for (TenantModel tenant : Database.TenantTableManager.getInstance().getTenants()) {
                        dataset.put(tenant.getFullName(), tenant.getCompressedFaceData());
                    }
                    deviceClient.getNetworkHandler().sendPacket(PacketType.FACE_RECOGNITION_FACE_DATASET, dataset);
                }
                case REPORT_FACE_RECOGNITION_DETECTION -> {
                    Logger.debug("Received a face report from client " + deviceClient.getDeviceUUID());
                    String[] faces = GsonUtils.getGson().fromJson(data.get("faces"), String[].class);
                    MainServer.getEventManager().callEvent(new FaceRecognitionEvent(faces, (CameraSettings) deviceClient.getSettings()));
                }
            }
        }
    }

    @Override
    public void handleConnection(Client client, PacketType packetType, JsonObject data) {
        if (client.getType() == Client.ClientType.IOT_DEVICE) {
            DeviceClient deviceClient = (DeviceClient) client;
            this.handleConnection(deviceClient, packetType, data);
        } else {
            WebClient webClient = (WebClient) client;
            this.handleConnection(webClient, packetType, data);
        }
    }

    private void handleConnection(WebClient webClient, PacketType packetType, JsonObject data) {
        if (packetType == PacketType.GET_CAMERAS) {
            List<Devices.JsonDevice> allCameras = Devices.getCameraDevicesInDBJson();
            webClient.send(PacketType.GET_CAMERAS_RESPONSE, GsonUtils.getGson().toJson(allCameras));
        }
    }

    public static class FaceRecognitionEvent extends Event {

        private final String[] faces;
        private final CameraSettings cameraSettings;

        public FaceRecognitionEvent(String[] faces, CameraSettings cameraSettings) {
            this.faces = faces;
            this.cameraSettings = cameraSettings;
        }

        public String[] getFaces() {
            return faces;
        }

        public CameraSettings getCameraSettings() {
            return cameraSettings;
        }
    }

}
