package main;

import entities.Certification;
import entities.Cours;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.CertificationService;
import services.CoursService;
import services.ProgressionCoursService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.layout.Priority;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursCandidatController {

    @FXML private Label lblTotalCours;
    @FXML private Label lblTotalCoursStat;
    @FXML private Label lblEnCours;
    @FXML private Label lblCompletes;
    @FXML private TextField txtRecherche;
    @FXML private TilePane tileCours;
    @FXML private ComboBox<String> comboDomaine;
    @FXML private ComboBox<String> comboNiveau;

    private CoursService coursServices;
    private List<Cours> tousLesCours;
    private int candidatId = 1; // À remplacer par l'ID du candidat connecté

    @FXML
    public void initialize() {
        coursServices = new CoursService();

        // Initialiser les ComboBox avec styles
        comboDomaine.getItems().addAll("Tous", "Développement", "Design", "Data Science", "Marketing");
        comboNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert", "Master");

        comboDomaine.setValue("Tous");
        comboNiveau.setValue("Tous");

        // Écouteurs pour les filtres
        comboDomaine.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());

        loadCoursFromDatabase();
        chargerStatistiques();

        // Recherche dynamique
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
    }

    private void loadCoursFromDatabase() {
        try {
            tousLesCours = coursServices.readAll();
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            lblTotalCoursStat.setText(String.valueOf(tousLesCours.size()));
            displayCours(tousLesCours);
        } catch (SQLException e) {
            e.printStackTrace();
            lblTotalCours.setText("0 cours disponibles");
            lblTotalCoursStat.setText("0");
        }
    }

    private void chargerStatistiques() {
        ProgressionCoursService progressionService = new ProgressionCoursService();
        try {
            int enCours = 0;
            int completes = 0;

            for (Cours c : tousLesCours) {
                double prog = progressionService.getProgressionCours(candidatId, c.getId());
                if (prog >= 99.9) {
                    completes++;
                } else if (prog > 0) {
                    enCours++;
                }
            }

            lblEnCours.setText(String.valueOf(enCours));
            lblCompletes.setText(String.valueOf(completes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayCours(List<Cours> list) {
        tileCours.getChildren().clear();
        for (Cours c : list) {
            tileCours.getChildren().add(createCoursCard(c));
        }
    }

    private VBox createCoursCard(Cours cours) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setSpacing(0);
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setEffect(new DropShadow(10, Color.rgb(94, 84, 142, 0.15)));

        // --- IMAGE DE COUVERTURE ---
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #231942, #5E548E);");

        ImageView imageView;
        if (cours.getImageCouverture() != null && cours.getImageCouverture().length > 0) {
            imageView = new ImageView(new Image(new ByteArrayInputStream(cours.getImageCouverture())));
        } else {
            // Image par défaut avec icône
            Label defaultIcon = new Label("📚");
            defaultIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white; -fx-opacity: 0.5;");
            imageContainer.getChildren().add(defaultIcon);
            imageView = new ImageView();
        }

        if (imageView.getImage() != null) {
            imageView.setFitWidth(350);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Rectangle clip = new Rectangle(350, 160);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);
            imageContainer.getChildren().add(imageView);
        }

        card.getChildren().add(imageContainer);

        // --- CONTENU DE LA CARTE ---
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 20 20;");

        // Badges
        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label lblNiveau = new Label(cours.getNiveau());
        lblNiveau.getStyleClass().addAll("level-badge", getLevelStyle(cours.getNiveau()));

        Label lblCategorie = new Label(cours.getCompetences_visees() != null ?
                cours.getCompetences_visees().split(",")[0] : "Général");
        lblCategorie.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #5E548E; -fx-padding: 4 12; -fx-background-radius: 50px; -fx-font-size: 12px; -fx-font-weight: 600;");

        badges.getChildren().addAll(lblNiveau, lblCategorie);

        // Titre
        Label lblTitre = new Label(cours.getTitre());
        lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #231942;");
        lblTitre.setWrapText(true);

        // Description
        Label lblDesc = new Label(cours.getDescription());
        lblDesc.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(60);

        // Métadonnées
        HBox metaBox = new HBox(20);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        HBox durationBox = new HBox(5);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        Label clockIcon = new Label("⏱️");
        clockIcon.setStyle("-fx-font-size: 14px;");
        Label lblDuration = new Label(cours.getDuree() + " heures");
        lblDuration.setStyle("-fx-text-fill: #5E548E; -fx-font-size: 13px; -fx-font-weight: 600;");
        durationBox.getChildren().addAll(clockIcon, lblDuration);

        metaBox.getChildren().add(durationBox);

        if (cours.isEst_obligatoire()) {
            HBox obligBox = new HBox(5);
            obligBox.setAlignment(Pos.CENTER_LEFT);
            Label obligIcon = new Label("📌");
            obligIcon.setStyle("-fx-font-size: 14px;");
            Label lblOblig = new Label("Obligatoire");
            lblOblig.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px; -fx-font-weight: 600;");
            obligBox.getChildren().addAll(obligIcon, lblOblig);
            metaBox.getChildren().add(obligBox);
        }

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnCommencer = new Button("Commencer");
        btnCommencer.setStyle(
                "-fx-background-color: linear-gradient(to right, #231942, #5E548E);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 50px;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        btnCommencer.setOnAction(e -> ouvrirCours(cours));
        btnCommencer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCommencer, Priority.ALWAYS);

        Button btnPDF = new Button("PDF");
        btnPDF.setStyle(
                "-fx-background-color: #10b981;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 10 15;" +
                        "-fx-background-radius: 50px;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        btnPDF.setOnAction(event -> genererCertifTest(cours));

        buttonBox.getChildren().addAll(btnCommencer, btnPDF);

        content.getChildren().addAll(badges, lblTitre, lblDesc, metaBox, buttonBox);
        card.getChildren().add(content);

        // Effet hover
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

    // Au lieu d'ouvrir un nouveau stage
    private void ouvrirCours(Cours cours) {
        CandidatShellController.getInstance().openCours(cours);
    }

    private void filtrerCours() {
        String recherche = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase() : "";
        String domaine = comboDomaine.getValue();
        String niveau = comboNiveau.getValue();

        List<Cours> filtered = tousLesCours.stream()
                .filter(c -> {
                    boolean matchesSearch = recherche.isEmpty() ||
                            (c.getTitre() != null && c.getTitre().toLowerCase().contains(recherche)) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));

                    boolean matchesDomaine = "Tous".equals(domaine) ||
                            (c.getCompetences_visees() != null &&
                                    c.getCompetences_visees().toLowerCase().contains(domaine.toLowerCase()));

                    boolean matchesNiveau = "Tous".equals(niveau) ||
                            (c.getNiveau() != null &&
                                    c.getNiveau().toLowerCase().contains(niveau.toLowerCase()));

                    return matchesSearch && matchesDomaine && matchesNiveau;
                })
                .collect(Collectors.toList());

        displayCours(filtered);
        lblTotalCours.setText(filtered.size() + " cours disponibles");
    }

    private void genererCertifTest(Cours cours) {
        if (cours == null) {
            showAlert("⚠️ Attention", "Cours invalide !");
            return;
        }

        ProgressionCoursService progressionService = new ProgressionCoursService();

        try {
            boolean estComplete = progressionService.estCoursComplete(candidatId, cours.getId());

            if (!estComplete) {
                showAlert("Cours non terminé",
                        "Vous devez compléter ce cours à 100% pour générer le certificat.\n" +
                                "Progression actuelle: " + progressionService.getProgressionCours(candidatId, cours.getId()) + "%");
                return;
            }

            String nomCandidat = "Bilal Eter"; // À remplacer par le vrai nom
            String cheminFichier = "C:/Users/MSI/Desktop/certificats/certificat_" +
                    cours.getTitre().replace(" ", "_") + ".pdf";

            CertificationService pdfService = new CertificationService();
            pdfService.genererCertification(nomCandidat, cours.getTitre(), cheminFichier);

            showAlert("✅ Succès", "Certificat généré : " + cheminFichier);

            Certification certif = new Certification(
                    candidatId,
                    cours.getId(),
                    java.time.LocalDateTime.now()
            );
            pdfService.ajouter(certif);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la génération du certificat: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherCours() {
        filtrerCours();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}