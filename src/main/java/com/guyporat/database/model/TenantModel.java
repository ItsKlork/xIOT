package com.guyporat.database.model;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.annotations.SerializedName;
import com.guyporat.utils.CompressionUtils;
import com.guyporat.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TenantModel {

    private String fullName;

    private boolean webUser;
    private String username;
    private String hashedPassword;

    private String faceDataType;
    private String faceDataPath; // Path to the face data file

    public TenantModel(String fullName, boolean webUser, String username, String hashedPassword, String faceDataType, String faceDataPath) {
        this.fullName = fullName;
        this.webUser = webUser;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.faceDataType = faceDataType;
        this.faceDataPath = faceDataPath;
    }

    /**
     * Temporary testing constructor for plaintext password
     */
    public @Deprecated TenantModel(String fullName, boolean webUser, String username, String plaintextPassword, String faceDataType, String faceDataPath, boolean ignore) {
        this.fullName = fullName;
        this.webUser = webUser;
        this.username = username;
        this.hashedPassword = BCrypt.withDefaults().hashToString(12, plaintextPassword.toCharArray());
        Logger.debug("Hashed password: " + this.hashedPassword);
        this.faceDataType = faceDataType;
        this.faceDataPath = faceDataPath;
    }

    public boolean isWebUser() {
        return webUser;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean comparePassword(String plaintextPassword) {
        if (this.hashedPassword == null) return false;
        return BCrypt.verifyer().verify(plaintextPassword.toCharArray(), this.hashedPassword).verified;
    }

    public TenantModel(String fullName, String faceDataType, String faceDataPath) {
        this(fullName, false, null, null, faceDataType, faceDataPath);
    }

    public byte[] getFaceData() throws IOException {
        return Files.readAllBytes(new File(faceDataPath).toPath());
    }

    public byte[] getCompressedFaceData() throws IOException {
        return CompressionUtils.compressData(getFaceData());
    }

    public CensoredTenantModel censor() {
        return new CensoredTenantModel(this);
    }

    public static class CensoredTenantModel {
        private String fullName;

        private boolean webUser;
        private String username;

        private String faceDataType;
        @SerializedName("face")
        private byte[] faceData;

        public CensoredTenantModel(TenantModel tenantModel) {
            this.fullName = tenantModel.fullName;
            this.webUser = tenantModel.webUser;
            this.username = tenantModel.username;
            this.faceDataType = tenantModel.faceDataType;

            try {
                this.faceData = tenantModel.getFaceData();
            } catch (IOException e) {
                Logger.error("Failed to get face data for censored tenant model");
            }
        }
    }
}
