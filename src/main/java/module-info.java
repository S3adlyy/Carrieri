module com.example.guser {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;//important
    requires com.dlsc.formsfx;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.services.s3;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.auth;
    //requires com.example.guser;
    requires java.net.http;
    requires org.fxmisc.richtext;
    requires javafx.media;
    requires org.fxmisc.flowless;
    requires javafx.graphics;



    opens com.example.guser to javafx.fxml;
    opens com.example.guser.controllers to javafx.fxml;


    exports com.example.guser;
}