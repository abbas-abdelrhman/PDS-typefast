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
                break; // Exit the loop if communication is handled successfully
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                closeConnection();
                System.out.println("Reconnecting...");
                try {
                    Thread.sleep(2000); // Wait for 2 seconds before retrying
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
        System.out.println("connected");
    }

    private void handleCommunication() throws IOException, ClassNotFoundException {
        while (true) {
            handleUserInput(scanner);

            if (request.equals("q")) {
                sendRequest();
                break;
            }

            if(!(user.inGame))sendRequest();
            
            handleServerResponse();

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        System.out.println("Connection will terminate");
    }

    private void handleUserInput(Scanner scanner) {
        if (!this.user.isLoggedIn()) {
            System.out.print("'q' : EXIT\n");
            System.out.print("GAME DESCRIPTION! Make a choice: 1- Register 2- Login\n");
            System.out.print("CHOOSE THEN PRESS ENTER: ");

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

            System.out.println("Enter Your Username: ");
            String username = scanner.next();
            System.out.println("Enter Your Password: ");
            String password = scanner.next();
            user = new User(username, password);
            scanner.nextLine(); // consume the newline
        } else if (!this.user.isTeamd()) {
            System.out.println("Hello " + this.user.getUsername() + " Score: " + this.user.getScore());
            System.out.println("Type ready to join a team or exit to quit.");
            String choice = scanner.next().toLowerCase();

            if (choice.equals("ready")) {
                System.out.println("Team making ... Please be patient");
                request = "make a team";
            } else if (choice.equals("exit")) {
                request = "q";
            }
            scanner.nextLine(); // consume the newline
        } else if (this.user.isTeamd() && !this.user.inGame()) {
            System.out.println("Type anything to start the game or exit to quit.");
            String choice = scanner.next();

            if (choice.equals("exit")) {
                request = "q";
            } else {
                user.setReady(true);
                request = "start a game";
            }
            scanner.nextLine(); // consume the newline

        } else if (this.user.isSpectator()) {
            System.out.println("(Q/q to exit the game)");
            request = "spectate";
        }
    }

    private void sendRequest() throws IOException {
        try {
            oos.writeObject(this.user);
            oos.writeObject(request);
            oos.flush();
            System.out.println("Sent request: " + request);
        } catch (SocketException e) {
            System.out.println("Connection lost while sending request.");
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
                        System.out.println("logged in successfully!");
                        System.out.println("\n");

                        user.setLoggedIn(true);
                    } else {
                        System.out.println("login failed");
                        System.out.println("\n");

                    }
                }
            } else if (request.equals("make a team")) {
                System.out.println("Please wait until enough members are ready...");
                System.out.println("\n");

                while (true) {
                    String serverMessage = br.readLine();
                    if (serverMessage == null) {
                        System.out.println("Server closed connection unexpectedly.");
                        System.out.println("\n");

                        break;
                    } else if (serverMessage.equals("Waiting for team members...")) {
                        System.out.println(serverMessage);
                        System.out.println("\n");

                        // Optionally, sleep for a short period to avoid busy-waiting
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore the interrupted status
                            throw new IOException("Thread was interrupted", e);
                        }
                    } else {
                        int teamID = Integer.parseInt(serverMessage);
                        user.setTeamID(teamID);
                        System.out.println("You are now in team '" + teamID + "'");
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

                        System.out.print("(Q/q to spectate) Your answer: ");
                        System.out.println("\n");

                        answer = scanner.nextLine();

                            oos.writeObject(answer);
                            oos.flush();
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
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
                System.out.print("(Q/q to spectate) Your answer: ");
                    answer = scanner.nextLine();
                    oos.writeObject(answer);
                    oos.flush();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handleGameSession();

            } else if (request.equals("wrong answer")) {
                System.out.print("(Q/q to spectate) Your answer: ");
                answer = scanner.nextLine();
                
                    oos.writeObject(answer);
                    oos.flush();
                    
                    handleGameSession();

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                
            } else if (request.equals("spectate")) {         
                    
                handleGameSession();
            }
        } catch (SocketException e) {
            System.out.println("Connection lost while receiving response.");
            throw e;
        }
    }

    private void handleGameSession() throws IOException, ClassNotFoundException {
        this.user.setInGame(true);
        while (this.user.inGame() && !this.user.isSpectator()) {
            String serverMessage = br.readLine();
            if (serverMessage != null) {
                
                if (serverMessage.startsWith("You are")){ 
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
            if(serverMessage != null) {    
                System.out.println(serverMessage);
                if(serverMessage.contains("All")){
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
            System.out.println("Starting in: " + timeLeft);
            try {
                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.out.println("Timer interrupted");
                return;
            }
            timeLeft--;
        }
    }
}
