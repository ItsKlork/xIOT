package com.guyporat.modules.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.guyporat.MainServer;
import com.guyporat.database.Database;
import com.guyporat.database.model.TenantModel;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.gson.GsonUtils;
import com.guyporat.utils.Logger;
import com.guyporat.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class Tenants extends Module {

    private ModuleStatus status;
    private static final UUID uuid = UUID.fromString("671b2cde-53a9-4ce3-8574-3841b7fdea2e");

    @Override
    public void start() {
        this.status = ModuleStatus.RUNNING;
    }

    @Override
    public void stop() {
        this.status = ModuleStatus.STOPPED;
    }

    public static UUID getStaticUUID() {
        return uuid;
    }

    @Override
    public void initialize() {
        this.status = ModuleStatus.STOPPED;
    }

    @Override
    public void handleConnection(Client client, PacketType packetType, JsonObject data) {
        if (client instanceof WebClient webClient) {
            switch (packetType) {
                case GET_TENANTS ->
                        webClient.send(PacketType.GET_TENANTS_RESPONSE, GsonUtils.getGson().toJson(Database.TenantTableManager.getInstance().getTenants().stream().map(TenantModel::censor).toList()));
                case UPDATE_TENANT -> updateTenant(webClient, data);
                case REMOVE_TENANT -> removeTenant(webClient, data);
                case ADD_TENANT -> addTenant(webClient, data);
            }
        }
    }

    private void addTenant(WebClient webClient, JsonObject data) {
        String fullName = data.get("fullName").getAsString();
        boolean webUser = data.get("webUser").getAsBoolean();
        String faceDataType = data.get("faceDataType").getAsString();
        String faceData = data.get("face").getAsString();

        if (webUser) {
            // Check if username already exists
            String username = data.get("webUsername").getAsString();
            if (Database.TenantTableManager.getInstance().doesTenantExistUsername(username)) {
                this.addTenantError(webClient, "שם המשתמש כבר בשימוש.");
                Logger.error("Failed to add tenant: Username already exists");
                return;
            }
        }

        UUID uuid = UUID.randomUUID();
        String faceDataPath = "faces/" + uuid + "/1." + Utils.getExtension(faceDataType);
        byte[] decodedFaceData = Base64.getDecoder().decode(faceData.getBytes());
        File faceDataFile = new File(faceDataPath);
        if (!faceDataFile.getParentFile().mkdirs()) {
            this.addTenantError(webClient, "Failed to create face data directory");
            Logger.error("Failed to create face data directory");
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(faceDataFile)) {
            fos.write(decodedFaceData);
        } catch (IOException e) {
            this.addTenantError(webClient, "Failed to write face data to file");
            Logger.error("Failed to write face data to file");
            return;
        }
        if (webUser) {
            String username = data.get("webUsername").getAsString();
            String password = data.get("webPassword").getAsString();
            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            Database.TenantTableManager.getInstance().addTenant(new TenantModel(fullName, true, username, hashedPassword, faceDataType, faceDataPath, uuid));
        } else {
            Database.TenantTableManager.getInstance().addTenant(new TenantModel(fullName, faceDataType, faceDataPath, uuid));
        }
        JsonObject response = new JsonObject();
        response.add("response", new JsonPrimitive("success"));
        webClient.send(PacketType.ADD_TENANT_RESPONSE, GsonUtils.getGson().toJson(response));

        // Send updated tenant data to all clients
        for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) {
            wc.send(PacketType.GET_TENANTS_RESPONSE, GsonUtils.getGson().toJson(Database.TenantTableManager.getInstance().getTenants().stream().map(TenantModel::censor).toList()));
        }
    }

    private void addTenantError(WebClient webClient, String message) {
        JsonObject response = new JsonObject();
        response.add("response", new JsonPrimitive("error"));
        response.add("error", new JsonPrimitive(message));
        webClient.send(PacketType.ADD_TENANT_RESPONSE, GsonUtils.getGson().toJson(response));
    }

    private void removeTenant(WebClient webClient, JsonObject data) {
        UUID uuid = UUID.fromString(data.get("uuid").getAsString());
        TenantModel targetTenant = Database.TenantTableManager.getInstance().getTenantByUUID(uuid);
        if (targetTenant == null) {
            this.removeTenantError(webClient, "Tenant not found");
            Logger.error("Failed to remove tenant: Tenant not found");
            return;
        }

        // Delete face data file
        String path = targetTenant.getFaceDataPath();
        try {
            if (!new File(path).delete()) {
                this.removeTenantError(webClient, "Couldn't delete the old face data file");
                Logger.error("Failed to delete face data of user " + targetTenant.getFullName());
                return;
            }
        } catch (SecurityException e) {
            this.removeTenantError(webClient, "Security exception occurred");
            Logger.error("Failed to delete face data of user " + targetTenant.getFullName());
            return;
        }

        Database.TenantTableManager.getInstance().removeTenant(targetTenant.getUUID());
        Logger.info("Removed tenant " + targetTenant.getFullName() + " (" + targetTenant.getUUID() + ")");

        JsonObject response = new JsonObject();
        response.add("response", new JsonPrimitive("success"));
        webClient.send(PacketType.REMOVE_TENANT_RESPONSE, GsonUtils.getGson().toJson(response));

        // Send updated tenant data to all clients
        for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) {
            wc.send(PacketType.GET_TENANTS_RESPONSE, GsonUtils.getGson().toJson(Database.TenantTableManager.getInstance().getTenants().stream().map(TenantModel::censor).toList()));
        }
    }

    private void removeTenantError(WebClient webClient, String message) {
        JsonObject response = new JsonObject();
        response.add("response", new JsonPrimitive("error"));
        response.add("error", new JsonPrimitive(message));
        webClient.send(PacketType.REMOVE_TENANT_RESPONSE, GsonUtils.getGson().toJson(response));
    }

    private void updateTenant(WebClient webClient, JsonObject data) {
        UUID uuid = UUID.fromString(data.get("uuid").getAsString());

        TenantModel targetTenant = Database.TenantTableManager.getInstance().getTenantByUUID(uuid);
        if (targetTenant == null)
            throw new RuntimeException("Tenant not found");

        boolean webUser = data.get("webUser").getAsBoolean();
        targetTenant.setWebUser(webUser);
        if (data.has("fullName"))
            targetTenant.setFullName(data.get("fullName").getAsString());

        String oldFaceDataType = targetTenant.getFaceDataType();
        if (data.has("faceDataType"))
            targetTenant.setFaceDataType(data.get("faceDataType").getAsString());
        if (data.has("face")) {
            // Delete previous face data from file
            String previousPath = "faces/" + uuid + "/1." + Utils.getExtension(oldFaceDataType);
            try {
                if (!new File(previousPath).delete())
                    Logger.error("Failed to delete previous face data of user " + targetTenant.getFullName());
            } catch (SecurityException e) {
                Logger.error("Failed to delete previous face data of user " + targetTenant.getFullName());
            }

            // Update file extension
            targetTenant.setFaceDataPath("faces/" + uuid + "/1." + Utils.getExtension(targetTenant.getFaceDataType()));

            // Write to file
            byte[] decodedFaceData = Base64.getDecoder().decode(data.get("face").getAsString().getBytes());
            try (FileOutputStream fos = new FileOutputStream(targetTenant.getFaceDataPath())) {
                fos.write(decodedFaceData); // Overwrite existing face data
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                Logger.error("Failed to write face data of user " + targetTenant.getFullName() + " to file");
            }
        }

        if (webUser && data.has("updatedPassword")) {
            String updatedPassword = data.get("updatedPassword").getAsString();
            targetTenant.setHashedPassword(BCrypt.withDefaults().hashToString(12, updatedPassword.toCharArray()));
        }
        if (webUser && data.has("webUsername")) {
            targetTenant.setUsername(data.get("webUsername").getAsString());
        }
        Database.TenantTableManager.getInstance().updateTenant(targetTenant);
        webClient.send(PacketType.UPDATE_TENANT_RESPONSE, "{\"response\":\"success\"}");
        Logger.info("Updated tenant " + targetTenant.getFullName() + " (" + targetTenant.getUUID() + ")");
        // Send updated tenant data to all clients
        for (WebClient wc : MainServer.getWebSocketNetworkHandler().getWebClients().values()) {
            wc.send(PacketType.GET_TENANTS_RESPONSE, GsonUtils.getGson().toJson(Database.TenantTableManager.getInstance().getTenants().stream().map(TenantModel::censor).toList()));
        }
    }

    private void updateTenantError(WebClient webClient, String message) {
        JsonObject response = new JsonObject();
        response.add("response", new JsonPrimitive("error"));
        response.add("error", new JsonPrimitive(message));
        webClient.send(PacketType.UPDATE_TENANT_RESPONSE, GsonUtils.getGson().toJson(response));
    }

    @Override
    public ModuleStatus getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return "Tenants";
    }

    @Override
    public String getDescription() {
        return "Tenants module";
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getVersion() {
        return "0.1b";
    }

}
