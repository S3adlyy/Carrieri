module com.exemple.grecrutement {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.exemple.grecrutement to javafx.fxml;
    opens entities to javafx.base;
    exports com.exemple.grecrutement;
}