package com.guyporat.modules.impl;

import com.google.gson.JsonObject;
import com.guyporat.MainServer;
import com.guyporat.modules.Module;
import com.guyporat.modules.ModuleStatus;
import com.guyporat.networking.PacketType;
import com.guyporat.networking.client.Client;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.GsonUtils;
import com.guyporat.utils.Logger;
import me.nurio.events.handler.EventHandler;
import me.nurio.events.handler.EventListener;

import java.util.*;

public class Notifications extends Module implements EventListener {

    private static final UUID uuid = UUID.fromString("5ae0d00b-1377-4c7c-bc1a-7ec5e63c25a8");

    private final int MAX_NOTIFICATIONS = 10;
    private List<Notification> notifications;

    private ModuleStatus status;

    @Override
    public void start() {
        this.status = ModuleStatus.RUNNING;
        MainServer.getEventManager().registerEvents(this);
    }

    @Override
    public void stop() {
        this.status = ModuleStatus.STOPPED;
    }

    @Override
    public void initialize() {
        this.notifications = new ArrayList<>();
        this.status = ModuleStatus.STOPPED;
    }

    @EventHandler
    public void onFaceRecognized(Camera.FaceRecognitionEvent event) {
        if (event.getFaces().length == 1 && event.getFaces()[0].equals("$Unknown")) {
            this.sendNotification(new Notification(NotificationType.CAMERA_BAD, "זוהה פנים לא מוכרות במצלמה " + event.getCameraName()));
        } else if (event.getFaces().length > 0) {
            String message = "זוהו במצלמה " + event.getCameraName() + ": " + String.join(", ", event.getFaces());
            this.sendNotification(new Notification(NotificationType.CAMERA_INFO, message));
        }
    }

    @Override
    public void handleConnection(Client client, PacketType packetType, JsonObject data) {
        if (packetType == PacketType.GET_NOTIFICATIONS && client instanceof WebClient webClient) {
            this.sendNotifications(webClient);
        }
    }

    private void sendNotifications(WebClient client) {
        // Send a notification to the client
        List<Notification> reversedNotifications = new ArrayList<>(this.notifications);
        Collections.reverse(reversedNotifications);
        client.send(PacketType.GET_NOTIFICATIONS_RESPONSE, GsonUtils.getGson().toJson(reversedNotifications));
    }

    public void sendNotification(Notification notification) {
        // Send a notification to all web clients
        this.addNotification(notification);
        List<WebClient> clients = new ArrayList<>(MainServer.getWebSocketNetworkHandler().getWebClients().values());
        for (WebClient client : clients) {
            client.send(PacketType.NEW_NOTIFICATION, GsonUtils.getGson().toJson(notification));
        }
    }

    private void addNotification(Notification notification) {
        if (this.notifications.size() == MAX_NOTIFICATIONS) {
            this.notifications.remove(0);
        }
        Logger.debug("Added notification: " + notification);
        this.notifications.add(notification);
    }

    @Override
    public ModuleStatus getStatus() {
        return this.status;
    }

    @Override
    public String getName() {
        return "Notifications";
    }

    @Override
    public String getDescription() {
        return "Notification module";
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getVersion() {
        return "0.1b";
    }

    public static UUID getStaticUUID() {
        return uuid;
    }

    public static class Notification {
        private final NotificationType notificationType;
        private final String message;

        private final long timestamp;

        public Notification(NotificationType notificationType, String message) {
            this.notificationType = notificationType;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public NotificationType getNotificationType() {
            return notificationType;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "Notification{" +
                    "notificationType=" + notificationType +
                    ", message='" + message + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Notification that = (Notification) o;

            if (notificationType != that.notificationType) return false;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            int result = notificationType != null ? notificationType.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }
    }

    public enum NotificationType {
        DOOR_BELL, // TODO: implement
        CAMERA_INFO,
        DEVICE_INFO,
        CAMERA_BAD
    }
}
