package com.guyporat.utils;

public class Utils {

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

}
