package com.group15.typefast;

public class Client {

    public static void main(String[] args) {

                ClientSocketTask clientThread = new ClientSocketTask(); // create a new socket task
                clientThread.run(); //Run Task
        }
    }