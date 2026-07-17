package com.example.parkbiz;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblFeedback;
    @FXML private Button btnLogin;
    @FXML private StackPane fan1;
    @FXML private StackPane fan2;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startFan(fan1, 2000, 360);
        startFan(fan2, 2500, -360);

        // FIX: Use Platform.runLater to check session after scene is ready
        javafx.application.Platform.runLater(() -> {
            // Check if already logged in AND session is valid
            if (SessionManager.getInstance().isLoggedIn()) {
                try {
                    String view = SessionManager.getInstance().getRole().equals("ADMIN") ?
                            "admin-view.fxml" : "dashboard-view.fxml";
                    String title = SessionManager.getInstance().getRole().equals("ADMIN") ?
                            "PARKBIZ - ADMIN_MAINFRAME" : "PARKBIZ - DRIVER_TERMINAL";

                    // Make sure we don't redirect if we're already on the login screen
                    Stage stage = (Stage) btnLogin.getScene().getWindow();
                    if (stage != null) {
                        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(view)));
                        Scene scene = new Scene(root);
                        stage.setScene(scene);
                        stage.setTitle(title);
                        stage.centerOnScreen();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startFan(StackPane fan, int ms, int angle) {
        if (fan != null) {
            RotateTransition rt = new RotateTransition(Duration.millis(ms), fan);
            rt.setByAngle(angle);
            rt.setCycleCount(Animation.INDEFINITE);
            rt.setInterpolator(Interpolator.LINEAR);
            rt.play();
        }
    }

    @FXML
    private void handleLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        String query = "SELECT id, role FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String role = rs.getString("role");

                // Store session in memory
                UserSession.getInstance().setUser(id, user);

                // Save to persistent session manager
                String view = role.equals("ADMIN") ? "admin-view.fxml" : "dashboard-view.fxml";
                SessionManager.getInstance().saveSession(id, user, role, view);

                if (role.equals("ADMIN")) {
                    switchToScene("admin-view.fxml", "PARKBIZ - ADMIN_MAINFRAME");
                } else {
                    switchToScene("dashboard-view.fxml", "PARKBIZ - DRIVER_TERMINAL");
                }
            } else {
                lblFeedback.setText("> ERROR: AUTH_FAILED . . . [INVALID]");
                lblFeedback.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';");
            }
        } catch (Exception e) {
            lblFeedback.setText("> SYSTEM_ERROR: DATABASE_OFFLINE");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowRegister() {
        try {
            switchToScene("register-view.fxml", "PARKBIZ - IDENTITY_MANAGEMENT");
        } catch (IOException e) {
            lblFeedback.setText("> SYSTEM_ERR: REGISTER_MODULE_NOT_FOUND");
            e.printStackTrace();
        }
    }

    private void switchToScene(String fxmlFile, String title) throws IOException {
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        if (stage == null) {
            // Fallback: try to get any showing stage
            stage = (Stage) javafx.stage.Window.getWindows().stream()
                    .filter(window -> window instanceof Stage && window.isShowing())
                    .findFirst()
                    .orElse(null);
        }

        if (stage == null) {
            throw new IOException("Cannot find stage to switch scene");
        }

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlFile)));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void handleMouseHover() {
        btnLogin.setStyle("-fx-background-color: #ffae00; -fx-text-fill: #000000; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, #ff8c00, 15, 0, 0, 0); -fx-cursor: hand;");
    }

    @FXML
    private void handleMouseExit() {
        btnLogin.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: #000000; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
    }
}