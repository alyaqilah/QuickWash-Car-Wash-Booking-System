package com.mycompany.quickwashproject;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileController {

    @FXML private Label accountDetailsLabel;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Integer> idCol;
    @FXML private TableColumn<Booking, String> plateCol;
    @FXML private TableColumn<Booking, String> sizeCol;
    @FXML private TableColumn<Booking, String> packageCol;
    @FXML private TableColumn<Booking, String> addOnsCol;
    @FXML private TableColumn<Booking, String> dateCol;
    @FXML private TableColumn<Booking, String> timeSlotCol;
    @FXML private TableColumn<Booking, String> createdAtCol;
    @FXML private TableColumn<Booking, String> statusCol;
    @FXML private TableColumn<Booking, Double> priceCol;

    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (idCol != null) idCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBookingId()).asObject());
        if (plateCol != null) plateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLicensePlate()));
        if (sizeCol != null) sizeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getVehicleSize()));
        if (packageCol != null) packageCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPackageType()));
        if (addOnsCol != null) addOnsCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAddOns()));
        if (dateCol != null) dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBookingDate()));
        if (timeSlotCol != null) timeSlotCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeSlot()));
        if (createdAtCol != null) createdAtCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt()));
        if (statusCol != null) statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        if (priceCol != null) priceCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalPrice()).asObject());

        loadUserProfileData();
        loadUserBookings();
    }

    private void loadUserProfileData() {
        String activeUser = UserSession.getUsername();
        
        if (activeUser == null || activeUser.trim().isEmpty() || activeUser.equalsIgnoreCase("null")) {
            String sqlCheck = "SELECT username FROM bookings ORDER BY booking_id DESC LIMIT 1";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlCheck);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    activeUser = rs.getString("username");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (activeUser == null || activeUser.trim().isEmpty()) {
            activeUser = "alya";
        }

        String role = "Registered Client";
        int points = 0;
        int bookingCount = 0;

        String sqlUser = "SELECT role, loyalty_points FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlUser)) {
            pstmt.setString(1, activeUser);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("role") != null) role = rs.getString("role");
                points = rs.getInt("loyalty_points");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sqlCount = "SELECT COUNT(*) FROM bookings WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlCount)) {
            pstmt.setString(1, activeUser);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                bookingCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int tierLevel = 1;
        String tierName = "Starter";
        double discountRate = 0.0;

        if (bookingCount >= 10) {
            tierLevel = 4;
            tierName = "Platinum";
            discountRate = 0.15;
        } else if (bookingCount >= 6) {
            tierLevel = 3;
            tierName = "Gold";
            discountRate = 0.10;
        } else if (bookingCount >= 3) {
            tierLevel = 2;
            tierName = "Silver";
            discountRate = 0.05;
        }

        if (accountDetailsLabel != null) {
            String displayText = String.format(
                "Username: %s\nRole: %s\nTotal Bookings: %d | Loyalty Points: %d\nMembership Tier: Level %d (%s) - %.0f%% Discount",
                activeUser, role, bookingCount, points, tierLevel, tierName, discountRate * 100
            );
            accountDetailsLabel.setText(displayText);
        }
    }

    private void loadUserBookings() {
        bookingList.clear();
        String activeUser = UserSession.getUsername();
        
        if (activeUser == null || activeUser.trim().isEmpty()) {
            activeUser = "alya";
        }
        
        String sql = "SELECT * FROM bookings WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, activeUser);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("booking_id");
                String plate = rs.getString("license_plate");
                String size = rs.getString("vehicle_size");
                String pkg = rs.getString("package_type");
                String addons = rs.getString("add_ons") != null ? rs.getString("add_ons") : "";
                String timeSlot = rs.getString("time_slot") != null ? rs.getString("time_slot") : "";
                double price = rs.getDouble("total_price");
                
                String rawDate = rs.getString("booking_date");
                String bookingDate = (rawDate != null && rawDate.length() >= 10) ? rawDate.substring(0, 10) : (rawDate != null ? rawDate : "");
                
                String createdAt = "N/A";
                try {
                    createdAt = rs.getString("created_at") != null ? rs.getString("created_at") : "";
                } catch (Exception ignored) {}

                String status = rs.getString("status") != null ? rs.getString("status") : "Pending Verification";

                bookingList.add(new Booking(id, activeUser, plate, size, pkg, addons, timeSlot, price, bookingDate, createdAt, status));
            }
            if (bookingTable != null) {
                bookingTable.setItems(bookingList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDownloadReceipt(ActionEvent event) {
        Booking selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a booking from your history table first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Booking Receipt PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("QuickWash_Receipt_" + selectedBooking.getBookingId() + ".pdf");

        File file = fileChooser.showSaveDialog(bookingTable.getScene().getWindow());

        if (file != null) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.setNonStrokingColor(new Color(44, 62, 53));
                    cs.addRect(40, 720, 515, 50);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(Color.WHITE);
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 15);
                    cs.newLineAtOffset(50, 737);
                    cs.showText("QUICKWASH - OFFICIAL TRANSACTION RECEIPT");
                    cs.endText();

                    cs.setNonStrokingColor(Color.BLACK);
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    cs.newLineAtOffset(40, 690);
                    cs.showText("Receipt ID: #QW-REC-" + selectedBooking.getBookingId());
                    cs.newLineAtOffset(0, -15);
                    cs.showText("Date Issued: " + java.time.LocalDate.now());
                    cs.endText();

                    cs.setStrokingColor(new Color(200, 200, 200));
                    cs.setLineWidth(1f);
                    cs.moveTo(40, 660);
                    cs.lineTo(555, 660);
                    cs.stroke();

                    float startY = 620;
                    float rowHeight = 26;

                    String[][] details = {
                        {"Customer Username:", selectedBooking.getUsername()},
                        {"Vehicle License Plate:", selectedBooking.getLicensePlate()},
                        {"Vehicle Size Class:", selectedBooking.getVehicleSize()},
                        {"Selected Package:", selectedBooking.getPackageType()},
                        {"Optional Add-ons:", !selectedBooking.getAddOns().isEmpty() ? selectedBooking.getAddOns() : "None"},
                        {"Preferred Appointment Date:", selectedBooking.getBookingDate()},
                        {"Reserved Time Slot:", selectedBooking.getTimeSlot()},
                        {"Booking Timestamp:", selectedBooking.getCreatedAt()}
                    };

                    for (int i = 0; i < details.length; i++) {
                        float y = startY - (i * rowHeight);

                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                        cs.newLineAtOffset(50, y);
                        cs.showText(details[i][0]);
                        cs.endText();

                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        cs.newLineAtOffset(230, y);
                        cs.showText(details[i][1]);
                        cs.endText();
                    }

                    float totalBoxY = startY - (details.length * rowHeight) - 15;
                    cs.setNonStrokingColor(new Color(240, 244, 241));
                    cs.addRect(40, totalBoxY - 10, 515, 36);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(new Color(44, 62, 53));
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                    cs.newLineAtOffset(50, totalBoxY + 2);
                    cs.showText("TOTAL AMOUNT PAID:");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
                    cs.newLineAtOffset(420, totalBoxY + 2);
                    cs.showText(String.format("RM %.2f", selectedBooking.getTotalPrice()));
                    cs.endText();

                    cs.beginText();
                    cs.setNonStrokingColor(new Color(100, 100, 100));
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
                    cs.newLineAtOffset(180, totalBoxY - 60);
                    cs.showText("Thank you for choosing QuickWash! Drive safely.");
                    cs.endText();
                }

                document.save(file);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Receipt PDF downloaded successfully!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to generate receipt: " + e.getMessage());
                e.printStackTrace();
            }
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
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}