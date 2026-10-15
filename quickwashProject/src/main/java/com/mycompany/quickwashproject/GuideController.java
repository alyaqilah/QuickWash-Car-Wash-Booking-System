package com.mycompany.quickwashproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class GuideController {

    @FXML
    private void goToReservation(ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/ReservationView.fxml", "QuickWash - Booking Dashboard");
    }

    @FXML
    private void goToPrice(ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/PriceView.fxml", "QuickWash - Price Listing");
    }

    @FXML
    private void goToGuide(ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/GuideView.fxml", "QuickWash - Station Guide");
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        switchScene(event, "/com/mycompany/quickwashproject/ProfileView.fxml", "QuickWash - User Profile");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        switchScene(event, "/com/mycompany/quickwashproject/LoginView.fxml", "QuickWash - Login");
    }

    /**
     * Unified scene switcher handling both MenuItems and standard Buttons safely.
     */
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage;

            if (event.getSource() instanceof MenuItem) {
                stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
            } else {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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