package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.config.Config;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.CompressionUtils;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.Event;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Camera extends Module {

    private ModuleStatus status;
    private static final UUID uuid = UUID.fromString("c2ca9921-80fe-48b0-9d64-6c74a0d03e9f");

    private Map<String, byte[]> facesDatasetCompressed;

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

        this.facesDatasetCompressed = new HashMap<>();
        try {
            facesDatasetCompressed.put("Barack Obama", CompressionUtils.compressData(Files.readAllBytes(new File("faces/Barack Obama/1.jpg").toPath())));
            facesDatasetCompressed.put("Guy Porat", CompressionUtils.compressData(Files.readAllBytes(new File("faces/Guy Porat/1.jpg").toPath())));
            facesDatasetCompressed.put("Joe Biden", CompressionUtils.compressData(Files.readAllBytes(new File("faces/Joe Biden/1.jpg").toPath())));
            /*facesDatasetCompressed.put("Barack Obama", Files.readAllBytes(new File("faces/Barack Obama/1.jpg").toPath()));
            facesDatasetCompressed.put("Guy Porat", Files.readAllBytes(new File("faces/Guy Porat/1.jpg").toPath()));
            facesDatasetCompressed.put("Joe Biden", Files.readAllBytes(new File("faces/Joe Biden/1.jpg").toPath()));*/
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
        return uuid;
    }

    public static UUID getStaticUUID() {
        return uuid;
    }

    @Override
    public String getVersion() {
        return "b0.2";
    }

    @Override
    public Config getConfig() {
        return null;
    }

    private void handleConnection(DeviceClient deviceClient, PacketType packetType, JsonObject data) {
        System.out.println(data.toString());
        if (deviceClient.getDeviceType() == DeviceClient.IOTDeviceType.CAMERA) { // Packets from a camera device
            switch (packetType) {
                case GET_CAMERA_SETTINGS -> {
                    deviceClient.getNetworkHandler().sendPacket(PacketType.DEVICE_SETTINGS, deviceClient.getSettings());
                }
                case GET_FACE_RECOGNITION_FACE_DATASET -> {
                    deviceClient.getNetworkHandler().sendPacket(PacketType.FACE_RECOGNITION_FACE_DATASET, facesDatasetCompressed);
                }
                case REPORT_FACE_RECOGNITION_DETECTION -> {
                    Logger.debug("Received a face report from client " + deviceClient.getDeviceUUID());
                    String[] faces = GsonUtils.getGson().fromJson(data.get("faces"), String[].class);
                    MainServer.getEventManager().callEvent(new FaceRecognitionEvent(faces));
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
        Logger.error("[UNIMPLEMENTED] handleConnection(WebClient, JsonObject)");
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
