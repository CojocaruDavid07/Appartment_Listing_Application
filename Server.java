package com.roommateapp;

import java.io.*;
import java.net.*;
import java.util.List;

public class Server {
    public static void main(String[] args) {
        DatabaseManager.initialize();
        System.out.println("Server started on port 8080...");

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                String request = (String) in.readObject();
                String[] parts = request.split(":", 2);
                String command = parts[0];

                if (command.equals("LOGIN")) {
                    String[] creds = parts[1].split(",", -1);
                    User u = null;
                    if (creds.length >= 2) {
                        u = DatabaseManager.login(creds[0], creds[1]);
                    }
                    out.writeObject(u);
                }
                else if (command.equals("REGISTER")) {
                    String[] data = parts[1].split(",", -1);
                    boolean success = false;
                    if (data.length >= 2) {
                        success = DatabaseManager.addUser(new User(data[0], data[1], "USER"));
                    }
                    out.writeObject(success ? "SUCCESS" : "FAIL");
                }
                else if (command.equals("GET_PROFILES")) {
                    List<Profile> profiles = DatabaseManager.getAllProfiles();
                    out.writeObject(profiles);
                }
                else if (command.equals("ADD_PROFILE")) {
                    // CHANGED: Now expects 5 parts (Owner, Name, Location, Budget, Contact)
                    String[] pData = parts[1].split(",", -1);
                    if (pData.length >= 5) {
                        Profile p = new Profile(pData[0], pData[1], pData[2], Double.parseDouble(pData[3]), pData[4]);
                        DatabaseManager.addProfile(p);
                        out.writeObject("SUCCESS");
                    } else {
                        out.writeObject("FAIL");
                    }
                }
                // NEW: Logic to delete a single profile
                else if (command.equals("DELETE_PROFILE")) {
                    int id = Integer.parseInt(parts[1]);
                    DatabaseManager.deleteProfile(id);
                    out.writeObject("SUCCESS");
                }
                else if (command.equals("DELETE_ALL")) {
                    DatabaseManager.clearAllProfiles();
                    out.writeObject("SUCCESS");
                }

                out.flush();
                out.reset();
            }
        } catch (Exception e) {
            System.err.println("Client Disconnected");
        }
    }
}