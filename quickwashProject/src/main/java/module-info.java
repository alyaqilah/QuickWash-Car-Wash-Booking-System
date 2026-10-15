module com.mycompany.quickwashproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.pdfbox;
    requires java.desktop;

    opens com.mycompany.quickwashproject to javafx.fxml;
    exports com.mycompany.quickwashproject;
}
