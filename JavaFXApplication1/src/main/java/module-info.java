module com.mycompany.javafxapplication1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.base;
    requires java.sql;
    
    

    opens com.mycompany.javafxapplication1 to javafx.fxml;
    exports com.mycompany.javafxapplication1;
    requires jsch;
     
}
