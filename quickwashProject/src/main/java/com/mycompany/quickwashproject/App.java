package com.mycompany.quickwashproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/quickwashproject/LoginView.fxml"));
        Scene scene = new Scene(root, 850, 650);
        
        scene.getStylesheets().add(getClass().getResource("/com/mycompany/quickwashproject/style.css").toExternalForm());
        
        primaryStage.setTitle("QuickWash Car Wash Booking System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}