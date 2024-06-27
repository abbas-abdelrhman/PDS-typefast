package com.group15.typefast;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * ClientSocketTask handles the client-side socket communication for the TypeFast game.
 * It manages user interactions, sends requests to the server, and processes server responses.
 */

public class ClientSocketTask implements Runnable {

    // User-related fields
    private User user = new User(); // User object representing the current user
    private String request = null; // The request to be sent to the server
    private String response = null; // The response received from the server
    private String answer = null; // The answer input by the user
    private Scanner scanner = new Scanner(System.in); // Scanner for reading user input
    private String ip = "localhost"; // Server IP address
    private int port = 8080; // Server port number

    // Socket and stream fields
    private Socket connection;
    private ObjectOutputStream oos;
    private BufferedReader br;
    private BufferedWriter bw;

    /**
     * Constructor to initialize the user object with default values.
     */
    public ClientSocketTask() {
        this.user.setLoggedIn(false);
        this.user.setTeamd(false);
        this.user.setReady(false);
        this.user.setSpectator(false);
    }

    /**
     * The main loop that manages the connection. It continually tries to establish a connection
     * and handle communication with the server. If an exception occurs, it waits for 2 seconds before retrying.
     */
    @Override
    public void run() {
        while (true) {
            try {
                establishConnection(); // Establish the connection to the server
                handleCommunication(); // Handle the communication with the server
                break; // Break the loop if the connection and communication are successful
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace(); // Print the stack trace for the exception
                closeConnection(); // Close the connection and resources
                System.out.println("\033[0;33mReconnecting...\033[0m");
                try {
                    Thread.sleep(2000); // Wait for 2 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                }
            }
        }
    }

    /**
     * Establishes the socket connection to the server and initializes the input/output streams.
     *
     * @throws IOException if an I/O error occurs when opening the socket or streams
     */
    private void establishConnection() throws IOException {
        connection = new Socket(ip, port);
        oos = new ObjectOutputStream(connection.getOutputStream());
        bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        System.out.println("\033[0;32mConnected to the server!\033[0m");
    }

    /**
     * Manages the communication between the client and the server.
     * It handles user inputs, sends requests, and processes server responses.
     *
     * @throws IOException if an I/O error occurs during communication
     * @throws ClassNotFoundException if a class cannot be found during object deserialization
     */
    private void handleCommunication() throws IOException, ClassNotFoundException {
        while (true) {
            handleUserInput(scanner); // Handle user input

            if (request.equals("q")) {
                sendRequest(); // Send the request to the server
                break;
            }

            if (!user.inGame) sendRequest(); // Send the request if the user is not in a game

            handleServerResponse(); // Handle the server's response

            try {
                Thread.sleep(300); // Wait for 300 milliseconds between requests
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\033[0;31mConnection will terminate\033[0m");
    }

    /**
     * Reads and processes user inputs from the console, updating the request field based on user actions.
     *
     * @param scanner the scanner for reading user input
     */
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
                    request = "register"; // Set request to "register" if the user chooses to register
                } else if (choice.equals("2")) {
                    request = "login"; // Set request to "login" if the user chooses to log in
                } else if (choice.equals("q")) {
                    request = "q"; // Set request to "q" if the user chooses to exit
                    return;
                }
            } while (!choice.equals("1") && !choice.equals("2"));

            System.out.print("\033[0;33mEnter Your Username: \033[0m");
            String username = scanner.next();
            System.out.print("\033[0;33mEnter Your Password: \033[0m");
            String password = scanner.next();
            user = new User(username, password); // Create a new user with the provided username and password
            scanner.nextLine(); // Consume the newline character
        } else if (!this.user.isTeamd()) {
            System.out.println("\033[0;34mHello " + this.user.getUsername() + " Score: " + this.user.getScore() + "\033[0m");
            System.out.println("\033[0;34mType 'ready' to join a team or 'exit' to quit.\033[0m");
            String choice = scanner.next().toLowerCase();

            if (choice.equals("ready")) {
                System.out.println("\n\033[0;32mTeam making ... Please be patient\033[0m");
                request = "make a team"; // Set request to "make a team" if the user chooses to join a team
            } else if (choice.equals("exit")) {
                request = "q"; // Set request to "q" if the user chooses to exit
            }
            scanner.nextLine(); // Consume the newline character
        } else if (this.user.isTeamd() && !this.user.inGame()) {
            System.out.println("\033[0;34mType anything to start the game or 'exit' to quit.\033[0m");
            String choice = scanner.next();

            if (choice.equals("exit")) {
                request = "q"; // Set request to "q" if the user chooses to exit
            } else {
                user.setReady(true);
                request = "start a game"; // Set request to "start a game" if the user chooses to start the game
            }
            scanner.nextLine(); // Consume the newline character
        } else if (this.user.isSpectator()) {
            System.out.println("\033[0;34m(Q/q to exit the game)\033[0m");
            request = "spectate"; // Set request to "spectate" if the user is a spectator
        }
    }

    /**
     * Sends the current user object and request string to the server using the ObjectOutputStream.
     *
     * @throws IOException if an I/O error occurs when sending the request
     */
    private void sendRequest() throws IOException {
        try {
            oos.writeObject(this.user); // Send the user object to the server
            oos.writeObject(request); // Send the request string to the server
            oos.flush();
            System.out.println("\033[0;32mSent request: " + request + "\033[0m");
        } catch (SocketException e) {
            System.out.println("\033[0;31mConnection lost while sending request.\033[0m");
            throw e;
        }
    }

    /**
     * Reads and processes the server's responses based on the current request.
     *
     * @throws IOException if an I/O error occurs when receiving the response
     * @throws ClassNotFoundException if a class cannot be found during object deserialization
     */
    private void handleServerResponse() throws IOException, ClassNotFoundException {
        try {
            if (request.equals("register")) {
                response = br.readLine(); // Read the server's response for registration
                if (response != null) {
                    System.out.println(response); // Print the response
                    System.out.println("\n");
                }
            } else if (request.equals("login")) {
                String line = br.readLine(); // Read the server's response for login
                if (line != null) {
                    boolean verified = Boolean.parseBoolean(line); // Parse the response to a boolean
                    if (verified) {
                        System.out.println("\033[0;32mlogged in successfully!\033[0m");
                        System.out.println("\n");
                        user.setLoggedIn(true); // Update the user's login status
                    } else {
                        System.out.println("\033[0;31mlogin failed\033[0m");
                        System.out.println("\n");
                    }
                }
            } else if (request.equals("make a team")) {
                System.out.println("\033[0;32mPlease wait until enough members are ready...\033[0m");
                System.out.println("\n");

                while (true) {
                    String serverMessage = br.readLine(); // Read the server's response for team formation
                    if (serverMessage == null) {
                        System.out.println("\033[0;31mServer closed connection unexpectedly.\033[0m");
                        System.out.println("\n");
                        break;
                    } else if (serverMessage.equals("Waiting for team members...")) {
                        System.out.println(serverMessage); // Print the waiting message
                        System.out.println("\n");

                        try {
                            Thread.sleep(1000); // Wait for 1 second before checking again
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore the interrupted status
                            throw new IOException("Thread was interrupted", e);
                        }
                    } else {
                        int teamID = Integer.parseInt(serverMessage); // Parse the team ID
                        user.setTeamID(teamID); // Set the user's team ID
                        System.out.println("\033[0;32mYou are now in team '" + teamID + "'\033[0m");
                        System.out.println("\n");
                        user.setTeamd(true); // Update the user's team status
                        break;
                    }
                }
            } else if (request.equals("start a game")) {
                String serverMessage = br.readLine(); // Read the server's response for starting the game
                if (serverMessage != null) {
                    System.out.println(serverMessage); // Print the response
                    System.out.println("\n");

                    serverMessage = br.readLine();
                    if (serverMessage != null && serverMessage.startsWith("Game started")) {
                        countDown(); // Start the countdown before the game
                        System.out.println(serverMessage);
                        System.out.println("\n");
                        serverMessage = br.readLine();
                        System.out.println(serverMessage);
                        System.out.println("\n");
                        System.out.print("\033[0;33m(Q/q to spectate) Your answer: \033[0m");
                        System.out.println("\n");

                        answer = scanner.nextLine(); // Read the user's answer

                        oos.writeObject(answer); // Send the answer to the server
                        oos.flush();
                        try {
                            Thread.sleep(300); // Wait for 300 milliseconds before continuing
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handleGameSession(); // Handle the game session
                    }
                }
            } else if (request.equals("submit answer")) {
                handleGameSession(); // Handle the game session
            } else if (request.equals("next round")) {
                this.user.resetTrials(); // Reset the user's trials
                String serverMessage = br.readLine(); // Read the server's response for the next round
                System.out.println(serverMessage);
                System.out.println("\n");

                if (serverMessage.startsWith("Congratulations")) {
                    this.user.gameOver();
                    return;
                }
                System.out.print("\033[0;33m(Q/q to spectate) Your answer: \033[0m");
                answer = scanner.nextLine(); // Read the user's answer
                oos.writeObject(answer); // Send the answer to the server
                oos.flush();
                try {
                    Thread.sleep(300); // Wait for 300 milliseconds before continuing
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handleGameSession(); // Handle the game session
            } else if (request.equals("wrong answer")) {
                System.out.print("\033[0;33m(Q/q to spectate) Your answer: \033[0m");
                answer = scanner.nextLine(); // Read the user's answer
                oos.writeObject(answer); // Send the answer to the server
                oos.flush();
                handleGameSession(); // Handle the game session
                try {
                    Thread.sleep(300); // Wait for 300 milliseconds before continuing
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (request.equals("spectate")) {
                handleGameSession(); // Handle the game session
            }
        } catch (SocketException e) {
            System.out.println("\033[0;31mConnection lost while receiving response.\033[0m");
            throw e;
        }
    }

    /**
     * Manages the game session, processing game-related messages from the server and updating the user's state accordingly.
     *
     * @throws IOException if an I/O error occurs during communication
     * @throws ClassNotFoundException if a class cannot be found during object deserialization
     */
    private void handleGameSession() throws IOException, ClassNotFoundException {
        this.user.setInGame(true); // Set the user's in-game status to true
        while (this.user.inGame() && !this.user.isSpectator()) {
            String serverMessage = br.readLine(); // Read the server's game message
            if (serverMessage != null) {
                if (serverMessage.startsWith("You are")) {
                    System.out.println(serverMessage); // Print the spectator message
                    user.setSpectator(true); // Set the user's spectator status to true
                    request = "spectate"; // Set the request to spectate
                    break;
                } else if (serverMessage.startsWith("Correct")) {
                    System.out.println(serverMessage + "    Trials = " + this.user.getCurrentTrials()); // Print the correct answer message and trials
                    serverMessage = br.readLine();
                    if (serverMessage != null && serverMessage.contains("All")) {
                        System.out.println(serverMessage); // Print the next round message
                        request = "next round"; // Set the request to next round
                        break;
                    }
                } else {
                    request = "wrong answer"; // Set the request to wrong answer
                    System.out.println(serverMessage); // Print the wrong answer message
                    this.user.setCurrentTrials(this.user.getCurrentTrials() + 1); // Increment the user's trials
                    break;
                }
            }
        }
        while (this.user.isSpectator()) {
            String serverMessage = br.readLine(); // Read the server's spectator message
            if (serverMessage != null) {
                System.out.println(serverMessage); // Print the spectator message
                if (serverMessage.contains("All")) {
                    serverMessage = br.readLine(); // Read the next message
                }
                if (serverMessage.contains("New word")) {
                    System.out.println(serverMessage); // Print the new word message
                }
                if (serverMessage.contains("Game Over")) {
                    System.out.println("Congratulations... Your team have finished the game with score of 10 Points!\n ");
                    user.gameOver(); // End the user's game session
                }
            }
        }
    }

    /**
     * Closes all open streams and the socket connection.
     */
    private void closeConnection() {
        try {
            if (oos != null) oos.close(); // Close the ObjectOutputStream
            if (bw != null) bw.close(); // Close the BufferedWriter
            if (br != null) br.close(); // Close the BufferedReader
            if (connection != null) connection.close(); // Close the socket connection
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a countdown from 3 to 1 before starting the game.
     */
    private void countDown() {
        int timeLeft = 3; // Set the countdown time
        while (timeLeft > 0) {
            System.out.println("\033[0;31mStarting in: " + timeLeft + "\033[0m"); // Print the countdown message
            try {
                Thread.sleep(1000); // Wait for 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.out.println("\033[0;31mTimer interrupted\033[0m");
                return;
            }
            timeLeft--;
        }
    }
}
