package com.guyporat.networking;

import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleManager;
import com.guyporat.modules.impl.Camera;
import com.guyporat.modules.impl.Devices;
import com.guyporat.modules.impl.Tenants;

import java.util.Optional;
import java.util.UUID;

public enum PacketType {

    // REGION Web client to server websocket packets (0 - 49)
    WEB_AUTHENTICATION(0, null),
    WEB_AUTH_SIGN_OUT(1, null),
    GET_DEVICES(2, Devices.getStaticUUID()),
    GET_DEVICE(3, Devices.getStaticUUID()),
    SET_DEVICE_SETTINGS(4, Devices.getStaticUUID()),
    CREATE_DEVICE(5, Devices.getStaticUUID()),
    GET_TENANTS(6, Tenants.getStaticUUID()),
    UPDATE_TENANT(7, Tenants.getStaticUUID()),
    // END REGION


    // REGION Server to web client websocket packets (50 - 99)
    WEB_AUTHENTICATION_RESPONSE(50),
    WEB_AUTH_SIGN_OUT_RESPONSE(51),
    GET_DEVICES_RESPONSE(52),
    GET_DEVICE_RESPONSE(53),
    SET_DEVICE_SETTINGS_RESPONSE(54),
    CREATE_DEVICE_RESPONSE(55),
    GET_TENANTS_RESPONSE(56),
    UPDATE_TENANT_RESPONSE(57),
    // END REGION


    // REGION Device client to server packets (100 - 149)
    DEVICE_AUTHENTICATION(100, null),

    // Camera module packets
    GET_CAMERA_SETTINGS(101, Camera.getStaticUUID()),
    GET_FACE_RECOGNITION_FACE_DATASET(102, Camera.getStaticUUID()),
    REPORT_FACE_RECOGNITION_DETECTION(103, Camera.getStaticUUID()),

    // Devices module packets

    // END REGION


    // REGION Server to device client packets (150 - 199)
    DEVICE_AUTHENTICATION_RESPONSE(150),
    DEVICE_SETTINGS(151),
    FACE_RECOGNITION_FACE_DATASET(152);
    // END REGION


    private final int id;
    private final UUID handlingModuleUUID;

    /**
     * Used for client to server packets
     * @param id the id of the packet
     * @param handlingModuleUUID the UUID of the module that should handle this packet
     */
    PacketType(int id, UUID handlingModuleUUID) {
        this.id = id;
        this.handlingModuleUUID = handlingModuleUUID;
    }

    /**
     * Use for response packets
     * @param id the id of the packet
     */
    PacketType(int id) {
        this.id = id;
        this.handlingModuleUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    public int getId() {
        return id;
    }

    public UUID getHandlingModuleUUID() {
        return handlingModuleUUID;
    }

    public boolean isResponse() {
        return handlingModuleUUID.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public Optional<Module> getHandlingModule() {
        return ModuleManager.getInstance().getModuleByUUID(handlingModuleUUID);
    }

    public static PacketType fromId(int id) {
        for (PacketType packetType : PacketType.values()) {
            if (packetType.getId() == id) {
                return packetType;
            }
        }
        return null;
    }



}
