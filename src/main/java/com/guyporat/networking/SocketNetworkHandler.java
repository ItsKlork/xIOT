package com.guyporat.networking;

import com.guyporat.MainServer;
import com.guyporat.networking.client.ClientSocketNetworkHandler;
import com.guyporat.utils.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketNetworkHandler {

    // Handler fields
    private ServerSocket serverSocket;

    private final List<ClientSocketNetworkHandler> clientThreads = Collections.synchronizedList(new ArrayList<>());

    public void networkLoop() {
        int socketPort = MainServer.getConfig().getInteger("socket_port");
        try {
            this.serverSocket = new ServerSocket(socketPort);
        } catch (IOException e) {
            Logger.error("Failed to start server socket on port " + socketPort + e.getMessage());
            return;
        }

        Logger.info("Started server socket on port " + socketPort);
        new Thread(() -> { // Starts socket accepting loop
            try {
                while (true) {
                    Socket currentClient = serverSocket.accept();
                    Logger.info("Accepted client " + currentClient.getInetAddress().getHostAddress() + ":" + currentClient.getPort());
                    ClientSocketNetworkHandler cnh = new ClientSocketNetworkHandler(currentClient);
                    cnh.start();
                    clientThreads.add(cnh);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "Socket network loop").start();
    }

    public void removeClient(ClientSocketNetworkHandler cnh) {
        clientThreads.remove(cnh);
    }

    public List<ClientSocketNetworkHandler> getAllDeviceNetworkHandlers() {
        return clientThreads;
    }

}
