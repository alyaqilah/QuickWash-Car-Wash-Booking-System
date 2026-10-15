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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AdminController {

    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Integer> idCol;
    @FXML private TableColumn<Booking, String> customerCol;
    @FXML private TableColumn<Booking, String> plateCol;
    @FXML private TableColumn<Booking, String> sizeCol;
    @FXML private TableColumn<Booking, String> packageCol;
    @FXML private TableColumn<Booking, String> addOnsCol;
    @FXML private TableColumn<Booking, String> dateCol;
    @FXML private TableColumn<Booking, String> timeSlotCol;
    @FXML private TableColumn<Booking, String> createdAtCol;
    @FXML private TableColumn<Booking, String> statusCol;
    @FXML private TableColumn<Booking, Double> priceCol;

    @FXML private BarChart<String, Number> packageBarChart;
    @FXML private TextField searchField;

    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (idCol != null) idCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBookingId()).asObject());
        if (customerCol != null) customerCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        if (plateCol != null) plateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLicensePlate()));
        if (sizeCol != null) sizeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getVehicleSize()));
        if (packageCol != null) packageCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPackageType()));
        if (addOnsCol != null) addOnsCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAddOns()));
        if (dateCol != null) dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBookingDate()));
        if (timeSlotCol != null) timeSlotCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeSlot()));
        if (createdAtCol != null) createdAtCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt()));
        if (statusCol != null) statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        
        if (priceCol != null) {
            priceCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getTotalPrice()).asObject());
            priceCol.setCellFactory(tc -> new TableCell<Booking, Double>() {
                @Override
                protected void updateItem(Double price, boolean empty) {
                    super.updateItem(price, empty);
                    if (empty || price == null) {
                        setText(null);
                    } else {
                        setText(String.format("RM %.2f", price));
                    }
                }
            });
        }

        loadAllBookings();
        loadChartData();
    }

    private void loadAllBookings() {
        bookingList.clear();
        String sql = "SELECT * FROM bookings ORDER BY booking_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("booking_id");
                String user = rs.getString("username");
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
                    String rawCreated = rs.getString("created_at");
                    if (rawCreated != null && rawCreated.length() >= 16) {
                        createdAt = rawCreated.substring(0, 16);
                    } else if (rawCreated != null) {
                        createdAt = rawCreated;
                    }
                } catch (Exception ignored) {}

                String status = rs.getString("status") != null ? rs.getString("status") : "Pending Verification";

                bookingList.add(new Booking(id, user, plate, size, pkg, addons, timeSlot, price, bookingDate, createdAt, status));
            }
            if (bookingTable != null) {
                bookingTable.setItems(bookingList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        
        if (keyword.isEmpty()) {
            bookingTable.setItems(bookingList);
            return;
        }

        ObservableList<Booking> filteredList = FXCollections.observableArrayList();
        for (Booking b : bookingList) {
            String idStr = String.valueOf(b.getBookingId());
            String plateStr = b.getLicensePlate() != null ? b.getLicensePlate().toLowerCase() : "";

            if (idStr.contains(keyword) || plateStr.contains(keyword)) {
                filteredList.add(b);
            }
        }
        bookingTable.setItems(filteredList);
    }

    private void loadChartData() {
        Map<String, Integer> packageCounts = new HashMap<>();

        String sql = "SELECT package_type, COUNT(*) as count FROM bookings GROUP BY package_type";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String pkg = rs.getString("package_type");
                int count = rs.getInt("count");
                packageCounts.put(pkg, count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bookings per Package");

        for (Map.Entry<String, Integer> entry : packageCounts.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());
            series.getData().add(data);
        }

        if (packageBarChart != null) {
            packageBarChart.getData().clear();
            packageBarChart.getData().add(series);
            
            // Apply Sage Green color to the chart bars once rendered
            javafx.application.Platform.runLater(() -> {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    if (data.getNode() != null) {
                        data.getNode().setStyle("-fx-bar-fill: #52796f;");
                    }
                }
            });
        }
    }

    @FXML
    private void handleVerifyBooking(ActionEvent event) {
        Booking selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a booking from the table to verify.");
            return;
        }

        String sql = "UPDATE bookings SET status = 'Verified' WHERE booking_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selectedBooking.getBookingId());
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Booking verified successfully!");
                loadAllBookings();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not verify booking: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportPDFReport(ActionEvent event) {
        if (bookingList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "There are no bookings available to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save System Bookings Report PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("QuickWash_System_Bookings_Report.pdf");

        File file = fileChooser.showSaveDialog(bookingTable.getScene().getWindow());

        if (file != null) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    float margin = 30;
                    float pageWidth = PDRectangle.A4.getWidth();
                    float contentWidth = pageWidth - (2 * margin);

                    cs.setNonStrokingColor(new Color(44, 74, 62));
                    cs.addRect(margin, 720, contentWidth, 50);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(Color.WHITE);
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 15);
                    cs.newLineAtOffset(margin + 15, 737);
                    cs.showText("QUICKWASH - SYSTEM BOOKINGS REPORT");
                    cs.endText();

                    String adminUser = UserSession.getUsername() != null ? UserSession.getUsername() : "admin";
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    cs.newLineAtOffset(margin, 690);
                    cs.showText("Generated by Admin: " + adminUser + " | Date: " + timestamp);
                    cs.endText();

                    float tableTopY = 660;
                    float rowHeight = 24;
                    
                    cs.setNonStrokingColor(new Color(230, 237, 232));
                    cs.addRect(margin, tableTopY - 18, contentWidth, rowHeight);
                    cs.fill();

                    float colID = margin + 5;         
                    float colCustomer = margin + 30;   
                    float colPlate = margin + 85;      
                    float colPackage = margin + 145;   
                    float colDate = margin + 220;      
                    float colTimeSlot = margin + 285;  
                    float colTotal = margin + 375;     
                    float colCreated = margin + 425;
                    float colStatus = margin + 495;

                    cs.setNonStrokingColor(new Color(44, 74, 62));
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 8f);

                    cs.beginText(); cs.newLineAtOffset(colID, tableTopY - 10); cs.showText("ID"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colCustomer, tableTopY - 10); cs.showText("Customer"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colPlate, tableTopY - 10); cs.showText("Plate"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colPackage, tableTopY - 10); cs.showText("Package"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colDate, tableTopY - 10); cs.showText("Date"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colTimeSlot, tableTopY - 10); cs.showText("Time Slot"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colTotal, tableTopY - 10); cs.showText("Total"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colCreated, tableTopY - 10); cs.showText("Timestamp"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(colStatus, tableTopY - 10); cs.showText("Status"); cs.endText();

                    float currentY = tableTopY - rowHeight - 8;
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);

                    double totalRevenue = 0.0;
                    int index = 0;

                    for (Booking b : bookingList) {
                        if (currentY < 120) break;

                        if (index % 2 == 1) {
                            cs.setNonStrokingColor(new Color(248, 250, 249));
                            cs.addRect(margin, currentY - 5, contentWidth, rowHeight);
                            cs.fill();
                        }

                        cs.setNonStrokingColor(Color.BLACK);

                        cs.beginText(); cs.newLineAtOffset(colID, currentY); cs.showText(String.valueOf(index + 1)); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colCustomer, currentY); cs.showText(b.getUsername() != null ? b.getUsername() : ""); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colPlate, currentY); cs.showText(b.getLicensePlate() != null ? b.getLicensePlate() : ""); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colPackage, currentY); cs.showText(b.getPackageType() != null ? b.getPackageType() : ""); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colDate, currentY); cs.showText(b.getBookingDate() != null ? b.getBookingDate() : ""); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colTimeSlot, currentY); cs.showText(b.getTimeSlot() != null ? b.getTimeSlot() : ""); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colTotal, currentY); cs.showText(String.format("RM%.2f", b.getTotalPrice())); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colCreated, currentY); cs.showText(b.getCreatedAt() != null ? b.getCreatedAt() : ""); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(colStatus, currentY); cs.showText(b.getStatus() != null ? b.getStatus() : ""); cs.endText();

                        totalRevenue += b.getTotalPrice();
                        currentY -= rowHeight;
                        index++;
                    }

                    float footerY = currentY - 10;
                    
                    cs.setStrokingColor(new Color(200, 200, 200));
                    cs.setLineWidth(1f);
                    cs.moveTo(margin, footerY + 12);
                    cs.lineTo(pageWidth - margin, footerY + 12);
                    cs.stroke();

                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                    cs.setNonStrokingColor(new Color(44, 74, 62));
                    cs.newLineAtOffset(margin, footerY - 8);
                    cs.showText(String.format("Total Bookings: %d    |    Total System Revenue: RM %.2f", bookingList.size(), totalRevenue));
                    cs.endText();
                }

                document.save(file);
                showAlert(Alert.AlertType.INFORMATION, "Success", "System Bookings Report PDF generated successfully!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to generate report: " + e.getMessage());
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

    @FXML
    private void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        switchScene(event, "/com/mycompany/quickwashproject/LoginView.fxml", "QuickWash - Login");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (event.getSource() instanceof MenuItem) ? 
                (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow() : 
                (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(view, 1050, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/quickwashproject/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}