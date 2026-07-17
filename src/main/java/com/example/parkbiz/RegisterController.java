package com.example.parkbiz;

import javafx.animation.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.sql.*;

public class RegisterController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass, txtConfirm;
    @FXML private Label lblStatus;
    @FXML private StackPane fan1;
    @FXML private Button btnRegister;

    @FXML
    public void initialize() {
        // Spin the aesthetic cooling fan
        RotateTransition rt = new RotateTransition(Duration.millis(2000), fan1);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.play();

        // Ensure the status label can handle the long sentence
        lblStatus.setWrapText(true);
    }

    /**
     * Security Protocol: Validates password strength
     * Pattern: 8+ chars, at least 1 Uppercase, 1 Lowercase, 1 Number
     */
    private boolean isPasswordSecure(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password.matches(pattern);
    }

    @FXML
    private void handleRegister() {
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();
        String confirm = txtConfirm.getText();

        // Check 1: Empty Fields
        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("> ERR: NULL_POINTER_IN_FIELDS");
            lblStatus.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';");
            return;
        }

        // Check 2: Specific Password Complexity Requirement
        if (!isPasswordSecure(pass)) {
            lblStatus.setText("[SECURITY BREACH PREVENTION]: PASSWORD CONSTRAINTS VIOLATED!\n" +
                    "Password must be at least 8 characters long and contain a mixture of numerical keys (0-9).");
            lblStatus.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced'; -fx-font-size: 10px;");
            return;
        }

        // Check 3: Password Match
        if (!pass.equals(confirm)) {
            lblStatus.setText("> ERR: VERIFICATION_HASH_MISMATCH");
            lblStatus.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';");
            return;
        }

        // Check 4: Username length
        if (user.length() < 4) {
            lblStatus.setText("> ERR: USER_ID_TOO_SHORT");
            lblStatus.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';");
            return;
        }

        // Database Injection via Shared Registry
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'DRIVER')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.executeUpdate();

            lblStatus.setText("> SUCCESS: SECURITY_KEY_ACCEPTED. REDIRECTING...");
            lblStatus.setStyle("-fx-text-fill: #38a169; -fx-font-weight: bold; -fx-font-family: 'Monospaced';");

            btnRegister.setDisable(true);
            showSuccessPopup();

            // Automatic redirection to Login
            PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
            delay.setOnFinished(event -> {
                try {
                    handleBack();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            delay.play();

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                lblStatus.setText("> ERR: IDENTITY_ALREADY_EXISTS_IN_DB");
            } else {
                lblStatus.setText("> SYSTEM_ERR: UPLINK_LOST");
            }
            lblStatus.setStyle("-fx-text-fill: #ff4500; -fx-font-family: 'Monospaced';");
        }
    }

    private void showSuccessPopup() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setContentText(">> SECURITY_PROTOCOL_VERIFIED <<\n\nNEW IDENTITY CREATED.");

        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: #000000; -fx-border-color: #ff8c00; -fx-border-width: 2;");
        dp.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: #ff8c00; -fx-font-family: 'Monospaced';"));

        Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
        okBtn.setText("[ INITIALIZE ]");
        okBtn.setStyle("-fx-background-color: #ff8c00; -fx-text-fill: black; -fx-font-family: 'Monospaced'; -fx-cursor: hand;");

        alert.show();
    }

    @FXML
    private void handleBack() throws Exception {
        Stage stage = (Stage) txtUser.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(root));
    }
}