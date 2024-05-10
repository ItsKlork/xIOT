package com.guyporat.modules.impl;

import com.guyporat.MainServer;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.networking.client.states.DoorLockSettings;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.EventHandler;
import me.nurio.events.handler.EventListener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DoorLock extends Module implements EventListener {

    private ModuleStatus status;
    private final UUID uuid = UUID.fromString("d009a719-db5d-4e38-87be-ec59b9f65630");

    private List<String> allowedUsers;


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

        this.allowedUsers = List.of("Guy Porat", "Joe Biden");

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

    @EventHandler
    public void onFaceRecognized(Camera.FaceRecognitionEvent event) {
        if (event.getFaces().length == 1 && event.getFaces()[0].equals("$Unknown"))
            return;
        UUID doorLockTargetUUID = event.getCameraSettings().getTargetDoorLock();
        Optional<DeviceClient> doorLockClient = Devices.getActiveDevices().stream().filter(device -> device.getDeviceType() == DeviceClient.IOTDeviceType.DOOR_LOCK && device.getDeviceUUID().equals(doorLockTargetUUID)).findAny();
        if (doorLockClient.isEmpty())
            return;
        DeviceClient doorLock = doorLockClient.get();
        DoorLockSettings doorLockSettings = (DoorLockSettings) doorLock.getSettings();
        if (doorLockSettings.isOpen())
            return;

        DeviceSettings newSettings = Devices.setDeviceSettings(doorLock.getDeviceUUID(), new DoorLockSettings(doorLockSettings.getDeviceName(), true));
        doorLock.setSettings(newSettings);
        doorLock.getNetworkHandler().sendPacket(PacketType.DEVICE_SETTINGS, newSettings);
    }
}