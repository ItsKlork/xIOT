package com.guyporat.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Utils {

    private static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        mimeTypes.put("image/jpeg", "jpg");
        mimeTypes.put("image/pjpeg", "jpg");
        mimeTypes.put("image/png", "png");
    }

    public static String padLeftSpaces(String inputString, int length) {
        return padLeftChar(inputString, length, ' ');
    }

    public static String padLeftChar(String inputString, int length, char paddingChar) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append(paddingChar);
        }
        sb.append(inputString);

        return sb.toString();
    }

    public static String getExtension(String mimeType) {
        return mimeTypes.get(mimeType);
    }

    public static boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Add dashes to a stripped UUID
     * @param strippedUUID UUID without dashes
     * @return UUID with dashes as a string
     */
    public static String getDashedUUID(String strippedUUID) {
        if (strippedUUID.length() != 32) throw new IllegalArgumentException("UUID must be 32 characters long");
        return strippedUUID.substring(0, 8) + "-" + strippedUUID.substring(8, 12) + "-" + strippedUUID.substring(12, 16) + "-" + strippedUUID.substring(16, 20) + "-" + strippedUUID.substring(20);
    }

}
