package com.mycompany.quickwashproject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class PriceController {

    @FXML
    private void goToReservation(javafx.event.ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/ReservationView.fxml", "QuickWash - Booking Dashboard");
    }

    @FXML
    private void goToPrice(javafx.event.ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/PriceView.fxml", "QuickWash - Price Listing");
    }

    @FXML
    private void goToGuide(javafx.event.ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/GuideView.fxml", "QuickWash - Station Guide");
    }

    @FXML
    private void goToProfile(javafx.event.ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/ProfileView.fxml", "QuickWash - User Profile");
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        UserSession.clearSession();
        switchScene(event, "/com/mycompany/quickwashproject/LoginView.fxml", "QuickWash - Login");
    }

    // Fixed helper method handling both Button and MenuItem clicks
    private void switchScene(javafx.event.ActionEvent event, String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage;
            
            // Check whether the event came from a MenuBar or a regular Button
            if (event.getSource() instanceof MenuItem) {
                stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
            } else {
                stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            }

            Scene scene = new Scene(view, 850, 650);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/quickwashproject/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}