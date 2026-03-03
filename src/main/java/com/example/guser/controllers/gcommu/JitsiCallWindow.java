package com.example.guser.controllers.gcommu;

import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class JitsiCallWindow extends Stage {

    private WebView webView;
    private WebEngine webEngine;
    private String roomName;
    private String userName;
    private boolean isVideo;

    public JitsiCallWindow(String roomName, String userName, boolean isVideo) {
        this.roomName = roomName;
        this.userName = userName;
        this.isVideo = isVideo;

        setTitle(isVideo ? "Appel vidéo - Jitsi Meet" : "Appel audio - Jitsi Meet");
        setWidth(1000);
        setHeight(700);

        BorderPane root = new BorderPane();

        // Barre de contrôle
        HBox controlBar = createControlBar();
        root.setTop(controlBar);

        // WebView pour Jitsi
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        // Gérer les erreurs
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED) {
                System.err.println("Erreur de chargement Jitsi");
            }
        });

        // Charger Jitsi
        loadJitsiMeet();

        root.setCenter(webView);

        Scene scene = new Scene(root);
        setScene(scene);

        // Fermeture propre
        setOnCloseRequest(e -> {
            webEngine.load(null);
            webView = null;
        });
    }

    private HBox createControlBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10));
        bar.setStyle("-fx-background-color: #1F2937;");

        Label statusLabel = new Label(roomName);
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button muteBtn = new Button("🔇 Mute");
        Button cameraBtn = new Button(isVideo ? "📹 Caméra" : "🎤 Audio");
        Button screenBtn = new Button("🖥️ Partage");
        Button hangupBtn = new Button("📞 Raccrocher");

        styleButton(muteBtn);
        styleButton(cameraBtn);
        styleButton(screenBtn);

        hangupBtn.setStyle(
            "-fx-background-color: #EF4444;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 20;" +
                "-fx-background-radius: 20;"
        );
        hangupBtn.setOnAction(e -> close());

        bar.getChildren().addAll(statusLabel, muteBtn, cameraBtn, screenBtn, hangupBtn);
        return bar;
    }

    private void styleButton(Button btn) {
        btn.setStyle(
            "-fx-background-color: #374151;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 8 15;" +
                "-fx-background-radius: 20;"
        );
        btn.setOnMouseEntered(e ->
            btn.setStyle(
                "-fx-background-color: #4B5563;" +
                    "-fx-text-fill: white;" +
                    "-fx-padding: 8 15;" +
                    "-fx-background-radius: 20;"
            ));
        btn.setOnMouseExited(e ->
            btn.setStyle(
                "-fx-background-color: #374151;" +
                    "-fx-text-fill: white;" +
                    "-fx-padding: 8 15;" +
                    "-fx-background-radius: 20;"
            ));
    }

    private void loadJitsiMeet() {
        String videoParam = isVideo ? "true" : "false";
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script src='https://meet.jit.si/external_api.js'></script>
                <style>
                    body { margin: 0; padding: 0; background-color: #1a1a1a; }
                    #jitsi-container { height: 100vh; }
                </style>
            </head>
            <body>
                <div id="jitsi-container"></div>
                <script>
                    const domain = 'meet.jit.si';
                    const options = {
                        roomName: '%s',
                        width: '100%%',
                        height: '100%%',
                        parentNode: document.querySelector('#jitsi-container'),
                        configOverwrite: {
                            startWithAudioMuted: false,
                            startWithVideoMuted: %s,
                            prejoinPageEnabled: false
                        },
                        interfaceConfigOverwrite: {
                            TOOLBAR_BUTTONS: [
                                'microphone', 'camera', 'closedcaptions', 'desktop',
                                'fullscreen', 'fodeviceselection', 'hangup',
                                'profile', 'chat', 'recording', 'livestreaming',
                                'etherpad', 'sharedvideo', 'settings', 'raisehand',
                                'videoquality', 'filmstrip', 'invite', 'feedback',
                                'stats', 'shortcuts', 'tileview', 'download'
                            ]
                        },
                        userInfo: {
                            displayName: '%s'
                        }
                    };

                    const api = new JitsiMeetExternalAPI(domain, options);

                    window.addEventListener('beforeunload', function() {
                        api.dispose();
                    });
                </script>
            </body>
            </html>
        """.formatted(roomName, videoParam, userName);

        webEngine.loadContent(html);
    }
}
