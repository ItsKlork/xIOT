package com.guyporat.networking.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.guyporat.modules.Module;
import com.guyporat.networking.PacketType;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.Utils;
import org.java_websocket.WebSocket;

import java.util.Optional;

public class WebClient extends Client {

    private static final Gson gson;

    static {
        gson = GsonUtils.getGson();
    }

    private WebSocket webSocket;
    private boolean isAuthenticated = false;
    private String username = null;

    public WebClient(WebSocket ws) {
        this.webSocket = ws;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void send(PacketType packetType, String message) {
        webSocket.send(Utils.padLeftChar(Integer.toString(packetType.getId()), 3, '0') + message);
    }

    public void receive(String message) {
        JsonObject packet = gson.fromJson(message, JsonObject.class);
        PacketType packetType = PacketType.fromId(packet.get("pid").getAsInt());
        if (packetType == null) {
            Logger.error("Client " + (this.username != null ? this.username : this.webSocket.getRemoteSocketAddress().toString()) + " sent an invalid packet type");
            return;
        }

        if (!isAuthenticated && packetType != PacketType.WEB_AUTHENTICATION) {
            System.out.println("Client is not authenticated");
            return;
        }
        if (!isAuthenticated) { // Web authentication attempt
            JsonObject loginData = packet.getAsJsonObject("data");
            String username = loginData.get("username").getAsString();
            String password = loginData.get("password").getAsString();
            if (username.equals("admin") && password.equals("password")) {
                isAuthenticated = true;
                this.username = username;
                send(PacketType.WEB_AUTHENTICATION_RESPONSE, gson.toJson(new AuthResponse("success", "admin", "גיא פורת")));
                System.out.println("Client authenticated");
            } else {
                send(PacketType.WEB_AUTHENTICATION_RESPONSE, gson.toJson(new AuthResponse("invalid_credentials", "פרטי התחברות שגויים")));

                System.out.println("Client failed to authenticate");
            }
            return;
        }

        if (packetType == PacketType.WEB_AUTH_SIGN_OUT) {
            isAuthenticated = false;
            this.username = null;
            send(PacketType.WEB_AUTH_SIGN_OUT_RESPONSE, gson.toJson(new AuthResponse("success", "admin", "גיא פורת")));
            System.out.println("Client signed out");
            return;
        }

        Optional<Module> optionalTarget = packetType.getHandlingModule();
        if (optionalTarget.isPresent())
            optionalTarget.get().handleConnection(this, packetType, packet.get("data").getAsJsonObject());
        else
            Logger.error("Packet type " + packetType + " does not have a valid handling module");
    }

    public void close() {
        webSocket.close();
    }

    @Override
    public ClientType getType() {
        return ClientType.USER;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    private static class AuthResponse {
        final String status;
        final String username;
        final String name;
        final String error;

        private AuthResponse(String status, String error) {
            this.status = status;
            this.username = null;
            this.name = null;
            this.error = error;
        }

        private AuthResponse(String status, String username, String name) {
            this.status = status;
            this.username = username;
            this.name = name;
            this.error = null;
        }
    }

}
