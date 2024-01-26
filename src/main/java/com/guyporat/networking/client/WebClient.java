package com.guyporat.networking.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleManager;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.Utils;
import org.java_websocket.WebSocket;

import java.util.Optional;
import java.util.UUID;

public class WebClient extends Client {

    private static final Gson gson;

    static {
        gson = GsonUtils.getGson();
    }

    private WebSocket webSocket;
    private boolean isAuth = false;
    private String username = null;

    public WebClient(WebSocket ws) {
        this.webSocket = ws;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void send(String channel, String message) {
        webSocket.send(Utils.padLeftSpaces(channel, 10) + message);
    }

    public void receive(String message) {
        JsonObject packet = gson.fromJson(message, JsonObject.class);
        if (!isAuth && !packet.get("type").getAsString().equals("auth")) {
            System.out.println("Client is not authenticated");
            return;
        }
        if (!isAuth) {
            JsonObject loginData = packet.getAsJsonObject("data");
            String username = loginData.get("username").getAsString();
            String password = loginData.get("password").getAsString();
            if (username.equals("admin") && password.equals("password")) {
                isAuth = true;
                this.username = username;
                send("auth", gson.toJson(new AuthResponse("success", "admin", "גיא פורת")));
                System.out.println("Client authenticated");
            } else {
                send("auth", gson.toJson(new AuthResponse("invalid_credentials", "פרטי התחברות שגויים")));

                System.out.println("Client failed to authenticate");
            }
            return;
        }
        if (packet.get("type").getAsString().equals("signout")) {
            isAuth = false;
            this.username = null;
            send("signout", gson.toJson(new AuthResponse("success", "admin", "גיא פורת")));
            System.out.println("Client signed out");
            return;
        }

        if (packet.get("type").getAsString().equals("module")) {
            String moduleUUID = packet.get("module_uuid").getAsString();
            Optional<Module> optionalTarget = ModuleManager.getInstance().getModuleByUUID(UUID.fromString(moduleUUID));
            if (optionalTarget.isPresent())
                optionalTarget.get().handleConnection(this, packet.get("data").getAsJsonObject());
            else
                Logger.warn("Client " + this.username + " tried to send a packet to a non-existent module " + moduleUUID);
            return;
        }
        System.out.println("Got message: " + message);
    }

    public void close() {
        webSocket.close();
    }

    @Override
    public ClientType getType() {
        return ClientType.USER;
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
