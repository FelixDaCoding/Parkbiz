package com.example.parkbiz;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("ParkBiz - Business Login");

        // Check for existing session
        SessionManager session = SessionManager.getInstance();

        if (session.isLoggedIn()) {
            // Restore previous session - skip login screen
            String view = session.getLastView();
            if (view == null || view.isEmpty()) {
                view = session.getRole().equals("ADMIN") ? "admin-view.fxml" : "dashboard-view.fxml";
            }

            String title = session.getRole().equals("ADMIN") ?
                    "PARKBIZ - ADMIN_MAINFRAME" :
                    "PARKBIZ - DRIVER_TERMINAL";

            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(view)));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
        } else {
            // Normal login flow
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login-view.fxml")));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
        }

        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            // Save session on close if logged in
            if (SessionManager.getInstance().isLoggedIn()) {
                // Session already saved, keep it
            }
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}