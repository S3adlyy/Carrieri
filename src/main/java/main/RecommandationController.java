package main;

import entities.Cours;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.CoursService;
import services.ProgressionCoursService;
import services.SystemeRecommandationService;
import utils.AlertUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecommandationController {

    @FXML private Label lblDetails;
    @FXML private TextArea txtDetailsCandidat;
    @FXML private Label lblStats;
    @FXML private ListView<String> listCoursSuivis;
    @FXML private VBox recommandationsContainer;
    @FXML private ProgressBar progressionBar;
    @FXML private Label lblProgression;
    @FXML private Label lblNiveauEstime; // NOUVEAU
    @FXML private ComboBox<String> filtreNiveau; // NOUVEAU - pour filtrer les recommandations

    private SystemeRecommandationService recommandationService;
    private CoursService coursService;
    private ProgressionCoursService progressionService;
    private int candidatId;
    private List<Cours> toutesLesRecommandations; // Pour garder les recommandations originales
    private String niveauActuel;

    @FXML
    public void initialize() {
        recommandationService = new SystemeRecommandationService();
        coursService = new CoursService();
        progressionService = new ProgressionCoursService();

        // Initialiser le filtre
        if (filtreNiveau != null) {
            filtreNiveau.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé", "Expert");
            filtreNiveau.setValue("Tous");
            filtreNiveau.setOnAction(e -> filtrerRecommandations());
        }

        System.out.println("✅ RecommandationController initialisé");
    }

    public void setCandidatId(int id) {
        this.candidatId = id;
        System.out.println("✅ ID candidat reçu: " + id);
        chargerRecommandationsPourCandidat();
    }

    private void chargerRecommandationsPourCandidat() {
        // Afficher un indicateur de chargement
        afficherChargement();

        // Exécuter en arrière-plan pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                List<Cours> tousLesCours = coursService.getAll();
                String niveau = determinerNiveauCandidat(candidatId, tousLesCours);
                this.niveauActuel = niveau;

                List<Integer> idsCoursSuivis = getCoursSuivisParCandidat(candidatId, tousLesCours);

                List<Cours> recommandations = recommandationService.getRecommandationsPourCandidat(
                        candidatId, niveau, idsCoursSuivis
                );

                // Mettre à jour l'UI sur le thread JavaFX
                Platform.runLater(() -> {
                    this.toutesLesRecommandations = recommandations;

                    afficherInfosCandidat(niveau, idsCoursSuivis.size(), tousLesCours.size());
                    afficherCoursSuivis(idsCoursSuivis, tousLesCours);
                    afficherProgressionGlobale(idsCoursSuivis.size(), tousLesCours.size());
                    afficherRecommandations(recommandations);

                    // Afficher le niveau estimé
                    if (lblNiveauEstime != null) {
                        lblNiveauEstime.setText("🎯 Niveau estimé: " + niveau);
                    }
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        AlertUtils.showError("❌ Erreur", "Impossible de charger les cours: " + e.getMessage())
                );
            }
        }).start();
    }

    private void afficherChargement() {
        recommandationsContainer.getChildren().clear();
        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(300);

        Label loadingIcon = new Label("⏳");
        loadingIcon.setStyle("-fx-font-size: 40px;");

        Label loadingText = new Label("Analyse de votre profil...");
        loadingText.setStyle("-fx-font-size: 16px; -fx-text-fill: #9ca3af;");

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(50, 50);

        loadingBox.getChildren().addAll(loadingIcon, loadingText, indicator);
        recommandationsContainer.getChildren().add(loadingBox);
    }

    private void afficherRecommandations(List<Cours> recommandations) {
        if (recommandationsContainer == null) return;

        recommandationsContainer.getChildren().clear();

        if (recommandations.isEmpty()) {
            VBox emptyBox = new VBox(15);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPrefHeight(300);
            emptyBox.getStyleClass().add("empty-state");

            Label emptyIcon = new Label("🎯");
            emptyIcon.getStyleClass().add("empty-state-icon");

            Label emptyText = new Label("Aucune recommandation pour le moment");
            emptyText.getStyleClass().add("empty-state-title");

            Label emptySubtext = new Label("Suivez des cours pour obtenir des recommandations personnalisées");
            emptySubtext.getStyleClass().add("empty-state-subtitle");

            emptyBox.getChildren().addAll(emptyIcon, emptyText, emptySubtext);
            recommandationsContainer.getChildren().add(emptyBox);
            return;
        }

        for (int i = 0; i < recommandations.size(); i++) {
            Cours cours = recommandations.get(i);
            recommandationsContainer.getChildren().add(creerCarteRecommandation(cours, i + 1));
        }
    }

    private VBox creerCarteRecommandation(Cours cours, int index) {
        VBox card = new VBox(12);
        card.getStyleClass().add("recommandation-card");

        // Ajouter un ID pour retrouver la carte facilement
        card.setId("card_" + cours.getId());

        // En-tête avec numéro et titre
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label numLabel = new Label(String.format("%02d", index));
        numLabel.getStyleClass().add("card-number");

        VBox titleBox = new VBox(5);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label titreLabel = new Label(cours.getTitre());
        titreLabel.getStyleClass().add("card-title");
        titreLabel.setWrapText(true);

        Label niveauLabel = new Label(cours.getNiveau());
        niveauLabel.getStyleClass().addAll("level-badge", getLevelStyle(cours.getNiveau()));

        titleBox.getChildren().addAll(titreLabel, niveauLabel);
        header.getChildren().addAll(numLabel, titleBox);

        // Description
        Label description = new Label(cours.getDescription());
        description.getStyleClass().add("card-description");
        description.setWrapText(true);
        description.setMaxHeight(40);

        // Tags de compétences
        FlowPane competencesPane = new FlowPane(5, 5);
        if (cours.getCompetences_visees() != null) {
            String[] competences = cours.getCompetences_visees().split(",");
            for (String comp : competences) {
                Label compTag = new Label(comp.trim());
                compTag.getStyleClass().add("competence-tag");
                competencesPane.getChildren().add(compTag);
            }
        }

        // Métadonnées et bouton
        HBox metaBox = new HBox(15);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        HBox dureeBox = new HBox(5);
        dureeBox.setAlignment(Pos.CENTER_LEFT);
        dureeBox.getStyleClass().add("duration-box");

        Label clockIcon = new Label("⏱️");
        clockIcon.getStyleClass().add("duration-icon");

        Label dureeLabel = new Label(cours.getDuree() + "h");
        dureeLabel.getStyleClass().add("duration-text");

        dureeBox.getChildren().addAll(clockIcon, dureeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button voirButton = new Button("Voir le cours");
        voirButton.getStyleClass().add("btn-recommandation-voir");
        voirButton.setOnAction(e -> ouvrirCours(cours));

        metaBox.getChildren().addAll(dureeBox, spacer, voirButton);

        card.getChildren().addAll(header, description, competencesPane, metaBox);

        return card;
    }

    // ✅ NOUVEAU : Filtrer les recommandations par niveau
    private void filtrerRecommandations() {
        if (toutesLesRecommandations == null || filtreNiveau == null) return;

        String niveauFiltre = filtreNiveau.getValue();

        if ("Tous".equals(niveauFiltre)) {
            afficherRecommandations(toutesLesRecommandations);
            return;
        }

        List<Cours> filtrees = new ArrayList<>();
        for (Cours cours : toutesLesRecommandations) {
            if (cours.getNiveau().equalsIgnoreCase(niveauFiltre)) {
                filtrees.add(cours);
            }
        }

        afficherRecommandations(filtrees);

        AlertUtils.showInfo("🔍 Filtre appliqué",
                filtrees.size() + " cours de niveau " + niveauFiltre + " trouvés");
    }

    private String getLevelStyle(String niveau) {
        if (niveau == null) return "level-intermediaire";

        switch (niveau.toLowerCase()) {
            case "débutant":
                return "level-debutant";
            case "intermédiaire":
                return "level-intermediaire";
            case "avancé":
                return "level-avance";
            case "expert":
                return "level-expert";
            default:
                return "level-intermediaire";
        }
    }

    // ✅ AMÉLIORÉ : Déterminer le niveau basé sur les cours suivis
    private String determinerNiveauCandidat(int id, List<Cours> tousLesCours) {
        List<Integer> idsCoursSuivis = getCoursSuivisParCandidat(id, tousLesCours);

        if (idsCoursSuivis.isEmpty()) {
            return "Débutant";
        }

        int scoreNiveau = 0;
        int compteur = 0;

        for (Integer coursId : idsCoursSuivis) {
            for (Cours cours : tousLesCours) {
                if (cours.getId() == coursId) {
                    switch (cours.getNiveau().toLowerCase()) {
                        case "débutant": scoreNiveau += 1; break;
                        case "intermédiaire": scoreNiveau += 2; break;
                        case "avancé": scoreNiveau += 3; break;
                        case "expert": scoreNiveau += 4; break;
                    }
                    compteur++;
                    break;
                }
            }
        }

        int moyenne = scoreNiveau / compteur;

        if (moyenne <= 1) return "Débutant";
        if (moyenne <= 2) return "Intermédiaire";
        if (moyenne <= 3) return "Avancé";
        return "Expert";
    }

    private List<Integer> getCoursSuivisParCandidat(int id, List<Cours> tousLesCours) {
        List<Integer> coursSuivis = new ArrayList<>();

        try {
            for (Cours cours : tousLesCours) {
                double progression = progressionService.getProgressionCours(id, cours.getId());
                if (progression > 0) {
                    coursSuivis.add(cours.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return coursSuivis;
    }

    private void afficherInfosCandidat(String niveau, int nbCoursSuivis, int totalCours) {
        txtDetailsCandidat.setText(
                "🆔 Candidat n°" + candidatId + "\n" +
                        "📊 Niveau estimé: " + niveau + "\n" +
                        "📚 Cours suivis: " + nbCoursSuivis + "/" + totalCours
        );

        lblDetails.setText("Candidat #" + candidatId);
    }

    private void afficherCoursSuivis(List<Integer> idsCoursSuivis, List<Cours> tousLesCours) {
        listCoursSuivis.getItems().clear();

        if (idsCoursSuivis.isEmpty()) {
            listCoursSuivis.getItems().add("📭 Aucun cours suivi pour le moment");
            return;
        }

        for (Integer coursId : idsCoursSuivis) {
            for (Cours cours : tousLesCours) {
                if (cours.getId() == coursId) {
                    listCoursSuivis.getItems().add("✅ " + cours.getTitre());
                    break;
                }
            }
        }
    }

    private void afficherProgressionGlobale(int coursSuivis, int totalCours) {
        if (progressionBar != null) {
            double progression = totalCours > 0 ? (double) coursSuivis / totalCours : 0;
            progressionBar.setProgress(progression);
        }
        if (lblProgression != null) {
            int pourcentage = totalCours > 0 ? (coursSuivis * 100) / totalCours : 0;
            lblProgression.setText(pourcentage + "% du catalogue exploré");
        }

        int recommandationsDispo = Math.max(0, totalCours - coursSuivis);
        lblStats.setText("📊 " + coursSuivis + " cours suivis, " +
                recommandationsDispo + " recommandations disponibles");
    }

    private void ouvrirCours(Cours cours) {
        // Utilisation d'AlertUtils avec le design de l'application
        boolean confirmé = AlertUtils.showConfirmation(
                "📚 Ouvrir le cours",
                "Voulez-vous ouvrir ce cours ?\n\n" +
                        "🎯 " + cours.getTitre() + "\n" +
                        "📊 Niveau: " + cours.getNiveau() + "\n" +
                        "⏱️ Durée: " + cours.getDuree() + " heures",
                "Oui, ouvrir",
                "Non, annuler"
        );

        if (confirmé) {
            CandidatShellController.getInstance().openCours(cours);
        }
    }



}