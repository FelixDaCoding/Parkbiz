package com.example.parkbiz;

import javafx.animation.Animation;
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
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblFeedback;
    @FXML private Button btnLogin;

    // The two fan containers from FXML
    @FXML private StackPane fan1;
    @FXML private StackPane fan2;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Create rotation for Fan 1 (Clockwise)
        RotateTransition rt1 = new RotateTransition(Duration.millis(2000), fan1);
        rt1.setByAngle(360);
        rt1.setCycleCount(Animation.INDEFINITE);
        rt1.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rt1.play();

        // Create rotation for Fan 2 (Counter-Clockwise)
        RotateTransition rt2 = new RotateTransition(Duration.millis(2500), fan2);
        rt2.setByAngle(-360);
        rt2.setCycleCount(Animation.INDEFINITE);
        rt2.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rt2.play();
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if ("admin".equals(username) && "password123".equals(password)) {
            try {
                // CHANGE THIS LINE to match your actual filename:
                var resource = getClass().getResource("dashboard-view.fxml");

                if (resource == null) {
                    lblFeedback.setText("> ERROR: dashboard-view.fxml NOT FOUND.");
                    return;
                }

                Stage stage = (Stage) btnLogin.getScene().getWindow();
                Parent root = FXMLLoader.load(resource);
                stage.setScene(new Scene(root));
                stage.setTitle("ParkBiz - System Monitor");

            } catch (IOException e) {
                lblFeedback.setText("> SYSTEM_ERROR: FAILED TO LOAD DASHBOARD.");
                e.printStackTrace();
            }
        } else {
            lblFeedback.setText("> ERROR: ACCESS DENIED.");
        }
    }

    @FXML
    private void handleMouseHover() {
        btnLogin.setStyle("-fx-background-color: #ffae00; -fx-text-fill: #000000; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, #ff8c00, 15, 0, 0, 0);");
    }

    @FXML
    private void handleMouseExit() {
        btnLogin.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: #000000; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
    }


}

