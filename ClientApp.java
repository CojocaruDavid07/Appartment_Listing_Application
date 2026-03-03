package com.roommateapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientApp extends Application {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private Stage primaryStage;

    // Local cache to store profiles for filtering
    private List<Profile> allProfilesCache = new ArrayList<>();

    // We make the list view a class variable so the filter method can access it
    private ListView<Profile> listView = new ListView<>();

    // The three search fields
    private TextField nameSearch = new TextField();
    private TextField citySearch = new TextField();
    private TextField priceSearch = new TextField();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        connectToServer();
        showLoginScene();
        stage.setTitle("Roommate Finder");
        stage.show();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8080);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Cannot connect to server").showAndWait();
        }
    }

    private void showLoginScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        TextField userField = new TextField();
        userField.setPromptText("Username");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");

        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register (Create User)");

        loginBtn.setOnAction(e -> {
            try {
                out.writeObject("LOGIN:" + userField.getText() + "," + passField.getText());
                User user = (User) in.readObject();
                if (user != null) {
                    currentUser = user;
                    showMainScene();
                } else {
                    showAlert("Failed", "Invalid credentials");
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        registerBtn.setOnAction(e -> {
            if (userField.getText().trim().isEmpty() || passField.getText().trim().isEmpty()) {
                showAlert("Error", "Fields cannot be empty");
                return;
            }
            try {
                out.writeObject("REGISTER:" + userField.getText() + "," + passField.getText());
                String resp = (String) in.readObject();
                showAlert("Info", "Registration: " + resp);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        root.getChildren().addAll(new Label("Welcome"), userField, passField, loginBtn, registerBtn);
        primaryStage.setScene(new Scene(root, 300, 250));
    }

    private void showMainScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label welcome = new Label("Logged in as: " + currentUser.getUsername() + " [" + currentUser.getRole() + "]");

        // --- NEW: 3 Separate Search Boxes ---
        Label searchLabel = new Label("Filter Listings:");

        nameSearch.setPromptText("Name...");
        citySearch.setPromptText("City...");
        priceSearch.setPromptText("Max Price...");

        // Setup the layout for search boxes (Horizontal row)
        HBox searchRow = new HBox(10);
        searchRow.getChildren().addAll(nameSearch, citySearch, priceSearch);

        // Add "Listeners" to all 3 boxes.
        // Whenever you type in ANY box, it runs 'updateListFilters()'
        nameSearch.textProperty().addListener((obs, old, nev) -> updateListFilters());
        citySearch.textProperty().addListener((obs, old, nev) -> updateListFilters());
        priceSearch.textProperty().addListener((obs, old, nev) -> updateListFilters());

        Button refreshBtn = new Button("Refresh Listings");

        // --- CHANGED: Cell Factory (No Colors) ---
        listView.setCellFactory(param -> new ListCell<Profile>() {
            @Override
            protected void updateItem(Profile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    // Standard black text
                    Label label = new Label(item.toString());
                    label.setStyle("-fx-text-fill: black;");

                    // Show Delete Button if (I am Owner) OR (I am Admin)
                    if (item.getOwner().equals(currentUser.getUsername()) || "ADMIN".equals(currentUser.getRole())) {
                        Button deleteBtn = new Button("Delete");
                        deleteBtn.setStyle("-fx-text-fill: red;");
                        deleteBtn.setOnAction(e -> {
                            try {
                                out.writeObject("DELETE_PROFILE:" + item.getId());
                                in.readObject(); // Wait for "SUCCESS"
                                refreshBtn.fire(); // Reload list
                            } catch (Exception ex) { ex.printStackTrace(); }
                        });
                        hbox.getChildren().addAll(label, deleteBtn);
                    } else {
                        hbox.getChildren().add(label);
                    }
                    setGraphic(hbox);
                }
            }
        });

        // REFRESH LOGIC
        refreshBtn.setOnAction(e -> {
            try {
                out.writeObject("GET_PROFILES:dummy");
                allProfilesCache = (List<Profile>) in.readObject(); // Save to local cache
                updateListFilters(); // Re-apply the current search text to the new data
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        Button addProfileBtn = new Button("Add Profile");
        addProfileBtn.setOnAction(e -> showAddProfileScene());

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });

        root.getChildren().addAll(welcome, searchLabel, searchRow, refreshBtn, listView, addProfileBtn, logoutBtn);

        // Admin Nuke Button
        if ("ADMIN".equals(currentUser.getRole())) {
            Button nukeBtn = new Button("ADMIN: Delete All Profiles");
            nukeBtn.setStyle("-fx-background-color: red; -fx-text-fill: white;");
            nukeBtn.setOnAction(e -> {
                try {
                    out.writeObject("DELETE_ALL:dummy");
                    String response = (String) in.readObject();
                    if ("SUCCESS".equals(response)) refreshBtn.fire();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            root.getChildren().add(new Label("--- Admin Controls ---"));
            root.getChildren().add(nukeBtn);
        }

        primaryStage.setScene(new Scene(root, 600, 550));
        refreshBtn.fire(); // Load data immediately
    }

    // --- NEW: Helper method to handle the 3-way filtering ---
    private void updateListFilters() {
        listView.getItems().clear();

        String nameFilter = nameSearch.getText().toLowerCase();
        String cityFilter = citySearch.getText().toLowerCase();
        String priceFilter = priceSearch.getText();

        for (Profile p : allProfilesCache) {
            boolean matchName = p.getName().toLowerCase().contains(nameFilter);
            boolean matchCity = p.getLocation().toLowerCase().contains(cityFilter);
            boolean matchPrice = true; // Default to true if box is empty

            // Only check price if the user actually typed a number
            if (!priceFilter.isEmpty()) {
                try {
                    double maxPrice = Double.parseDouble(priceFilter);
                    if (p.getBudget() > maxPrice) {
                        matchPrice = false; // Too expensive
                    }
                } catch (NumberFormatException e) {
                    // If user types letters in price box, ignore the price filter
                }
            }

            // If matches ALL criteria, show it
            if (matchName && matchCity && matchPrice) {
                listView.getItems().add(p);
            }
        }
    }

    private void showAddProfileScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        TextField nameTf = new TextField(); nameTf.setPromptText("Name");
        TextField cityTf = new TextField(); cityTf.setPromptText("City");
        TextField budgetTf = new TextField(); budgetTf.setPromptText("Budget (€)");
        TextField contactTf = new TextField(); contactTf.setPromptText("Email/Phone");

        Button submitBtn = new Button("Submit");
        submitBtn.setOnAction(e -> {
            try {
                double b = Double.parseDouble(budgetTf.getText());
                if (nameTf.getText().isEmpty()) throw new Exception("Name empty");

                String data = String.join(",", currentUser.getUsername(), nameTf.getText(), cityTf.getText(), String.valueOf(b), contactTf.getText());
                out.writeObject("ADD_PROFILE:" + data);
                in.readObject();
                showMainScene();
            } catch (NumberFormatException nfe) {
                showAlert("Error", "Budget must be a valid number");
            } catch (Exception ex) {
                showAlert("Error", "Invalid Input: " + ex.getMessage());
            }
        });

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> showMainScene());

        root.getChildren().addAll(new Label("Add Roommate Profile"), nameTf, cityTf, budgetTf, contactTf, submitBtn, backBtn);
        primaryStage.setScene(new Scene(root, 300, 300));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}