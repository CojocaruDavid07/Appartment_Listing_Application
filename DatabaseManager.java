package com.roommateapp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:roommate_db.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            String sqlUser = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "role TEXT NOT NULL)";
            stmt.execute(sqlUser);

            // CHANGED: Added 'owner' column to track who created the profile
            String sqlProfile = "CREATE TABLE IF NOT EXISTS profiles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "owner TEXT," +
                    "name TEXT," +
                    "location TEXT," +
                    "budget REAL," +
                    "contact TEXT)";
            stmt.execute(sqlProfile);

            // Create default admin
            addUser(new User("admin", "admin123", "ADMIN"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean addUser(User user) {
        String sql = "INSERT INTO users(username, password, role) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("password"), rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // CHANGED: Saves the 'owner' to the database
    public static void addProfile(Profile p) {
        String sql = "INSERT INTO profiles(owner, name, location, budget, contact) VALUES(?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getOwner());
            pstmt.setString(2, p.getName());
            pstmt.setString(3, p.getLocation());
            pstmt.setDouble(4, p.getBudget());
            pstmt.setString(5, p.getContact());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // CHANGED: Retrieves 'id' and 'owner' so the UI knows who owns what
    public static List<Profile> getAllProfiles() {
        List<Profile> list = new ArrayList<>();
        String sql = "SELECT * FROM profiles";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Profile(
                        rs.getInt("id"),
                        rs.getString("owner"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getDouble("budget"),
                        rs.getString("contact")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // NEW: Method to delete a specific profile by ID
    public static void deleteProfile(int id) {
        String sql = "DELETE FROM profiles WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void clearAllProfiles() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM profiles");
        } catch (SQLException e) { e.printStackTrace(); }
    }
}