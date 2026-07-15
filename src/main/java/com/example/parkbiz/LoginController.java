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
        // Start Fan Animations
        startFan(fan1, 2000, 360);
        startFan(fan2, 2500, -360);
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
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        try {
            // Admin Credentials
            if ("admin".equals(username) && "admin123".equals(password)) {
                // Changed filename to match yours: admin-view.fxml
                switchToScene("admin-view.fxml", "ParkBiz - Admin Mainframe V1.0");
            }
            // Driver Credentials
            else if ("driver".equals(username) && "1234".equals(password)) {
                switchToScene("dashboard-view.fxml", "ParkBiz - Driver Terminal");
            }
            else {
                lblFeedback.setText("> ACCESS_DENIED: INVALID_CREDENTIALS");
                lblFeedback.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';");
            }
        } catch (Exception e) {
            lblFeedback.setText("> SYSTEM_ERROR: MODULE_LOAD_FAILED");
            System.err.println("Error loading FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void switchToScene(String fxmlFile, String title) throws IOException {
        Stage stage = (Stage) btnLogin.getScene().getWindow();

        // Load the FXML
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlFile)));

        // Update the stage
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setResizable(false);
        stage.centerOnScreen();
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