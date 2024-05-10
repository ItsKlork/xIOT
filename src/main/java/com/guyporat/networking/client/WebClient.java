package com.guyporat.networking.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.database.Database;
import com.guyporat.database.model.TenantModel;
import com.guyporat.modules.Module;
import com.guyporat.networking.PacketType;
import com.guyporat.utils.gson.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.Utils;
import org.java_websocket.WebSocket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

public class WebClient extends Client {

    private static final Gson gson;
    private static final String secret;

    static {
        gson = GsonUtils.getGson();
        secret = MainServer.getConfig().get("jwt_secret");
    }

    private final WebSocket webSocket;
    private boolean isAuthenticated = false;
    private String username = null;

    public WebClient(WebSocket ws) {
        this.webSocket = ws;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public static String getSecret() {
        return secret;
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

        if (!isAuthenticated && packetType != PacketType.WEB_AUTHENTICATION && packetType != PacketType.WEB_AUTH_TOKEN) {
            System.out.println("Client is not authenticated");
            return;
        }
        if (!isAuthenticated && packetType == PacketType.WEB_AUTHENTICATION) { // Web authentication attempt (used PacketType.WEB_AUTHENTICATION)
            JsonObject loginData = packet.getAsJsonObject("data");
            String username = loginData.get("username").getAsString();
            String password = loginData.get("password").getAsString();
            TenantModel targetTenant = Database.TenantTableManager.getInstance().getTenantByUsername(username);
            if (targetTenant == null) {
                send(PacketType.WEB_AUTHENTICATION_RESPONSE, gson.toJson(new AuthResponse("invalid_credentials", "פרטי התחברות שגויים")));
                Logger.debug("Client " + webSocket.getRemoteSocketAddress() + " failed to authenticate");
                return;
            }

            if (targetTenant.comparePassword(password)) {
                isAuthenticated = true;
                this.username = username;
                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("username", this.username);
                response.addProperty("name", targetTenant.getFullName());
                Algorithm algorithm = Algorithm.HMAC256(secret);

                JsonObject tenantInToken = new JsonObject();
                tenantInToken.addProperty("uuid", targetTenant.getUUID().toString());
                tenantInToken.addProperty("username", targetTenant.getUsername());
                tenantInToken.addProperty("name", targetTenant.getFullName());
                String jwtToken = JWT.create().withIssuedAt(Instant.now()).withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).withPayload(tenantInToken.toString()).sign(algorithm);

                response.addProperty("token", jwtToken);
                send(PacketType.WEB_AUTHENTICATION_RESPONSE, response.toString());
                Logger.debug("Client " + webSocket.getRemoteSocketAddress() + " authenticated");
            } else {
                send(PacketType.WEB_AUTHENTICATION_RESPONSE, gson.toJson(new AuthResponse("invalid_credentials", "פרטי התחברות שגויים")));
                Logger.debug("Client " + webSocket.getRemoteSocketAddress() + " failed to authenticate");
            }
            return;
        }
        if (!isAuthenticated) { // Auth with token
            JsonObject packetData = packet.getAsJsonObject("data");
            String token = packetData.get("token").getAsString();
            try {
                Algorithm algorithm = Algorithm.HMAC256(secret);
                DecodedJWT decodedJWT = JWT.require(algorithm).build().verify(token);
                JsonObject payloadUser = GsonUtils.getGson().fromJson(new String(Base64.getDecoder().decode(decodedJWT.getPayload())), JsonObject.class);
                isAuthenticated = true;
                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("username", payloadUser.get("username").getAsString());
                response.addProperty("name", payloadUser.get("name").getAsString());
                send(PacketType.WEB_AUTH_TOKEN_RESPONSE, response.toString());
                Logger.debug("Client " + webSocket.getRemoteSocketAddress() + " authenticated with token");
            } catch (JWTVerificationException e) {
                send(PacketType.WEB_AUTH_TOKEN_RESPONSE, gson.toJson(new AuthResponse("invalid_token", "טוקן לא תקין, אנא בצע התחברות מחדש")));
                Logger.debug("Client " + webSocket.getRemoteSocketAddress() + " failed to authenticate with token");
            }
            return;
        }
        if (packetType == PacketType.WEB_AUTH_SIGN_OUT) {
            isAuthenticated = false;
            this.username = null;
            send(PacketType.WEB_AUTH_SIGN_OUT_RESPONSE, gson.toJson(new AuthResponse("success", null, null)));
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
