package main;

import entities.Certification;
import entities.Cours;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.CertificationService;
import services.CoursService;
import services.EmailService;
import services.ProgressionCoursService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.layout.Priority;
import utils.AlertUtils;
import javafx.stage.DirectoryChooser;
import java.io.File;
import services.TraductionService;
import main.LangueTest;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursCandidatController {
    @FXML private ComboBox<String> comboLangueTest;
    private TraductionService traductionService = new TraductionService();
    private String langueTest = "fr";
    private Map<Integer, Cours> coursTraduits = new HashMap<>();
    @FXML private Label lblTotalCours;
    @FXML private Label lblTotalCoursStat;
    @FXML private Label lblEnCours;
    @FXML private Label lblCompletes;
    @FXML private TextField txtRecherche;
    @FXML private TilePane tileCours;

    @FXML private ComboBox<String> comboNiveau;

    private CoursService coursServices;
    private List<Cours> tousLesCours;
    private int candidatId = 1;

    @FXML
    public void initialize() {
        coursServices = new CoursService();


        comboNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert", "Master");


        comboNiveau.setValue("Tous");

        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        setupLangueSelector();
        loadCoursFromDatabase();
        chargerStatistiques();

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
            AlertUtils.showError("❌ Erreur", "Impossible de charger les cours depuis la base de données.");
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
            AlertUtils.showError("❌ Erreur", "Impossible de charger les statistiques de progression.");
        }
    }

    private void displayCours(List<Cours> list) {
        tileCours.getChildren().clear();
        for (Cours c : list) {
            Cours coursTraduit = traduireCours(c);
            tileCours.getChildren().add(createCoursCard(coursTraduit));
        }
    }

    private VBox createCoursCard(Cours cours) {
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setSpacing(0);
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setEffect(new DropShadow(10, Color.rgb(94, 84, 142, 0.15)));

        // --- IMAGE DE COUVERTURE AVEC COINS ARRONDIS ---
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

                // Configuration de l'image
                imageView.setFitWidth(350);
                imageView.setFitHeight(160);
                imageView.setPreserveRatio(false);
                imageView.setSmooth(true);

                // ✅ AJOUTER LE CLIP POUR LES COINS ARRONDIS
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

        // --- RESTE DU CODE INCHANGÉ ---
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("card-content");

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

        Label lblDesc = new Label(cours.getDescription());
        lblDesc.getStyleClass().add("course-description-label");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(60);

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

        if (cours.isEst_obligatoire()) {
            HBox obligBox = new HBox(5);
            obligBox.setAlignment(Pos.CENTER_LEFT);
            Label obligIcon = new Label("📌");
            obligIcon.setStyle("-fx-font-size: 14px;");
            Label lblOblig = new Label("Obligatoire");
            lblOblig.getStyleClass().add("obligatoire-label");
            obligBox.getChildren().addAll(obligIcon, lblOblig);
            metaBox.getChildren().add(obligBox);
        }

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnCommencer = new Button("Commencer");
        btnCommencer.getStyleClass().add("btn-commencer");
        btnCommencer.setOnAction(e -> ouvrirCours(cours));
        btnCommencer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCommencer, Priority.ALWAYS);

        buttonBox.getChildren().addAll(btnCommencer); // PLUS DE BOUTON PDF

        content.getChildren().addAll(badges, lblTitre, lblDesc, metaBox, buttonBox);
        card.getChildren().add(content);

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

    private void ouvrirCours(Cours cours) {
        CandidatShellController.getInstance().openCours(cours);
    }

    private void filtrerCours() {
        String recherche = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase() : "";
        String niveau = comboNiveau.getValue();

        List<Cours> filtered = tousLesCours.stream()
                .filter(c -> {
                    boolean matchesSearch = recherche.isEmpty() ||
                            (c.getTitre() != null && c.getTitre().toLowerCase().contains(recherche)) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));


                    boolean matchesNiveau = "Tous".equals(niveau) ||
                            (c.getNiveau() != null &&
                                    c.getNiveau().toLowerCase().contains(niveau.toLowerCase()));

                    return matchesSearch &&  matchesNiveau;
                })
                .collect(Collectors.toList());

        displayCours(filtered);
        lblTotalCours.setText(filtered.size() + " cours disponibles");
    }

    // ============================================
    // GÉNÉRER CERTIFICAT - MODIFIÉ
    // ============================================

    /**
     * Génère un certificat professionnel sur une seule page
     */
    /**
     * Génère un certificat professionnel sur une seule page avec le logo Carrieri
     */
    /**
     * Génère un certificat professionnel sur une seule page avec le logo Carrieri (image)
     */

    /**
     * Génère un PDF de certificat professionnel avec le logo Carrieri
     */

    @FXML
    private void rechercherCours() {
        filtrerCours();
    }

    private void setupLangueSelector() {
        // Initialiser le ComboBox
        comboLangueTest.setItems(FXCollections.observableArrayList(
                "🇫🇷 Français (fr)",
                "🇬🇧 English (en)",
                "🇪🇸 Español (es)",
                "🇩🇪 Deutsch (de)",
                "🇮🇹 Italiano (it)",
                "🇵🇹 Português (pt)",
                "🇳🇱 Nederlands (nl)",
                "🇷🇺 Русский (ru)",
                "🇨🇳 中文 (zh-Hans)",
                "🇯🇵 日本語 (ja)",
                "🇰🇷 한국어 (ko)",
                "🇸🇦 العربية (ar)",
                "🇮🇳 हिन्दी (hi)"
        ));

        // Récupérer la langue depuis LangueTest
        langueTest = LangueTest.getInstance().getLangue();

        // Sélectionner la bonne valeur
        for (String item : comboLangueTest.getItems()) {
            if (item.contains("(" + langueTest + ")")) {
                comboLangueTest.setValue(item);
                break;
            }
        }

        comboLangueTest.setOnAction(e -> changerLangue());
    }
    @FXML
    private void changerLangue() {
        String selection = comboLangueTest.getValue();
        if (selection != null && selection.contains("(")) {
            langueTest = selection.substring(
                    selection.indexOf("(") + 1,
                    selection.indexOf(")")
            );

            LangueTest.getInstance().setLangue(langueTest);
            rechargerCoursAvecTraduction();

            AlertUtils.showInfo("🌐 Langue changée",
                    "Les cours sont maintenant en " + selection.split(" ")[0]);
        }
    }
    private void rechargerCoursAvecTraduction() {
        try {
            tousLesCours = coursServices.readAll();
            coursTraduits.clear();
            displayCours(tousLesCours);
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            System.out.println(traductionService.getStatistiques());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private Cours traduireCours(Cours cours) {
        if (coursTraduits.containsKey(cours.getId())) {
            return coursTraduits.get(cours.getId());
        }

        Cours coursTraduit = new Cours(
                traductionService.traduire(cours.getTitre(), langueTest),
                traductionService.traduire(cours.getDescription(), langueTest),
                cours.getDuree(),
                cours.getNiveau(),
                traductionService.traduire(cours.getCompetences_visees(), langueTest),
                cours.isEst_obligatoire(),
                cours.getCreatedBy(),
                cours.getImageCouverture()
        );
        coursTraduit.setId(cours.getId());

        coursTraduits.put(cours.getId(), coursTraduit);
        return coursTraduit;
    }
}