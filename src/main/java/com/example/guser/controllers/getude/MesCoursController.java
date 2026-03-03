package com.example.guser.controllers.getude;

import com.example.guser.controllers.guser.AppNavController;
import entities.getude.Cours;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import services.getude.CoursService;
import services.getude.ProgressionCoursService;
import session.SessionContext;
import utils.getude.AlertUtils;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MesCoursController {

    @FXML private Label lblStats;
    @FXML private Label lblEnCours;
    @FXML private Label lblTermines;
    @FXML private Label lblTotalHeures;
    @FXML private ToggleButton btnFiltreTous;
    @FXML private ToggleButton btnFiltreEnCours;
    @FXML private ToggleButton btnFiltreTermines;
    @FXML private TextField txtRecherche;
    @FXML private VBox coursContainer;

    private CoursService coursService = new CoursService();
    private ProgressionCoursService progressionService = new ProgressionCoursService();
    int me = SessionContext.getCurrentUser().getId();
    private int candidatId = me;
    private List<Cours> tousLesCours = new ArrayList<>();
    private List<Cours> coursAvecProgression = new ArrayList<>();
    private String filtreActuel = "TOUS";

    @FXML
    public void initialize() {
        setupFiltres();
        chargerCours();

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            filtrerCours();
        });
    }

    private void setupFiltres() {
        ToggleGroup groupeFiltre = new ToggleGroup();
        btnFiltreTous.setToggleGroup(groupeFiltre);
        btnFiltreEnCours.setToggleGroup(groupeFiltre);
        btnFiltreTermines.setToggleGroup(groupeFiltre);

        btnFiltreTous.setOnAction(e -> {
            filtreActuel = "TOUS";
            updateButtonStyles();
            filtrerCours();
        });
        btnFiltreEnCours.setOnAction(e -> {
            filtreActuel = "EN_COURS";
            updateButtonStyles();
            filtrerCours();
        });
        btnFiltreTermines.setOnAction(e -> {
            filtreActuel = "TERMINES";
            updateButtonStyles();
            filtrerCours();
        });
    }

    private void updateButtonStyles() {
        btnFiltreTous.setStyle(btnFiltreTous.isSelected() ?
                "-fx-background-radius: 20; -fx-padding: 5 15; -fx-background-color: #5E548E; -fx-text-fill: white;" :
                "-fx-background-radius: 20; -fx-padding: 5 15; -fx-background-color: #f3e8ff; -fx-text-fill: #5E548E;");

        btnFiltreEnCours.setStyle(btnFiltreEnCours.isSelected() ?
                "-fx-background-radius: 20; -fx-padding: 5 15; -fx-background-color: #5E548E; -fx-text-fill: white;" :
                "-fx-background-radius: 20; -fx-padding: 5 15; -fx-background-color: #f3e8ff; -fx-text-fill: #5E548E;");

        btnFiltreTermines.setStyle(btnFiltreTermines.isSelected() ?
                "-fx-background-radius: 20; -fx-padding: 5 15; -fx-background-color: #5E548E; -fx-text-fill: white;" :
                "-fx-background-radius: 20; -fx-padding: 5 15; -fx-background-color: #f3e8ff; -fx-text-fill: #5E548E;");
    }

    private void chargerCours() {
        try {
            tousLesCours = coursService.getAll();
            coursAvecProgression.clear();

            int enCours = 0;
            int termines = 0;
            int totalHeures = 0;

            for (Cours cours : tousLesCours) {
                double progression = progressionService.getProgressionCours(candidatId, cours.getId());

                if (progression > 0) {
                    coursAvecProgression.add(cours);

                    if (progression >= 99.9) {
                        termines++;
                    } else {
                        enCours++;
                    }

                    totalHeures += (cours.getDuree() * progression / 100);
                }
            }

            lblEnCours.setText(String.valueOf(enCours));
            lblTermines.setText(String.valueOf(termines));
            lblTotalHeures.setText(totalHeures + "h");
            lblStats.setText(coursAvecProgression.size() + " cours suivis");

            afficherCours(coursAvecProgression);

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger les cours.");
        }
    }

    private void afficherCours(List<Cours> coursList) {
        coursContainer.getChildren().clear();

        if (coursList.isEmpty()) {
            VBox emptyBox = new VBox(20);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPrefHeight(300);

            Label icon = new Label("📚");
            icon.setStyle("-fx-font-size: 60px; -fx-opacity: 0.3;");

            Label title = new Label("Aucun cours " + (filtreActuel.equals("EN_COURS") ? "en cours" :
                    filtreActuel.equals("TERMINES") ? "terminé" : ""));
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #9ca3af;");

            Label subtitle = new Label("Commencez à suivre des cours depuis le catalogue");
            subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #d1d5db;");

            Button btnExplorer = new Button("📖 Explorer le catalogue");
            btnExplorer.setStyle("-fx-background-color: #5E548E; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 25; -fx-cursor: hand;");
            btnExplorer.setOnAction(e -> AppNavController.getInstance().onEtudeCatalogue());

            emptyBox.getChildren().addAll(icon, title, subtitle, btnExplorer);
            coursContainer.getChildren().add(emptyBox);
            return;
        }

        for (Cours cours : coursList) {
            coursContainer.getChildren().add(creerCarteCours(cours));
        }
    }

    private VBox creerCarteCours(Cours cours) {
        double progression = progressionService.getProgressionCours(candidatId, cours.getId());
        boolean estTermine = progression >= 99.9;

        VBox card = new VBox(15);
        card.getStyleClass().add("course-card");
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setEffect(new DropShadow(10, Color.rgb(94, 84, 142, 0.15)));

        // --- IMAGE DE COUVERTURE ---
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.setMinHeight(160);
        imageContainer.setMaxHeight(160);
        imageContainer.setPrefWidth(350);
        imageContainer.setMinWidth(350);
        imageContainer.setMaxWidth(350);
        imageContainer.setStyle("-fx-background-color: #5E548E; -fx-background-radius: 20 20 0 0;");

        if (cours.getImageCouverture() != null && cours.getImageCouverture().length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(cours.getImageCouverture()));
                ImageView imageView = new ImageView(image);

                imageView.setFitWidth(350);
                imageView.setFitHeight(160);
                imageView.setPreserveRatio(false);
                imageView.setSmooth(true);

                Rectangle clip = new Rectangle(350, 160);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                imageView.setClip(clip);

                imageContainer.getChildren().add(imageView);

            } catch (Exception e) {
                System.err.println("Erreur image: " + e.getMessage());
            }
        }

        card.getChildren().add(imageContainer);

        // --- CONTENU DE LA CARTE ---
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("card-content");

        // Badges et titre
        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label lblNiveau = new Label(cours.getNiveau());
        lblNiveau.getStyleClass().addAll("level-badge", getLevelStyle(cours.getNiveau()));

        Label lblCategorie = new Label(cours.getCompetences_visees() != null ?
                cours.getCompetences_visees().split(",")[0] : "Général");
        lblCategorie.getStyleClass().add("category-badge");

        badges.getChildren().addAll(lblNiveau, lblCategorie);

        Label lblTitre = new Label(cours.getTitre());
        lblTitre.getStyleClass().add("course-title-label");
        lblTitre.setWrapText(true);

        // Progression
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar(progression / 100);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(8);
        progressBar.getStyleClass().add("progress-bar");

        Label lblProgression = new Label(String.format("%.0f%%", progression));
        lblProgression.setStyle("-fx-font-weight: bold; -fx-text-fill: #5E548E;");

        progressBox.getChildren().addAll(progressBar, lblProgression);

        // Métadonnées
        HBox metaBox = new HBox(20);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        HBox durationBox = new HBox(5);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        Label clockIcon = new Label("⏱");
        clockIcon.setStyle("-fx-font-size: 14px;");
        Label lblDuration = new Label(cours.getDuree() + " heures");
        lblDuration.getStyleClass().add("duration-label");
        durationBox.getChildren().addAll(clockIcon, lblDuration);

        metaBox.getChildren().add(durationBox);

        // Statut
        HBox statusBox = new HBox(5);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusIcon = new Label(estTermine ? "✅" : "📖");
        statusIcon.setStyle("-fx-font-size: 14px;");
        Label lblStatus = new Label(estTermine ? "Terminé" : "En cours");
        lblStatus.setStyle(estTermine ? "-fx-text-fill: #10b981; -fx-font-weight: bold;" :
                "-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        statusBox.getChildren().addAll(statusIcon, lblStatus);
        metaBox.getChildren().add(statusBox);

        // Bouton continuer
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnContinuer = new Button(estTermine ? "Revoir le cours" : "Continuer");
        btnContinuer.getStyleClass().add("btn-commencer");
        btnContinuer.setOnAction(e -> ouvrirCours(cours));
        btnContinuer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnContinuer, Priority.ALWAYS);

        buttonBox.getChildren().addAll(btnContinuer);

        content.getChildren().addAll(badges, lblTitre, progressBox, metaBox, buttonBox);
        card.getChildren().add(content);

        // Effets de survol
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(94,84,142,0.3), 20, 0, 0, 5);");
            card.setTranslateY(-3);
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(94,84,142,0.15), 10, 0, 0, 2);");
            card.setTranslateY(0);
        });

        return card;
    }

    private String getLevelStyle(String niveau) {
        if (niveau == null) return "level-intermediaire";
        switch (niveau.toLowerCase()) {
            case "débutant": return "level-debutant";
            case "intermédiaire": return "level-intermediaire";
            case "avancé": return "level-avance";
            case "expert": return "level-expert";
            case "master": return "level-master";
            default: return "level-intermediaire";
        }
    }

    private void filtrerCours() {
        String recherche = txtRecherche.getText().toLowerCase().trim();

        List<Cours> filtres = coursAvecProgression.stream()
                .filter(cours -> {
                    boolean matchRecherche = recherche.isEmpty() ||
                            cours.getTitre().toLowerCase().contains(recherche) ||
                            cours.getDescription().toLowerCase().contains(recherche);

                    boolean matchStatut = true;
                    double progression = progressionService.getProgressionCours(candidatId, cours.getId());

                    if (filtreActuel.equals("EN_COURS")) {
                        matchStatut = progression < 99.9;
                    } else if (filtreActuel.equals("TERMINES")) {
                        matchStatut = progression >= 99.9;
                    }

                    return matchRecherche && matchStatut;
                })
                .collect(Collectors.toList());

        afficherCours(filtres);
    }

    private void ouvrirCours(Cours cours) {
        AppNavController.getInstance().etudeOpenCours(cours);
    }


}