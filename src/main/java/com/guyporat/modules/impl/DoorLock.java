package com.guyporat.modules.impl;

import com.guyporat.MainServer;
import com.guyporat.config.Config;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.EventHandler;
import me.nurio.events.handler.EventListener;

import java.util.List;
import java.util.UUID;

public class DoorLock extends Module implements EventListener {

    private ModuleStatus status;
    private final UUID uuid = UUID.fromString("d009a719-db5d-4e38-87be-ec59b9f65630");

    private List<String> allowedUsers;

    private DoorState doorState;


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

        this.allowedUsers = List.of("Joe Biden");
        this.doorState = DoorState.CLOSED;

        MainServer.getEventManager().registerEvents(this);
    }

    @Override
    public ModuleStatus getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return "Door Lock";
    }

    @Override
    public String getDescription() {
        return "The module that controls the door lock";
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
        return null;
    }

    @EventHandler
    public void onFaceRecognized(FaceRecognition.FaceRecognitionEvent event) {
        for (String face : event.getFaces()) {
            if (allowedUsers.contains(face)) {
                if (doorState == DoorState.CLOSED)
                    Logger.info("Opened door for " + face);
                doorState = DoorState.OPEN;
                return;
            }
        }

        if (doorState == DoorState.OPEN) {
            Logger.info("No recognized faces, closing door");
            doorState = DoorState.CLOSED;
        }
    }

    private enum DoorState {
        OPEN,
        CLOSED
    }

}
