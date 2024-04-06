package com.guyporat.networking.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleManager;
import com.guyporat.modules.impl.Devices;
import com.guyporat.modules.impl.Notifications;
import com.guyporat.networking.PacketType;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.NetworkProtocol;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class ClientSocketNetworkHandler extends Thread {

    /*
        The protocol goes as follows:
        1. Client connects to server
        2. Client sends a handshake packet with the following data:
            {
                "module_uuid": "00000000-0000-0000-0000-000000000000",
                "data": {
                    "username": "username",
                    "password": "password"
                }
            }
        3. Client sends a packet with the following data:
            {
                "module_uuid": "<target module uuid>",
                "data": {
                    "some": "data"
                }
            }
     */

    private final Gson gson;

    private final Socket socket;
    private boolean handshakeComplete;

    private DeviceClient client;

    public ClientSocketNetworkHandler(Socket socket) {
        this.socket = socket;
        this.handshakeComplete = false;
        this.gson = GsonUtils.getGson();
    }

    public DeviceClient getClient() {
        return client;
    }

    @Override
    public void run() {
        // Perform connection login, either an endpoint or a client
        Devices devicesModule = (Devices) ModuleManager.getInstance().getModuleByUUID(Devices.getStaticUUID()).orElseThrow();
        try {
            while (!this.handshakeComplete) {
                byte[] packetData = NetworkProtocol.receive(socket.getInputStream());

                String packetDataDecoded = new String(packetData, StandardCharsets.UTF_8);
                JsonObject packetJsonRoot = gson.fromJson(packetDataDecoded, JsonObject.class);
                int packetIdentifier = packetJsonRoot.get("pid").getAsInt();
                if (packetIdentifier == PacketType.DEVICE_AUTHENTICATION.getId())
                    this.handshakeComplete = this.attemptLogin(packetJsonRoot.getAsJsonObject("data"));
                else
                    throw new RuntimeException("Client tried to send a packet before completing handshake");
            }

            // Successful login, notify all web clients of new device
            MainServer.getWebSocketNetworkHandler().getWebClients().values().stream().filter(WebClient::isAuthenticated).forEach(webClient -> webClient.send(PacketType.GET_DEVICES_RESPONSE, GsonUtils.getGson().toJson(Devices.getDevicesInDBJson())));

            // Send notification that the device has connected
            ((Notifications)ModuleManager.getInstance().getModuleByUUID(Notifications.getStaticUUID()).get()).sendNotification(new Notifications.Notification(Notifications.NotificationType.DEVICE_INFO, "המכשיר " + this.client.getSettings().getDeviceName() + " התחבר"));

            while (true) {
                byte[] packetData = NetworkProtocol.receive(socket.getInputStream());

                String packetDataDecoded = new String(packetData, StandardCharsets.UTF_8); // Decode the bytes to a string using UTF-8
                JsonObject packetJsonRoot = gson.fromJson(packetDataDecoded, JsonObject.class);

                int packetIdentifier = packetJsonRoot.get("pid").getAsInt();

                PacketType packetType = PacketType.fromId(packetIdentifier);
                if (packetType == null) {
                    Logger.error("Packet type with id " + packetIdentifier + " does not exist");
                    break;
                }
                if (packetType.isResponse()) {
                    Logger.error("Client tried to send a response packet (" + socket.getInetAddress().getHostAddress()  + ":" + socket.getPort() + ")");
                    break;
                }

                Optional<Module> optionalTargetModule = packetType.getHandlingModule();
                if (optionalTargetModule.isEmpty()) {
                    Logger.error("Received a packet for a module that does not exist from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                    break;
                }

                Module targetModule = optionalTargetModule.get();
                targetModule.handleConnection(this.client, packetType, packetJsonRoot.get("data").getAsJsonObject());
            }

        } catch (RuntimeException e) {
            Logger.error("Client " + (this.client != null && this.client.getDeviceUUID() != null ? this.client.getDeviceUUID() : socket.getInetAddress().getHostAddress()) + " disconnected: " + e.getMessage());
        } catch (IOException e) {
            Logger.error(e.getMessage());
        } finally {
            try {
                socket.close();
                MainServer.getSocketNetworkHandler().removeClient(this);
                if (this.handshakeComplete)
                    MainServer.getWebSocketNetworkHandler().getWebClients().values().stream().filter(WebClient::isAuthenticated).forEach(webClient -> webClient.send(PacketType.GET_DEVICES_RESPONSE, GsonUtils.getGson().toJson(Devices.getDevicesInDBJson())));
            } catch (IOException ignored) {
            }
        }
    }

    public void sendPacket(PacketType packetType, Object packet) {
        this.sendPacket(packetType, gson.toJsonTree(packet));
    }

    public void sendPacket(PacketType packetType, JsonElement packet) {
        JsonObject packetJson = new JsonObject();
        packetJson.addProperty("pid", packetType.getId());
        packetJson.add("data", packet);
        try {
            NetworkProtocol.send(socket.getOutputStream(), gson.toJson(packetJson).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean attemptLogin(JsonObject loginData) {
        // TODO: implement authentication
        if (!loginData.has("uuid") || !loginData.has("secret")) {
            return false;
        }
        String deviceUUID = loginData.get("uuid").getAsString();
        String deviceSecret = loginData.get("secret").getAsString();
        String deviceType = loginData.get("type").getAsString();

        this.client = new DeviceClient(this, UUID.fromString(deviceUUID), DeviceClient.IOTDeviceType.valueOf(deviceType));
        this.client.loadSettings();
        Logger.info("Device " + this.socket.getInetAddress().toString() + " logged in with uuid " + deviceUUID);
        return true;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isAuthenticated() {
        return this.handshakeComplete;
    }
}
