package com.guyporat.modules.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.database.Database;
import com.guyporat.database.model.DeviceModel;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.ClientSocketNetworkHandler;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.networking.client.states.*;
import com.guyporat.utils.gson.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.PasswordGenerator;

import java.util.List;
import java.util.UUID;

public class Devices extends Module {

    private ModuleStatus status;
    private static final UUID uuid = UUID.fromString("41747d8e-a73d-4087-b0d6-fe680cc31c00");

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

    /**
     * Set the settings of a device. Takes a JsonElement representation of the settings. Updates it in the database
     *
     * @param deviceUUID The UUID of the device
     * @param settings   The new serialized settings as a JsonElement
     */
    public static DeviceSettings setDeviceSettings(UUID deviceUUID, JsonElement settings) {
        return setDeviceSettings(deviceUUID, GsonUtils.getGson().fromJson(settings, DeviceSettings.class));
    }

    public static DeviceSettings setDeviceSettings(UUID deviceUUID, DeviceSettings deviceSettings) {
        DeviceModel deviceModel = Database.DeviceTableManager.getInstance().getDeviceByUUID(deviceUUID);
        if (deviceModel == null) {
            Logger.error("Device " + deviceUUID + " not found");
            return null;
        }
        deviceModel.deviceSettings = deviceSettings;
        Database.DeviceTableManager.getInstance().updateDevice(deviceModel);
        return deviceModel.deviceSettings;
    }

    @Override
    public void initialize() {
        this.status = ModuleStatus.STOPPED;
    }

    public static List<DeviceClient> getActiveDevices() {
        return MainServer.getSocketNetworkHandler().getAllDeviceNetworkHandlers().stream().filter(ClientSocketNetworkHandler::isAuthenticated).map(ClientSocketNetworkHandler::getClient).toList();
    }

    public static List<JsonDevice> getDevicesInDBJson() {
        return Database.DeviceTableManager.getInstance().getDevices().stream().map(deviceModel -> new JsonDevice(deviceModel.deviceUUID, deviceModel.deviceSettings.getDeviceName(), deviceModel.deviceType, getActiveDevices().stream().anyMatch(deviceClient -> deviceClient.getDeviceUUID().equals(deviceModel.deviceUUID)))).toList();
    }

    public static List<JsonDevice> getCameraDevicesInDBJson() {
        return Database.DeviceTableManager.getInstance().getCameraDevices().stream().map(deviceModel -> new JsonDevice(deviceModel.deviceUUID, deviceModel.deviceSettings.getDeviceName(), deviceModel.deviceType, getActiveDevices().stream().anyMatch(deviceClient -> deviceClient.getDeviceUUID().equals(deviceModel.deviceUUID)))).toList();
    }

    public static UUID getStaticUUID() {
        return uuid;
    }

