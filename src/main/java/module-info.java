module com.example.guser {
    requires javafx.controls;
    requires javafx.base;
    requires javafx.fxml;
    requires java.sql;
    requires com.dlsc.formsfx;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.services.s3;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.auth;
    requires java.net.http;
    requires org.fxmisc.richtext;
    requires javafx.media;
    requires org.fxmisc.flowless;
    requires javafx.graphics;
    requires layout;
    requires kernel;
    requires twilio;
    requires com.google.gson;
    requires com.fasterxml.jackson.annotation;
    requires java.mail;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires com.fasterxml.jackson.databind;
    requires okhttp3;
    requires javafx.web;
    requires stripe.java;
    requires jdk.jsobject;
    requires java.desktop;
    requires java.prefs;
    requires io;
    requires com.google.zxing;
    requires org.json;
    requires com.google.zxing.javase;
    requires javafx.swing;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires jjwt.api;


    opens com.example.guser to javafx.fxml;
    opens com.example.guser.controllers.guser to javafx.fxml;
    opens com.example.guser.controllers.grecru to javafx.fxml, javafx.graphics;
    exports com.example.guser.controllers.grecru;
    opens entities.grecru to javafx.base;
    exports entities.grecru;
    opens services.grecru to com.fasterxml.jackson.databind, com.fasterxml.jackson.annotation;
    exports services.grecru;
    exports com.example.guser;

    exports com.example.guser.controllers.getude;

    // OPEN ENTITY PACKAGES TO JAVAFX BASE - THIS IS THE KEY FIX
    opens entities.getude to javafx.base;

    // ONLY open/exports packages that ACTUALLY EXIST
    opens com.example.guser.controllers.getude to javafx.fxml;
    //ons
    opens com.example.guser.controllers.goffre to javafx.fxml;
    opens entities.goffre to javafx.base, javafx.graphics;
    exports com.example.guser.controllers.goffre;

    //selim reclam
    opens com.example.guser.controllers.greclam to javafx.fxml;
    opens entities.greclam to javafx.base, javafx.graphics;
    exports com.example.guser.controllers.greclam;

    //youssef commu
    opens com.example.guser.controllers.gcommu to javafx.fxml;
    opens entities.gcommu to javafx.base, javafx.graphics;
    exports com.example.guser.controllers.gcommu;

}