package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.config.Config;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.client.Client;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.Event;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FaceRecognition extends Module {

    private ModuleStatus status;
    private final UUID uuid = UUID.fromString("2e0c3bd9-a50b-4adc-befa-4e81e7683a8b");
    private Config config;

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
        this.config = new Config("face_recognition.cfg");
        try {
            this.config.load();
        } catch (IOException e) {
            Logger.error("Failed to load config for module " + this.getName());
        }

        nu.pattern.OpenCV.loadLocally();

        // Load sample faces
        savedFaces = new HashMap<>();
        try {
            savedFaces.put("Barack Obama", List.of(
                    Files.readAllBytes(new File("faces/Barack Obama/obama.jpg").toPath())
            ));
            savedFaces.put("Guy Porat", List.of(
                    Files.readAllBytes(new File("faces/Guy Porat/guy.jpg").toPath())
            ));
            savedFaces.put("Joe Biden", List.of(
                    Files.readAllBytes(new File("faces/Joe Biden/biden.jpg").toPath())
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
        return "Face Recognition";
    }

    @Override
    public String getDescription() {
        return "Face recognition from a camera";
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getVersion() {
        return "beta-0.1";
    }

    @Override
    public Config getConfig() {
        return this.config;
    }

    @Override
    public void handleConnection(Client client, JsonObject data) {
        String action = data.get("action").getAsString();
        if (action.equals("get_faces")) {
            client.getNetworkHandler().sendPacket(uuid, savedFaces);
            Logger.debug("Sent dataset to client " + client.getUUID());
        } else if (action.equals("report_faces")) {
            // Logger.debug("Received a face report from client " + client.getUUID());
            String[] faces = GsonUtils.getGson().fromJson(data.get("faces"), String[].class);
            MainServer.getEventManager().callEvent(new FaceRecognitionEvent(faces));
        } else {
            Logger.warn("Received an unknown action from client " + client.getUUID() + ": " + action);
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
