package com.mycompany.quickwashproject;

public class UserSession {
    private static String username;
    private static String role;

    public static void setSession(String user, String userRole) {
        username = user;
        role = userRole;
    }

    public static void setUsername(String user) {
        username = user;
    }

    public static String getUsername() {
        return username;
    }

    public static String getRole() {
        return role;
    }

    public static void clearSession() {
        username = null;
        role = null;
    }
}