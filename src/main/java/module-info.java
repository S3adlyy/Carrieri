module com.exemple.grecrutement {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires com.fasterxml.jackson.core;

    // Open ALL packages to Jackson and JavaFX
    opens com.exemple.grecrutement to javafx.fxml;
    opens services to com.fasterxml.jackson.databind, javafx.fxml;
    opens entities to com.fasterxml.jackson.databind, javafx.fxml;

    // Export packages
    exports com.exemple.grecrutement;
    exports services;
    exports entities;
}