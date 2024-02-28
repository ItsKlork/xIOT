package com.guyporat.modules.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.config.Config;
import com.guyporat.database.model.DeviceModel;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.ClientSocketNetworkHandler;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.networking.client.states.CameraSettings;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.networking.client.states.DoorLockSettings;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.PasswordGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Devices extends Module {

    private ModuleStatus status;
    private static final UUID uuid = UUID.fromString("41747d8e-a73d-4087-b0d6-fe680cc31c00");

    private static List<DeviceModel> devicesInDB;

    @Override
    public void start() {
        if (this.status == ModuleStatus.RUNNING) {
            Logger.warn("Module " + this.getName() + " is already running");
            return;
        }
        this.status = ModuleStatus.RUNNING;
        Logger.info("Started module " + this.getName());

        // Load all devices from the database
        devicesInDB = new ArrayList<>(List.of(
                new DeviceModel(UUID.fromString("2e0c3bd9-a50b-4adc-befa-4e81e7683a8c"), DeviceClient.IOTDeviceType.CAMERA, new CameraSettings("מצלמה ניסיונית", true, UUID.fromString("eb1450ca-c5eb-4446-8a6f-7612f9ce0ade")), "SuperSecretKey"),
                new DeviceModel(UUID.fromString("eb1450ca-c5eb-4446-8a6f-7612f9ce0ade"), DeviceClient.IOTDeviceType.DOOR_LOCK, new DoorLockSettings("מנעול לדוגמה", false), "SuperSecretKey2")
        ));
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
    public static void setDeviceSettings(UUID deviceUUID, JsonElement settings) {
        DeviceModel deviceModel = devicesInDB.stream().filter(device -> device.deviceUUID.equals(deviceUUID)).findAny().orElse(null);
        if (deviceModel == null) {
            Logger.error("Device " + deviceUUID + " not found");
            return;
        }
        if (deviceModel.deviceType == DeviceClient.IOTDeviceType.CAMERA) {
            deviceModel.deviceSettings = GsonUtils.getGson().fromJson(settings, CameraSettings.class);
        } else if (deviceModel.deviceType == DeviceClient.IOTDeviceType.DOOR_LOCK) {
            deviceModel.deviceSettings = GsonUtils.getGson().fromJson(settings, DoorLockSettings.class);
        } else {
            Logger.error("Device " + deviceUUID + " has an unknown device type");
        }
    }

    @Override
    public void initialize() {
        this.status = ModuleStatus.STOPPED;
    }

    public static List<DeviceClient> getIOTDevices() {
        return MainServer.getSocketNetworkHandler().getAllDeviceNetworkHandlers().stream().filter(ClientSocketNetworkHandler::isAuthenticated).map(ClientSocketNetworkHandler::getClient).toList();
    }

    public static List<DeviceModel> getDevicesInDB() {
        return devicesInDB;
    }

    public static List<JsonDevice> getDevicesInDBJson() {
        return getDevicesInDB().stream().map(deviceModel -> new JsonDevice(deviceModel.deviceUUID, deviceModel.deviceSettings.getDeviceName(), deviceModel.deviceType, getIOTDevices().stream().anyMatch(deviceClient -> deviceClient.getDeviceUUID().equals(deviceModel.deviceUUID)))).toList();
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
                    DeviceModel deviceModel = getDevicesInDB().stream().filter(device -> device.deviceUUID.equals(deviceUUID)).findAny().orElse(null);
                    if (deviceModel == null) {
                        Logger.error("Device " + deviceUUID + " not found in database.");
                        return;
                    }
                    DeviceClient deviceClient = getIOTDevices().stream().filter(device -> device.getDeviceUUID().equals(deviceUUID)).findFirst().orElse(null);

                    if (packetType == PacketType.GET_DEVICE) { // Get all device info
                        webClient.send(PacketType.GET_DEVICE_RESPONSE, GsonUtils.getGson().toJson(deviceModel.getCensored()));
                    } else { // SET_DEVICE_SETTINGS
                        // check if the name was changed
                        DeviceSettings oldSettings = deviceModel.deviceSettings;
                        setDeviceSettings(deviceModel.deviceUUID, data.get("settings")); // Update the settings in the database
                        DeviceSettings newSettings = deviceModel.deviceSettings;

                        if (!oldSettings.getDeviceName().equals(newSettings.getDeviceName())) { // If the name has changed
                            Logger.info("Device " + deviceUUID + " name was changed");
                            // send the new name to all authenticated clients
                            for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) {
                                if (wc.isAuthenticated()) {
                                    wc.send(PacketType.GET_DEVICES_RESPONSE, GsonUtils.getGson().toJson(getDevicesInDBJson()));
                                }
                            }
                        }

                        if (deviceClient != null) { // The device is active
                            deviceClient.setSettings(data.get("settings")); // Update the settings in the device in memory
                            deviceClient.getNetworkHandler().sendPacket(PacketType.DEVICE_SETTINGS, deviceClient.getSettings()); // Send the new settings to the device
                        }
                    }
                }
                case CREATE_DEVICE -> {
                    DeviceClient.IOTDeviceType deviceType = DeviceClient.IOTDeviceType.valueOf(data.get("device_type").getAsString());
                    String deviceName = data.get("device_name").getAsString();
                    UUID newDeviceUUID = UUID.randomUUID();
                    String secret = new PasswordGenerator.PasswordGeneratorBuilder().useDigits(true).useLower(true).useUpper(true).build().generate(16); // Generate a random secret

                    DeviceModel deviceModel = new DeviceModel(newDeviceUUID, deviceType, switch (deviceType) {
                        case CAMERA ->
                                new CameraSettings(deviceName, false, UUID.fromString("00000000-0000-0000-0000-000000000000")); // Nil UUID
                        case DOOR_LOCK -> new DoorLockSettings(deviceName, false);
                        default -> throw new UnsupportedOperationException("Not yet implemented");
                    }, secret);
                    devicesInDB.add(deviceModel);
                    webClient.send(PacketType.CREATE_DEVICE_RESPONSE, GsonUtils.getGson().toJson(deviceModel));

                    for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) { // Let all authenticated clients know about the new device
                        if (wc.isAuthenticated()) {
                            wc.send(PacketType.GET_DEVICES_RESPONSE, GsonUtils.getGson().toJson(getDevicesInDBJson()));
                        }
                    }
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
    }

}
