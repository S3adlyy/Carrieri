package main;

import entities.Certification;
import entities.Cours;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.CertificationService;
import services.CoursService;
import utils.AlertUtils;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CertificatsCandidatController {

    @FXML private Label lblTotalCertificats;
    @FXML private Label lblTotalStats;
    @FXML private Label lblCetteAnnee;
    @FXML private Label lblCeMois;
    @FXML private TextField txtRecherche;
    @FXML private TilePane tileCertificats;
    @FXML private VBox boxEmpty;

    private CertificationService certificationService = new CertificationService();
    private CoursService coursService = new CoursService();
    private List<CertificatInfo> tousLesCertificats = new ArrayList<>();
    private int candidatId = 1;

    @FXML
    public void initialize() {
        chargerCertificats();
        setupRecherche();
    }

    private void chargerCertificats() {
        try {
            System.out.println("\n🔍 CHARGEMENT DES CERTIFICATS POUR CANDIDAT ID: " + candidatId);

            List<Certification> certifications = certificationService.readByCandidat(candidatId);
            System.out.println("📊 Certifications trouvées en base: " + certifications.size());

            tousLesCertificats.clear();
            for (Certification cert : certifications) {
                System.out.println("\n--- Certification ID: " + cert.getId() + " ---");

                Cours cours = coursService.getById(cert.getCoursId());
                if (cours != null) {
                    System.out.println("  Cours: " + cours.getTitre());

                    // Récupérer le chemin depuis la base
                    String cheminPDF = certificationService.getCheminFichier(cert.getId());
                    System.out.println("  Chemin en base: " + cheminPDF);

                    // Vérifier si le PDF est en base
                    boolean aPDFEnBase = certificationService.aPDFEnBase(cert.getId());
                    System.out.println("  PDF en base: " + aPDFEnBase);

                    tousLesCertificats.add(new CertificatInfo(
                            cert.getId(),
                            cours.getTitre(),
                            cours.getDescription(),
                            cert.getDateObtention(),
                            cheminPDF,
                            aPDFEnBase,
                            cours.getImageCouverture()
                    ));
                }
            }

            afficherCertificats(tousLesCertificats);
            mettreAJourStatistiques();

            boolean vide = tousLesCertificats.isEmpty();
            tileCertificats.setVisible(!vide);
            tileCertificats.setManaged(!vide);
            boxEmpty.setVisible(vide);
            boxEmpty.setManaged(vide);

            lblTotalCertificats.setText(tousLesCertificats.size() + " certificat" +
                    (tousLesCertificats.size() > 1 ? "s" : "") + " obtenu" +
                    (tousLesCertificats.size() > 1 ? "s" : ""));

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger les certificats.");
        }
    }

    private void afficherCertificats(List<CertificatInfo> certificats) {
        tileCertificats.getChildren().clear();
        for (CertificatInfo cert : certificats) {
            tileCertificats.getChildren().add(createCertificatCard(cert));
        }
    }

    private VBox createCertificatCard(CertificatInfo cert) {
        VBox card = new VBox();
        card.getStyleClass().add("certificat-card");
        card.setSpacing(0);
        card.setPrefWidth(350);
        card.setMaxWidth(350);

        // --- EN-TÊTE DU CERTIFICAT ---
        StackPane headerContainer = new StackPane();
        headerContainer.setPrefHeight(120);
        headerContainer.getStyleClass().add("header-container");

        VBox headerContent = new VBox(5);
        headerContent.setAlignment(Pos.CENTER);

        Label iconCertif = new Label("🎓");
        iconCertif.getStyleClass().add("header-icon");

        Label lblCertificat = new Label("CERTIFICAT");
        lblCertificat.getStyleClass().add("header-title");

        Label lblDateCertif = new Label(formatDate(cert.dateObtention));
        lblDateCertif.getStyleClass().add("header-date");

        headerContent.getChildren().addAll(iconCertif, lblCertificat, lblDateCertif);
        headerContainer.getChildren().add(headerContent);
        card.getChildren().add(headerContainer);

        // --- CONTENU PRINCIPAL ---
        VBox content = new VBox(15);
        content.setPadding(new Insets(25, 20, 20, 20));
        content.getStyleClass().add("card-content");

        // Badge "OBTENU"
        HBox badgeBox = new HBox(10);
        badgeBox.setAlignment(Pos.CENTER);

        Label lblObtenu = new Label("🏆 OBTENU AVEC SUCCÈS");
        lblObtenu.getStyleClass().add("badge-obtenu");
        badgeBox.getChildren().add(lblObtenu);

        // Titre du cours
        Label lblTitre = new Label(cert.titreCours);
        lblTitre.getStyleClass().add("course-title");
        lblTitre.setWrapText(true);
        lblTitre.setAlignment(Pos.CENTER);
        lblTitre.setMaxWidth(300);
        HBox titreBox = new HBox(lblTitre);
        titreBox.setAlignment(Pos.CENTER);

        // Description
        Label lblDesc = new Label();
        if (cert.description != null && !cert.description.isEmpty()) {
            String desc = cert.description.length() > 80 ? cert.description.substring(0, 80) + "..." : cert.description;
            lblDesc.setText(desc);
        } else {
            lblDesc.setText("Certificat de réussite");
        }
        lblDesc.getStyleClass().add("course-description");
        lblDesc.setWrapText(true);
        lblDesc.setAlignment(Pos.CENTER);
        lblDesc.setMaxWidth(300);
        HBox descBox = new HBox(lblDesc);
        descBox.setAlignment(Pos.CENTER);

        // Séparateur
        Separator separator = new Separator();
        separator.getStyleClass().add("separator");
        separator.setMaxWidth(200);

        // Informations (N° et Année)
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER);

        VBox idBox = new VBox(2);
        idBox.setAlignment(Pos.CENTER);
        Label idLabel = new Label("N°");
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        Label idValue = new Label("CERT-" + cert.id);
        idValue.getStyleClass().add("cert-number");
        idBox.getChildren().addAll(idLabel, idValue);

        VBox anneeBox = new VBox(2);
        anneeBox.setAlignment(Pos.CENTER);
        Label anneeLabel = new Label("Année");
        anneeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        Label anneeValue = new Label(String.valueOf(cert.dateObtention.getYear()));
        anneeValue.getStyleClass().add("cert-year");
        anneeBox.getChildren().addAll(anneeLabel, anneeValue);

        infoBox.getChildren().addAll(idBox, anneeBox);

        // --- BOUTONS ---
        VBox actionsBox = new VBox(10);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setFillWidth(true);

        // BOUTON DE TÉLÉCHARGEMENT
        Button btnDownload = new Button();
        btnDownload.setMaxWidth(Double.MAX_VALUE);
        btnDownload.setPrefHeight(45);
        btnDownload.setMinHeight(45);

        // Vérifier si le PDF est disponible
        boolean pdfDisponible = false;

        if (cert.cheminPDF != null && new File(cert.cheminPDF).exists()) {
            pdfDisponible = true;
        } else if (cert.aPDFEnBase) {
            pdfDisponible = true;
        }

        if (pdfDisponible) {
            btnDownload.setText("⬇️ TÉLÉCHARGER LE PDF");
            btnDownload.getStyleClass().add("btn-download");
            btnDownload.setOnAction(e -> telechargerCertificat(cert));
        } else {
            btnDownload.setText("❌ NON TÉLÉCHARGEABLE");
            btnDownload.getStyleClass().add("btn-download");
            btnDownload.getStyleClass().add("btn-download-disabled");
            btnDownload.setDisable(true);
        }

        // BOUTON PARTAGER
        Button btnShare = new Button("📤 Partager");
        btnShare.setMaxWidth(Double.MAX_VALUE);
        btnShare.setPrefHeight(35);
        btnShare.getStyleClass().add("btn-share");

        if (pdfDisponible) {
            btnShare.setOnAction(e -> partagerCertificat(cert));
        } else {
            btnShare.setDisable(true);
            btnShare.getStyleClass().add("btn-share-disabled");
        }

        actionsBox.getChildren().addAll(btnDownload, btnShare);

        // Pied de page
        HBox footerBox = new HBox();
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblFooter = new Label("Carrieri · Certificat officiel");
        lblFooter.getStyleClass().add("footer");
        footerBox.getChildren().add(lblFooter);

        // Assemblage
        content.getChildren().addAll(
                badgeBox,
                titreBox,
                descBox,
                separator,
                infoBox,
                actionsBox,
                footerBox
        );

        card.getChildren().add(content);

        // Effet de survol
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

    private void telechargerCertificat(CertificatInfo cert) {
        try {
            // 1. Essayer avec le fichier sur disque
            if (cert.cheminPDF != null && new File(cert.cheminPDF).exists()) {
                java.awt.Desktop.getDesktop().open(new File(cert.cheminPDF));
                return;
            }

            // 2. Sinon, récupérer depuis la base
            if (cert.aPDFEnBase) {
                byte[] pdfBytes = certificationService.getPDFBytes(cert.id);

                if (pdfBytes != null && pdfBytes.length > 0) {
                    // Créer un dossier temporaire
                    String dossierTemp = System.getProperty("java.io.tmpdir") + "CarrieriCertificats/";
                    File dossier = new File(dossierTemp);
                    if (!dossier.exists()) {
                        dossier.mkdirs();
                    }

                    // Créer un fichier temporaire
                    String nomFichier = "certificat_" + cert.id + "_" +
                            System.currentTimeMillis() + ".pdf";
                    File fichierTemp = new File(dossier, nomFichier);

                    // Écrire les bytes dans le fichier
                    Files.write(fichierTemp.toPath(), pdfBytes);

                    // Ouvrir le fichier
                    java.awt.Desktop.getDesktop().open(fichierTemp);

                    AlertUtils.showInfo("✅ Téléchargement réussi",
                            "Le certificat a été ouvert temporairement.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de télécharger le certificat.");
        }
    }

    private void partagerCertificat(CertificatInfo cert) {
        try {
            String sujet = "Mon certificat - " + cert.titreCours;
            String corps = "Je viens d'obtenir mon certificat pour le cours : " + cert.titreCours;

            String sujetEncode = URLEncoder.encode(sujet, StandardCharsets.UTF_8.toString());
            String corpsEncode = URLEncoder.encode(corps, StandardCharsets.UTF_8.toString());

            String mailto = "mailto:?subject=" + sujetEncode + "&body=" + corpsEncode;
            java.awt.Desktop.getDesktop().mail(new java.net.URI(mailto));

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le client email.");
        }
    }

    private void mettreAJourStatistiques() {
        int total = tousLesCertificats.size();
        int cetteAnnee = 0;
        int ceMois = 0;

        int anneeActuelle = java.time.Year.now().getValue();
        int moisActuel = java.time.LocalDate.now().getMonthValue();

        for (CertificatInfo cert : tousLesCertificats) {
            if (cert.dateObtention.getYear() == anneeActuelle) {
                cetteAnnee++;
                if (cert.dateObtention.getMonthValue() == moisActuel) {
                    ceMois++;
                }
            }
        }

        lblTotalStats.setText(String.valueOf(total));
        lblCetteAnnee.setText(String.valueOf(cetteAnnee));
        lblCeMois.setText(String.valueOf(ceMois));
    }

    private String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void setupRecherche() {
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrerCertificats());
    }

    @FXML
    private void rechercherCertificats() {
        filtrerCertificats();
    }

    private void filtrerCertificats() {
        String recherche = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase() : "";

        List<CertificatInfo> filtres = tousLesCertificats.stream()
                .filter(c -> recherche.isEmpty() ||
                        c.titreCours.toLowerCase().contains(recherche))
                .collect(Collectors.toList());

        afficherCertificats(filtres);
    }

    @FXML
    private void allerAuCatalogue() {
        CandidatShellController.getInstance().showCatalogue();
    }

    // ✅ BOUTON DE DÉBOGAGE
    @FXML
    private void debugDossier() {
        String[] dossiers = {
                "C:/Users/MSI/Desktop/certif/",
                "C:/Users/MSI/Desktop/certificats/",
                "C:/Users/MSI/Desktop/"
        };

        System.out.println("\n📁 CONTENU DES DOSSIERS:");
        for (String dossier : dossiers) {
            File dir = new File(dossier);
            if (dir.exists() && dir.isDirectory()) {
                System.out.println("\nDossier: " + dossier);
                File[] fichiers = dir.listFiles((d, name) -> name.startsWith("Carrieri_") && name.endsWith(".pdf"));
                if (fichiers != null && fichiers.length > 0) {
                    for (File f : fichiers) {
                        System.out.println("  📄 " + f.getName());
                    }
                } else {
                    System.out.println("  ⚠️ Aucun fichier Carrieri trouvé");
                }
            } else {
                System.out.println("❌ Dossier inexistant: " + dossier);
            }
        }
    }

    // Classe interne pour stocker les infos du certificat
    private static class CertificatInfo {
        int id;
        String titreCours;
        String description;
        LocalDateTime dateObtention;
        String cheminPDF;
        boolean aPDFEnBase;
        byte[] image;

        CertificatInfo(int id, String titreCours, String description,
                       LocalDateTime dateObtention, String cheminPDF,
                       boolean aPDFEnBase, byte[] image) {
            this.id = id;
            this.titreCours = titreCours;
            this.description = description;
            this.dateObtention = dateObtention;
            this.cheminPDF = cheminPDF;
            this.aPDFEnBase = aPDFEnBase;
            this.image = image;
        }
    }
}