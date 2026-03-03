package com.example.guser.controllers.getude;

import entities.getude.Cours;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import services.getude.CoursService;
import services.getude.PaiementService;
import services.getude.ProgressionCoursService;
import services.getude.TraductionService;
import session.SessionContext;
import utils.getude.AlertUtils;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    int me = SessionContext.getCurrentUser().getId();
    private int candidatId = me;

    // ✅ NOUVEAU : Service de paiement
    private PaiementService paiementService = new PaiementService();
    private List<Integer> coursAchetes = new ArrayList<>();

    @FXML
    public void initialize() {
        coursServices = new CoursService();

        comboNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert", "Master");

        comboNiveau.setValue("Tous");

        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        setupLangueSelector();
        loadCoursFromDatabase();
        chargerStatistiques();

        // ✅ NOUVEAU : Charger les achats
        chargerAchats();

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
    }

    // ✅ NOUVELLE MÉTHODE : Charger les achats du candidat
    private void chargerAchats() {
        try {
            coursAchetes = paiementService.getCoursAchetes(candidatId);
            System.out.println("✅ Cours achetés chargés: " + coursAchetes.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        // --- CONTENU DE LA CARTE ---
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

        // ✅ AFFICHER LE PRIX SI LE COURS EST PAYANT
        boolean dejaAchete = coursAchetes.contains(cours.getId());

        if (cours.getPrix() > 0) {
            HBox prixBox = new HBox(5);
            prixBox.setAlignment(Pos.CENTER_LEFT);

            // Changer le nom de la variable et le texte
            Label deviseIcon = new Label(dejaAchete ? "✅" : "🇹🇳"); // Ou "💰" si vous préférez
            deviseIcon.setStyle("-fx-font-size: 14px;");

            String textePrix = dejaAchete ? "Acheté" : String.format("%.2f TND", cours.getPrix()); // ← MODIFIÉ
            Label lblPrix = new Label(textePrix);
            lblPrix.setStyle(dejaAchete ?
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;" :
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #5E548E;");

            prixBox.getChildren().addAll(deviseIcon, lblPrix);
            metaBox.getChildren().add(prixBox);
        }

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

        // ✅ BOUTON DYNAMIQUE (Acheter ou Commencer)
        // ✅ VERSION CORRECTE
        Button btnAction;
        if (cours.getPrix() > 0 && !dejaAchete) {
            btnAction = new Button("Acheter " + String.format("%.2f TND", cours.getPrix())); // ← MODIFIÉ
            btnAction.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 20; -fx-cursor: hand;");
            btnAction.setOnAction(e -> acheterCours(cours));
        } else if (dejaAchete) {
            btnAction = new Button("Accéder au cours");
            btnAction.getStyleClass().add("btn-commencer");
            btnAction.setOnAction(e -> ouvrirCours(cours));   // ✅ Pour les cours déjà achetés
        } else {
            btnAction = new Button("Commencer");
            btnAction.getStyleClass().add("btn-commencer");
            btnAction.setOnAction(e -> ouvrirCours(cours));   // ✅ Pour les cours gratuits
        }

        btnAction.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAction, Priority.ALWAYS);

        buttonBox.getChildren().addAll(btnAction);

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

    // ✅ NOUVELLE MÉTHODE POUR ACHETER UN COURS
    private void acheterCours(Cours cours) {
        try {
            boolean confirm = AlertUtils.showConfirmation(
                    "💰 Achat du cours",
                    "Vous allez acheter le cours :\n\n" +
                            "📚 " + cours.getTitre() + "\n" +
                            "💰 Prix: " + String.format("%.2f TND", cours.getPrix()) + "\n\n" + // ← MODIFIÉ
                            "Voulez-vous continuer ?",
                    "Oui, acheter",
                    "Non, annuler"
            );

            if (!confirm) return;

            // Charger la vue paiement
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/Paiement.fxml"));
            Node paiementView = loader.load();

            PaiementController controller = loader.getController();
            var nav = com.example.guser.controllers.guser.AppNavController.getInstance();
            if (nav == null) {
                AlertUtils.showError("❌ Erreur", "Navigation shell not ready.");
                return;
            }
            controller.setCours(cours, candidatId, nav);
            nav.showView(paiementView);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Erreur lors de l'achat : " + e.getMessage());
        }
    }
    public void rafraichirAchats() {
        try {
            coursAchetes = paiementService.getCoursAchetes(candidatId);
            System.out.println("✅ Achats rechargés: " + coursAchetes.size());
            displayCours(tousLesCours);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        var nav = com.example.guser.controllers.guser.AppNavController.getInstance();
        if (nav == null) {
            AlertUtils.showError("❌ Erreur", "Navigation shell not ready.");
            return;
        }
        nav.etudeOpenCours(cours);
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
        // ✅ AJOUTER CES LIGNES
        coursTraduit.setPrix(cours.getPrix());
        coursTraduit.setEstPayant(cours.isEstPayant());

        // Pour déboguer
        System.out.println("   🔄 Traduction de " + cours.getTitre() +
                " | Prix original: " + cours.getPrix() +
                " | Prix copié: " + coursTraduit.getPrix());
        coursTraduits.put(cours.getId(), coursTraduit);
        return coursTraduit;
    }

}