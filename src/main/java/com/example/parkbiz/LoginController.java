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

    @FXML private StackPane fan1;
    @FXML private StackPane fan2;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        RotateTransition rt1 = new RotateTransition(Duration.millis(2000), fan1);
        rt1.setByAngle(360);
        rt1.setCycleCount(Animation.INDEFINITE);
        rt1.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rt1.play();

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

        try {
            if ("admin".equals(username) && "admin123".equals(password)) {
                // GO TO ADMIN DASHBOARD
                switchToScene("admin-view.fxml", "ParkBiz - Admin Mainframe");
            }
            else if ("driver".equals(username) && "1234".equals(password)) {
                // GO TO DRIVER DASHBOARD
                switchToScene("dashboard-view.fxml", "ParkBiz - Driver Terminal");
            }
            else {
                lblFeedback.setText("> ACCESS_DENIED: INVALID_CREDENTIALS");
            }
        } catch (IOException e) {
            lblFeedback.setText("> SYSTEM_ERROR: MODULE_NOT_FOUND");
        }
    }

    // Helper method to reduce duplicate code
    private void switchToScene(String fxmlFile, String title) throws IOException {
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        stage.setScene(new Scene(root));
        stage.setTitle(title);
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

