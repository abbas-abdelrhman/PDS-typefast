package com.group15.typefast;

import java.io.Serializable;

public class ScoreObject implements Serializable , Comparable {
    long score;

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }


    @Override
    public int compareTo(Object o) {
        long compareScore=((ScoreObject)o).getScore();
        /* For Ascending order*/
        return (int) (this.score-compareScore);
    }
}
