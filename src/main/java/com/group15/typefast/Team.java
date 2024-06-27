package com.group15.typefast;

import java.io.Serializable;
import java.util.ArrayList;

public class Team implements Serializable {

    private int teamID;
    private int teamScore;
    private ArrayList<User> teamMates;
    private long totalTime;

    // Constructor to initialize the teamMates list
    public Team() {
        this.teamMates = new ArrayList<>();
    }

    public void addUser(User user) {
        teamMates.add(user);
    }

    public boolean isFull() {
        return teamMates.size() == 3;
    }

    public ArrayList<User> getTeamUsers() {
        return this.teamMates;
    }

    public int getTeamID() {
        return teamID;
    }

    public void setTeamID(int teamID) {
        this.teamID = teamID;
    }

    public int getTeamScore() {
        return teamScore;
    }

    public void setTeamScore(int teamScore) {
        this.teamScore = teamScore;
    }

    public ArrayList<User> getInGameUsers(){
        ArrayList <User> activePlayers = new ArrayList<User>();
        for (User user: this.teamMates){
            if (user.isInGame() && !user.isSpectator()){
                activePlayers.add(user);
            }
        }
        return activePlayers;
    }

    public ArrayList<User> getTeamMates() {
        return teamMates;
    }

    public void setTeamMates(ArrayList<User> teamMates) {
        this.teamMates = teamMates;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
}
