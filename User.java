package com.roommateapp;
import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String password;
    private String role; 

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public int getId() { return id; }
    
    @Override
    public String toString() { return username + " (" + role + ")"; }
}