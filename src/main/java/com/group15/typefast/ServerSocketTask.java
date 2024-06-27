package com.group15.typefast;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * ServerSocketTask handles the server-side socket communication for the TypeFast game.
 * It manages user registration, login, team formation, and game sessions.
 */
public class ServerSocketTask implements Runnable {

    private Socket connection; // Client socket connection
    private final List<User> usersList; // List of registered users
    private final List<ScoreObject> scoreList; // List of scores
    private static final List<Team> teamList = Collections.synchronizedList(new ArrayList<>()); // Synchronized list of teams
    private int tgc = 0; // Team generation counter
    private String response = null; // Server response
    private boolean startUp = true; // Flag for initial team creation
    private static final ConcurrentHashMap<Integer, String> currentWords = new ConcurrentHashMap<>(); // Map of current words by team ID
    private static final ConcurrentHashMap<Integer, Long> wordStartTimes = new ConcurrentHashMap<>(); // Map of word start times by team ID
    private static final List<String> WORDS = List.of("cat", "animal", "umbrella", "acronym", "difficult", "synchronous", "appropriation", "sophisticated", "apprenticeship", "designation", "End"); // List of words for the game
    private static final ConcurrentHashMap<Integer, Integer> teamScores = new ConcurrentHashMap<>(); // Map of team scores by team ID

    /**
     * Constructor to initialize the ServerSocketTask with client socket, user list, score list, and best score.
     *
     * @param s         the client socket
     * @param usersList the list of registered users
     * @param scoreList the list of scores
     * @param bestScore the best score among users
     */
    public ServerSocketTask(Socket s, List<User> usersList, List<ScoreObject> scoreList, int bestScore) {
        this.connection = s;
        this.usersList = usersList;
        this.scoreList = scoreList;
    }

    /**
     * The main loop that manages the server-side communication. It handles user registration, login,
     * team formation, and game sessions based on client requests.
     */
    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
             BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

            System.out.println("connected");

