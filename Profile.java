package com.roommateapp;
import java.io.Serializable;

public class Profile implements Serializable {
    private int id;        // Database ID
    private String owner;  // Username of who posted it
    private String name;
    private String location;
    private double budget;
    private String contact;

    // Constructor used when reading FROM database (includes ID and Owner)
    public Profile(int id, String owner, String name, String location, double budget, String contact) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.location = location;
        this.budget = budget;
        this.contact = contact;
    }

    // Constructor used when creating a NEW profile (ID is 0 initially)
    public Profile(String owner, String name, String location, double budget, String contact) {
        this(0, owner, name, location, budget, contact);
    }

    public int getId() { return id; }
    public String getOwner() { return owner; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public double getBudget() { return budget; }
    public String getContact() { return contact; }

    @Override
    public String toString() {
        // We include the Owner name in the display now
        return String.format("%s | %s | €%.2f | %s (Posted by: %s)", name, location, budget, contact, owner);
    }
}