package com.group15.typefast;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ClientSocketTask implements Runnable {

    private User user = new User(); 
    private String request = null; 
    private String response = null; 
    private String answer = null; 
    private Scanner scanner = new Scanner(System.in); 
    private String ip = "localhost"; 
    private int port = 8080; 

    Socket connection;
    ObjectOutputStream oos;
    BufferedReader br;
    BufferedWriter bw;

    public ClientSocketTask() {
        this.user.setLoggedIn(false);
        this.user.setTeamd(false);
        this.user.setReady(false);
        this.user.setSpectator(false);
    }

    @Override
    public void run() {
        while (true) {
            try {
                establishConnection(); 
                handleCommunication(); 
                break; 
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace(); 
                closeConnection(); 
                System.out.println("\033[0;33mReconnecting...\033[0m");
                try {
                    Thread.sleep(2000); 
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); 
                }
            }
        }
    }

    private void establishConnection() throws IOException {
        connection = new Socket(ip, port);
        oos = new ObjectOutputStream(connection.getOutputStream());
        bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        System.out.println("\033[0;32mConnected to the server!\033[0m");
    }

    private void handleCommunication() throws IOException, ClassNotFoundException {
        while (true) {
            handleUserInput(scanner); 

            if (request.equals("q")) {
                sendRequest(); 
                break;
            }

            if (!user.inGame) sendRequest(); 

            handleServerResponse(); 

            try {
                Thread.sleep(300); 
            } catch (InterruptedException e) {
                e.printStackTrace(); 
            }
        }

        System.out.println("\033[0;31mConnection will terminate\033[0m");
    }

    private void handleUserInput(Scanner scanner) {
        if (!this.user.isLoggedIn()) {
            System.out.println("\033[0;34m'q' : EXIT\033[0m\n");
            System.out.println("\033[0;36mTypeFast is an exciting team-based typing game designed to test your speed and accuracy. " +
                               "In this game, you and your two teammates must type a given word correctly. " +
                               "Once all three players have successfully typed the first word, a new word is provided to the team. " +
                               "The challenge continues with increasingly complex words, pushing your typing skills to the limit. " +
                               "Team coordination and quick reflexes are essential to succeed in TypeFast. " +
                               "Get ready to type fast and work together to achieve the highest score!!\033[0m\n");
            System.out.print("\033[0;35mMake a choice: 1- Register 2- Login\nCHOOSE THEN PRESS ENTER: \033[0m");

            String choice;
            do {
                choice = scanner.nextLine();
                if (choice.equals("1")) {
                    request = "register"; 
                } else if (choice.equals("2")) {
                    request = "login"; 
                } else if (choice.equals("q")) {
                    request = "q"; 
                    return;
                }
            } while (!choice.equals("1") && !choice.equals("2"));

            System.out.print("\033[0;33mEnter Your Username: \033[0m");
            String username = scanner.next();
            System.out.print("\033[0;33mEnter Your Password: \033[0m");
            String password = scanner.next();
            user = new User(username, password); 
            scanner.nextLine(); 
        } else if (!this.user.isTeamd()) {
            System.out.println("\033[0;34mHello " + this.user.getUsername() + " Score: " + this.user.getScore() + "\033[0m");
            System.out.println("\033[0;34mType 'ready' to join a team or 'exit' to quit.\033[0m");
            String choice = scanner.next().toLowerCase();

            if (choice.equals("ready")) {
                System.out.println("\n\033[0;32mTeam making ... Please be patient\033[0m");
                request = "make a team"; 
            } else if (choice.equals("exit")) {
                request = "q"; 
            }
            scanner.nextLine(); 
        } else if (this.user.isTeamd() && !this.user.inGame()) {
            System.out.println("\033[0;34mType anything to start the game or 'exit' to quit.\033[0m");
            String choice = scanner.next();

            if (choice.equals("exit")) {
                request = "q"; 
            } else {
                user.setReady(true);
                request = "start a game"; 
            }
            scanner.nextLine(); 
        } else if (this.user.isSpectator()) {
            System.out.println("\033[0;34m(Q/q to exit the game)\033[0m");
            request = "spectate"; 
        }
    }

    private void sendRequest() throws IOException {
        try {
            oos.writeObject(this.user); 
            oos.writeObject(request); 
            oos.flush();
            System.out.println("\033[0;32mSent request: " + request + "\033[0m");
        } catch (SocketException e) {
            System.out.println("\033[0;31mConnection lost while sending request.\033[0m");
            throw e;
        }
    }

    private void handleServerResponse() throws IOException, ClassNotFoundException {
        try {
            if (request.equals("register")) {
                response = br.readLine(); 
                if (response != null) {
                    System.out.println(response); 
                    System.out.println("\n");
                }
            } else if (request.equals("login")) {
                String line = br.readLine(); 
                if (line != null) {
                    boolean verified = Boolean.parseBoolean(line); 
                    if (verified) {
                        System.out.println("\033[0;32mlogged in successfully!\033[0m");
                        System.out.println("\n");
                        user.setLoggedIn(true); 
                    } else {
                        System.out.println("\033[0;31mlogin failed\033[0m");
                        System.out.println("\n");
                    }
                }
            } else if (request.equals("make a team")) {
                System.out.println("\033[0;32mPlease wait until enough members are ready...\033[0m");
                System.out.println("\n");

                while (true) {
                    String serverMessage = br.readLine(); 
                    if (serverMessage == null) {
                        System.out.println("\033[0;31mServer closed connection unexpectedly.\033[0m");
                        System.out.println("\n");
                        break;
                    } else if (serverMessage.equals("Waiting for team members...")) {
                        System.out.println(serverMessage); 
                        System.out.println("\n");

                        try {
                            Thread.sleep(1000); 
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); 
                            throw new IOException("Thread was interrupted", e);
                        }
                    } else {
                        int teamID = Integer.parseInt(serverMessage); 
                        user.setTeamID(teamID); 
                        System.out.println("\033[0;32mYou are now in team '" + teamID + "'\033[0m");
                        System.out.println("\n");
                        user.setTeamd(true); 
                        break;
                    }
                }
            } else if (request.equals("start a game")) {
                String serverMessage = br.readLine(); 
                if (serverMessage != null) {
                    System.out.println(serverMessage); 
                    System.out.println("\n");

                    serverMessage = br.readLine();
                    if (serverMessage != null && serverMessage.startsWith("Game started")) {
                        countDown(); 
                        System.out.println(serverMessage);
                        System.out.println("\n");
                        serverMessage = br.readLine();
                        System.out.println(serverMessage);
                        System.out.println("\n");
                        System.out.print("\033[0;33m(Q/q to spectate) Your answer: \033[0m");
                        System.out.println("\n");

                        answer = scanner.nextLine(); 

                        oos.writeObject(answer); 
                        oos.flush();
                        try {
                            Thread.sleep(300); 
                        } catch (InterruptedException e) {
                            e.printStackTrace(); 
                        }

                        handleGameSession(); 
                    }
                }
            } else if (request.equals("submit answer")) {
                handleGameSession(); 
            } else if (request.equals("next round")) {
                this.user.resetTrials(); 
                String serverMessage = br.readLine(); 
                System.out.println(serverMessage);
                System.out.println("\n");

                if (serverMessage.startsWith("Congratulations")) {
                    this.user.setSpectator(true); 
                    return;
                }
                System.out.print("\033[0;33m(Q/q to spectate) Your answer: \033[0m");
                answer = scanner.nextLine();
                oos.writeObject(answer); 
                oos.flush();
                try {
                    Thread.sleep(300); 
                } catch (InterruptedException e) {
                    e.printStackTrace(); 
                }
                handleGameSession(); 
            } else if (request.equals("wrong answer")) {
                System.out.print("\033[0;33m(Q/q to spectate) Your answer: \033[0m");
                answer = scanner.nextLine();
                oos.writeObject(answer); 
                oos.flush();
                handleGameSession(); 
                try {
                    Thread.sleep(300); 
                } catch (InterruptedException e) {
                    e.printStackTrace(); 
                }
            } else if (request.equals("spectate")) {
                handleGameSession(); 
            }
        } catch (SocketException e) {
            System.out.println("\033[0;31mConnection lost while receiving response.\033[0m");
            throw e;
        }
    }

    private void handleGameSession() throws IOException, ClassNotFoundException {
        this.user.setInGame(true); 
        while (this.user.inGame() && !this.user.isSpectator()) {
            String serverMessage = br.readLine(); 
            if (serverMessage != null) {
                if (serverMessage.startsWith("You are")) {
                    System.out.println(serverMessage); 
                    user.setSpectator(true); 
                    request = "spectate"; 
                    break;
                } else if (serverMessage.startsWith("Correct")) {
                    System.out.println(serverMessage + "    Trials = " + this.user.getCurrentTrials()); 
                    serverMessage = br.readLine();
                    if (serverMessage != null && serverMessage.contains("All")) {
                        System.out.println(serverMessage); 
                        request = "next round"; 
                        break;
                    }
                } else {
                    request = "wrong answer"; 
                    System.out.println(serverMessage); 
                    this.user.setCurrentTrials(this.user.getCurrentTrials() + 1); 
                    break;
                }
            }
        }
        while (this.user.isSpectator()) {
            String serverMessage = br.readLine(); 
            if (serverMessage != null) {
                System.out.println(serverMessage); 
                if (serverMessage.contains("All")) {
                    serverMessage = br.readLine(); 
                }
                if (serverMessage.contains("New word")) {
                    System.out.println(serverMessage); 
                }
            }
        }
    }

    private void closeConnection() {
        try {
            if (oos != null) oos.close(); 
            if (bw != null) bw.close(); 
            if (br != null) br.close(); 
            if (connection != null) connection.close(); 
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }

    private void countDown() {
        int timeLeft = 3; 
        while (timeLeft > 0) {
            System.out.println("\033[0;31mStarting in: " + timeLeft + "\033[0m"); 
            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                System.out.println("\033[0;31mTimer interrupted\033[0m");
                return;
            }
            timeLeft--;
        }
    }
}
