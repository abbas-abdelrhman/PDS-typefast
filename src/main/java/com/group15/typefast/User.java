package com.group15.typefast;

import java.io.BufferedWriter;
import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private boolean isLoggedIn;
    private boolean isTeamd;
    private boolean isReady;
    private boolean isSpectator;
    boolean inGame;
    private int teamID;
    private int score = 0;
    private long lastResponseTime;
    private int correctWordCount;
    private int currentLevel = 0;
    private int currentTrials = 1;
    private transient BufferedWriter writer; // Transient because BufferedWriter is not serializable

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public boolean isTeamd() {
        return isTeamd;
    }

    public void setTeamd(boolean teamd) {
        isTeamd = teamd;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public boolean isSpectator() {
        return isSpectator;
    }

    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
    }

    public int getTeamID() {
        return teamID;
    }

    public void setTeamID(int teamID) {
        this.teamID = teamID;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getLastResponseTime() {
        return lastResponseTime;
    }

    public void setLastResponseTime(long lastResponseTime) {
        this.lastResponseTime = lastResponseTime;
    }

    public int getCorrectWordCount() {
        return correctWordCount;
    }

    public void setCorrectWordCount(int correctWordCount) {
        this.correctWordCount = correctWordCount;
    }

    public BufferedWriter getBufferedWriter() {
        return writer;
    }

    public void setBufferedWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public boolean inGame(){
        return this.inGame;
    }

    public boolean isInGame() {
        return inGame;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getCurrentTrials() {
        return currentTrials;
    }

    public void setCurrentTrials(int currentTrials) {
        this.currentTrials = currentTrials;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public void setWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void resetTrials(){
        this.currentTrials = 1;
    }
    
    public void gameOver(){
        this.setCorrectWordCount(0);
        this.setCurrentLevel(0);
        this.setInGame(false);
        this.setSpectator(false);
        this.setReady(false);
        this.setTeamd(false);
    }
}
