package com.group15.typefast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public static void main(String[] args) {
        int port = 8080; // Initialize port number
        List<ScoreObject> scoreList = new ArrayList<>(15);
        ArrayList<User> usersList = new ArrayList<>(15);
        ArrayList<Boolean> stat = new ArrayList<>(15);
        for (int i = 0; i < 15; i++) {
            stat.add(false);
        }
        int bestScore = 99999;

        ExecutorService executorService = Executors.newFixedThreadPool(15);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for connections");

            while (true) {
                try {
                    // Create Socket
                    Socket connection = serverSocket.accept();
                    ServerSocketTask serverTask = new ServerSocketTask(connection, usersList, scoreList, bestScore);
                    executorService.submit(serverTask);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure proper shutdown of the executor service
            executorService.shutdown();
        }
    }
}

