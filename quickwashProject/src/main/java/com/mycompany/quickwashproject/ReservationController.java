package com.mycompany.quickwashproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationController {

    @FXML private TextField licensePlateField;
    @FXML private RadioButton sedanRadio;
    @FXML private RadioButton suvRadio;
    @FXML private RadioButton quickWashRadio;
    @FXML private RadioButton deluxeWashRadio;
    @FXML private RadioButton fullDetailRadio;
    @FXML private CheckBox vacuumCheckBox;
    @FXML private CheckBox waxCheckBox;
    @FXML private CheckBox tireCheckBox;
    @FXML private DatePicker bookingDatePicker;
    @FXML private ComboBox<String> timeSlotCombo;
    @FXML private Label totalPriceLabel;
    @FXML private Label discountInfoLabel;
    @FXML private Label tierBadgeLabel;

    private double finalCalculatedTotal = 0.0;

    @FXML
    public void initialize() {
        if (quickWashRadio != null && sedanRadio != null) {
            quickWashRadio.setSelected(true);
            sedanRadio.setSelected(true);
        }
        
        // Populate time slots
        if (timeSlotCombo != null) {
            timeSlotCombo.getItems().addAll(
                "09:00 AM - 10:00 AM",
                "10:00 AM - 11:00 AM",
                "11:00 AM - 12:00 PM",
                "02:00 PM - 03:00 PM",
                "03:00 PM - 04:00 PM",
                "04:00 PM - 05:00 PM"
            );
            timeSlotCombo.getSelectionModel().selectFirst();
        }

        // Default date picker to today
        if (bookingDatePicker != null) {
            bookingDatePicker.setValue(LocalDate.now());
        }
        
        loadUserTierBadge();
    }

    private double[] getMembershipTierAndDiscount(String username) {
        int bookingCount = 0;
        String sql = "SELECT COUNT(*) FROM bookings WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    bookingCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int tierLevel = 1;
        double discountRate = 0.0; // 0%

        if (bookingCount >= 10) {
            tierLevel = 4; // Platinum
            discountRate = 0.15; // 15% discount
        } else if (bookingCount >= 6) {
            tierLevel = 3; // Gold
            discountRate = 0.10; // 10% discount
        } else if (bookingCount >= 3) {
            tierLevel = 2; // Silver
            discountRate = 0.05; // 5% discount
        }

        return new double[]{ tierLevel, discountRate };
    }

    @FXML
    private void handleCalculateTotal(ActionEvent event) {
        double basePrice = 0.0;

        // 1. Get Base Package Price
        if (quickWashRadio != null && quickWashRadio.isSelected()) {
            basePrice = 25.0;
        } else if (deluxeWashRadio != null && deluxeWashRadio.isSelected()) {
            basePrice = 55.0;
        } else if (fullDetailRadio != null && fullDetailRadio.isSelected()) {
            basePrice = 120.0;
        }

        // 2. Add Vehicle Size Surcharge
        if (suvRadio != null && suvRadio.isSelected()) {
            basePrice += 20.0;
        }

        // 3. Add Optional Add-ons
        if (vacuumCheckBox != null && vacuumCheckBox.isSelected()) basePrice += 15.0;
        if (waxCheckBox != null && waxCheckBox.isSelected()) basePrice += 40.0;
        if (tireCheckBox != null && tireCheckBox.isSelected()) basePrice += 20.0;

        // 4. Calculate Membership Tier Discount Percentage
        double discountRate = 0.0;
        int bookingCount = getUserBookingCount();

        if (bookingCount >= 10) {
            discountRate = 0.15; // Level 4 (Platinum): 15% off
        } else if (bookingCount >= 6) {
            discountRate = 0.10; // Level 3 (Gold): 10% off
        } else if (bookingCount >= 3) {
            discountRate = 0.05; // Level 2 (Silver): 5% off
        }

        double discountAmount = basePrice * discountRate;
        double finalPrice = basePrice - discountAmount;

        // Assign to class-level variable so saving works properly
        finalCalculatedTotal = finalPrice;

        // 5. Update the Total Price Label
        if (totalPriceLabel != null) {
            if (discountRate > 0) {
                totalPriceLabel.setText(String.format("RM %.2f (%.0f%% Tier Disc.)", finalPrice, discountRate * 100));
            } else {
                totalPriceLabel.setText(String.format("RM %.2f", finalPrice));
            }
        }
    }

    // Helper method to get total bookings for current user from database
    private int getUserBookingCount() {
        String username = UserSession.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "alya"; // Fallback username
        }

        int count = 0;
        String sql = "SELECT COUNT(*) FROM bookings WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    @FXML
    private void handleSaveToDatabase(ActionEvent event) {
        if (finalCalculatedTotal <= 0.0) {
            showAlert(Alert.AlertType.WARNING, "Calculation Required", "Please click 'Calculate Total' before saving your reservation.");
            return;
        }

        String licensePlate = licensePlateField.getText().trim();
        if (licensePlate.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing License Plate", "Please enter your vehicle license plate.");
            return;
        }

        LocalDate selectedDate = bookingDatePicker.getValue();
        if (selectedDate == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Date", "Please select a booking date.");
            return;
        }

        String timeSlot = timeSlotCombo.getValue();
        if (timeSlot == null || timeSlot.isEmpty()) {
            timeSlot = "09:00 AM - 10:00 AM";
        }

        String sql = "INSERT INTO bookings (username, license_plate, vehicle_size, package_type, add_ons, time_slot, total_price, booking_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Pending')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String currentUser = UserSession.getUsername() != null ? UserSession.getUsername() : "guest";
            pstmt.setString(1, currentUser);
            pstmt.setString(2, licensePlate);
            pstmt.setString(3, sedanRadio.isSelected() ? "Sedan" : "SUV/Van");
            pstmt.setString(4, quickWashRadio.isSelected() ? "Quick Wash" : (deluxeWashRadio.isSelected() ? "Deluxe Wash" : "Full Detail"));

            StringBuilder addOnsStr = new StringBuilder();
            if (vacuumCheckBox.isSelected()) addOnsStr.append("Vacuum ");
            if (waxCheckBox.isSelected()) addOnsStr.append("Wax ");
            if (tireCheckBox.isSelected()) addOnsStr.append("Tire Shine");
            pstmt.setString(5, addOnsStr.toString().trim());

            pstmt.setString(6, timeSlot);
            pstmt.setDouble(7, finalCalculatedTotal);
            pstmt.setString(8, selectedDate.toString());

            pstmt.executeUpdate();

            updateLoyaltyPoints(currentUser, 10);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation Saved Successfully for " + selectedDate + " (" + timeSlot + ")! Earned 10 Loyalty Points.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save booking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateLoyaltyPoints(String username, int pointsToAdd) {
        String updateSql = "UPDATE users SET loyalty_points = loyalty_points + ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, pointsToAdd);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            checkAndUpdateTier(username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkAndUpdateTier(String username) {
        if (username == null || username.trim().isEmpty() || username.equalsIgnoreCase("guest")) {
            return;
        }

        int bookingCount = 0;
        String countSql = "SELECT COUNT(*) FROM bookings WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    bookingCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String newTier = "Level 1 (Starter)";
        if (bookingCount >= 10) {
            newTier = "Level 4 (Platinum)";
        } else if (bookingCount >= 6) {
            newTier = "Level 3 (Gold)";
        } else if (bookingCount >= 3) {
            newTier = "Level 2 (Silver)";
        }

        String updateTierSql = "UPDATE users SET membership_tier = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateTierSql)) {
            pstmt.setString(1, newTier);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("QuickWash System");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void goToReservation(ActionEvent event) { switchScene(event, "/com/mycompany/quickwashproject/ReservationView.fxml", "QuickWash - Booking Dashboard"); }
    @FXML private void goToPrice(ActionEvent event) { switchScene(event, "/com/mycompany/quickwashproject/PriceView.fxml", "QuickWash - Price Listing"); }
    @FXML private void goToGuide(ActionEvent event) { switchScene(event, "/com/mycompany/quickwashproject/GuideView.fxml", "QuickWash - Station Guide"); }
    @FXML private void goToProfile(ActionEvent event) { switchScene(event, "/com/mycompany/quickwashproject/ProfileView.fxml", "QuickWash - User Profile"); }
    @FXML private void handleLogout(ActionEvent event) { UserSession.clearSession(); switchScene(event, "/com/mycompany/quickwashproject/LoginView.fxml", "QuickWash - Login"); }

    private void loadUserTierBadge() {
        String username = UserSession.getUsername(); 
        if (username == null || username.trim().isEmpty()) {
            username = "alya";
        }

        int bookingCount = 0;
        String sql = "SELECT COUNT(*) FROM bookings WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    bookingCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String tierName = "Level 1 (Starter)";
        if (bookingCount >= 10) {
            tierName = "Level 4 (Platinum)";
        } else if (bookingCount >= 6) {
            tierName = "Level 3 (Gold)";
        } else if (bookingCount >= 3) {
            tierName = "Level 2 (Silver)";
        }

        if (tierBadgeLabel != null) {
            tierBadgeLabel.setText("Tier: " + tierName);
        }
    }
    
    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (event.getSource() instanceof MenuItem) ? 
                (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow() : 
                (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(view, 850, 650);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/quickwashproject/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}