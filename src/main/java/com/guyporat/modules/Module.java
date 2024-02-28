package com.guyporat.modules;

import com.google.gson.JsonObject;
import com.guyporat.config.Config;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import me.nurio.events.handler.Event;

import java.util.UUID;

public abstract class Module {

    // Power state
    public abstract void start();

    public abstract void stop();

    public abstract void initialize();

    public abstract ModuleStatus getStatus();

    // Module details
    public abstract String getName();

    public abstract String getDescription();

    public abstract UUID getUUID();

    public abstract String getVersion();

    // Config file
    public abstract Config getConfig();

    // Connection handler
    public void handleConnection(Client client, PacketType packetType, JsonObject data) {
    }

    public Event getEvent() {
        return null;
    }

}
