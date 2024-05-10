package com.guyporat.networking;

import com.guyporat.networking.client.WebClient;
import com.guyporat.utils.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;

public class WebSocketNetworkHandler extends WebSocketServer {

    private final Map<InetSocketAddress, WebClient> webClients = new java.util.HashMap<>();

    public WebSocketNetworkHandler(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        Logger.debug(webSocket.getRemoteSocketAddress().getAddress() + " opened");
        webClients.put(webSocket.getRemoteSocketAddress(), new WebClient(webSocket));
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        webClients.remove(webSocket.getRemoteSocketAddress());
        Logger.debug(webSocket.getRemoteSocketAddress().getAddress() + " closed");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        if (webClients.containsKey(webSocket.getRemoteSocketAddress()))
            webClients.get(webSocket.getRemoteSocketAddress()).receive(s);
        else
            System.out.println("Client not found: " + webSocket.getRemoteSocketAddress().toString());
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        webClients.remove(webSocket.getRemoteSocketAddress());
        e.printStackTrace();
        Logger.error("WebSocket error: " + e.getMessage());
    }

    @Override
    public void onClosing(WebSocket conn, int code, String reason, boolean remote) {
        super.onClosing(conn, code, reason, remote);
    }

    @Override
    public void onStart() {
        Logger.info("Started WebSocket server on port " + this.getPort());
    }

    public Map<InetSocketAddress, WebClient> getWebClients() {
        return webClients;
    }
}
