package com.guyporat.database.model;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.annotations.SerializedName;
import com.guyporat.utils.CompressionUtils;
import com.guyporat.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class TenantModel {

    private String fullName;

    private boolean webUser;
    private String username;
    private String hashedPassword;

    private String faceDataType;
    private String faceDataPath; // Path to the face data file

    private final UUID uuid;

    public TenantModel(String fullName, boolean webUser, String username, String hashedPassword, String faceDataType, String faceDataPath, UUID uuid) {
        this.fullName = fullName;
        this.webUser = webUser;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.faceDataType = faceDataType;
        this.faceDataPath = faceDataPath;
        this.uuid = uuid;
    }

    /**
     * Temporary testing constructor for plaintext password
     */
    public @Deprecated TenantModel(String fullName, boolean webUser, String username, String plaintextPassword, String faceDataType, String faceDataPath, UUID uuid, boolean ignore) {
        this.fullName = fullName;
        this.webUser = webUser;
        this.username = username;
        this.hashedPassword = BCrypt.withDefaults().hashToString(12, plaintextPassword.toCharArray());
        Logger.debug("Hashed password: " + this.hashedPassword);
        this.faceDataType = faceDataType;
        this.faceDataPath = faceDataPath;
        this.uuid = uuid;
    }

    public boolean isWebUser() {
        return this.webUser;
    }

    public String getUsername() {
        return this.username;
    }

    public String getFullName() {
        return this.fullName;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean comparePassword(String plaintextPassword) {
        if (this.hashedPassword == null) return false;
        return BCrypt.verifyer().verify(plaintextPassword.toCharArray(), this.hashedPassword).verified;
    }

    public TenantModel(String fullName, String faceDataType, String faceDataPath, UUID uuid) {
        this(fullName, false, null, null, faceDataType, faceDataPath, uuid);
    }

    public byte[] getFaceData() {
        try {
            return Files.readAllBytes(new File(faceDataPath).toPath());
        } catch (IOException e) {
            Logger.error("Failed to read face data of user " + fullName + " (" + e + ")");
            return null;
        }
    }

    public byte[] getCompressedFaceData() {
        return CompressionUtils.compressData(getFaceData());
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setWebUser(boolean webUser) {
        this.webUser = webUser;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setFaceDataType(String faceDataType) {
        this.faceDataType = faceDataType;
    }

    public void setFaceDataPath(String faceDataPath) {
        this.faceDataPath = faceDataPath;
    }

    public CensoredTenantModel censor() {
        return new CensoredTenantModel(this);
    }

    public String getFaceDataType() {
        return this.faceDataType;
    }

    public String getFaceDataPath() {
        return this.faceDataPath;
    }

    public static class CensoredTenantModel {
        private String fullName;

        private boolean webUser;
        private String username;

        private UUID uuid;

        private String faceDataType;
        @SerializedName("face")
        private byte[] faceData;

        public CensoredTenantModel(TenantModel tenantModel) {
            this.fullName = tenantModel.fullName;
            this.webUser = tenantModel.webUser;
            this.username = tenantModel.username;
            this.faceDataType = tenantModel.faceDataType;
            this.uuid = tenantModel.uuid;

            this.faceData = tenantModel.getFaceData();
        }
    }
}
