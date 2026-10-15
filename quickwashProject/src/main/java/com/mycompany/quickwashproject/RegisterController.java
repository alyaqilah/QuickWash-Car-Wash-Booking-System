package com.mycompany.quickwashproject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class RegisterController {

    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleRegister(javafx.event.ActionEvent event) {
        String username = regUsernameField.getText().trim();
        String password = regPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #d93025;");
            statusLabel.setText("Error: All fields must be filled.");
            return;
        }

        // 1. First, check if the username already exists in the database
        String checkSql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                statusLabel.setStyle("-fx-text-fill: #d93025;");
                statusLabel.setText("Error: Username already exists.");
                return;
            }
        } catch (SQLException e) {
            statusLabel.setStyle("-fx-text-fill: #d93025;");
            statusLabel.setText("DB Error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. If it doesn't exist, proceed with the insertion
        String sql = "INSERT INTO users (username, password, role, loyalty_points, membership_tier) VALUES (?, ?, 'user', 0, 'Level 1 (Starter)')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            statusLabel.setStyle("-fx-text-fill: #34a853;");
            statusLabel.setText("Registration successful! Redirecting to login...");

            // Switch back to Login view after successful registration
            Parent loginView = FXMLLoader.load(getClass().getResource("/com/mycompany/quickwashproject/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loginView, 850, 650);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/quickwashproject/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("QuickWash - Login");
            stage.setResizable(true);
            stage.show();

        } catch (SQLException e) {
            statusLabel.setStyle("-fx-text-fill: #d93025;");
            if (e.getErrorCode() == 1062) { 
                statusLabel.setText("Error: Username already exists.");
            } else {
                statusLabel.setText("DB Error: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin(javafx.event.ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/com/mycompany/quickwashproject/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loginView, 850, 650);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/quickwashproject/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("QuickWash - Login");
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}