package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.config.Config;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.ClientSocketNetworkHandler;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;

import java.util.List;
import java.util.UUID;

public class Devices extends Module {

    private ModuleStatus status;
    private final UUID uuid = UUID.fromString("41747d8e-a73d-4087-b0d6-fe680cc31c00");

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

    private List<DeviceClient> getIOTDevices() {
        return MainServer.getSocketNetworkHandler().getAllDeviceNetworkHandlers().stream().map(ClientSocketNetworkHandler::getClient).toList();
    }

    @Override
    public void handleConnection(Client client, JsonObject data) {
        if (client instanceof WebClient webClient) {
            String action = data.get("action").getAsString();
            if (action.equals("get_devices")) {
                webClient.send("devices", GsonUtils.getGson().toJson(this.getIOTDevices().stream().map(DeviceClient::getAsShortObject).toList()));
            } else if (action.equals("get_device") || action.equals("set_device_settings")) {
                UUID deviceUUID = UUID.fromString(data.get("device_uuid").getAsString());
                DeviceClient deviceClient = this.getIOTDevices().stream().filter(device -> device.getDeviceUUID().equals(deviceUUID)).findFirst().orElse(null);
                if (deviceClient == null) {
                    Logger.error("Device " + deviceUUID + " not found");
                    return;
                }
                if (action.equals("get_device")) {
                    webClient.send("device", GsonUtils.getGson().toJson(deviceClient));
                } else {
                    deviceClient.setSettings(data.get("settings"));
                    webClient.send("devices", GsonUtils.getGson().toJson(this.getIOTDevices().stream().map(DeviceClient::getAsShortObject).toList()));
                }
            }
        }
    }

    @Override
    public ModuleStatus getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return "Devices";
    }

    @Override
    public String getDescription() {
        return "Module used to gather information about devices in the house";
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getVersion() {
        return "0.1b";
    }

    @Override
    public Config getConfig() {
        return null;
    }
}