    @Override
    public void handleConnection(Client client, PacketType packetType, JsonObject data) {
        if (client instanceof WebClient webClient) {
            switch (packetType) {
                case GET_DEVICES ->
                        webClient.send(PacketType.GET_DEVICES_RESPONSE, GsonUtils.getGson().toJson(getDevicesInDBJson())); // Sends all devices
                case GET_DEVICE, SET_DEVICE_SETTINGS -> {
                    UUID deviceUUID = UUID.fromString(data.get("device_uuid").getAsString());
                    DeviceModel deviceModel = Database.DeviceTableManager.getInstance().getDeviceByUUID(deviceUUID);
                    if (deviceModel == null) {
                        Logger.error("Device " + deviceUUID + " not found in database.");
                        return;
                    }
                    DeviceClient deviceClient = getActiveDevices().stream().filter(device -> device.getDeviceUUID().equals(deviceUUID)).findFirst().orElse(null);

                    if (packetType == PacketType.GET_DEVICE) { // Get all device info
                        webClient.send(PacketType.GET_DEVICE_RESPONSE, GsonUtils.getGson().toJson(deviceModel.getCensored()));
                    } else { // SET_DEVICE_SETTINGS
                        // check if the name was changed
                        Logger.info("Device " + deviceUUID + " settings were changed TO " + data.get("settings"));
                        DeviceSettings oldSettings = deviceModel.deviceSettings;
                        DeviceSettings newSettings = setDeviceSettings(deviceModel.deviceUUID, data.get("settings")); // Update the settings in the database
                        if (newSettings == null) {
                            Logger.error("Failed to update settings for device " + deviceUUID);
                            return;
                        }
                        if (!oldSettings.getDeviceName().equals(newSettings.getDeviceName())) { // If the name has changed
                            Logger.info("Device " + deviceUUID + " name was changed");
                            // send the new name to all authenticated clients
                            String updatedListParsed = GsonUtils.getGson().toJson(getDevicesInDBJson());
                            for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) {
                                if (wc.isAuthenticated()) {
                                    wc.send(PacketType.GET_DEVICES_RESPONSE, updatedListParsed);
                                }
                            }
                        }
                        Logger.debug("Device " + deviceUUID + " settings were changed: " + newSettings);
                        if (deviceClient != null) { // The device is active
                            Logger.debug("Sent new settings to device " + deviceUUID);
                            deviceClient.setSettings(newSettings); // Update the settings in the device in memory
                            deviceClient.getNetworkHandler().sendPacket(PacketType.DEVICE_SETTINGS, newSettings); // Send the new settings to the device
                        }
                    }
                }
                case CREATE_DEVICE -> {
                    DeviceClient.IOTDeviceType deviceType = DeviceClient.IOTDeviceType.valueOf(data.get("device_type").getAsString());
                    String deviceName = data.get("device_name").getAsString();
                    UUID newDeviceUUID = UUID.randomUUID();
                    String secret = new PasswordGenerator.PasswordGeneratorBuilder().useDigits(true).useLower(true).useUpper(true).build().generate(16); // Generate a random secret

                    DeviceModel deviceModel = new DeviceModel(newDeviceUUID, deviceType, switch (deviceType) { // Default values
                        case CAMERA ->
                                new CameraSettings(deviceName, false, UUID.fromString("00000000-0000-0000-0000-000000000001"), 7072); // Nil UUID
                        case DOOR_LOCK -> new DoorLockSettings(deviceName, false);
                        case AIRCON -> new AirConSettings(deviceName, 22, false);
                        case LIGHT -> new LightSettings(deviceName, "#FFFFFF", 100, false);
                        case WATER_HEATER -> new WaterHeaterSettings(false, deviceName, "2024-04-01T00:00:00");
                    }, BCrypt.withDefaults().hashToString(12, secret.toCharArray()));
                    Database.DeviceTableManager.getInstance().addDevice(deviceModel);
                    JsonObject jsonElement = new JsonObject();
                    jsonElement.addProperty("deviceUUID", newDeviceUUID.toString());
                    jsonElement.addProperty("secret", secret);
                    jsonElement.addProperty("deviceType", deviceType.toString());
                    webClient.send(PacketType.CREATE_DEVICE_RESPONSE, GsonUtils.getGson().toJson(jsonElement));

                    String listOfDevicesParsed = GsonUtils.getGson().toJson(getDevicesInDBJson());
                    for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) { // Let all authenticated clients know about the new device
                        if (wc.isAuthenticated()) {
                            wc.send(PacketType.GET_DEVICES_RESPONSE, listOfDevicesParsed);
                        }
                    }
                }
                case REMOVE_DEVICE -> {
                    UUID deviceUUID = UUID.fromString(data.get("device_uuid").getAsString());
                    DeviceModel deviceModel = Database.DeviceTableManager.getInstance().getDeviceByUUID(deviceUUID);
                    if (deviceModel == null) {
                        Logger.error("Device " + deviceUUID + " not found in database.");
                        webClient.send(PacketType.REMOVE_DEVICE_RESPONSE, "{\"status\": \"error\", \"message\": \"Device not found in database.\"}");
                        return;
                    }
                    Database.DeviceTableManager.getInstance().removeDevice(deviceModel.deviceUUID);
                    String listOfDevicesParsed = GsonUtils.getGson().toJson(getDevicesInDBJson());
                    webClient.send(PacketType.REMOVE_DEVICE_RESPONSE, "{\"status\": \"success\"}");
                    for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) { // Let all authenticated clients know about the new device
                        if (wc.isAuthenticated()) {
                            wc.send(PacketType.GET_DEVICES_RESPONSE, listOfDevicesParsed);
                        }
                    }
                }
            }
        } else {
            DeviceClient deviceClient = (DeviceClient) client;
            if (packetType == PacketType.GET_SETTINGS)
                deviceClient.getNetworkHandler().sendPacket(PacketType.DEVICE_SETTINGS, deviceClient.getSettings());
            if (packetType == PacketType.UPDATE_SELF_SETTINGS) {
                DeviceSettings newSettings = setDeviceSettings(deviceClient.getDeviceUUID(), data);
                deviceClient.setSettings(newSettings);
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

    public static class JsonDevice {
        private final UUID deviceUUID;
        private final String name;
        private final DeviceClient.IOTDeviceType deviceType;
        private boolean active;

        public JsonDevice(UUID deviceUUID, String name, DeviceClient.IOTDeviceType deviceType, boolean active) {
            this.deviceUUID = deviceUUID;
            this.name = name;
            this.deviceType = deviceType;
            this.active = active;
        }

        public DeviceClient.IOTDeviceType getDeviceType() {
            return deviceType;
        }
    }

}
