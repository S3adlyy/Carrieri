package main;

import entities.Cours;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.CoursServices;
import java.sql.SQLException;
import java.util.List;

public class CoursCandidatController {

    @FXML private Label lblTotalCours;
    @FXML private Label lblTotalCoursStat;
    @FXML private Label lblEnCours;
    @FXML private Label lblCompletes;
    @FXML private TextField txtRecherche;
    @FXML private TilePane tileCours;
    @FXML private ComboBox<String> comboDomaine;
    @FXML private ComboBox<String> comboNiveau;

    private CoursServices coursServices;
    private List<Cours> tousLesCours;

    @FXML
    public void initialize() {
        coursServices = new CoursServices();

        // Initialiser les ComboBox
        comboDomaine.getItems().addAll("Tous", "Développement Web", "Design", "Data Science", "Marketing");
        comboNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert", "Master");

        comboDomaine.setValue("Tous");
        comboNiveau.setValue("Tous");

        // Écouteurs pour les filtres
        comboDomaine.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> filtrerCours());

        loadCoursFromDatabase();

        // Recherche dynamique
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrerCours());
    }

    private void loadCoursFromDatabase() {
        try {
            tousLesCours = coursServices.read();
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            lblTotalCoursStat.setText(String.valueOf(tousLesCours.size()));
            displayCours(tousLesCours);
        } catch (SQLException e) {
            e.printStackTrace();
            lblTotalCours.setText("0 cours disponibles");
            lblTotalCoursStat.setText("0");
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
        card.setSpacing(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(340);
        card.setMaxWidth(340);

        // Header avec niveau
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        // Badge de niveau avec TOUS les niveaux
        Label lblNiveau = new Label(cours.getNiveau());
        lblNiveau.getStyleClass().add("level-badge");

        String niveau = cours.getNiveau().toLowerCase();
        if (niveau.contains("débutant") || niveau.contains("debutant")) {
            lblNiveau.getStyleClass().add("level-debutant");
        } else if (niveau.contains("intermédiaire") || niveau.contains("intermediaire")) {
            lblNiveau.getStyleClass().add("level-intermediaire");
        } else if (niveau.contains("avancé") || niveau.contains("avance")) {
            lblNiveau.getStyleClass().add("level-avance");
        } else if (niveau.contains("expert")) {
            lblNiveau.getStyleClass().add("level-expert");
        } else if (niveau.contains("master")) {
            lblNiveau.getStyleClass().add("level-master");
        } else {
            lblNiveau.getStyleClass().add("level-debutant");
        }

        // Domaine
        Label lblDomaine = new Label(cours.getCompetences_visees() != null ?
                cours.getCompetences_visees().split(",")[0] : "Général");
        lblDomaine.getStyleClass().add("course-domain");

        header.getChildren().addAll(lblNiveau, lblDomaine);
        HBox.setMargin(lblDomaine, new Insets(0, 0, 0, 10));

        // Titre
        Label lblTitre = new Label(cours.getTitre());
        lblTitre.getStyleClass().add("course-title");
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(300);

        // Description
        Label lblDesc = new Label(cours.getDescription());
        lblDesc.getStyleClass().add("course-description");
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(300);
        lblDesc.setMaxHeight(60);

        // Métadonnées
        HBox metaBox = new HBox(15);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        // Durée
        HBox durationBox = new HBox(5);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        Label lblDuration = new Label("⏱️ " + cours.getDuree() + "h");
        lblDuration.getStyleClass().add("course-meta");
        durationBox.getChildren().add(lblDuration);

        // Obligatoire
        if (cours.isEst_obligatoire()) {
            HBox obligBox = new HBox(5);
            obligBox.setAlignment(Pos.CENTER_LEFT);
            Label lblOblig = new Label("📌 Obligatoire");
            lblOblig.getStyleClass().add("course-obligatoire");
            obligBox.getChildren().add(lblOblig);
            metaBox.getChildren().add(obligBox);
        }

        metaBox.getChildren().add(durationBox);

        // Bouton
        Button btn = new Button("Commencer le cours");
        btn.getStyleClass().add("course-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);

        // Assemblage
        card.getChildren().addAll(header, lblTitre, lblDesc, metaBox, btn);
        return card;
    }

    private void filtrerCours() {
        String recherche = txtRecherche.getText().toLowerCase();
        String domaine = comboDomaine.getValue();
        String niveau = comboNiveau.getValue();

        if ((recherche == null || recherche.isEmpty()) &&
                "Tous".equals(domaine) && "Tous".equals(niveau)) {
            displayCours(tousLesCours);
            lblTotalCours.setText(tousLesCours.size() + " cours disponibles");
            return;
        }

        List<Cours> filtered = tousLesCours.stream()
                .filter(c -> (recherche.isEmpty() ||
                        c.getTitre().toLowerCase().contains(recherche) ||
                        c.getDescription().toLowerCase().contains(recherche)))
                .filter(c -> "Tous".equals(domaine) ||
                        (c.getCompetences_visees() != null &&
                                c.getCompetences_visees().toLowerCase().contains(domaine.toLowerCase())))
                .filter(c -> "Tous".equals(niveau) ||
                        c.getNiveau().equalsIgnoreCase(niveau))
                .toList();

        displayCours(filtered);
        lblTotalCours.setText(filtered.size() + " cours disponibles");
    }

    @FXML
    private void rechercherCours() {
        filtrerCours();
    }
}