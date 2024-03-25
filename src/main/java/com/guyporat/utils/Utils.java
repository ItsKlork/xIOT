package com.guyporat.utils;

import java.util.HashMap;
import java.util.Map;

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

}
