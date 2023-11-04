package com.guyporat.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private final Map<String, String> data;
    private final File file;

    public Config(String file_name) {
        this.file = new File(file_name);
        this.data = new HashMap<>();

        // Create config file if it doesn't exist
        if (!this.file.exists()) {
            try {
                boolean creationResult = this.file.createNewFile();
                if (!creationResult)
                    System.err.println("Failed to create config file " + file_name + " (createNewFile returned false)");
                else
                    System.out.println("Created config file " + file_name);
            } catch (IOException e) {
                System.err.println("Failed to create config file " + file_name + ":");
                e.printStackTrace();
            }
        }
    }

    public static boolean doesConfigExist(String file_name) {
        return new File(file_name).exists();
    }

    public void load() throws IOException {
        if (!data.isEmpty())
            data.clear();
        for (String line : Files.readAllLines(this.file.toPath(), StandardCharsets.UTF_8)) {
            System.out.println(line);
            if (!line.startsWith("\""))
                continue;
            int keyEndIndex = line.indexOf('"', 1);
            String key = line.substring(1, keyEndIndex);
            String value = line.substring(keyEndIndex + 3);
            System.out.println(key + " " + value);
            data.put(key, value);
        }
    }

    /**
     * Returns the value of the key.
     *
     * @param key The key to get the value of.
     * @return The value of the key.
     */
    public String get(String key) {
        return data.get(key);
    }

    /**
     * Returns the value of the key as an integer.
     *
     * @param key The key to get the value of.
     * @return The value of the key as an integer.
     * @throws NumberFormatException If the value of the key is not an integer.
     */
    public int getInteger(String key) {
        return Integer.parseInt(data.get(key));
    }

    public void set(String key, String value, boolean save) {
        data.put(key, value);
        if (save) save();
    }

    public void set(String key, int value, boolean save) {
        this.set(key, String.valueOf(value), save);
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.file))) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                builder.append('"');
                builder.append(entry.getKey());
                builder.append('"');
                builder.append(": ");
                builder.append(entry.getValue());
                builder.append("\n");
            }
            writer.write(builder.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
