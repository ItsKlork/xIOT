package com.guyporat.modules;

import com.guyporat.modules.impl.DoorLock;
import com.guyporat.modules.impl.FaceRecognition;
import com.guyporat.utils.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModuleManager {

    // region Singleton
    private static ModuleManager instance;

    public static ModuleManager getInstance() {
        if (instance == null)
            instance = new ModuleManager();
        return instance;
    }

    private ModuleManager() {
    }
    // endregion

    private List<Module> modules;

    public void initializeManager() {
        Logger.debug("Initializing module manager");
        this.modules = List.of(
                new FaceRecognition(),
                new DoorLock()
        );

        for (Module module : this.modules) {
            module.initialize();
            module.start();
            Logger.debug("Initialized and started module " + module.getName() + " (" + module.getVersion() + ")");
        }
    }

    /**
     * Returns a list of all com.guyporat.modules.
     *
     * @return An <b>unmodifiable</b> list of all com.guyporat.modules.
     */
    public List<Module> getModules() {
        return this.modules;
    }

    public Optional<Module> getModuleByUUID(UUID uuid) {
        return this.modules.stream().filter(module -> module.getUUID().equals(uuid)).findFirst();
    }

}
