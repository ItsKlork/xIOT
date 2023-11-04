package com.guyporat;

import com.guyporat.config.Config;
import com.guyporat.modules.ModuleManager;
import com.guyporat.networking.NetworkHandler;
import com.guyporat.utils.Logger;
import me.nurio.events.EventManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MainServer {

    private static Config config;
    private static final String mainConfigName = "main.cfg";

    private static NetworkHandler networkHandler;

    private static final EventManager eventManager = new EventManager();

    public static void main(String[] args) {
        initializeMainConfig();

        ModuleManager.getInstance().initializeManager();

        networkHandler = new NetworkHandler();
        networkHandler.networkLoop();

    }

    private static void initializeMainConfig() {
        try {
            if (!Config.doesConfigExist(mainConfigName)) {
                InputStream is = MainServer.class.getResourceAsStream("/" + mainConfigName);
                if (is == null) {
                    Logger.error("Failed to load main config file from resources");
                    return;
                }

                Files.copy(is, new File(mainConfigName).toPath());
                is.close();
                Logger.info("Created a default main config file");
            }
            config = new Config("main.cfg");
            config.load();
            Logger.debug("Loaded main com.guyporat.config file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Config getConfig() {
        return config;
    }

    public static NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public static EventManager getEventManager() {
        return eventManager;
    }
}
