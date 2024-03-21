package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.config.Config;
import com.guyporat.database.model.TenantModel;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.GsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Tenants extends Module {

    private ModuleStatus status;
    private static final UUID uuid = UUID.fromString("671b2cde-53a9-4ce3-8574-3841b7fdea2e");

    private static List<TenantModel> tenantDatabase;

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

    public static List<TenantModel> getTenantDatabase() {
        return tenantDatabase;
    }

    @Override
    public void initialize() {
        this.status = ModuleStatus.STOPPED;
        tenantDatabase = new ArrayList<>();
        tenantDatabase.add(new TenantModel("גיא פורת", true, "guy", "$2a$12$Sgv6TFy4z5JtZQ1SPOO0SeD/npOHXRx7nTE9Gg1o4wpaQddXhgs5a", "image/jpeg", "faces/Guy Porat/1.jpg"));
        tenantDatabase.add(new TenantModel("ג'ו ביידן", "image/jpeg", "faces/Joe Biden/1.jpg"));
    }

    @Override
    public void handleConnection(Client client, PacketType packetType, JsonObject data) {
        if (client instanceof WebClient webClient) {
            if (packetType == PacketType.GET_TENANTS) {
                webClient.send(PacketType.GET_TENANTS_RESPONSE, GsonUtils.getGson().toJson(tenantDatabase.stream().map(TenantModel::censor).toList()));
            }
        }
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

    @Override
    public Config getConfig() {
        return null;
    }

}
