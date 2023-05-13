package com.kinghouser.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

    private Socket client;

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            System.out.println("Server started on port " + serverSocket.getLocalPort());

            client = serverSocket.accept();
            System.out.println("Client connected: " + client.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getClient() {
        return client;
    }
}