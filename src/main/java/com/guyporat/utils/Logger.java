package com.guyporat.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final boolean debug = true;

    public static void debug(String message) {
        if (debug)
            System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]" + " [DEBUG] " + message);
    }

    public static void info(String message) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]" + " [INFO] " + message);
    }

    public static void warn(String message) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]" + " [WARN] " + message);
    }

    public static void error(String message) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]" + " [ERROR] " + message);
    }

}