            while (true) {
                // Read user and request from client
                User receivedUser = (User) ois.readObject();
                receivedUser.setBufferedWriter(bw); // Set the BufferedWriter for the user
                String request = (String) ois.readObject();
                System.out.println("Received request: " + request);

                if (request == null) break;

                switch (request) {
                    case "register":
                        register(receivedUser); // Handle user registration
                        response = receivedUser.getUsername() + " is Registered Successfully";
                        sendResponse(bw, response); // Send response to client
                        break;

                    case "login":
                        boolean verified = verify(receivedUser); // Handle user login
                        sendResponse(bw, Boolean.toString(verified)); // Send verification result to client
                        if (verified) {
                            updateLoginStatus(receivedUser); // Update user login status
                        }
                        break;

                    case "make a team":
                        handleTeamMaking(receivedUser, bw); // Handle team formation
                        break;

                    case "start a game":
                        handleStartGame(receivedUser, request, bw, ois); // Handle game start
                        break;

                    case "q":
                        return; // Handle client disconnect

                    default:
                        // Handle unknown requests
                        break;
                }

                Thread.sleep(300); // Wait for 300 milliseconds between requests
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace(); // Print the stack trace for the exception
        } finally {
            try {
                connection.close(); // Close the connection
            } catch (IOException e) {
                e.printStackTrace(); // Print the stack trace for the exception
            }
        }
    }

    /**
     * Synchronized method to register a user by adding them to the users list.
     *
     * @param user the user to be registered
     */
    private synchronized void register(User user) {
        usersList.add(user);
    }

    /**
     * Synchronized method to verify a user's login credentials.
     *
     * @param user the user to be verified
     * @return true if the user's credentials are valid, false otherwise
     */
    private synchronized boolean verify(User user) {
        for (User u : usersList) {
            if (u.getUsername().equalsIgnoreCase(user.getUsername()) &&
                u.getPassword().equals(user.getPassword())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Synchronized method to update the login status of a user.
     *
     * @param user the user whose login status is to be updated
     */
    private synchronized void updateLoginStatus(User user) {
        for (User u : usersList) {
            if (u.getUsername().equals(user.getUsername())) {
                u.setLoggedIn(true);
            }
        }
    }

    /**
     * Synchronized method to handle team formation. It assigns users to teams and sends appropriate responses.
     *
     * @param user the user requesting team formation
     * @param bw   the BufferedWriter to send responses to the user
     * @throws IOException if an I/O error occurs when sending the response
     */
    private synchronized void handleTeamMaking(User user, BufferedWriter bw) throws IOException {
        // Initialize the teamList if this is the first time
        if (startUp) {
            Team firstTeam = new Team();
            teamList.add(firstTeam);
            startUp = false;
        }

        user.setTeamID(tgc); // Set the user's team ID
        teamList.get(tgc).addUser(user); // Add the user to the team

        if (teamList.get(tgc).isFull()) { // Check if the team is full
            for (User teamUser : teamList.get(tgc).getTeamUsers()) {
                teamUser.setTeamd(true); // Update the user's team status
                sendResponse(teamUser.getBufferedWriter(), String.valueOf(tgc)); // Send team ID to the users
            }
            tgc += 1; // Increment the team generation counter
            Team newTeam = new Team();
            teamList.add(newTeam); // Add a new team to the list
        } else {
            sendResponse(bw, "Waiting for team members..."); // Inform the user to wait for team members
        }
    }

    /**
     * Handles the start of a game session. It waits for all team members to be ready, then starts the game.
     *
     * @param user    the user requesting to start the game
     * @param request the request string
     * @param bw      the BufferedWriter to send responses to the user
     * @param ois     the ObjectInputStream to read user answers
     * @throws IOException            if an I/O error occurs during communication
     * @throws ClassNotFoundException if a class cannot be found during object deserialization
     * @throws InterruptedException   if the thread is interrupted
     */
    private void handleStartGame(User user, String request, BufferedWriter bw, ObjectInputStream ois) throws IOException, ClassNotFoundException, InterruptedException {
        Team team = teamList.get(user.getTeamID());
        user.setReady(true); // Set user as ready
        sendResponse(bw, "Waiting for all team members to be ready...");

        while (true) {
            boolean allReady = team.getTeamUsers().stream().allMatch(User::isReady); // Check if all team members are ready
            if (allReady) {
                sendResponse(user.getBufferedWriter(), "Game started for team " + team.getTeamID()); // Inform the user that the game has started
                sendNewWordToUser(user); // Send the first word to the user
                handleAnswerSubmission(user, request, ois, bw); // Handle answer submission
                break;
            }
            Thread.sleep(300); // Wait for 300 milliseconds before checking again
        }
    }

    /**
     * Sends a new word to the user for the current round.
     *
     * @param user the user to receive the new word
     * @throws IOException if an I/O error occurs when sending the response
     */
    private void sendNewWordToUser(User user) throws IOException {
        String newWord = WORDS.get(user.getCurrentLevel() % WORDS.size()); // Get the new word for the current level
        currentWords.put(user.getTeamID(), newWord); // Store the word in the map
        wordStartTimes.put(user.getTeamID(), System.currentTimeMillis()); // Store the word start time in the map
        if (newWord.equals("End")) { // Check if the word is the end marker
            sendResponse(user.getBufferedWriter(), "Congratulations... Your team have finished the game with score of " + user.getScore() + " Points! Time =" + (teamList.get(user.getTeamID()).getTotalTime())/1000 +"seconds");
            sendResponse(user.getBufferedWriter(), "Game Over in "+ (teamList.get(user.getTeamID()).getTotalTime())/1000 +"seconds");
            return;
        }
        sendResponse(user.getBufferedWriter(), "Your Team Score: " + user.getScore() + " points!" + " New word: " + newWord); // Send the new word and team score to the user
        user.setScore(user.getScore() + 1); // Increment the user's score
        user.setCurrentLevel(user.getCurrentLevel() + 1); // Increment the user's level
    }

    /**
     * Handles the submission of user answers during a game session.
     *
     * @param user    the user submitting the answer
     * @param request the request string
     * @param ois     the ObjectInputStream to read user answers
     * @param bw      the BufferedWriter to send responses to the user
     * @throws IOException            if an I/O error occurs during communication
     * @throws ClassNotFoundException if a class cannot be found during object deserialization
     * @throws InterruptedException   if the thread is interrupted
     */
    private void handleAnswerSubmission(User user, String request, ObjectInputStream ois, BufferedWriter bw) throws IOException, ClassNotFoundException, InterruptedException {
        int teamID = user.getTeamID();
        Team team = teamList.get(teamID);
        user.setInGame(true); // Set the user's in-game status to true

        while (user.inGame()) {
            String correctWord = currentWords.get(teamID); // Get the correct word for the team
            String answer = (String) ois.readObject(); // Read the user's answer

            if (answer.equalsIgnoreCase("q")) {
                sendResponse(bw, "You are now spectating.");
                user.setCorrectWordCount(user.getCorrectWordCount() + 1);
                user.setSpectator(true);
                // Check if all players in the team have answered correctly
                int count = user.getCorrectWordCount();

                while (true) {
                    boolean allAnswered = team.getInGameUsers().stream().allMatch(u -> u.getCorrectWordCount() == count);
                    if (allAnswered) {
                        user.setCorrectWordCount(user.getCorrectWordCount() + 1);
                        long responseTime = System.currentTimeMillis() - wordStartTimes.get(user.getTeamID()); // Calculate the response time
                        team.setTotalTime(team.getTotalTime()+responseTime);
                        Thread.sleep(500);
                        sendResponse(bw, "All your team answered! You got 1 point");
                        Thread.sleep(500);
                        sendNewWordToUser(user); // Send a new word for the next round
                        Thread.sleep(500);
                        handleSpectate(user, bw, ois); // Handle spectating
                        return;
                    }
                }
            } else if (answer.equalsIgnoreCase(correctWord)) {
                user.setCorrectWordCount(user.getCorrectWordCount() + 1); // Increment the user's correct word count
                long responseTime = System.currentTimeMillis() - wordStartTimes.get(user.getTeamID()); // Calculate the response time
                user.setLastResponseTime(responseTime); // Set the user's last response time
                sendResponse(bw, "Correct! Time: " + responseTime + "ms"); // Inform the user that their answer is correct

                // Check if all players in the team have answered correctly
                int count = user.getCorrectWordCount();

                while (true) {
                    boolean allAnswered = team.getInGameUsers().stream().allMatch(u -> u.getCorrectWordCount() == count);
                    if (allAnswered) {
                        sendResponse(bw, "All your team answered! You got 1 point");
                        sendNewWordToUser(user); // Send a new word for the next round
                        request = "next round";
                        System.out.println("user: " + user.getUsername() + " got the right word");
                        break;
                    }
                }
            } else {
                sendResponse(bw, "Incorrect. Try again."); // Inform the user that their answer is incorrect
                request = "wrong answer";
            }
        }
    }

    /**
     * Handles spectating for users who choose to spectate during the game.
     *
     * @param user the user spectating
     * @param bw   the BufferedWriter to send responses to the user
     * @param ois  the ObjectInputStream to read user answers
     * @throws IOException            if an I/O error occurs during communication
     * @throws ClassNotFoundException if a class cannot be found during object deserialization
     * @throws InterruptedException   if the thread is interrupted
     */
    private void handleSpectate(User user, BufferedWriter bw, ObjectInputStream ois) throws IOException, ClassNotFoundException, InterruptedException {
        int teamID = user.getTeamID();
        Team team = teamList.get(teamID);
        while (user.isSpectator()) {
            // Send notification updates to the spectator
            System.out.println("I made it at the server");
            int count = user.getCorrectWordCount();
            while (true) {
                boolean allAnswered = team.getTeamUsers().stream().allMatch(u -> u.getCorrectWordCount() == count);
                if (allAnswered) {
                    System.out.println("user: " + user.getUsername() + " made it to handle spectate");
                    user.setCorrectWordCount(user.getCorrectWordCount() + 1);
                    sendResponse(bw, "All your team answered! You got 1 point");
                    sendNewWordToUser(user); // Send a new word for the next round
                    Thread.sleep(300);
                    break;
                }
            }
        }
    }

    /**
     * Synchronized method to update the score of a team.
     *
     * @param teamID       the ID of the team
     * @param responseTime the response time of the team
     */

    /**
     * Sends a response to the client.
     *
     * @param bw       the BufferedWriter to send the response
     * @param response the response string
     * @throws IOException if an I/O error occurs when sending the response
     */
    private void sendResponse(BufferedWriter bw, String response) throws IOException {
        bw.write(response);
        bw.write("\n");
        bw.flush();
    }
}
