package com.mycompany.quickwashproject;

public class Booking {
    private int id;
    private String username;
    private String licensePlate;
    private String vehicleSize;
    private String packageType;
    private String addOns;
    private String timeSlot;
    private double totalPrice;
    private String bookingDate;
    private String createdAt;
    private String status;

    // Constructor with status
    public Booking(int id, String username, String licensePlate, String vehicleSize, String packageType, String addOns, String timeSlot, double totalPrice, String bookingDate, String createdAt, String status) {
        this.id = id;
        this.username = username;
        this.licensePlate = licensePlate;
        this.vehicleSize = vehicleSize;
        this.packageType = packageType;
        this.addOns = addOns;
        this.timeSlot = timeSlot;
        this.totalPrice = totalPrice;
        this.bookingDate = bookingDate;
        this.createdAt = createdAt;
        this.status = status;
    }

    // Overloaded constructor without status (defaults to Pending)
    public Booking(int id, String username, String licensePlate, String vehicleSize, String packageType, String addOns, String timeSlot, double totalPrice, String bookingDate, String createdAt) {
        this(id, username, licensePlate, vehicleSize, packageType, addOns, timeSlot, totalPrice, bookingDate, createdAt, "Pending");
    }

    // Getters
    public int getBookingId() { return id; }
    public String getUsername() { return username; }
    public String getLicensePlate() { return licensePlate; }
    public String getVehicleSize() { return vehicleSize; }
    public String getPackageType() { return packageType; }
    public String getAddOns() { return addOns; }
    public String getTimeSlot() { return timeSlot; }
    public double getTotalPrice() { return totalPrice; }
    public String getBookingDate() { return bookingDate; }
    public String getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
}