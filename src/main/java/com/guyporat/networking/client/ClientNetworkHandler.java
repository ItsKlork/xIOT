package com.guyporat.networking.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleManager;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.NetworkProtocol;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class ClientNetworkHandler extends Thread {

    private Gson gson;

    private final Socket socket;
    private boolean handshakeComplete;

    private Client client;

    public ClientNetworkHandler(Socket socket) {
        this.socket = socket;
        this.handshakeComplete = false;
        this.gson = GsonUtils.getGson();
    }

    @Override
    public void run() {
        // Perform connection login, either an endpoint or a client

        try {
            while (!this.handshakeComplete) {
                byte[] packetData = NetworkProtocol.receive(socket.getInputStream());

                String packetDataDecoded = new String(packetData, StandardCharsets.UTF_8);
                JsonObject packetJsonRoot = gson.fromJson(packetDataDecoded, JsonObject.class);

                String targetUUID = packetJsonRoot.get("module_uuid").getAsString();
                if (targetUUID.equals("00000000-0000-0000-0000-000000000000"))
                    this.handshakeComplete = this.attemptLogin(packetJsonRoot.getAsJsonObject("data"));
                else
                    throw new RuntimeException("Client tried to send a packet before completing handshake");
            }

            while (true) {
                byte[] packetData = NetworkProtocol.receive(socket.getInputStream());

                String packetDataDecoded = new String(packetData, StandardCharsets.UTF_8);
                JsonObject packetJsonRoot = gson.fromJson(packetDataDecoded, JsonObject.class);

                UUID packetTarget = UUID.fromString(packetJsonRoot.get("module_uuid").getAsString());

                Optional<Module> optionalTargetModule = ModuleManager.getInstance().getModuleByUUID(packetTarget);
                if (optionalTargetModule.isEmpty()) {
                    Logger.error("Received a packet for a module that does not exist from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                    break;
                }

                Module targetModule = optionalTargetModule.get();
                targetModule.handleConnection(this.client, packetJsonRoot.get("data").getAsJsonObject());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
                MainServer.getNetworkHandler().removeClient(this);
            } catch (IOException ignored) {
            }
        }
    }

    public void sendPacket(UUID origin, Object packet) {
        this.sendPacket(origin, gson.toJsonTree(packet));
    }

    public void sendPacket(UUID origin, JsonElement packet) {
        JsonObject packetJson = new JsonObject();
        packetJson.addProperty("module_uuid", origin.toString());
        packetJson.add("data", packet);
        try {
            NetworkProtocol.send(socket.getOutputStream(), gson.toJson(packetJson).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean attemptLogin(JsonObject loginData) {
        // TODO: implement authentication
        if (!loginData.has("username") || !loginData.has("password") || !loginData.has("client_type")) {
            return false;
        }
        String deviceUsername = loginData.get("username").getAsString();
        String devicePassword = loginData.get("password").getAsString();
        String deviceType = loginData.get("client_type").getAsString();
        this.client = new Client(this, UUID.randomUUID(), Client.ClientType.valueOf(deviceType), deviceUsername);
        Logger.info("Client " + deviceUsername + " logged in as " + deviceType + " with uuid " + this.client.getUUID());
        return true;
    }

}
