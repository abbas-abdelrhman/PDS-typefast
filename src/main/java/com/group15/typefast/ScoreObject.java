package com.group15.typefast;

import java.io.Serializable;

@SuppressWarnings("rawtypes")
public class ScoreObject implements Serializable , Comparable {
    int score;
    Long time;

    public ScoreObject(int score, long time){
        this.score = score;
        this.time = time;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }


    @Override
    public int compareTo(Object o) {
        long compareScore=((ScoreObject)o).getScore();
        /* For Ascending order*/
        return (int) (this.score-compareScore);
    }
}