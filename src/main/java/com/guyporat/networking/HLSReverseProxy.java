package com.guyporat.networking;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.guyporat.modules.impl.Devices;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.Logger;
import com.guyporat.utils.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class HLSReverseProxy {

    /*
    cameras: [{"name": "gay", uuid: "965b568f-cb7a-435b-9cab-ee4f46995bc7", ip: "192.168.1.69"}, {...}]
     */

    /*
        HTTP SERVER
        http://localhost:6969/camera/965b568f-cb7a-435b-9cab-ee4f46995bc7/master.m3u8
        =
        http://cameraIP/master.m3u8
     */
    public void startHTTPServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 10);
        server.createContext("/", new HLSReverseProxyHandler());
        server.setExecutor(null);
        server.start();
        Logger.info("HLS Reverse proxy http server started on port " + port);
    }

    static class HLSReverseProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authentication");
            if (exchange.getRequestMethod().equals("GET")) {
                try {
                    String[] uriParts = exchange.getRequestURI().getPath().substring(1).split("/");
                    if (exchange.getRequestHeaders().get("Authentication") == null) {
                        Logger.error("No authentication header found");
                        exchange.sendResponseHeaders(401, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    String token = exchange.getRequestHeaders().get("Authentication").get(0);
                    if (!token.startsWith("Bearer ")) {
                        Logger.error("Invalid token format: " + token);
                        exchange.sendResponseHeaders(401, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    token = token.substring(7);
                    Algorithm algorithm = Algorithm.HMAC256(WebClient.getSecret());
                    JWT.require(algorithm).build().verify(token);

                    if (uriParts.length != 2) {
                        Logger.error("Invalid URI parts: " + Arrays.toString(uriParts));
                        exchange.sendResponseHeaders(404, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    String cameraUUID = uriParts[0];
                    if (!Utils.isValidUUID(cameraUUID)) {
                        Logger.info("Camera lookup attempt with UUID " + cameraUUID + " failed (invalid UUID)");
                        exchange.sendResponseHeaders(404, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    Logger.debug("Received a request for camera with UUID " + cameraUUID);

                    // Get camera from uuid
                    Optional<DeviceClient> optionalCamera = Devices.getActiveDevices().stream().filter(device -> device.getDeviceUUID().equals(UUID.fromString(cameraUUID)) && device.getDeviceType() == DeviceClient.IOTDeviceType.CAMERA).findAny();
                    if (optionalCamera.isEmpty()) {
                        Logger.info("Camera with UUID " + cameraUUID + " not found");
                        exchange.sendResponseHeaders(404, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    DeviceClient camera = optionalCamera.get();
                    URL url = new URL(camera.getHLSUrl() + "/" + uriParts[1]);
                    URLConnection connection = url.openConnection();
                    Logger.debug("Opening connection to " + url);
                    connection.setDoInput(true);
                    try (InputStream inputStreamFromCamera = connection.getInputStream();
                         OutputStream outputStreamToClient = exchange.getResponseBody()) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        exchange.sendResponseHeaders(200, connection.getContentLength());
                        while (true) {
                            bytesRead = inputStreamFromCamera.read(buffer);
                            if (bytesRead == -1) break;
                            outputStreamToClient.write(buffer, 0, bytesRead);
                        }
                        outputStreamToClient.flush();
                    }
                } catch (JWTVerificationException e) {
                    Logger.error("HLS viewing attempt with invalid token: " + e.getMessage());
                    exchange.sendResponseHeaders(401, 0);
                }
            } else if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, 0);
            } else {
                Logger.debug("Reverse proxy received a non-GET request (" + exchange.getRequestMethod() + ")");
                exchange.sendResponseHeaders(405, 0);
            }
            exchange.getResponseBody().close();
        }
    }

}
