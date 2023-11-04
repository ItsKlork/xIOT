package com.guyporat.networking;

import com.guyporat.MainServer;
import com.guyporat.networking.client.ClientNetworkHandler;
import com.guyporat.utils.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkHandler {

    // Handler fields
    private ServerSocket serverSocket;

    private List<ClientNetworkHandler> clientThreads;

    public void networkLoop() {
        clientThreads = Collections.synchronizedList(new ArrayList<>());
        try {
            this.serverSocket = new ServerSocket(MainServer.getConfig().getInteger("port"));
        } catch (IOException e) {
            Logger.error("Failed to start server socket on port " + MainServer.getConfig().getInteger("port") + e.getMessage());
            return;
        }
        Logger.info("Started server socket on port " + MainServer.getConfig().getInteger("port"));
        new Thread(() -> {
            try {
                while (true) {
                    Socket currentClient = serverSocket.accept();
                    Logger.info("Accepted client " + currentClient.getInetAddress().getHostAddress() + ":" + currentClient.getPort());
                    ClientNetworkHandler cnh = new ClientNetworkHandler(currentClient);
                    cnh.start();
                    clientThreads.add(cnh);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "Network loop").start();
    }

    public void removeClient(ClientNetworkHandler cnh) {
        clientThreads.remove(cnh);
    }

}
